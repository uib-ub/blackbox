/**
 * Controller file for Marcus-search system
 * @author: Hemed, Tarje
 **/

'use strict';
var app = angular.module('marcus', ["checklist-model", "ui.bootstrap", "settings", "ngAnimate"]);

/**========= Search Controller ===========**/
app.controller('freeTextSearch', function ($scope, $http, $location, $window, mySetting) {

    //Initialize default variables
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
        })
            .success(function (data, status, headers, config) {
                $scope.results = data;
                $scope.show_loading = false;
                $scope.ready = true;

                /*$location.html5Mode(true);
                alert(JSON.stringify(config.params));
                $location.url(config.params);
                $location.replace();
                $window.history.pushState(null, 'locationURL', $location.absUrl());
                */


            })
            .error(function (data, status, headers, config) {
                $scope.log = 'Error occured while querying' + data;
                $scope.show_loading = false;
                $scope.ready = true;
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
        })
            .success(function (data, status, headers, config) {
                $scope.suggestion_list = data;
            })
            .error(function (data, status, headers, config) {
                $scope.log = 'Error occured while querying';
            });
    };

    //Call this function on pageload
    $scope.search();
});


/**
 * Year Controller
 */
app.controller('nextYear', function ($scope, $filter) {
    var myDate = new Date();
    var previousYear = new Date(myDate);
    var nextYear = new Date(myDate);

    previousYear.setYear(myDate.getFullYear() - 1);
    nextYear.setYear(myDate.getFullYear() + 1);

    $scope.year = $filter('date')(myDate, 'yyyy');//2014 like
    $scope.nextYear = $filter('date')(nextYear, 'yyyy');
    $scope.prevYear = $filter('date')(previousYear, 'yyyy');
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


/**
 A method to append * or ~ in the query string
 <p/>
 @param query_string a query string.
 @param default_freetext_fuzzify - should be either * or ~,
 if *, * will be prepended and appended to each string in the freetext search term
 if ~, then ~ will be appended to each string in the freetext search term.
 If * or ~ or : are already in the freetext search term, no action will be taken.
 @param query_string - a query string.
 **/
function fuzzify(query_string, default_freetext_fuzzify) {
    var rqs = query_string;
    if (default_freetext_fuzzify !== undefined) {
        if (default_freetext_fuzzify === "*" || default_freetext_fuzzify === "~") {
            //Do not do anything if query string has either one of the following chars.
            if (query_string.indexOf('*') === -1 && query_string.indexOf('~') === -1 &&
                query_string.indexOf(':') === -1 && query_string.indexOf('"') === -1 &&
                query_string.indexOf('[') === -1) {
                var option_parts = query_string.split(' ');
                var pq = "";
                for (var oi = 0; oi < option_parts.length; oi++) {
                    var oip = option_parts[oi];

                    //We want the string part to be greater than 1 char,
                    // and it should not contain the following special chars.
                    if (oip.length > 1 && oip.indexOf(')') === -1 && oip.indexOf('(') === -1) {
                        oip = oip + default_freetext_fuzzify;
                    }
                    pq += oip + ' ';
                }
                rqs = pq;
            }
        }
    }
    return rqs;
}
