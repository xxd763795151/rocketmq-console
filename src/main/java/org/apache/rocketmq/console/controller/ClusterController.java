/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.rocketmq.console.controller;

import java.util.Iterator;
import java.util.Map;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.service.ClusterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/cluster")
public class ClusterController {

    @Resource
    private ClusterService clusterService;

    @Autowired
    private RMQConfigure configure;

    @RequestMapping(value = "/list.query", method = RequestMethod.GET)
    @ResponseBody
    public Object list() {
        return clusterService.list();
    }

    @RequestMapping(value = "/list/exclude.query", method = RequestMethod.GET)
    @ResponseBody
    public Object listExcludeSpecificNode() {
        Map<String, Object> list = clusterService.list();

        if (configure.getTraceNode().isSpecific()) {
            Map<String, Object> brokerServer = (Map<String, Object>)list.get("brokerServer");
            Iterator<Map.Entry<String, Object>> entryIterator = brokerServer.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<String, Object> entry = entryIterator.next();
                if (entry.getKey().startsWith(configure.getTraceNode().getPrefix())) {
                    entryIterator.remove();
                }
            }
        }

        return list;
    }


    @RequestMapping(value = "/brokerConfig.query", method = RequestMethod.GET)
    @ResponseBody
    public Object brokerConfig(@RequestParam String brokerAddr) {
        return clusterService.getBrokerConfig(brokerAddr);
    }
}
