'use strict';
//Contoller file for Marcus-search system

var app = angular.module('marcus', ["checklist-model"]) 
        //Predefined aggregations
        .constant('predefined_aggs' , '[{"field": "status", "size": 15} , {"field" : "assigned_to"}]'); 

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


app.controller('freeTextSearch' , function ($scope , $http , predefined_aggs) {
   //See here: <http://vitalets.github.io/checklist-model/>
    $scope.status_aggs = [];
    $scope.assignee_aggs = [];
    
    var selected_aggs = "status:" + JSON.stringify($scope.status_aggs) + "assignee:" + $scope.assignee_aggs;
    
    
    $scope.getCheckedValue = function(field, aggValue) {
        return field + ":" + aggValue;
    };
    console.log("Predefined aggregations:" + predefined_aggs);
    
    $scope.query_search = function () {
        var q = "";
        $scope.query_string === undefined ? q = "*" : q = $scope.query_string + "*";
        
        $http({method: 'POST', 
            url: 'search?q='+ q +
                    "&status=" + $scope.status_aggs + 
                    "&assignee=" + $scope.assignee_aggs 
             })

                .success(function (data, status, headers, config) {
                    $scope.results = data;
            
                // If we have aggregations, store them in the scope.
                if (data.aggregations) {
                     $scope.aggregations = data.aggregations;
                 }
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
    
    $scope.add_aggregations = function(){
      $scope.selected_aggregations = [];//Get Values from users
      $scope.query_search();
    };
});
