/**
 * Controller file for Marcus-search system
 * @author: Hemed, Tarje
 **/

'use strict';
var app = angular.module('marcus', ["checklist-model", "ui.bootstrap", "settings", "ngAnimate", "ngRoute"]);
app.config(["$routeProvider", function($routeProvider) {
    $routeProvider
        .when('/search', {
            templateUrl : 'home.html',
            controller  : 'freeTextSearch'
        })
        .when('/test', {
            templateUrl : 'search',
            controller  : 'freeTextSearch'
        })
        .otherwise({
            redirectTo: 'home.html',
            controller  : 'freeTextSearch'
        });
    //$locationProvider.html5Mode(true);
}]);
app.config(function($locationProvider) {
    $locationProvider.html5Mode(true);
});

/**
 * ================================
 *     Search Controller
 * ================================
 **/
app.controller('freeTextSearch', function ($scope, $http, $location, mySetting) {

    //Declare variables
    $scope.isArray = angular.isArray;
    $scope.isString = angular.isString;

    //Get parameters from the URL
    var urlParams = $location.search();

    console.log("URL Params: " + JSON.stringify(urlParams));

    //Initialize variables to default values
    $scope.query_string = null;
    $scope.sort_by = null;
    $scope.selected_filters = [];
    $scope.from_date = null;
    $scope.to_date = null;
    $scope.current_page = 1;
    $scope.page_size = 10;
    $scope.from = ($scope.current_page - 1) * $scope.page_size;
    $scope.ready = false;

    //Show loading gif
    $(".blackbox-loading").show();

    /**
     * Get values from checkboxes.
     * The method returns a string concatenation of a field and the selected value.
     * Note that, a field and it's selected value must be separated by a hash ('#'),
     * otherwise the server will not know which is which and aggregations will fail.
     **/
    $scope.getCheckedValue = function (field, filterValue) {
        if (field !== undefined && filterValue !== undefined) {
            //Separate a field and the selected value
            return field + '#' + filterValue;
        }
        return null;
    };

    //Send requests to search servlet
    $scope.search = function () {

        var defaultParams = {
            q: stripEmpty($scope.query_string),
            index: mySetting.index,
            type: mySetting.type,
            from_date: stripEmpty($scope.from_date),
            to_date: stripEmpty($scope.to_date),
            filter: $scope.selected_filters,
            from: $scope.from,
            size: $scope.page_size,
            sort: stripEmpty($scope.sort_by)
        };

        //Merge and/or extend default params with URL params. URL params take precedence.
        var extendedParams = $.extend(defaultParams, urlParams);
        //Clear URL params after use.
        urlParams = {};
        $http({
            method: 'GET',
            url: 'search?aggs=' + JSON.stringify(mySetting.facets),
            params: extendedParams
        }).then(function (response) {
            //Initialize the view with extended parameters

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
                //$("#searchController").hide();
                //$("#loadingGif").show();
                 **/

            //Parameters that have been used to generate response.
            // In principle, they are the same as extendedParams
            var responseParams = response.config.params;
                if(response.data) {

                    //Initialize the view by copying response params to scope variables
                    if ("q" in responseParams) {
                        $scope.query_string = responseParams.q
                    }
                    if ("from_date" in responseParams) {
                        $scope.from_date = responseParams.from_date
                    }
                    if ("to_date" in responseParams) {
                        $scope.to_date = responseParams.to_date
                    }
                    if ("from" in responseParams) {
                        $scope.from = responseParams.from
                    }
                    if ("size" in responseParams) {
                        $scope.size = responseParams.size
                    }
                    if ("sort" in responseParams) {
                        $scope.sort = responseParams.sort
                    }
                    if ("filter" in responseParams) {
                        if (responseParams.filter instanceof Array) {
                            $scope.selected_filters = responseParams.filter
                        }
                        else {
                            if ($scope.selected_filters.indexOf(responseParams.filter) === -1) {
                                $scope.selected_filters.push(responseParams.filter);
                                responseParams.filter = $scope.selected_filters;
                            }
                        }
                    }
                    console.log("extended Params: " + JSON.stringify(extendedParams));
                    //TODO: Initialize dropdown lists and facets.
                    $scope.results = response.data;
                    $scope.ready = true;
                }
                else{
                    //Empty everything in search controller
                    $("#searchController").empty();
                    var alert = "<div id='alert-server-status' class='ui large red message' " +
                                      "<strong> Service is temporarily unavailable </strong>" +
                                "</div>";
                    $("#searchController").append(alert);
                    console.log("No response from Elasticsearch")
                }
            //Set params to the browser history
            $location.search(responseParams);
               $(".blackbox-loading").hide();
            });
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



