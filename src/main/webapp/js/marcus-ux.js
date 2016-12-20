/* 
 * Disse trigger ikke på includes!
 */

$('.ui.main-nav-dropdown.dropdown').dropdown({
    transition: 'scale',
});

$('.ui.doctype.dropdown').dropdown({
    transition: 'scale',
    context: '.search-hits',
    direction: 'downward'
});

/*
 * Lager mindre header når man scroller ned, css i menu.overrides
 */
$(function() {
    $(window).scroll(function() {
        var scroll = $(window).scrollTop();
        if (scroll >= 30) {
            $("#header").addClass('smaller');
        } else {
            $("#header").removeClass("smaller");
        }
    });
});

$('.search-help.ui.modal')
    .modal('attach events', '.search-help-button', 'show')
;