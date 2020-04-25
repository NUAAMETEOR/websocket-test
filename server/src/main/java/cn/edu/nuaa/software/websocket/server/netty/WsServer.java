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

import java.net.InetSocketAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.ImmediateEventExecutor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: WsServer
 * Description:
 * Contact: nuaameteor@qq.com
 * author: Niezhi
 * version: V1.0
 */
@Slf4j
@Data
public class WsServer {

    private final int port;
    DefaultChannelGroup channelGroup   = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
    NioEventLoopGroup   eventLoopGroup = new NioEventLoopGroup();
    private Channel channel;

    public WsServer(int port) {
        this.port = port;
    }

    public void start() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(eventLoopGroup)
                 .channel(NioServerSocketChannel.class)
                 .childHandler(new WsChannelInitializer(channelGroup));
        ChannelFuture bind = bootstrap.bind(new InetSocketAddress(port));
        bind.syncUninterruptibly();
        channel = bind.channel();
        log.info("netty start on " + port);
        channel.closeFuture().syncUninterruptibly();
    }

    public void shutdown() {
        log.info("netty shutdown");
        if (channel != null) {
            channel.close();
        }
        channelGroup.close();
        eventLoopGroup.shutdownGracefully();
        log.info("netty shutdown complete");
    }
}
