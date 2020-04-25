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

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

import cn.edu.nuaa.software.websocket.client.WsClientApplication;
import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: CountController
 * Description:
 * Contact: nuaameteor@qq.com
 * author: Niezhi
 * version: V1.0
 */
@RestController
@Slf4j
@RequestMapping("count")
public class CountController {

    @RequestMapping("/{namespace}")
    public long get(@PathVariable("namespace") String namespace) {
        return WsClientApplication.COUNT_MAP.getOrDefault(namespace, new AtomicInteger(0)).get();
    }

}
