'use strict';
//Contoller file for Marcus-search system

var app = angular.module('marcus', []);

/**app.controller('showAllResults',
 function ($scope, $http) {
 $http({method: 'GET', url: 'discover'})
 
 .success(function (data, status, headers, config) {
 $scope.results = data;
 //alert(JSON.stringify(data));
 })
 
 .error(function (data, status, headers, config) {
 $scope.log = 'Error occured while querying';
 });
 });**/

app.controller('freeTextSearch', function ($scope, $http) {
    $scope.query_search = function () {
        var q = "";
        $scope.query_string === undefined ? q = "" : q = $scope.query_string + "*";
        
        $http({method: 'POST', url: 'search?q=' + q})

                .success(function (data, status, headers, config) {
                    $scope.results = data;
                    //alert(JSON.stringify(data));
                })

                .error(function (data, status, headers, config) {
                    $scope.log = 'Error occured while querying';
                });
    };

    $scope.auto_complete = function () {
        var q = "";
        $scope.query_string === undefined ? q = "" : q = $scope.query_string;
        $http({method: 'POST', url: 'suggest?q=' + q})
                .success(function (data, status, headers, config) {
                    $scope.suggestion_list = data;
                })
                .error(function (data, status, headers, config) {
                    $scope.log = 'Error occured while querying';
                });
    };
    
});



    