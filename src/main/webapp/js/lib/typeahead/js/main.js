$(document).ready(function () {
    var engine, remoteHost, template, empty;

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
    engine.get('1090217586', '58502284', '10273252', '24477185')

    function engineWithDefaults(q, sync, async) {
        if (q === '') {
            sync(engine.get('1090217586', '58502284', '10273252', '24477185'));
            async([]);
        }

        else {
            engine.search(q, sync, async);
        }
    }




    //============== Tring out ================       

    var engine2 = new Bloodhound({
        name: 'suggest',
        datumTokenizer: Bloodhound.tokenizers.whitespace,
        queryTokenizer: Bloodhound.tokenizers.whitespace,
        prefetch: 'suggest?q=blabla',
        cache: false,
        remote: {
            url: 'suggest?q=%QUERY',
            wildcard:'%QUERY'
            /**transform : function(response){
                return JSON.parse(response);
            }**/
        }
    });

    /**function engineWithDefaults2(q, sync, async) {
     if (q === '') {
     sync(['Hemed', 'Billed Samlingen', 'Hello World']);
     async([]);
     }
     
     else {
     engine2.search(q, sync, async);
     alert(JSON.stringify(engine2.search(q, sync, async)));
     }
     }**/

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
        name: 'suggest',
        source: engine2
    }).on('typeahead:asyncrequest', function () {
        $('.Typeahead-spinner').show();
    })
            .on('typeahead:asynccancel typeahead:asyncreceive', function () {
                $('.Typeahead-spinner').hide();
            });


    /**$('#demo-input').typeahead({
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
     source: engine2
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