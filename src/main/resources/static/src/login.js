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

app.controller('loginController', ['$scope', 'ngDialog', '$location', '$http', 'Notification', '$cookies', '$window', function ($scope, ngDialog, $location, $http, Notification, $cookies, $window) {
    $scope.login = function () {
        if (!$("#username").val()) {
            alert("用户名不能为空");
            return;
        }
        if (!$("#password").val()) {
            alert("密码不能为空");
            return;
        }

        $http({
            method: "POST",
            url: "login/login.do",
            params: {username: $("#username").val(), password: $("#password").val()}
        }).success(function (resp) {
            if (resp.status == 0) {
                Notification.info({message: 'Login successful, redirect now', delay: 2000});
                $window.sessionStorage.setItem("username", $("#username").val());
                //$window.sessionStorage.setItem("sessionId", resp.data.sessionId);
                window.location = "/";
                initFlag = false;
            } else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    };

    $scope.registerAccount = function () {
        $http({
            method: "GET",
            url: "login/info/all.query"
        }).success(function (resp) {
            if (resp.status == 0) {
                ngDialog.open({
                    // preCloseCallback: function (value) {
                    //     // Refresh topic list
                    //     $scope.refreshTopicList();
                    // },
                    template: 'registerAccountDialog',
                    controller: 'registerAccountDialogController',
                    data: {
                        email: '',
                        itemList: resp.data.item
                    }
                });
            } else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });

    }

    $scope.forgetPassword = function () {
        ngDialog.open({
            template: 'forgetPasswordDialog',
            controller: 'forgetPasswordDialogController',
            data: {
                email: ''
            }
        });
    }
}]);

module.controller('registerAccountDialogController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {

        $scope.registerAccountRequest = function (request) {
            if (request.email == undefined || request.email.trim().length == 0) {
                Notification.warning({message: '邮箱不能为空', delay: 3000, positionX: 'center'});
                return false;
            }
            if (!request.item) {
                Notification.warning({
                    message: '请选择自己所属项目组，如果找不到自己项目组，请联系基础架构组同事',
                    delay: 3000,
                    positionX: 'center'
                });
                return false;
            }
            $http({
                method: "POST",
                url: "login/register.do",
                data: request
            }).success(function (resp) {
                if (resp.status == 0) {
                    Notification.info({
                        message: "注册成功，账号及密码将会通过邮件发送，若未收到邮件请确认邮箱是否正确或联系管理员处理!",
                        delay: 10000,
                        startTop: 50,
                        positionX: 'center'
                    });
                    ngDialog.closeThisDialog('Cancel');
                } else {
                    Notification.error({message: resp.errMsg, positionX: 'center'});
                }
            });
        }
    }]
);

module.controller('forgetPasswordDialogController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {

        $scope.forgetPasswordRequest = function (request) {
            if (request.email == undefined || request.email.trim().length == 0) {
                Notification.warning({message: '邮箱不能为空', delay: 3000, positionX: 'center'});
                return false;
            }
            $http({
                method: "POST",
                url: "login/forget.do",
                data: request
            }).success(function (resp) {
                if (resp.status == 0) {
                    Notification.info({
                        message: "账号及密码将会通过邮件发送，若未收到邮件请确认邮箱是否正确或联系管理员处理!",
                        delay: 10000,
                        startTop: 50,
                        positionX: 'center'
                    });
                    ngDialog.closeThisDialog('Cancel');
                } else {
                    Notification.error({message: resp.errMsg, positionX: 'center'});
                }
            });
        }
    }]
);