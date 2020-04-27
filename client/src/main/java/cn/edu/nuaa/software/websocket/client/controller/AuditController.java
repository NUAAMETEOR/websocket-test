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

import com.alibaba.fastjson.JSON;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import cn.edu.nuaa.software.websocket.client.WsClientApplication;
import cn.edu.nuaa.software.websocket.client.common.WsUtil;
import cn.edu.nuaa.software.websocket.client.dto.AuditPojo;
import cn.edu.nuaa.software.websocket.client.dto.ReportDto;
import cn.edu.nuaa.software.websocket.client.dto.WsClientTestParameteor;
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
@RequestMapping("audit")
public class AuditController {

    @Autowired
    private WsClientTestParameteor clientTestParameteor;

    @RequestMapping("/conn/{namespace}")
    public long conn(@PathVariable("namespace") String namespace) {
        return WsClientApplication.CONNECTION_COUNT_MAP.getOrDefault(namespace, new AtomicInteger(0)).get();
    }

    @RequestMapping("/send/{namespace}")
    public long send(@PathVariable("namespace") String namespace) {
        WsClientApplication.AUDIT_MAP.putIfAbsent(namespace, new AuditPojo());
        return WsClientApplication.AUDIT_MAP.get(namespace).getSendCount().get();
    }

    @RequestMapping("/recv/{namespace}")
    public long receive(@PathVariable("namespace") String namespace) {
        WsClientApplication.AUDIT_MAP.putIfAbsent(namespace, new AuditPojo());
        return WsClientApplication.AUDIT_MAP.get(namespace).getReceiveCount().get();
    }

    @PostMapping("/reportTask")
    public String reportTask(@RequestBody List<ReportDto> reportDtoList) {
        if (reportDtoList != null && !reportDtoList.isEmpty()) {
            reportDtoList.forEach(v -> {
                if (v != null) {
                    WsClientApplication.REPORTS_MAP.put(v.getNamespace(), v);
                    log.debug("receive task report from {} -> {}", v.getIp(), v.getFrom());
                }
            });
            return "OK";
        }
        return "empty report";
    }

    @Scheduled(cron = "0/15 * * * * ?")
    public void reportTaskToMaster() {
        if (StringUtils.hasText(clientTestParameteor.getMaster())) {
            try {
                RestTemplate    restTemplate = new RestTemplate();
                HttpHeaders     headers      = new HttpHeaders();
                List<ReportDto> reportDtos   = taskList();
                log.info("report task to master <{}>", reportDtos);
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<List<ReportDto>> request        = new HttpEntity<>(reportDtos, headers);
                String                      masterUrl      = "http://" + clientTestParameteor.getMaster() + "/audit/reportTask";
                ResponseEntity<String>      responseEntity = restTemplate.postForEntity(masterUrl, request, String.class);
                if (responseEntity.getStatusCode() == HttpStatus.OK) {
                    log.debug("report task to master OK");
                } else {
                    log.error("report task to master failed");
                }
            } catch (RestClientException e) {
                e.printStackTrace();
            }
        }
    }

    @Scheduled(cron = "0/3 * * * * ?")
    @RequestMapping("/taskList")
    public List<ReportDto> taskList() {
        List<ReportDto> list = new ArrayList();
        if (!WsClientApplication.AUDIT_MAP.isEmpty()) {
            WsClientApplication.AUDIT_MAP.forEach((k, v) -> {
                WsClientApplication.AUDIT_MAP.putIfAbsent(k, new AuditPojo());
                ReportDto reportDto = new ReportDto();
                reportDto.setNamespace(k);
                reportDto.setFrom(System.getProperty("user.name"));
                reportDto.setIp(WsUtil.getLocalIPs().get(0));
                reportDto.setConnections(WsClientApplication.CONNECTION_COUNT_MAP.getOrDefault(k, new AtomicInteger(0)).get());
                reportDto.setRecvCount(WsClientApplication.AUDIT_MAP.get(k).getReceiveCount().get());
                reportDto.setSendCount(WsClientApplication.AUDIT_MAP.get(k).getSendCount().get());
                reportDto.setDelta(reportDto.getSendCount() - reportDto.getRecvCount());
                list.add(reportDto);
            });
        }
        String arg = JSON.toJSONString(list);
        log.info("report task:[{}]", arg);
        return list;
    }

    @Scheduled(cron = "15 * * * * ?")
    @RequestMapping("masterReport")
    public String masterReport() {
        if (!WsClientApplication.REPORTS_MAP.isEmpty()) {
            ReportDto              statistics = new ReportDto();
            Map<String, ReportDto> map        = new HashMap();
            WsClientApplication.REPORTS_MAP.forEach((k, v) -> {
                ReportDto target = new ReportDto();
                BeanUtils.copyProperties(v, target);
                map.put(k, target);
            });
            map.values().stream().forEach(v -> {
                statistics.setSendCount(statistics.getSendCount() + v.getSendCount());
                statistics.setRecvCount(statistics.getRecvCount() + v.getRecvCount());
                statistics.setConnections(statistics.getConnections() + v.getConnections());
            });
            statistics.setDelta(statistics.getSendCount() - statistics.getRecvCount());
            LinkedHashMap<String, ReportDto> result = new LinkedHashMap();
            result.put("statistics", statistics);
            result.putAll(map);
            String s = JSON.toJSONString(result);
            log.info("master report task [{}]", s);
            return s;
        }
        return "no report";
    }
}
