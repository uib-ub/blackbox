/**
 * Controller file for Marcus-search system
 * @author: Hemed, Tarje
 **/

'use strict';
var app = angular.module('marcus', ["checklist-model", "ui.bootstrap", "settings", "ngAnimate", "ngRoute"]);

app.config(["$routeProvider", function($routeProvider) {
    $routeProvider
        .when('/h', {
            templateUrl : 'home.html',
            controller  : 'freeTextSearch'
        })
        .when('/test', {
            templateUrl : 'search',
            controller  : 'freeTextSearch'
        })
        .otherwise({
           redirectTo: 'home.html'
        });
    //$locationProvider.html5Mode(true);
}]);
app.config(function($locationProvider) {
    $locationProvider.html5Mode(true).hashPrefix('');
});

/**========= Search Controller ===========**/
app.controller('freeTextSearch', function ($scope, $http, $location, $window, mySetting) {

    //Initialize default variables
    $(".blackbox-loading").show();
    $scope.show_loading = true;
    $scope.ready = false;
    $scope.query_string = "";
    $scope.sort_by = "";
    $scope.selected_filters = [];
    $scope.from_date = null;
    $scope.to_date = null;
    $scope.current_page = 1;
    $scope.page_size = 10;
    $scope.isArray = angular.isArray;
    $scope.isString = angular.isString;

    /**
     * Get values from checkboxes.
     * The method returns a string concatenation of a field and the selected value.
     * Note that, a field and it's selected value must be separated by a hash ('#'),
     * otherwise the server will not know which is which and aggregations will fail.
     **/
    $scope.getCheckedValue = function (field, filterValue) {
        if (field !== undefined && filterValue !== undefined) {
            /*Separate a field and the selected value by a hash*/
            return field + '#' + filterValue;
        }
        return null;
    };

    //Clear date fields
    $scope.clearDates = function () {
        $scope.from_date = null;
        $scope.to_date = null;

        //After clearing the dates, send new search request.
        $scope.search();
    };

    //Send requests to search servlet
    $scope.search = function () {
        /*We are assigning null to these values so that, if empty, they should not appear in query string*/
        var q = $scope.query_string === "" ? null : $scope.query_string /**fuzzify($scope.query_string, "*")**/;
        var sort = $scope.sort_by === "" ? null : $scope.sort_by;
        var fromPage = ($scope.current_page - 1) * $scope.page_size;
        var fromDate = $scope.from_date === "" ? null : $scope.from_date;
        var toDate = $scope.to_date === "" ? null : $scope.to_date;

        $http({
            method: 'GET',
            url: 'search?aggs=' + JSON.stringify(mySetting.facets),
            params: {
                q: q,
                index: mySetting.index,
                type: mySetting.type,
                from_date: fromDate,
                to_date: toDate,
                filter: $scope.selected_filters,
                from: fromPage,
                size: $scope.page_size,
                sort: sort
            }
        }).then(function (response) {
                /**console.log("URL: " + $location.url());
                console.log("Path: " + $location.path());
                console.log("absUrl: " + $location.absUrl());
                var params = JSON.stringify(response.config.params);
                //Response params:  data, status, headers, config
                //$location.html5Mode(true);
                //console.log(JSON.stringify(config.params));
                //$location.url(JSON.stringify(response.config));
                //$location.replace();
                 //$window.history.pushState(null, 'locationURL', ($location.search(response.config.params));
                 **/
                //$("#searchController").hide();
                //$("#loadingGif").show();
                if(!response.data){
                    $(".blackbox-loading").hide();
                    var alert = "<div class='ui large red message' " +
                                   "style='width: 50%; margin: 10% auto; text-align: center; font-size: 2em'>" +
                                   "<strong> Service is temporarily unavailable </strong>" +
                                "</div>";
                   $("#searchController").append(alert);
                }
                if(response.data) {
                    $location.search(response.config.params);
                    $scope.results = response.data;
                    $(".blackbox-loading").hide();
                    //$scope.show_loading = false;
                    $scope.ready = true;
                }
            });
            /**.error(function (data, status, headers, config) {
                $scope.log = 'Error occured while querying' + data;
                $scope.show_loading = false;
                $scope.ready = true;"
            });**/
    };

    //Send suggestion request to "suggestion" servlet for autocompletion.
    $scope.autoSuggest = function () {
        $http({
            method: 'GET',
            url: 'suggest',
            params: {
                q: $scope.query_string,
                index: mySetting.index
            }
        }).then(function (response) {
                $scope.suggestion_list = response.data;
            });
    };

    //Call this function on pageload
    $scope.search();
});

/**
 * A filter
 */
app.filter('unsafe', function ($sce) {
    return function (val) {
        return $sce.trustAsHtml(val);
    };
});

/**
 * A directive.
 */
app.directive('includeReplace', function () {
    return {
        require: 'ngInclude',
        restrict: 'A', /* optional */
        link: function (scope, el, attrs) {
            el.replaceWith(el.children());
        }
    };
});


