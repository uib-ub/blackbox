'use strict';
//Contoller file for Marcus-search system

var app = angular.module('marcus', ["checklist-model", "settings"]);
//Predefined aggregations
//.constant('aggs', '[{"field": "status", "size": 15} , {"field" : "assigned_to"}]');

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

/**app.controller('checkBoxCtrl', function ($scope) {
 $scope.selected_facets = [];
 if($scope.selected_facets.length > 0){
 alert($scope.selected_facets[0]);
 }
 
 $scope.uncheckAll = function () {
 $scope.selected_facets = [];
 };
 });**/


app.controller('freeTextSearch', function ($scope, $http, $location, mySetting) {
    //See here: <http://vitalets.github.io/checklist-model/>


    $scope.selected_filters = [];
    $scope.getCheckedValue = function (field, filterValue) {
        if (field !== undefined && filterValue !== undefined) {
            return field + "." + filterValue;
        }
        return null;
    };
    //alert("Predefined settings:" + JSON.stringify(mySettings));


    //Testing for location
    // var s = $location.search();
    // $scope.selected_aggs.push(s.agg);

    //Send requests to search servlet
    $scope.query_search = function () {
        var q = "";
        $scope.query_string === undefined ? q = "*" : q = $scope.query_string + "*";
        $http({
            method: 'GET',
            url: 'search?aggs=' + JSON.stringify(mySetting.facets),
            params: {
                q: q,
                filter: $scope.selected_filters,
                index: mySetting.index,
                type: mySetting.type
            }
        })
                .success(function (data, status, headers, config) {
                    $scope.results = data;
                    // If we have aggregations, store them in the scope.
                   /**if ($scope.results.hasOwnProperty('aggregations')) {
                       //Append extra field to aggregations
                       //var field = ["status" , "assigned_to"];
                        var aggs = $scope.results.aggregations;
                        var buckets = aggs.status.buckets;
                        angular.forEach(buckets, function(value, key){
                           var t = value.doc_count;
                           if(value.doc_count > 0){
                             t = value.doc_count 
                           }
                           value.total_doc_count = t;
                        });
                        $scope.aggregations = aggs;    
                    }**/
                })
                .error(function (data, status, headers, config) {
                    $scope.log = 'Error occured while querying' + data;
                });
    };
    //Suggest endpoint
    $scope.auto_complete = function () {
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
    $scope.query_search();
});
