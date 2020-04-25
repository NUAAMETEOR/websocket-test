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
package cn.edu.nuaa.software.websocket.client.controller;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cn.edu.nuaa.software.websocket.client.WsClientApplication;
import cn.edu.nuaa.software.websocket.client.common.WsUtil;
import cn.edu.nuaa.software.websocket.client.dto.CheckResult;
import cn.edu.nuaa.software.websocket.client.dto.WsTestParameter;
import cn.edu.nuaa.software.websocket.client.netty.Receiver;
import cn.edu.nuaa.software.websocket.client.netty.SenderChannelHandler;
import cn.edu.nuaa.software.websocket.client.task.ConnectionTask;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: ProduerController
 * Description:
 * Contact: nuaameteor@qq.com
 * author: Niezhi
 * version: V1.0
 */
@Slf4j
@RestController
public class SenderController implements DisposableBean {

    ThreadPoolExecutor executor = new ThreadPoolExecutor(200, 200, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(5000), new ThreadFactory() {
        private AtomicLong index = new AtomicLong(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "netty-receiver-" + index.getAndIncrement());
        }
    });
    private ConcurrentMap<String, EventLoopGroup> currentTask = new ConcurrentHashMap<>();


    @RequestMapping("producer/{namespace}/{threadCount}/{taskCountPerThread}")
    public String sendMessage(@PathVariable("namespace") String namespace, @PathVariable("threadCount") int threadCount, @PathVariable("taskCountPerThread") int taskCountPerThread) {
        if (StringUtils.hasText(namespace) && threadCount > 0 && taskCountPerThread > 0) {
            if (WsClientApplication.STATUS_MAP.getOrDefault(namespace, false)) {
                return "task is running";
            }
            WsClientApplication.LOCK_MAP.putIfAbsent(namespace, new ReentrantLock());
            Lock lock = WsClientApplication.LOCK_MAP.get(namespace);
            try {
                if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                    CheckResult checkResult = prepare(namespace, threadCount, taskCountPerThread);
                    if (checkResult.isSucc()) {
                        start(namespace, threadCount, taskCountPerThread);
                        return "send OK";
                    } else {
                        return checkResult.getMessage();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
            return "task is running";
        } else {
            return "参数不正确";
        }
    }

    @RequestMapping("connect/{namespace}/{threadCount}/{taskCountPerThread}")
    private CheckResult prepare(@PathVariable("namespace") String namespace, @PathVariable("threadCount") int threadCount, @PathVariable("taskCountPerThread") int taskCountPerThread) {
        WsClientApplication.STATUS_MAP.putIfAbsent(namespace, Boolean.FALSE);
        if (WsClientApplication.STATUS_MAP.get(namespace)) {
            return new CheckResult(false, "task is running", 0);
        }
        WsTestParameter newPara = new WsTestParameter(namespace, threadCount, taskCountPerThread);
        WsTestParameter oldPara = WsClientApplication.TEST_PARAMETER_CONCURRENT_MAP.putIfAbsent(namespace, newPara);
        if (!newPara.equals(oldPara)) {
            WsClientApplication.TEST_PARAMETER_CONCURRENT_MAP.replace(namespace, newPara);
            stopExistTask(namespace);
            establishConnections(namespace, threadCount, taskCountPerThread);
        } else {
            if (threadCount * taskCountPerThread != WsClientApplication.CONNECTION_COUNT_MAP.getOrDefault(namespace, new AtomicInteger(0)).get()) {
                stopExistTask(namespace);
                establishConnections(namespace, threadCount, taskCountPerThread);
            }
        }
        return new CheckResult(true, "", WsClientApplication.CLIENT_MAP.get(namespace).size());
    }

    private void start(String namespace, int threadCount, int taskCountPerThread) {
        int            count     = threadCount * taskCountPerThread;
        CountDownLatch latch     = new CountDownLatch(count);
        CyclicBarrier  barrier   = new CyclicBarrier(count);
        String         host      = WsClientApplication.url.replace("ws://", "");
        Bootstrap      bootstrap = new Bootstrap();
        EventLoopGroup group     = new NioEventLoopGroup(count);
        currentTask.put(namespace, group);
        List<String>         list    = new ArrayList<>();
        SenderChannelHandler handler = new SenderChannelHandler(namespace, barrier, list);
        bootstrap.group(group)
                 .channel(NioSocketChannel.class)
                 .option(ChannelOption.SO_KEEPALIVE, true)
                 .option(ChannelOption.TCP_NODELAY, true)
                 .handler(new ChannelInitializer<NioSocketChannel>() {
                     @Override
                     protected void initChannel(NioSocketChannel ch) throws Exception {
                         ch.pipeline()
                           .addLast(new HttpClientCodec())
                           .addLast(new HttpContentDecompressor())
                           .addLast(new HttpObjectAggregator(8 * 1024 * 1024))
                           .addLast(handler);
                     }
                 });
        WsClientApplication.STATUS_MAP.put(namespace, true);
        for (int i = 0; i < threadCount; i++) {
            for (int j = 0; j < taskCountPerThread; j++) {
                String name = WsUtil.makeName(namespace, i, j);
                list.add(name);
                ChannelFuture sync = null;
                try {
                    sync = bootstrap.connect(host, 80).sync();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sync.channel().closeFuture().addListeners(v -> {
                    log.debug("channel {} close", name);
                    latch.countDown();
                });
            }
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
            currentTask.remove(namespace);
            WsClientApplication.STATUS_MAP.put(namespace, false);
        }
        log.info("done.current connection is {}", WsClientApplication.CONNECTION_COUNT_MAP.getOrDefault(namespace, new AtomicInteger(0)).get());
    }

    private void stopExistTask(String namespace) {
        Queue<Receiver> receivers = WsClientApplication.CLIENT_MAP.getOrDefault(namespace, null);
        if (receivers != null) {
            log.info("exists not matched connections,begin to stop them");
            receivers.stream().forEach(Receiver::close);
            log.info("exists not matched connections,finish stop them");
            receivers.clear();
        }
    }

    private void establishConnections(String namespace, int threadCount, int taskCountPerThread) {
        log.info("start to establish connections");
        try {
            CountDownLatch latch = new CountDownLatch(threadCount);
            for (int i = 0; i < threadCount; i++) {
                executor.execute(new ConnectionTask(namespace, i, taskCountPerThread, latch));
            }
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.info("establishConnections error,{}", e.getMessage());
            throw new RuntimeException("unable to establish connections");
        }
        log.info("end to establish {} connectinos", threadCount * taskCountPerThread);
    }

    @Override
    public void destroy() throws Exception {
        currentTask.forEach((k, v) -> {
            log.info("application shutdown,waiting for io");
            v.shutdownGracefully();
        });
    }
}
