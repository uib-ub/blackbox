
$(function () {

    //Configure from_date
    $("#from_date").datepicker({
        showOn: "both",
        //buttonImage: "img/calendar.gif",
        buttonText: '<span class="glyphicon glyphicon-calendar" style="border:0"></span>',
        changeMonth: true,
        changeYear: true,
        yearRange: "1700:+1",
        numberOfMonths: 1,
        showButtonPanel: true,
        onClose: function (selectedDate) {
            $("#to_date").datepicker("option", "minDate", selectedDate);
        }
    });

    //Configure to_date
    $("#to_date").datepicker({
        showOn: "both",
        //buttonImage: "img/calendar.gif",
        buttonText: '<span class="glyphicon glyphicon-calendar" style="border:0"></span>',
        changeMonth: true,
        changeYear: true,
        yearRange: "1700:+1",
        numberOfMonths: 1,
        showButtonPanel: true,
        onClose: function (selectedDate) {
            $("#from_date").datepicker("option", "maxDate", selectedDate);
        }
    });

    //Set defaults for Norwegian Calender
    var defaults = {
        closeText: "Lukk",
        prevText: "&#xAB;Forrige",
        nextText: "Neste&#xBB;",
        currentText: "I dag",
        monthNames: ["januar", "februar", "mars", "april", "mai", "juni", "juli", "august", "september", "oktober", "november", "desember"],
        monthNamesShort: ["jan", "feb", "mar", "apr", "mai", "jun", "jul", "aug", "sep", "okt", "nov", "des"],
        dayNamesShort: ["søn", "man", "tir", "ons", "tor", "fre", "lør"],
        dayNames: ["søndag", "mandag", "tirsdag", "onsdag", "torsdag", "fredag", "lørdag"],
        dayNamesMin: ["sø", "ma", "ti", "on", "to", "fr", "lø"],
        weekHeader: "Uke",
        dateFormat: 'yy-mm-dd',
        firstDay: 1,
        isRTL: false,
        showOptions: {direction: "up"},
        //showMonthAfterYear: true,
        yearSuffix: ""
    };

    //Apply default settings to all datepicker instances
    $.datepicker.setDefaults(defaults);

    //This is a workaround for getting "today" button working.
    var _gotoToday = $.datepicker._gotoToday;
    $.datepicker._gotoToday = function (a) {
        var target = $(a);
        var inst = this._getInst(target[0]);
        _gotoToday.call(this, a);
        $.datepicker._selectDate(a, $.datepicker._formatDate(inst, inst.selectedDay, inst.selectedMonth, inst.selectedYear));
    };
});