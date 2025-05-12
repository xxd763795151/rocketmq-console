/**
 * Created by tcrow on 2017/1/12 0012.
 */
var module = app;

module.controller('subItemInfoController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
    $scope.paginationConf = {
        currentPage: 1,
        totalItems: 0,
        itemsPerPage: 10,
        pagesLength: 15,
        perPageOptions: [10],
        rememberPerPage: 'perPageItems',
        onChange: function () {
            $scope.refreshSubItemList($scope.username, this.currentPage, this.itemsPerPage);
        }
    };

    $scope.user = {};

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

    $scope.username = '';
    $scope.subItemData = {};
    $scope.subItemDataList = [];

    $scope.showSubItemList = function (data) {
        $scope.subItemDataList = data.list;
        $scope.paginationConf.totalItems = data.total;
    }

    $scope.refreshSubItemList = function (username, page, limit) {
        let params = '?page=' + page + "&limit=" + limit + (username ? "&username=" + username : '');
        $http({
            method: "GET",
            url: "sub/item" + params
        }).success(function (resp) {
            if (resp.status == 0) {
                $scope.subItemData = resp.data;
                $scope.showSubItemList($scope.subItemData);
            } else {
                Notification.error({message: resp.errMsg, delay: 5000});
            }
        });
    }

    $scope.$watch('username', function (){
        $scope.initList();
    });

    $scope.initList = function () {
        $scope.refreshSubItemList($scope.username, 1, $scope.paginationConf.itemsPerPage);
    }

    $scope.initList();

    $scope.addSubItem = function () {
        ngDialog.open({
            preCloseCallback: function (value) {
                $scope.refreshSubItemList($scope.username, $scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
            },
            template: 'addSubItemDialog',
            controller: 'addSubItemDialogController',
            data: {
                subItemCode: '',
                subItemName: '',
                description: ''
            }
        });
    }

    $scope.openUpdateSubItemDialog = function (data) {
        ngDialog.open({
            preCloseCallback: function (value) {
                $scope.refreshSubItemList($scope.username, $scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
            },
            template: 'updateSubItemDialog',
            controller: 'updateSubItemDialogController',
            data: data
        });
    }

    $scope.deleteSubItem = function (request) {
        $http({
            method: "DELETE",
            url: "sub/item?id=" + request.id
        }).success(function (resp) {
            if (resp.status == 0) {
                Notification.info({message: "删除成功!", delay: 3000, positionX: 'center'});
                $scope.refreshSubItemList($scope.username, $scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
            } else {
                Notification.error({message: resp.errMsg, delay: 10000});
            }
        });
    }
}]);

module.controller('addSubItemDialogController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
        $scope.applyTopicRequest = function (form) {
            let request = $.extend(true, {}, form);
            delete request.ngDialogId;

            if (!request.subItemCode) {
                Notification.warning({message: '项目编码不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            if (!request.subItemName) {
                Notification.warning({message: '项目名称不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            $('#addSubItemBtn').attr('disabled', true);
            $http({
                method: "POST",
                url: "sub/item",
                data: request
            }).success(function (resp) {
                $('#addSubItemBtn').attr('disabled', false);
                if (resp.status == 0) {
                    Notification.info({message: "新增成功!", delay: 3000, positionX: 'center'});
                } else {
                    Notification.error({message: resp.errMsg, delay: 10000});
                }
            });
        }
    }]
);

module.controller('updateSubItemDialogController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
        $scope.applyTopicRequest = function (form) {
            let request = $.extend(true, {}, form);
            delete request.ngDialogId;

            if (!request.subItemCode) {
                Notification.warning({message: '项目编码不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            if (!request.subItemName) {
                Notification.warning({message: '项目名称不能为空', delay: 3000, positionX: 'center'});
                return false;
            }

            $('#updateSubItemBtn').attr('disabled', true);
            $http({
                method: "PUT",
                url: "sub/item",
                data: request
            }).success(function (resp) {
                $('#updateSubItemBtn').attr('disabled', false);
                if (resp.status == 0) {
                    Notification.info({message: "更新成功!", delay: 3000, positionX: 'center'});
                } else {
                    Notification.error({message: resp.errMsg, delay: 10000});
                }
            });
        }
    }]
);

