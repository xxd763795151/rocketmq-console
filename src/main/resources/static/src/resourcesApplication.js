/**
 * Created by tcrow on 2017/1/12 0012.
 */
var module = app;

module.controller('resourcesApplicationController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
    $scope.paginationConf = {
        currentPage: 1,
        totalItems: 0,
        itemsPerPage: 10,
        pagesLength: 15,
        perPageOptions: [10],
        rememberPerPage: 'perPageItems',
        onChange: function () {
            if ($scope.initialize[0]) {
                $scope.initialize[0] = false;
                return false;
            }
            $scope.refreshApplyTopicList($scope.username, this.currentPage, this.itemsPerPage);
        }
    };

    $scope.subscriptionPaginationConf = {
        currentPage: 1,
        totalItems: 0,
        itemsPerPage: 10,
        pagesLength: 15,
        perPageOptions: [10],
        rememberPerPage: 'perPageItems',
        onChange: function () {
            if ($scope.initialize[5]) {
                $scope.initialize[5] = false;
                return false;
            }
            $scope.refreshApplySubscriptionGroupList($scope.username, this.currentPage, this.itemsPerPage);
        }
    };

    $scope.user = {};

    $scope.initialize = [false, false, false, false, false, false, false, false, false]
    $http({
        method: "GET",
        url: "login/user"
    }).success(function (resp) {
        if (resp.status == 0) {
            $scope.user = resp.data.user;
            $scope.show = (resp.role == 1);
            $scope.customize = (resp.role == 2) || $scope.show;
        } else {
            Notification.error({message: resp.errMsg, delay: 2000});
        }
    });

    $scope.topicFilter;
    $scope.username = '';
    $scope.applyTopicData = {};
    $scope.applyTopicDataList = [];

    $scope.subscriptionGroupFilter;
    $scope.applySubscriptionGroupData = {};
    $scope.applySubscriptionGroupDataList = [];


    $scope.showApplyTopicList = function (data) {
        $scope.applyTopicDataList = data.list;
        $scope.paginationConf.totalItems = data.total;
    }

    $scope.showApplySubscriptionGroupList = function (data) {
        $scope.applySubscriptionGroupDataList = data.list;
        $scope.subscriptionPaginationConf.totalItems = data.total;
    }

    $scope.refreshApplyTopicList = function (username, page, limit) {
        let params = '?page=' + page + "&limit=" + limit + (username ? "&username=" + username : '') + ($scope.topicFilter ? "&topic=" + $scope.topicFilter : '');
        $http({
            method: "GET",
            url: "apply/topic/list" + params
        }).success(function (resp) {
            if (resp.status == 0) {
                $scope.applyTopicData = resp.data;
                $scope.showApplyTopicList($scope.applyTopicData);
            } else {
                Notification.error({message: resp.errMsg, delay: 5000});
            }
        });
    }

    $scope.refreshApplySubscriptionGroupList = function (username, page, limit) {
        let params = '?page=' + page + "&limit=" + limit + (username ? "&username=" + username : '') + ($scope.subscriptionGroupFilter ? "&subscriptionGroup=" + $scope.subscriptionGroupFilter : '');
        $http({
            method: "GET",
            url: "apply/subscription/list" + params
        }).success(function (resp) {
            if (resp.status == 0) {
                $scope.applySubscriptionGroupData = resp.data;
                $scope.showApplySubscriptionGroupList($scope.applySubscriptionGroupData);
            } else {
                Notification.error({message: resp.errMsg, delay: 5000});
            }
        });
    }

    $scope.$watch('username', function () {
        if ($scope.initialize[1]) {
            $scope.initialize[1] = false;
            return false;
        }
        $scope.initApplyTopicList();
    });

    $scope.$watch('topicFilter', function () {
        if ($scope.initialize[2]) {
            $scope.initialize[2] = false;
            return false;
        }
        $scope.initApplyTopicList();
    });

    $scope.$watch('subscriptionGroupFilter', function () {
        if ($scope.initialize[6]) {
            $scope.initialize[6] = false;
            return false;
        }
        $scope.initApplySubscriptionGroupList();
    });

    $scope.initList = function () {
        $scope.initApplyTopicList();
        $scope.initApplySubscriptionGroupList();
    }

    $scope.initApplyTopicList = function () {
        $scope.refreshApplyTopicList($scope.username, 1, $scope.paginationConf.itemsPerPage);
    }

    $scope.initApplySubscriptionGroupList = function () {
        $scope.refreshApplySubscriptionGroupList($scope.username, 1, $scope.subscriptionPaginationConf.itemsPerPage);
    }

    $scope.applyTopic = function () {

        $http({
            method: "GET",
            url: "sub/item/group"
        }).success(function (resp) {
            if (resp.status == 0) {

                let subItemList = [];
                $.each(resp.data, function (i, e) {
                    e = $.extend(e, {'composite': e.subItemCode + '(' + e.subItemName + ')'});
                    subItemList.push(e);
                });
                ngDialog.open({
                    preCloseCallback: function (value) {
                        $scope.refreshApplyTopicList($scope.username, $scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
                    },
                    template: 'applyTopicDialog',
                    controller: 'applyTopicDialogController',
                    data: {
                        topic: '',
                        producerGroup: '',
                        subItem: '',
                        queueNum: 8,
                        remark: '',
                        username: $scope.user.name,
                        subItemList: subItemList
                    }
                });
            } else {
                Notification.error({message: resp.errMsg, delay: 3000});
            }
        });
    }

    $scope.applySubscriptionGroup = function () {

        $http({
            method: "GET",
            url: "sub/item/group"
        }).success(function (resp) {
            if (resp.status == 0) {

                let subItemList = [];
                $.each(resp.data, function (i, e) {
                    e = $.extend(e, {'composite': e.subItemCode + '(' + e.subItemName + ')'});
                    subItemList.push(e);
                });
                ngDialog.open({
                    preCloseCallback: function (value) {
                        $scope.refreshApplySubscriptionGroupList($scope.username, $scope.subscriptionPaginationConf.currentPage, $scope.subscriptionPaginationConf.itemsPerPage);
                    },
                    template: 'applySubscriptionGroupDialog',
                    controller: 'applySubscriptionGroupDialogController',
                    data: {
                        topic: '',
                        subscriptionGroup: '',
                        subItem: '',
                        remark: '',
                        username: $scope.user.name,
                        consumeBroadcastEnable: false,
                        subItemList: subItemList
                    }
                });
            } else {
                Notification.error({message: resp.errMsg, delay: 3000});
            }
        });
    }

    $scope.openApproveTopicDialog = function (applyTopicData) {
        $http({
            method: "GET",
            url: "acl/belong/item/group.query?itemId=" + applyTopicData.itemTeamId
        }).success(function (resp) {
            if (resp.status == 0) {
                ngDialog.open({
                    preCloseCallback: function (value) {
                        $scope.refreshApplyTopicList($scope.username, $scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
                    },
                    template: 'approveTopicDialog',
                    controller: 'approveTopicDialogController',
                    data: {
                        applyTopicData: applyTopicData,
                        accessKeyList: resp.data
                    }
                });

            } else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    }

    // 审批消费组对话框
    $scope.openApproveSubscriptionGroupDialog = function (approveSubscriptionData) {
        $http({
            method: "GET",
            url: "acl/belong/item/group.query?itemId=" + approveSubscriptionData.itemTeamId
        }).success(function (resp) {
            if (resp.status == 0) {
                ngDialog.open({
                    preCloseCallback: function (value) {
                        $scope.refreshApplySubscriptionGroupList($scope.username, $scope.subscriptionPaginationConf.currentPage, $scope.subscriptionPaginationConf.itemsPerPage);
                    },
                    template: 'approveSubscriptionGroupDialog',
                    controller: 'approveSubscriptionGroupDialogController',
                    data: {
                        approveSubscriptionData: approveSubscriptionData,
                        accessKeyList: resp.data
                    }
                });

            } else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    }

    $scope.resendApproveTopicRequest = function (approveTopicData) {
        $('#approveTopicResendBtn').attr('disabled', true);
        $http({
            method: "POST",
            url: "apply/topic/approve/resend.do",
            data: approveTopicData
        }).success(function (resp) {
            $('#approveTopicResendBtn').attr('disabled', false);
            $scope.refreshApplyTopicList($scope.username, $scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
            if (resp.status == 0) {
                Notification.info({message: "操作成功!", delay: 3000, positionX: 'center'});
            } else {
                Notification.error({message: resp.errMsg, delay: 10000});
            }
        });
    }

    $scope.resendApproveSubscriptionRequest = function (approveData) {
        $('#approveSubscriptionResendBtn').attr('disabled', true);
        $http({
            method: "POST",
            url: "apply/subscription/approve/resend.do",
            data: approveData
        }).success(function (resp) {
            $('#approveSubscriptionResendBtn').attr('disabled', false);
            $scope.refreshApplySubscriptionGroupList($scope.username, $scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
            if (resp.status == 0) {
                Notification.info({message: "操作成功!", delay: 3000, positionX: 'center'});
            } else {
                Notification.error({message: resp.errMsg, delay: 10000});
            }
        });
    }

    $scope.revocationRequest = function (approveData, type, btn) {
        $(btn).attr('disabled', true);
        $http({
            method: "POST",
            url: "apply/revocation.do",
            data: {id: approveData.id + '', type: type}
        }).success(function (resp) {
            $(btn).attr('disabled', false);
            if (type == 'topic') {
                $scope.refreshApplyTopicList($scope.username, $scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
            } else if (type == 'consumer') {
                $scope.refreshApplySubscriptionGroupList($scope.username, $scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
            } else {
                $scope.refreshApplyTopicList($scope.username, $scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
                $scope.refreshApplySubscriptionGroupList($scope.username, $scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
            }
            if (resp.status == 0) {
                Notification.info({message: "操作成功!", delay: 3000, positionX: 'center'});
            } else {
                Notification.error({message: resp.errMsg, delay: 10000});
            }
        });
    }
}]);

module.controller('applyTopicDialogController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
        $scope.applyTopicRequest = function (form) {
            let request = $.extend(true, {}, form);
            delete request.ngDialogId;
            delete request.subItemList;

            if (!request.topic) {
                Notification.warning({message: 'topic不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            // if (!request.topic.endsWith('_topic')) {
            //     Notification.warning({message: 'topic后缀要求为_topic', delay: 3000, positionX: 'center'});
            //     return false;
            // }

            if (!request.producerGroup) {
                Notification.warning({message: '生产组不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            // if (!request.producerGroup.endsWith("_producer")) {
            //     Notification.warning({message: '生产组后缀要求为_producer', delay: 3000, positionX: 'center'});
            //     return false;
            // }

            if (!request.subItem || request.subItem.length == 0) {
                Notification.warning({message: '项目不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            let subItem = request.subItem.join(',');
            request.subItem = subItem;

            $('#applyTopicBtn').attr('disabled', true);
            $http({
                method: "POST",
                url: "apply/topic.do",
                data: request
            }).success(function (resp) {
                $('#applyTopicBtn').attr('disabled', false);
                if (resp.status == 0) {
                    Notification.info({message: "已成功提交topic创建申请!", delay: 3000, positionX: 'center'});
                } else {
                    Notification.error({message: resp.errMsg, delay: 10000});
                }
            });
        }
    }]
);

module.controller('applySubscriptionGroupDialogController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
        $scope.applySubscriptionGroupRequest = function (form) {
            let request = $.extend(true, {}, form);
            delete request.ngDialogId;
            delete request.subItemList;

            if (!request.topic) {
                Notification.warning({message: 'topic不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            // if (!request.topic.endsWith('_topic')) {
            //     Notification.warning({message: 'topic后缀要求为_topic', delay: 3000, positionX: 'center'});
            //     return false;
            // }

            if (!request.subscriptionGroup) {
                Notification.warning({message: '消费组不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            // if (!request.subscriptionGroup.endsWith("_consumer")) {
            //     Notification.warning({message: '消费组后缀要求为_consumer', delay: 3000, positionX: 'center'});
            //     return false;
            // }

            if (!request.subItem || request.subItem.length == 0) {
                Notification.warning({message: '项目不能为空', delay: 3000, positionX: 'center'});
                return false;
            }
            let subItem = request.subItem.join(',');
            request.subItem = subItem;

            $('#applySubscriptionGroupBtn').attr('disabled', true);
            $http({
                method: "POST",
                url: "apply/subscription.do",
                data: request
            }).success(function (resp) {
                $('#applySubscriptionGroupBtn').attr('disabled', false);
                if (resp.status == 0) {
                    Notification.info({message: "已成功提交消费组创建申请!", delay: 3000, positionX: 'center'});
                } else {
                    Notification.error({message: resp.errMsg, delay: 10000});
                }
            });
        }
    }]
);

module.controller('approveTopicDialogController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
        $scope.approveTopicRequest = function (form, applyResult) {
            let request = $.extend(true, {}, form.applyTopicData);
            delete request.ngDialogId;
            delete request.$$hashKey;
            delete request.createTime;
            delete request.sendStatus;

            request.applyResult = applyResult;
            request.applyStage = 1;
            if (applyResult == 1 && !request.topic) {
                Notification.warning({message: 'topic不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            if (applyResult == 1 && !request.producerGroup) {
                Notification.warning({message: '生产组不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            if (applyResult == 1 && !request.producerGroup.endsWith("_producer")) {
                Notification.warning({message: '生产组后缀要求为_producer', delay: 3000, positionX: 'center'});
                return false;
            }

            if (applyResult == 1 && !request.subItem) {
                Notification.warning({message: '项目不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            if (applyResult == 1 && !request.accessKey) {
                Notification.warning({message: 'accessKey不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            $('#approvePassTopicBtn').attr('disabled', true);
            $http({
                method: "POST",
                url: "apply/topic/approve.do",
                data: request
            }).success(function (resp) {
                $('#approvePassTopicBtn').attr('disabled', false);
                if (resp.status == 0) {
                    Notification.info({message: "操作成功!", delay: 3000, positionX: 'center'});
                } else {
                    Notification.error({message: resp.errMsg, delay: 10000});
                }
            });
        }
    }]
);

module.controller('approveSubscriptionGroupDialogController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
        $scope.approveSubscriptionGroupRequest = function (form, applyResult, btnId) {
            let request = $.extend(true, {}, form.approveSubscriptionData);
            delete request.ngDialogId;
            delete request.$$hashKey;
            delete request.createTime;
            delete request.sendStatus;

            request.applyResult = applyResult;
            request.applyStage = 1;
            let applyPass = applyResult == 1;

            if (applyPass && !request.topic) {
                Notification.warning({message: 'topic不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            if (applyPass && !request.subscriptionGroup) {
                Notification.warning({message: '消费组不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            if (applyPass && !request.subscriptionGroup.endsWith("_consumer")) {
                Notification.warning({message: '消费组后缀要求为_consumer', delay: 3000, positionX: 'center'});
                return false;
            }

            if (applyPass && !request.subItem) {
                Notification.warning({message: '项目不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            if (applyPass && !request.accessKey) {
                Notification.warning({message: 'accessKey不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            console.log(btnId);
            $('#' + btnId).attr('disabled', true);
            $http({
                method: "POST",
                url: "apply/subscription/approve.do",
                data: request
            }).success(function (resp) {
                $('#' + btnId).attr('disabled', false);
                if (resp.status == 0) {
                    Notification.info({message: "操作成功!", delay: 3000, positionX: 'center'});
                } else {
                    Notification.error({message: resp.errMsg, delay: 10000});
                }
            });
        }
    }]
);