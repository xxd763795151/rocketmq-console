/**
 * Created by tcrow on 2017/1/12 0012.
 */
var module = app;

module.controller('accountOperationRecordController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
    $scope.paginationConf = {
        currentPage: 1,
        totalItems: 0,
        itemsPerPage: 10,
        pagesLength: 15,
        perPageOptions: [10],
        rememberPerPage: 'perPageItems',
        onChange: function () {
            $scope.refresh($scope.username, this.currentPage, this.itemsPerPage);
        }
    };
    $http({
        method: "GET",
        url: "login/user"
    }).success(function (resp) {
        if (resp.status == 0) {
            $scope.show = (resp.role == 1);
            $scope.customize = (resp.role == 2) || $scope.show;
        } else {
            Notification.error({message: resp.errMsg, delay: 2000});
        }
    });

    $scope.username = '';
    $scope.recordData = {};
    $scope.recordDataList = [];

    $scope.showList = function (data) {
        $scope.recordDataList = data.list;
        $scope.paginationConf.totalItems = data.total;
    }

    $scope.refresh = function (username, page, limit) {
        let params = '?page=' + page + "&limit=" + limit + (username ? "&username=" + username : '');
        $http({
            method: "GET",
            url: "aor" + params
        }).success(function (resp) {
            if (resp.status == 0) {
                $scope.recordData = resp.data;
                $scope.showList($scope.recordData);
            } else {
                Notification.error({message: resp.errMsg, delay: 5000});
            }
        });
    }

    $scope.$watch('username', function (){
        $scope.initList();
    });

    $scope.initList = function () {
        $scope.refresh($scope.username, 1, $scope.paginationConf.itemsPerPage);
    }

    $scope.initList();

    $scope.openResendDialog = function (record) {
        ngDialog.open({
            preCloseCallback: function (value) {
                $scope.refresh($scope.username, $scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
            },
            template: 'resendDialog',
            controller: 'resendDialogController',
            data: record
        });
    }
}]);

module.controller('resendDialogController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
        $scope.resendRequest = function (request) {
            $http({
                method: "POST",
                url: "aor/resend.do",
                data: request
            }).success(function (resp) {
                if (resp.status == 0) {
                    Notification.info({message: "邮件发送成功!", delay: 3000});
                } else {
                    Notification.error({message: resp.errMsg, delay: 10000});
                }
            });
        }
    }]
);
