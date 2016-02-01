'use strict';
//Contoller file for Marcus-search system

var app = angular.module('marcus', ["checklist-model", "settings"]);

/**========= Search Controller ===========**/
app.controller('freeTextSearch', function ($scope, $http, $location, mySetting) {
    
    //Initialize variables
    $scope.selected_filters = [];
    $scope.from_date = null;
    $scope.to_date = null;
    
    //Get values from checkboxes
    $scope.getCheckedValue = function (field, filterValue) {
        if (field !== undefined && filterValue !== undefined) {
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
        var q = "";
        $scope.query_string === undefined ? q = "*" : q = $scope.query_string + "*";
        $http({
            method: 'GET',
            url: 'search?aggs=' + JSON.stringify(mySetting.facets),
            params: {
                q: q,
                index: mySetting.index,
                type: mySetting.type,
                from_date: $scope.from_date,
                to_date: $scope.to_date,
                filter: $scope.selected_filters
              }
           })
            .success(function (data, status, headers, config) {
                    $scope.results = data;
             })
            .error(function (data, status, headers, config) {
                   $scope.log = 'Error occured while querying' + data;
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
