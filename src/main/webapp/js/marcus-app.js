'use strict';
//Controller file for Marcus-search system

var app = angular.module('marcus', ["checklist-model", "ui.bootstrap", "settings"]);

/**========= Search Controller ===========**/
app.controller('freeTextSearch', function ($scope, $http, $location, mySetting) {

    //Initialize default variables
    $scope.show_loading = true;
    $scope.show_search_results = false;
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
     * Note that, a field and it's selected value must be seperated by a dot ('.'),
     * otherwise the server will not know which is which and aggregations will fail.
     **/
    $scope.getCheckedValue = function (field, filterValue) {
        if (field !== undefined && filterValue !== undefined) {
            /*Separate a field and the selected value by a dot.*/
            return field + "." + filterValue;
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
        var q = $scope.query_string === "" ? null : fuzzify($scope.query_string, "*");
        var sort = $scope.sort_by === "" ? null : $scope.sort_by;
        var from_page = ($scope.current_page - 1)*$scope.page_size;
        var from_date = $scope.from_date === ""? null : $scope.from_date;
        var to_date = $scope.to_date === ""? null : $scope.to_date;
        $scope.test = q;

        $http({
            method: 'GET',
            url: 'search?aggs=' + JSON.stringify(mySetting.facets),
            params: {
                q: q,
                index: mySetting.index,
                type: mySetting.type,
                from_date: from_date,
                to_date: to_date,
                filter: $scope.selected_filters,
                from: from_page,
                size: $scope.page_size,
                sort: sort
            }
        })
            .success(function (data, status, headers, config) {
                $scope.results = data;
                $scope.show_loading = false;
                $scope.ready = true;
            })
            .error(function (data, status, headers, config) {
                $scope.log = 'Error occured while querying' + data;
                $scope.show_loading = false;
                $scope.ready = true;
            });
    };
    

    //Send suggest request to "suggest" servlet for autocompleting.
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
                query_string.indexOf(':') === -1 && query_string.indexOf('"') === -1) {

                /*if(query_string.indexOf('"') >= 0){
                    var quote_index =  query_string.lastIndexOf('"');
                    var quote_part =  query_string.substring(0, quote_index);
                    query_string = query_string.substring(quote_index+1, rqs.length);

                    alert("Quotes: " + quote_part +  "\n Other parts: " +  query_string);
                }
                **/
                var option_parts = query_string.split(' ');
                var pq = "";
                for (var oi = 0; oi < option_parts.length; oi++) {
                    var oip = option_parts[oi];

                    //We want the string part to be greater than 1 char,
                    // and it should not contain the following special chars.
                    if (oip.length > 1 && oip.indexOf(')') === -1 && oip.indexOf('(') === -1){

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

    app.controller('nextYear', function ($scope, $filter) {
        var myDate = new Date();
        var previousYear = new Date(myDate);

        previousYear.setYear(myDate.getFullYear()-1);

        var nextYear = new Date(myDate);

        nextYear.setYear(myDate.getFullYear()+1);

        $scope.year = $filter('date')(myDate,'yyyy');//2014 like
        $scope.nextYear = $filter('date')(nextYear,'yyyy');
        $scope.prevYear = $filter('date')(previousYear,'yyyy');
    });
    
    app.filter('unsafe', function($sce) {
        return function(val) {
            return $sce.trustAsHtml(val);
        };
    });
    
    app.directive('includeReplace', function () {
        return {
            require: 'ngInclude',
            restrict: 'A', /* optional */
            link: function (scope, el, attrs) {
                el.replaceWith(el.children());
            }
        };
    });

