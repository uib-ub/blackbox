/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

// Hjelp, menu item in header-common
$('.help-popup')
  .popup({
    inline   : true,
    hoverable: true,
    position : 'bottom right',
    inverted: true,
    offset: -15,
    delay: {
      show: 300,
      hide: 800
    }
  })
;

// Menu item in header-common
$('.menu-popup')
  .popup({
    inline   : true,
    hoverable: true,
    position : 'bottom left',
    inverted: false,
    variation:'large',
    offset: 0,
    delay: {
      show: 300,
      hide: 800
    }
  })
;

// Sidebar
$('.ui.sidebar')
  .sidebar({
    context: $('.bottom.segment'),
    dimPage: false,
    closable: false
  })
  .sidebar('attach events', '#show-menu')
;
$('#show-menu').click(function() {
  $('.pusher').toggleClass('pusher-padding');
});

