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
package cn.edu.nuaa.software.websocket.client.task;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;

import cn.edu.nuaa.software.websocket.client.WsClientApplication;
import cn.edu.nuaa.software.websocket.client.common.WsUtil;
import cn.edu.nuaa.software.websocket.client.netty.Receiver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: Task
 * Description:
 * Contact: nuaameteor@qq.com
 * author: Niezhi
 * version: V1.0
 */
@Slf4j
@AllArgsConstructor
public class ConnectionTask implements Runnable {

    private String         namespace;
    private int            taskNo;
    private int            connectionCount;
    private CountDownLatch latch;

    @Override
    public void run() {
        ConcurrentLinkedDeque<Receiver> defaultValue = new ConcurrentLinkedDeque<>();
        WsClientApplication.CLIENT_MAP.putIfAbsent(namespace, defaultValue);
        Queue<Receiver> receivers = WsClientApplication.CLIENT_MAP.get(namespace);
        for (int i = 0; i < connectionCount; i++) {
            String s = WsUtil.makeName(namespace, taskNo, i);
            String name = Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
            String   url      = WsClientApplication.url + "?token=" + name;
            Receiver receiver = new Receiver(namespace, s, url);
            receivers.offer(receiver);
            receiver.connect();
        }
        latch.countDown();
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public int getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(int taskNo) {
        this.taskNo = taskNo;
    }

    public int getConnectionCount() {
        return connectionCount;
    }

    public void setConnectionCount(int connectionCount) {
        this.connectionCount = connectionCount;
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }
}
