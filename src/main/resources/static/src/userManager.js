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

module.controller('userController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
    $scope.paginationConf = {
        currentPage: 1,
        totalItems: 0,
        itemsPerPage: 10,
        pagesLength: 15,
        perPageOptions: [10],
        rememberPerPage: 'perPageItems',
        onChange: function () {
            $scope.showUserList(this.currentPage, this.totalItems);
        }
    };

    $scope.selectedTopic = [];
    $scope.key = "";
    $scope.messageId = "";
    $scope.queryMessageByTopicAndKeyResult = [];
    $scope.queryMessageByMessageIdResult = {};
    $scope.queryMessageTraceListsByTopicAndKeyResult = [];
    $scope.userList = [];
    $scope.allUserList = [];
    $scope.roleList = [];
    $scope.allRoleList = [];
    $scope.allItemList = [];

    $scope.refreshUserList = function () {
        $http({
            method: "GET",
            url: "user/list.query",
            params: {
                skipSysProcess: "true"
            }
        }).success(function (resp) {
            if (resp.status == 0) {
                $scope.allUserList = resp.data.sort();
                $scope.showUserList(1, $scope.allUserList.length);
            } else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    }
    $scope.refreshUserList();

    $scope.refreshRoleList = function () {
        $http({
            method: "GET",
            url: "user/role/list.query",
            params: {
                skipSysProcess: "true"
            }
        }).success(function (resp) {
            if (resp.status == 0) {
                $scope.allRoleList = resp.data.sort();
            } else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    }
    $scope.refreshRoleList();

    $scope.refreshItemList = function () {
        $http({
            method: "GET",
            url: "user/item/list.query",
            params: {
                skipSysProcess: "true"
            }
        }).success(function (resp) {
            if (resp.status == 0) {
                $scope.allItemList = resp.data.sort();
            } else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    }
    $scope.refreshItemList();

    $scope.timepickerBegin = moment().subtract(1, 'hour').format('YYYY-MM-DD HH:mm');
    $scope.timepickerEnd = moment().add(1, 'hour').format('YYYY-MM-DD HH:mm');
    $scope.timepickerOptions = {format: 'YYYY-MM-DD HH:mm', showClear: true};

    $scope.openCreateDialog = function (request, sysFlag) {
        $http({
            method: "GET",
            url: "user/info/all.query"
        }).success(function (resp) {
            if (resp.status == 0) {
                ngDialog.open({
                    preCloseCallback: function (value) {
                        $scope.refreshUserList();
                    },
                    template: 'addUserDialog',
                    controller: 'addUserDialogController',
                    data: {
                        sysFlag: sysFlag,
                        roleList: resp.data.role,
                        itemList: resp.data.item,
                        password: $scope.randomPass(9, true, true, false, true, false)
                    }
                });
            } else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });

    }

    $scope.openAddDialog = function () {
        $scope.openCreateDialog(null, false);
    }

    $scope.openAddRoleDialog = function ()  {
        ngDialog.open({
            preCloseCallback: function (value) {
                $scope.refreshRoleList();
            },
            template: 'addRoleDialog',
            controller: 'addRoleDialogController',
            data: {}
        });
    }

    $scope.openAddItemDialog = function ()  {
        ngDialog.open({
            preCloseCallback: function (value) {
                $scope.refreshItemList();
            },
            template: 'addItemDialog',
            controller: 'addItemDialogController',
            data: {}
        });
    }

    // update user
    $scope.openUpdateUserDialog = function (user) {
        $http({
            method: "GET",
            url: "user/info/all.query"
        }).success(function (resp) {
            if (resp.status == 0) {
                user.notify = user.notify != 0;
                ngDialog.open({
                    preCloseCallback: function (value) {
                        $scope.refreshUserList();
                    },
                    template: 'updateUserDialog',
                    controller: 'updateUserDialogController',
                    data: {
                        user: user,
                        roleList: resp.data.role,
                        itemList: resp.data.item
                    }
                });
            } else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    }
    $scope.filterStr="";
    $scope.$watch('filterStr', function() {
        $scope.filterList(1);
    });

    $scope.filterList = function (currentPage) {
        // if ($scope.filterStr == '' || $scope.filterStr == undefined) {
        //     $scope.userList = $scope.allUserList;
        //     return ;
        // }
        var lowExceptStr = $scope.filterStr.toLowerCase();
        var canShowList = [];

        $scope.allUserList.forEach(function (element) {
            if (element.name.toLowerCase().indexOf(lowExceptStr) != -1) {
                canShowList.push(element);
            }
        });
        $scope.paginationConf.totalItems = canShowList.length;
        var perPage = $scope.paginationConf.itemsPerPage;
        var from = (currentPage - 1) * perPage;
        var to = (from + perPage) > canShowList.length ? canShowList.length : from + perPage;
        $scope.userList = canShowList.slice(from, to);
    };

    $scope.showUserList = function (currentPage, totalItem) {
        var perPage = $scope.paginationConf.itemsPerPage;
        var from = (currentPage - 1) * perPage;
        var to = (from + perPage) > totalItem ? totalItem : from + perPage;
        $scope.userList = $scope.allUserList.slice(from, to);
        $scope.paginationConf.totalItems = totalItem;
        $scope.filterList($scope.paginationConf.currentPage)
    };

    $scope.deleteUser = function (id) {
        $http({
            method: "DELETE",
            url: "user/delete/" + id
        }).success(function (resp) {
            if (resp.status == 0) {
                Notification.info({message: "success!", delay: 2000});
                $scope.refreshUserList();
            } else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    }

    $scope.openUpdateRoleDialog = function (role) {
        ngDialog.open({
            preCloseCallback: function (value) {
                $scope.refreshRoleList();
            },
            template: 'updateRoleDialog',
            controller: 'updateRoleDialogController',
            data: {
                role: role
            }
        });
    }

    $scope.openUpdateItemDialog = function (item) {
        ngDialog.open({
            preCloseCallback: function (value) {
                $scope.refreshRoleList();
            },
            template: 'updateItemDialog',
            controller: 'updateItemDialogController',
            data: {
                item: item
            }
        });
    }

    /**
     * 生成密码字符串
     * 33~47：!~/
     * 48~57：0~9
     * 58~64：:~@
     * 65~90：A~Z
     * 91~96：[~`
     * 97~122：a~z
     * 123~127：{~
     * @param length 长度
     * @param hasNum 是否包含数字 1-包含 0-不包含
     * @param hasChar 是否包含字母 1-包含 0-不包含
     * @param hasSymbol 是否包含其他符号 1-包含 0-不包含
     * @param caseSense 是否大小写敏感 1-敏感 0-不敏感
     * @param lowerCase 是否只需要小写，只有当hasChar为0且caseSense为1时起作用 1-全部小写 0-全部大写
     */
    $scope.randomPass = function (length, hasNum, hasChar, hasSymbol, caseSense, lowerCase) {
        var m = "";
        if (hasNum == "0" && hasChar == "0" && hasSymbol == "0") return m;
        for (var i = length; i >= 0; i--) {
            var num = Math.floor((Math.random() * 94) + 33);
            if (
                (
                    (hasNum == "0") && ((num >= 48) && (num <= 57))
                ) || (
                    (hasChar == "0") && ((
                        (num >= 65) && (num <= 90)
                    ) || (
                        (num >= 97) && (num <= 122)
                    ))
                ) || (
                    (hasSymbol == "0") && ((
                        (num >= 33) && (num <= 47)
                    ) || (
                        (num >= 58) && (num <= 64)
                    ) || (
                        (num >= 91) && (num <= 96)
                    ) || (
                        (num >= 123) && (num <= 127)
                    ))
                )
            ) {
                i++;
                continue;
            }
            m += String.fromCharCode(num);
        }
        if(caseSense == "0"){
            m = (lowerCase == "0")?m.toUpperCase():m.toLowerCase();
        }
        return m;
    }
}]);

module.controller('addUserDialogController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
        $scope.postUserRequest = function (requestItem) {
            //var request = JSON.parse(JSON.stringify(topicRequestItem));
            $http({
                method: "POST",
                url: "user/add.do",
                data: requestItem
            }).success(function (resp) {
                if (resp.status == 0) {
                    Notification.info({message: "success!", delay: 2000});
                } else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        }
    }]
);

module.controller('updateUserDialogController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {

        $scope.updateUserRequest = function (requestItem) {
            //var request = JSON.parse(JSON.stringify(topicRequestItem));
            $http({
                method: "POST",
                url: "user/update.do",
                data: requestItem
            }).success(function (resp) {
                if (resp.status == 0) {
                    Notification.info({message: "success!", delay: 2000});
                } else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        }
    }]
);

module.controller('addRoleDialogController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
        $scope.addRoleRequest = function (requestItem) {
            $http({
                method: "POST",
                url: "user/role/add.do",
                data: requestItem
            }).success(function (resp) {
                if (resp.status == 0) {
                    Notification.info({message: "success!", delay: 2000});
                } else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        }
    }]
);

module.controller('updateRoleDialogController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
        $scope.updateRoleRequest = function (requestItem) {
            $http({
                method: "POST",
                url: "user/role/update.do",
                data: requestItem
            }).success(function (resp) {
                if (resp.status == 0) {
                    Notification.info({message: "success!", delay: 2000});
                } else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        }
    }]
);

module.controller('addItemDialogController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
        $scope.addItemRequest = function (requestItem) {
            $http({
                method: "POST",
                url: "user/item/add.do",
                data: requestItem
            }).success(function (resp) {
                if (resp.status == 0) {
                    Notification.info({message: "success!", delay: 2000});
                } else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        }
    }]
);

module.controller('updateItemDialogController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
        $scope.updateItemRequest = function (requestItem) {
            $http({
                method: "POST",
                url: "user/item/update.do",
                data: requestItem
            }).success(function (resp) {
                if (resp.status == 0) {
                    Notification.info({message: "success!", delay: 2000});
                } else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        }
    }]
);