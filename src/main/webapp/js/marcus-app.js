/**
 * Main file for the Business logic
 * @author: Hemed, Tarje, Simon
 * University of Bergen Library
 **/

'use strict';
var app = angular.module('marcus', [
    "checklist-model" , 
    "ui.bootstrap", 
    "settings", 
    "ngAnimate", 
    "ngRoute", 
    "pascalprecht.translate", 
    "monospaced.qrcode",
    ]);

//Configure URL rooting
app.config(["$routeProvider", function($routeProvider) {
    $routeProvider
        .when('/', {
            templateUrl : 'index.html',
            controller  : 'freeTextSearch'
        })
        .when('/search', {
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
app.controller('freeTextSearch', function ($scope, $http, $location, $timeout, mySetting) {
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

    /**
     * Set controller variables based on a set of URL params. Values
     * not specified in params are set to default. i.e. call
     * setVariables({}) to initialize to default;
     */
    var setVariables = function(params) {
        /*
          Initialize the view by copying response params to scope
          variables.  By updating scope variables, the view is
          automatically updated, thanks to angular two-way binding.
        */
        if (!params) {
            params = {};
        }

        var defaultState =  {
            queryString: null,
            sortBy: '',
            settingFilter: [],
            selectedFilters: [],
            fromDate: null,
            toDate: null,
            currentPage: 1,
            pageSize: '10',
            from: 0,
        };

        /*
          Variables are set to defaults one by one when necessary. If
          they are all initialized to default and then the parameters
          read, the gui will flicker between default and requested
          values.
         */
        if ("q" in params) {
            $scope.queryString = params.q;
            //When "q" is found in URL (on page load), copy it's value to the Typeahead input. 
            $("#search-input").typeahead('val',  $scope.queryString);
            //After copying the value, close the suggestion menu.
            $('#search-input').typeahead('close');
        } else {
            $scope.queryString = defaultState.queryString;
        }

        if ("from_date" in params) {
            $scope.fromDate = params.from_date;
        } else {
            $scope.fromDate = defaultState.fromDate;
        }

        if ("to_date" in params) {
            $scope.toDate = params.to_date;
        } else {
            $scope.toDate = defaultState.fromDate;
        }

        if ("size" in params) {
            $scope.pageSize = params.size;
        } else {
            $scope.pageSize = defaultState.pageSize;
        }

        if ("from" in params) {
            $scope.from = params.from;
        } else {
            $scope.from = defaultState.from;
        }
        //Set current page
        $scope.currentPage = ($scope.from/$scope.pageSize) + 1;

        if ("sort" in params) {
            $scope.sortBy = params.sort;
        } else {
            $scope.sortBy = defaultState.sortBy;
        }

        if ("filter" in params) {
            if (params.filter instanceof Array) {
                $scope.selectedFilters = params.filter;
            } else {
                $scope.selectedFilters = [];
                $scope.selectedFilters.push(params.filter);
                params.filter = $scope.selectedFilters;
            }
        } else {
            $scope.selectedFilters = defaultState.selectedFilters;
        }
    };

    /**
     * Remove some default values from URL parameters if present
     */
    var stripParams = function(params) {
        //Hide "index" from the URL params, "index" is unlikely to change.
        if ('index' in params) {
            delete params['index'];
        }
        
        //Hide "from" from the URL params, if it has a default value (0).
        if (params.from === 0) {
            delete params['from'];
        }

        //Hide "size" from the URL params, if it has a default value (10).
        if (parseInt(params.size) === 10) {
            delete params['size'];
        }

        return params;
    };

    /**
     * Generate a set of URL parameters based on scope variables
     */
    var generateParams = function() {
        return {
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
    };

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
     * Reset search
     */
    $scope.resetSearch = function() {
        setVariables({});
        $scope.triggerSearch();
    }

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
     * Add a filter to the list of selected filters
     */
    $scope.addFilter = function(item) {
        if (item && $.inArray(item, $scope.selectedFilters) === -1) {
            $scope.selectedFilters.push(item);
            $scope.search();
        }
    };

    /**
     * Is a filter already selected
     */
    $scope.isSelected = function(item) {
        if (item && $.inArray(item, $scope.selectedFilters) !== -1) {
            return true;
        }
        return false;
    }

    /**
     * Getters and setters for dates
     */
    $scope.getFromDate = function() {
        return $scope.fromDate;
    }
    $scope.setFromDate = function(date) {
        $scope.fromDate = date;
        $scope.search();
    }
    $scope.getToDate = function() {
        return $scope.toDate;
    }
    $scope.setToDate = function(date) {
        $scope.toDate = date;
        $scope.search();
    }

    /**
     * Reset the current page to 1 and then perform search
     */
    $scope.search = function() {
        $scope.currentPage = 1;
        $scope.triggerSearch();
    }
    
    var performRequest = function(params) {
        var responseParams = {};

        // show spinner
        $scope.searching = true;

        //Send ajax request to Blackbox
        $http({
            method: 'GET',
            url: 'http://localhost:8080/blackbox/search?aggs=' + JSON.stringify(mySetting.facets),
            params: params,
            cache: true,
        }).then(function (response) {
            //Parameters that have been used to generate response.
            setVariables(response.config.params);
            if(response.data) {
                //Assign response data to the results scope
                $scope.results = response.data;
                $scope.ready = true;
            } else { //If no data in the response, means server is unavailable
                $("#searchController").empty(); //Empty everything in search controller using jQuery
                var alert = "<div id='alert-server-status' class='ui large red message' " +
                    "<strong> Service is temporarily unavailable.</strong>" +
                    "</div>";
                $("#searchController").append(alert);
                console.log("No response from Elasticsearch. The server is unreachable");
            }
        });

        //Hide spinner. Timeout necessary because sometimes DOM is
        //updated quite a lot slower than the request is processed.
        $timeout(function() {$scope.searching = false}, 500);
    }

    /**
     * Trigger search based on scope variable values
     */
    $scope.triggerSearch = function () {
        $("html, body").animate({ scrollTop: 0 });

        //Set the response params to the browser history, this triggers the AJAX request
        $location.search(stripParams(generateParams()));

        // Changing location triggers actual search, done this way to
        // avoid double requests when navigating with back/forward.
        // Seems to be no Angular method to change location without
        // triggering locationChangeStart (https://github.com/angular/angular.js/issues/1699)

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

    /**
     * Execute search and scroll to the top of the page.
     */
    $scope.searchAndScrollToTop = function () {
        $scope.search();
        $window.scrollTo(0,0);
    }

    $scope.$on('$locationChangeStart', function(event, newUrl, oldUrl, newState, oldState) {
        if (newUrl !== oldUrl) {
            setVariables($location.search());
            performRequest(generateParams());
        }
    });


    /***********************************
     ** Perform the following on load **
     ***********************************/

    //Show loading gif
    $(".blackbox-loading").show();

    $scope.searching = true;
    $scope.ready = false;

    // init to defaults
    setVariables({});
    var defaultParams = generateParams();

    //Get parameters from the search URL
    var extendedParams = $.extend(defaultParams, $location.search());

    //Init variables
    setVariables(extendedParams);

    //Call this function on pageload
    performRequest(extendedParams);

    $scope.ready = true;

    //Hide loading gif (only after first request)
    $(".blackbox-loading").hide();
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
 * A filter.
 */
app.filter('iif', function () {
   return function(input, trueValue, falseValue) {
        return input ? trueValue : falseValue;
   };
});

/**
 * Returns the name of the filter type, to be used in styling
 * TODO modify to use mySetting (should be trivial)
 */
app.filter('filterType', function(mySetting) {
    var tags = mySetting.tags;
    return function(filter) {
        var field = filter.split('#')[0];
        for (var type in tags) {
            if (tags[type].queryField === field) {
                return tags[type].css;
            }
        }
        return '';
    }
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
 * A directive that validates input dates
**/
app.directive('dateRange', function() {
    return {
        restrict: 'E',
        replace: true,
        scope: true,
        template:
        '<div class="ui stackable middle aligned grid">' +
            '<div class="two wide column" style="padding-bottom: 0">' +
                '<label>{{ \'filters.fromDate\' | translate }}</label>' +
            '</div>' +
            '<div class="ui input fourteen wide column" ng-class="{error: !fromDate.valid}" style="padding-bottom: 0">' +
                '<input ng-model="fromDate.string" ng-change="updateFromDate()" type="text" placeholder="1950-01-01" title="Fra dato" ></input>' +
            '</div>' +
            '<div class="two wide column">' +
                '<label>{{ \'filters.toDate\' | translate }}</label>' +
            '</div>' +
            '<div class="ui input fourteen wide column" ng-class="{error: !toDate.valid}">' +
                '<input ng-model="toDate.string" ng-change="updateToDate()" type="text" placeholder="1959-12-31" title="Til dato"></input>' +
            '</div>' +
        '</div>',
        link: function(scope, element, attrs) {
            var useDate = function(input, setDate) {
                if (input.string.length === 0) {
                    input.valid = true;
                    setDate(input.string);
                    return;
                }
                input.string = input.string.replace(/\./, '-');

                // If we are in the process of typing a valid date, no error
                if (input.string.match(/(^\d+)(-\d{0,2}){0,2}$/)) {
                    input.valid=true;
                } else {
                    input.valid = false;
                }

                // perform search once we have YYYY, YYYY-MM or YYYY-MM-DD
                if (input.string.match(/(^\d{4})(-\d{2}){0,2}$/)) {
                    setDate(input.string);
                }
            }

            // constructed as an object to allow pass by reference
            scope.fromDate = {valid: true, string: scope.$parent.getFromDate()};
            scope.toDate = {valid: true, string: scope.$parent.getToDate()};

            scope.updateFromDate = function() {
                useDate(scope.fromDate, scope.$parent.setFromDate);
            }

            scope.updateToDate = function() {
                useDate(scope.toDate, scope.$parent.setToDate);
            }
        }
    };
});


/**
 * A directive that change the view of the facets based on whether they are selected or not.
**/
app.directive('facet', function() {
    return {
        restrict: 'E',
        replace: true,
        scope: {
            id: "@",
            value: "@",
            key: "@",
            count: "@",
        },
        template:
        '<span ng-class="{selected: isSelected}" ng-click="toggle()">' +
            '<a>' +
                '<i class="icon" ng-class="{check: isSelected, square: isSelected, add: !isSelected}"></i>' +
            '</a>' +
            '{{key}} ' +
            '<span ng-if="count != 0" class="doc-count">({{count}})</span>' +
        '</span>',
        link: function(scope, element, attrs) {
            var update = function() {
                scope.isSelected = scope.$parent.isSelected(scope.value);
            };
            update();

            scope.toggle = function() {
                if (scope.$parent.isSelected(scope.value)) {
                    scope.$parent.removeFilter(scope.value);
                } else {
                    scope.$parent.addFilter(scope.value);
                }
                update();
            };
        }
    };
});

app.directive('tagController', function(mySetting) {
    return {
        restrict: 'A',
        replace: true,
        transclude: true,
        template: '<div ng-transclude></div>',
        controller: function($scope) {
            var doc = $scope.doc;
            var tags = mySetting.tags;

            var getFilter = function(field, value) {
                return $scope.getCheckedValue(field, value);
            };

            var toArray = function (stringOrArray) {
                if (angular.isString(stringOrArray)) {
                    var tmp = [];
                    tmp.push(stringOrArray);
                    return tmp;
                } else if (angular.isArray(stringOrArray)) {
                    return stringOrArray;
                }
                return null;
            };

            var addFilter = function(field, value) {
                return $scope.addFilter(getFilter(field, value));
            };

            var removeFilter = function(field, value) {
                return $scope.removeFilter(getFilter(field, value));
            };

            var filterSelected = function(field, value) {
                return $scope.isSelected(getFilter(field, value));
            };

            this.getTagIndex = function(tagType) {
                for (var i in tags) {
                    console.log(tags[i].label + " " + tagType);
                    if (tags[i].label == tagType) {
                        console.log(tags[i]);
                        return i;
                    }
                }
                return null;
            };

            this.getTags = function(tagType) {
                if (tagType in tags) {
                    return toArray(doc._source[tags[tagType].responseField]);
                }
                return null;
            };

            this.hasTags = function(tagType) {
                if (tagType in tags) {
                    return this.getTags(tagType) !== null;
                }
                return false;
            };

            this.toggle = function(tagType, tag) {
                var field;
                if (tagType in tags) {
                    field = tags[tagType].queryField;
                    if (filterSelected(field, tag)) {
                        removeFilter(field, tag);
                    } else {
                        addFilter(field, tag);
                    }
                }
            };

            this.isSelected = function(tagType, tag) {
                if (tagType in tags) {
                    return filterSelected(tags[tagType].queryField, tag);
                }
                return false;
            };

            this.getIcon = function(tagType) {
                if (tagType in tags) {
                    return tags[tagType].icon;
                }
                return "";
            };

            this.getClass = function(tagType) {
                if (tagType in tags) {
                    return tags[tagType].css;
                }
                return "";
            }
        }
    };
});

app.directive('selectable', function() {
    return {
        restrict: 'E',
        replace: true,
        require: '^?tagController',
        transclude: true,
        scope: {
            type: "@",
        },
        template: '<div class="item selectable" ng-class="tagClass" ng-show="hasTags">' +
            '<i ng-if="hasTags" class="ui icon" ng-class="iconClass"></i>' +
            '<div class="content" ng-if="hasTags">' +
            '<a ng-repeat="tag in tags" ng-click="toggle(tag)" ng-class="{selected: isSelected(tag)}">' +
               '{{tag}}' +
               '<i class="ui delete icon" ng-show="isSelected(tag)"></i>' +
            '</a>' +
            '</div>',
        link: function(scope, element, attr, tagController) {
            scope.hasTags = tagController.hasTags(scope.type);
            scope.tags = tagController.getTags(scope.type);
            scope.iconClass = tagController.getIcon(scope.type);
            scope.tagClass = tagController.getClass(scope.type);
            scope.isSelected = function(tag) {
                return tagController.isSelected(scope.type, tag);
            }
            scope.toggle = function(tag) {
                tagController.toggle(scope.type, tag);
            }
        }
    };
});
