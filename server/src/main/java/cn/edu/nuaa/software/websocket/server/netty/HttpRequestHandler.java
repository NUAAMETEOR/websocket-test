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
package cn.edu.nuaa.software.websocket.server.netty;

import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;
import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: HttpRequestHandler
 * Description:
 * Contact: nuaameteor@qq.com
 * author: Niezhi
 * version: V1.0
 */
@Slf4j
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final String index;
    private final        String wsUrl;

    static {
        URL location = HttpRequestHandler.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            String temp = location.toURI() + "static/index.html";
            index = temp.contains("file:") ? temp.substring(5) : temp;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("invalid location" + location);
        }
    }

    public HttpRequestHandler(String wsUrl) {
        this.wsUrl = wsUrl;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception {
        if (wsUrl.equalsIgnoreCase(fullHttpRequest.uri())) {
            log.info("receive websocket request");
            channelHandlerContext.fireChannelRead(fullHttpRequest.retain());
        } else {
            log.info("receive http request");
            if (HttpUtil.is100ContinueExpected(fullHttpRequest)) {
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
                channelHandlerContext.writeAndFlush(response);
            }
            RandomAccessFile file     = new RandomAccessFile(index, "r");
            HttpResponse     response = new DefaultHttpResponse(fullHttpRequest.protocolVersion(), HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");
            boolean isAlive = HttpUtil.isKeepAlive(fullHttpRequest);
            if (isAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.CONNECTION);
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.length());
            }
            channelHandlerContext.write(response);
            if (channelHandlerContext.pipeline().get(SslHandler.class) == null) {
                channelHandlerContext.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));
            } else {
                channelHandlerContext.write(new ChunkedNioFile(file.getChannel()));
            }
            ChannelFuture channelFuture = channelHandlerContext.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            if (!isAlive) {
                channelFuture.addListeners(ChannelFutureListener.CLOSE);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error("error:{}", cause.getMessage());
    }
}
