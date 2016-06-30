/**
 * Controller file for Blackbox
 * @author: Hemed, Tarje
 * University of Bergen Library
 **/

'use strict';
var app = angular.module('marcus', ["checklist-model" , "ui.bootstrap", "settings", "ngAnimate", "ngRoute", "pascalprecht.translate"]);

//Configure URL rooting
app.config(["$routeProvider", function($routeProvider) {
    $routeProvider
        .when('/', {
            templateUrl : 'index.html',
            controller  : 'freeTextSearch'
        })
        .when('/s', {
            templateUrl : 'index.html',
            controller  : 'freeTextSearch'
        })
        .otherwise({
            redirectTo: 'index.html',
            controller  : 'freeTextSearch'
        });
}]);

//Enable HTML5 mode for browser history
app.config(function($locationProvider) {
    $locationProvider.html5Mode({
        enabled : true,
        requireBase: false,
        rewriteLinks : false });
});


/**
 * ==============================
 * i18n - angular-translate
 * ==============================
 **/
app.config(['$translateProvider', function ($translateProvider) {
  // add translation tables
  $translateProvider.useStaticFilesLoader({
    prefix: 'locales/locale-',
    suffix: '.json'
  });
  $translateProvider.preferredLanguage('no');
  $translateProvider.fallbackLanguage('en');
  $translateProvider.useSanitizeValueStrategy('sanitizeParameters');
  $translateProvider.forceAsyncReload(true);
  $translateProvider.useMessageFormatInterpolation();
  $translateProvider.addInterpolation('$translateMessageFormatInterpolation');
}]);
 
app.controller('Ctrl', ['$translate', '$scope', function ($translate, $scope) {
 
  $scope.changeLanguage = function (langKey) {
    $translate.use(langKey);
  };
}]);

/**
 * ==============================
 * Search Controller
 * ==============================
 **/
app.controller('freeTextSearch', function ($scope, $http, $location, mySetting) {
    //Declare variables
    $scope.isArray = angular.isArray;
    $scope.isString = angular.isString;
    $scope.sortOptions = [
        {value: '', displayName: 'Mest relevant'},
        {value: 'identifier:asc', displayName: 'Signatur ASC'},
        {value: 'identifier:desc', displayName: 'Signatur DES'},
        {value: 'available:asc', displayName: 'Tilgjengeliggort ASC'},
        {value: 'available:desc', displayName: 'Tilgjengeliggort DES'},
        {value: 'dateSort:asc', displayName: 'Skapt ASC'},
        {value: 'dateSort:desc', displayName: 'Skapt DES'}
    ];
    //Get parameters from the search URL
    var urlParams = $location.search();

    //Initialize scope variables to default values
    $scope.queryString = null;
    $scope.sortBy = '';
    $scope.settingFilter = [];
    $scope.selectedFilters = [];
    $scope.fromDate = null;
    $scope.toDate = null;
    $scope.currentPage = 1;
    $scope.pageSize = '10';
    $scope.from =  0;
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

    /**
     * Remove a filter in the list of selected filters
     */
    $scope.removeFilter = function(item) {
        if(item){
            remove($scope.selectedFilters, item);
            //After removing, execute search
            $scope.search();
        }
    }

    /**
     * Send requests to search servlet and prepare the view for rendering
     */
    $scope.search = function () {

        var defaultParams = {
            q: stripEmptyString($scope.queryString),
            index: mySetting.index,
            type: mySetting.type,
            from_date: stripEmptyString($scope.fromDate),
            to_date: stripEmptyString($scope.toDate),
            filter: $scope.selectedFilters,
            setting_filter : $scope.settingFilter,
            from: ($scope.currentPage - 1) * $scope.pageSize,
            size: $scope.pageSize,
            sort: stripEmptyString($scope.sortBy)
        };

        //Merge or extend default params with URL params. If parameter exists, URL param take precedence.
        var extendedParams = $.extend(defaultParams, urlParams);
        //Clear URL params once after use.
        urlParams = {};
        //Send ajax request to the server
        $http({
            method: 'GET',
            url: 'search?aggs=' + JSON.stringify(mySetting.facets),
            params: extendedParams
        }).then(function (response) {
            //Parameters that have been used to generate response.
            var responseParams = response.config.params;
                if(response.data) {
                    /*
                      Initialize the view by copying response params to scope variables.
                      By updating scope variables, the view is automatically updated, thanks to angular two-way binding.
                    */
                    if ("q" in responseParams) {
                        $scope.queryString = responseParams.q;
                    }
                    if ("from_date" in responseParams) {
                        $scope.fromDate = responseParams.from_date;
                    }
                    if ("to_date" in responseParams) {
                        $scope.toDate = responseParams.to_date;
                    }
                    if ("from" in responseParams) {
                        $scope.from = responseParams.from;
                        //Set current page
                        $scope.currentPage = ($scope.from/$scope.pageSize) + 1;
                    }
                    if ("size" in responseParams) {
                        $scope.pageSize = responseParams.size;
                    }
                    if ("sort" in responseParams) {
                        if (responseParams.sort) {
                            $scope.sortBy = responseParams.sort;
                        }
                    }
                    if ("filter" in responseParams) {
                        if (responseParams.filter instanceof Array) {
                            $scope.selectedFilters = responseParams.filter;
                        }
                        else {
                            if ($scope.selectedFilters.indexOf(responseParams.filter) === -1) {
                                $scope.selectedFilters.push(responseParams.filter);
                                responseParams.filter = $scope.selectedFilters;
                            }
                        }
                    }
                    //Assign response data to the results scope
                    $scope.results = response.data;
                    $scope.ready = true;
                }
                else{ //If no data in the response, means server is unavailable
                    $("#searchController").empty(); //Empty everything in search controller using jQuery
                    var alert = "<div id='alert-server-status' class='ui large red message' " +
                                      "<strong> Service is temporarily unavailable.</strong>" +
                                "</div>";
                    $("#searchController").append(alert);
                    console.log("No response from Elasticsearch. The server is unreachable");
                }
                //Set the response params to the browser history
                $location.search(responseParams);

               //Hide "index" from the browser URL params, "index" is unlikely to change.
                $location.search('index', null);

                //Hide "from" from the URL params, if it has a default value (0).
                if(responseParams.from === 0)
                    $location.search('from', null);

                //Hide "size" from the URL params, if it has a default value (10).
                if(parseInt(responseParams.size) === 10)
                    $location.search('size', null);

            //Hide loading
               $(".blackbox-loading").hide();
            });
    };

    //Send suggestion request to "suggestion" servlet for autocompletion.
    $scope.autoSuggest = function () {
        $http({
            method: 'GET',
            url: 'suggest',
            params: {
                q: $scope.queryString,
                index: mySetting.index
            }
        }).then(function (response) {
                $scope.suggestion_list = response.data;
        });
    }
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



