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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var module = app;

module.controller('backupMessageController', ['$scope', 'ngDialog', '$http','Notification',function ($scope, ngDialog, $http,Notification) {
    $scope.allTopicList = [];
    $scope.selectedTopic =[];
    $scope.key ="";
    $scope.messageId ="";
    $scope.queryMessageByTopicResult=[];
    $scope.queryMessageByTopicAndKeyResult=[];
    $scope.queryMessageByMessageIdResult={};
    $scope.disableSearch = false;
    $http({
        method: "GET",
        url: "login/user"
    }).success(function (resp) {
        if (resp.status == 0) {
            $scope.show = resp.role == 1;
        }else{
            Notification.error({message: resp.errMsg, delay: 2000});
        }
    });
    $http({
        method: "GET",
        url: "topic/list.query",
        params: {
            skipSysProcess: 'true'
        }
    }).success(function (resp) {
        if(resp.status ==0){
            var topicList = [];
            $.each(resp.data.topicList, function (i, e) {
                topicList.push(e.topic);
            });
            $scope.allTopicList = topicList.sort(function (a, b) {
                if ((a.startsWith('%') && b.startsWith("%")) || (!a.startsWith('%') && !b.startsWith("%"))) {
                    return a.localeCompare(b);
                } else if (a.startsWith('%') && !b.startsWith("%")){
                    return 1;
                } else {
                    return -1;
                }
            });
        }else {
            Notification.error({message: resp.errMsg, delay: 2000});
        }
    });
    $scope.timepickerBegin = moment().subtract(1, 'hour').format('YYYY-MM-DD HH:mm');
    $scope.timepickerEnd = moment().add(1,'hour').format('YYYY-MM-DD HH:mm');
    $scope.timepickerOptions ={format: 'YYYY-MM-DD HH:mm', showClear: true};
    $scope.messageTotal = 0;

    $scope.paginationConf = {
        currentPage: 1,
        totalItems: 0,
        itemsPerPage: 20,
        pagesLength: 15,
        perPageOptions: [10],
        rememberPerPage: 'perPageItems',
        onChange: function () {
            $scope.changeShowMessageList(this.currentPage,this.totalItems);
        }
    };


    $scope.queryMessageByTopic = function () {
        if ($scope.timepickerEnd < $scope.timepickerBegin) {
            Notification.error({message: "endTime is later than beginTime!", delay: 2000});
            return
        }

        $scope.disableSearch = true;
        $http({
            method: "GET",
            url: "backupMessage/queryMessageByTopic.query",
            params: {
                topic: $scope.selectedTopic.startsWith('%SYS') ? $scope.selectedTopic.substr(5) : $scope.selectedTopic,
                begin: $scope.timepickerBegin.valueOf(),
                end: $scope.timepickerEnd.valueOf()

            }
        }).success(function (resp) {
            $scope.disableSearch = false;
            if (resp.status == 0) {
                $scope.queryMessageByTopicResult = resp.data;
                $scope.changeShowMessageList(1,$scope.queryMessageByTopicResult.length);
            }else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
        $http({
            method: "GET",
            url: "backupMessage/queryMessageTotalByTopic.query",
            params: {
                topic: $scope.selectedTopic.startsWith('%SYS') ? $scope.selectedTopic.substr(5) : $scope.selectedTopic,
                begin: $scope.timepickerBegin.valueOf(),
                end: $scope.timepickerEnd.valueOf()
            }
        }).success(function (resp) {
            if (resp.status == 0) {
                $scope.messageTotal = resp.data.total;
            }else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    };

    $scope.queryMessageByTopicAndKey = function () {
        $http({
            method: "GET",
            url: "backupMessage/queryMessageByTopicAndKey.query",
            params: {
                topic: $scope.selectedTopic,
                key:$scope.key
            }
        }).success(function (resp) {
            if (resp.status == 0) {
                console.log(resp);
                $scope.queryMessageByTopicAndKeyResult = resp.data;
                console.log($scope.queryMessageByTopicAndKeyResult);
            }else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    };

    $scope.queryMessageByBrokerAndOffset = function (storeHost,commitLogOffset) {
        $http({
            method: "GET",
            url: "message/viewMessageByBrokerAndOffset.query",
            params: {
                brokerHost: storeHost.address,
                port:storeHost.port,
                offset: commitLogOffset
            }
        }).success(function (resp) {
            if (resp.status == 0) {
                ngDialog.open({
                    template: 'messageDetailViewDialog',
                    controller: 'messageDetailViewDialogController',
                    data: $.extend(resp.data, {admin: resp.role == 1})
                });
            } else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    };

    $scope.queryMessageByMessageId = function (messageId,topic) {
        $http({
            method: "GET",
            url: "backupMessage/viewMessage.query",
            params: {
                msgId: messageId,
                topic:topic
            }
        }).success(function (resp) {
            if (resp.status == 0) {
                console.log(resp);
                ngDialog.open({
                    template: 'messageDetailViewDialog',
                    controller: 'messageDetailViewDialogController',
                    data: $.extend(resp.data, {admin: resp.role == 1})
                });
            }else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    };


    $scope.changeShowMessageList = function (currentPage,totalItem) {
        var perPage = $scope.paginationConf.itemsPerPage;
        var from = (currentPage - 1) * perPage;
        var to = (from + perPage)>totalItem?totalItem:from + perPage;
        $scope.messageShowList = $scope.queryMessageByTopicResult.slice(from, to);
        $scope.paginationConf.totalItems = totalItem ;
    };
}]);

module.controller('messageDetailViewDialogController',['$scope', 'ngDialog', '$http','Notification', function ($scope, ngDialog, $http,Notification) {

        $scope.resendMessage = function (msgId,topic,consumerGroup) {
            $http({
                method: "POST",
                url: "message/consumeMessageDirectly.do",
                params: {
                    msgId: msgId,
                    consumerGroup:consumerGroup,
                    topic:topic
                }
            }).success(function (resp) {
                if (resp.status == 0) {
                    ngDialog.open({
                        template: 'operationResultDialog',
                        data:{
                            result:resp.data
                        }
                    });
                }
                else {
                    ngDialog.open({
                        template: 'operationResultDialog',
                        data:{
                            result:resp.errMsg
                        }
                    });
                }
            });
        };
        $scope.showExceptionDesc = function (errmsg) {
            if(errmsg == null){
                errmsg = "Don't have Exception"
            }
            ngDialog.open({
                template: 'operationResultDialog',
                data:{
                    result:errmsg
                }
            });
        };
    }]
);