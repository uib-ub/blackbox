'use strict';
//Contoller file for Marcus-search system

var app = angular.module('marcus', ["checklist-model", "settings"]);


app.controller('Datepicker', function ($scope) {
    $scope.today = function () {
        $scope.dt = new Date();
    };
    $scope.today();

    $scope.clear = function () {
        $scope.dt = null;
    };

    // Disable weekend selection
    $scope.disabled = function (date, mode) {
        return mode === 'day' && (date.getDay() === 0 || date.getDay() === 6);
    };

    $scope.toggleMin = function () {
        $scope.minDate = $scope.minDate ? null : new Date();
    };

    $scope.toggleMin();
    $scope.maxDate = new Date(2020, 5, 22);

    $scope.open1 = function () {
        $scope.popup1.opened = true;
    };

    $scope.open2 = function () {
        $scope.popup2.opened = true;
    };

    $scope.setDate = function (year, month, day) {
        $scope.dt = new Date(year, month, day);
    };

    $scope.dateOptions = {
        formatYear: 'yy',
        startingDay: 1
    };

    $scope.formats = ['dd-MMMM-yyyy', 'yyyy/MM/dd', 'dd.MM.yyyy', 'shortDate'];
    $scope.format = $scope.formats[0];
    $scope.altInputFormats = ['M!/d!/yyyy'];

    $scope.popup1 = {
        opened: false
    };

    $scope.popup2 = {
        opened: false
    };

    var tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    var afterTomorrow = new Date();
    afterTomorrow.setDate(tomorrow.getDate() + 1);
    $scope.events =
            [
                {
                    date: tomorrow,
                    status: 'full'
                },
                {
                    date: afterTomorrow,
                    status: 'partially'
                }
            ];

    $scope.getDayClass = function (date, mode) {
        if (mode === 'day') {
            var dayToCheck = new Date(date).setHours(0, 0, 0, 0);

            for (var i = 0; i < $scope.events.length; i++) {
                var currentDay = new Date($scope.events[i].date).setHours(0, 0, 0, 0);

                if (dayToCheck === currentDay) {
                    return $scope.events[i].status;
                }
            }
        }

        return '';
    };
});



/*========= Search Controller ===========**/
app.controller('freeTextSearch', function ($scope, $http, $location, mySetting) {
    $scope.selected_filters = [];
    $scope.from_date = "";
    $scope.to_date = "";
    
    $scope.getCheckedValue = function (field, filterValue) {
        if (field !== undefined && filterValue !== undefined) {
            return field + "." + filterValue;
        }
        return null;
    };

     $scope.getDateValues = function (){
         //var from_date = $("#from_date").val();
         //var to_date = $("#to_date").val();
         alert($scope.from_date + ": " + $scope.to_date);
     }
     
     
    $scope.clearDates = function () {
        $scope.from_date = null;
        $scope.to_date = null;
    };
     

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
