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
package cn.edu.nuaa.software.websocket.server;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import cn.edu.nuaa.software.websocket.server.netty.WsServer;
import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: WsServerApplication
 * Description:
 * Contact: nuaameteor@qq.com
 * author: Niezhi
 * version: V1.0
 */
@SpringBootApplication
@Slf4j
@EnableAsync
public class WsServerApplication implements CommandLineRunner, DisposableBean {

    private WsServer wsServer;

    public static void main(String[] args) {
        SpringApplication.run(WsServerApplication.class, args);
    }

    @Override
    @Async
    public void run(String... args) throws Exception {
        int port = Integer.valueOf(args[0]);
        wsServer = new WsServer(port);
        wsServer.start();
    }

    @Override
    public void destroy() throws Exception {
        wsServer.shutdown();
    }
}
