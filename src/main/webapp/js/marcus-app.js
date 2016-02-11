'use strict';
//Contoller file for Marcus-search system

var app = angular.module('marcus', ["checklist-model", "ui.bootstrap", "settings"]);

/**========= Search Controller ===========**/
app.controller('freeTextSearch', function ($scope, $http, $location, mySetting) {

    //Initialize variables
    $scope.show_loading = true;
    $scope.show_search_results = false;
    $scope.query_string = "";
    $scope.sort_by = "";
    $scope.selected_filters = [];
    $scope.from_date = null;
    $scope.to_date = null;
    $scope.current_page = 0;
    $scope.page_size = 10;
    

    /**
     * Get values from checkboxes. 
     * The method returns a string concatenation of a field and the selected value.
     * Note that, a field and it's selected value must be seperated by a dot ('.'), 
     * otherwise the server will not know which is which and aggregations will fail.
     **/
    $scope.getCheckedValue = function (field, filterValue) {
        if (field !== undefined && filterValue !== undefined) {
            /**Seperate a field and the selected value by a dot.**/
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
        /**We are assigning null to these values so that, if empty, they should not appear in query string**/
        var q = $scope.query_string === "" ? null : fuzzify($scope.query_string , "*");
        var sort = $scope.sort_by === "" ? null : $scope.sort_by;

        $http({
            method: 'GET',
            url: 'search?aggs=' + JSON.stringify(mySetting.facets),
            params: {
                q: q,
                index: mySetting.index,
                type: mySetting.type,
                from_date: $scope.from_date,
                to_date: $scope.to_date,
                filter: $scope.selected_filters,
                from: $scope.current_page * $scope.page_size,
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
        var q = "";
        $scope.query_string === undefined ? q = "" : q = $scope.query_string;
        $http({
            method: 'GET',
            url: 'suggest',
            params: {
                q: q
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
     * A method to apped * or ~ in the query string.
     * 
      @param descriptiondefault_freetext_fuzzify - should be either * or ~, 
      if *, * will be prepended and appended to each string in the freetext search term
      if ~, then ~ will be appended to each string in the freetext search term. 
      If * or ~ or : are already in the freetext search term, no action will be taken. 
      @param querystr - a query string.
     **/
    function fuzzify(querystr, default_freetext_fuzzify) {
        var rqs = querystr;
        if (default_freetext_fuzzify !== undefined) {
            if (default_freetext_fuzzify === "*" || default_freetext_fuzzify === "~") {
                if (querystr.indexOf('*') === -1 && querystr.indexOf('~') === -1 && querystr.indexOf(':') === -1) {
                    var optparts = querystr.split(' ');
                    var pq = "";
                    for (var oi = 0; oi < optparts.length; oi++) {
                        var oip = optparts[oi];
                        if (oip.length > 0) {
                            oip = oip + default_freetext_fuzzify;
                            default_freetext_fuzzify === "*" ? oip : false;
                            //default_freetext_fuzzify == "*" ? oip = "*" + oip : false;
                            pq += oip + " ";
                        }
                    }
                    rqs = pq;
                }
            }
        }
        return rqs;
    }