/**
 * Copyright 2009-2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.edu.nuaa.software.websocket.client.netty;

import com.alibaba.fastjson.JSON;

import org.springframework.http.MediaType;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: ClientHandler
 * Description:
 * Contact: nuaameteor@qq.com
 * author: Niezhi
 * version: V1.0
 */
@Slf4j
@ChannelHandler.Sharable
public class SenderChannelHandler extends ChannelInboundHandlerAdapter {
    private static final String        senderUrl;
    private final        CyclicBarrier barrier;
    private final        List<String>  list;
    private final        AtomicInteger index = new AtomicInteger(-1);

    static {
        String temp;
        try {
            temp = new URI("/vertws/produceMessage").toASCIIString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            temp = "/";
        }
        senderUrl = temp;
    }

    public SenderChannelHandler(CyclicBarrier barrier, List<String> list) {
        this.barrier = barrier;
        this.list = list;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("channel active ,waiting...");
        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
            ctx.pipeline().close();
        }
        ByteBuf         content = makeRequestBody();
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, senderUrl, content);
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
        ctx.writeAndFlush(request);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("channel now inactive");
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpResponse response = (FullHttpResponse) msg;
        ByteBuf          content  = response.content();
        log.debug("response content [{}]", content.toString(StandardCharsets.UTF_8));
        ReferenceCountUtil.release(msg);
        ctx.channel().close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        ctx.channel().close();
    }

    private ByteBuf makeRequestBody() {
        Map map = new HashMap();
        map.put("content", "test");
        map.put("receiveId", getName());
        return Unpooled.copiedBuffer(JSON.toJSONString(map).getBytes(StandardCharsets.UTF_8));
    }

    private String getName() {
        int    i = this.index.incrementAndGet();
        String s = list.get(i);
        log.debug("get name {}", s);
        return s;
    }
}
