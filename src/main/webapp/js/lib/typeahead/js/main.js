/**
 * This is a Javascript file for getting completion suggestions.
 * Please note that, suggestion is executed accross indices,
 * therefore index must be explicitly specified in
 * the remote URL.
**/

$(document).ready(function () {
    /**
     *===============================================================
     *Typeahead and Bloodhound settings for Marcus suggestion engine
     *=============================================================== 
     */

    //Initialize suggestion engine
    var marcusSuggestionEngine = new Bloodhound({
        name: 'suggestion',
        datumTokenizer: Bloodhound.tokenizers.whitespace,
        queryTokenizer: Bloodhound.tokenizers.whitespace,
        //prefetch: 'suggestion?q=m&index=admin',
        limit: 10,
        remote: {
            /*You must manually append indices here*/
            url: 'suggest?q=%QUERY&index=admin-test',
            wildcard: '%QUERY',
            rateLimitWait: 20
        }
    });

    //Set default suggestion
    function marcusSuggestionEngineWithDefaults(q, sync, async) {
        if (q === '') {
            /**sync(['Hemed Al Ruwehy', 'Tarje Lavik', 'Øyvind Gjesdal', 'Marcus']);**/
            sync([]);
            //async([]);
            $('.Typeahead-search-icon').hide();
        } else {
            marcusSuggestionEngine.search(q, sync, async);
        }
    }

    //Put settings to Typeahead
    $('#search-input').typeahead({
        hint: $('.Typeahead-hint'),
        menu: $('.Typeahead-menu'),
        minLength: 0,
        highlight: true,
        classNames: {
            open: 'is-open',
            empty: 'is-empty',
            cursor: 'is-active',
            suggestion: 'Typeahead-suggestion ProfileCard',
            selectable: 'Typeahead-selectable'
        }
    },
            {
                name: 'suggestion',
                source: marcusSuggestionEngineWithDefaults,
                limit: 5
                        /**templates: {
                         suggestion: '<div>{{value}}</div>'
                         }**/
            })
            .on('typeahead:asyncrequest', function () {
                //$('.Typeahead-spinner').show();
                $('.Typeahead-search-icon').show();
            })
            .on('typeahead:asynccancel typeahead:asyncreceive', function () {
                //$('.Typeahead-spinner').hide();
            })
            //When suggestion value is selected, update angular contoller with new value
            .on('typeahead:select', function (event, suggestionValue) {
                var angularScope = angular.element($('#searchController')).scope();
                angularScope.queryString = suggestionValue;
                //Execute search
                angularScope.search();
                angularScope.$apply();
            })
            //When Enter key is clicked, close the suggestion box.
            .on('keyup', function (event) {
                if (event.which === 13) {
                    $('#search-input').typeahead('close');
                }
            });





    //============================
    // Examples usage of Typeahead
    //============================
    /** var engine, remoteHost, template, empty;
     $.support.cors = true;

     remoteHost = 'https://typeahead-js-twitter-api-proxy.herokuapp.com';
     template = Handlebars.compile($("#result-template").html());
     empty = Handlebars.compile($("#empty-template").html());

     engine = new Bloodhound({
        identify: function (o) {
            return o.id_str;
        },
        queryTokenizer: Bloodhound.tokenizers.whitespace,
        datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name', 'screen_name'),
        dupDetector: function (a, b) {
            return a.id_str === b.id_str;
        },
        prefetch: remoteHost + '/demo/prefetch',
        remote: {
            url: remoteHost + '/demo/search?q=%QUERY',
            wildcard: '%QUERY'
        }
    });

     // ensure default users are read on initialization
     engine.get('1090217586', '58502284', '10273252', '24477185');

     function engineWithDefaults(q, sync, async) {
        if (q === '') {
            sync(engine.get('1090217586', '58502284', '10273252', '24477185'));
            async([]);
        } else {
            engine.search(q, sync, async);
        }
    }

    $('#demo-input').typeahead({
     hint: $('.Typeahead-hint'),
     menu: $('.Typeahead-menu'),
     minLength: 0,
     highlight: true,
     classNames: {
     open: 'is-open',
     empty: 'is-empty',
     cursor: 'is-active',
     suggestion: 'Typeahead-suggestion',
     selectable: 'Typeahead-selectable'
     }
     }, {
     source: engineWithDefaults
     displayKey: 'screen_name',
     templates: {
     suggestion: template,
     empty: empty
     }
     })
     .on('typeahead:asyncrequest', function () {
     $('.Typeahead-spinner').show();
     })
     .on('typeahead:asynccancel typeahead:asyncreceive', function () {
     $('.Typeahead-spinner').hide();
     });**/


});