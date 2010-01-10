
var viewer = function() {
  var current_queue = null;
  function smartPoller(wait, poller) {
      //var do_reset = false
    if ($.isFunction(wait)) {
      poller = wait
      wait = 1000;
      max = 8000;
    }
    
    (function startPoller(action) {
        //console.log(wait);
      if (action === 'stop') {
        console.log('stopping');
         
      }
      if (wait < max) {
        wait = wait * 1.3;
      }
      else {
        wait = max;
      }
      if (action === 'reset') {
        console.log("reseting");
        wait = 1000; // repeating polls should have this
          //do_reset = false;
      }
      if (typeof console !== "undefined") {
        console.debug(wait);
      }
      if (action !== 'stop') {
        console.debug("pausing, " + action);
        setTimeout(function() {
          poller.call(this, startPoller);
        }, wait)
      }
    })()
  }

  function pollNewMessages(queue_id) {
    smartPoller(function(retry) {

      if (window.location.hash.split('/')[2] !== queue_id) {
        console.log("not current");
        retry('stop');
        return
      } 
      $.ajax({
        url: "queue/" + queue_id,
        type: "GET",
        dataType: "json",
        data: {},
        complete: function () {
          //called when complete
        },
        success: function (data) {
          //called when successful
          if (data.length) {
            $.each(data, function (i, msg) { 
              $('.queue_title').after($('<p/>').haml(message(msg)).html());
            });
            console.log("got data");
            retry('reset');
          }
          else {
              //console.log("retrying");
            retry();
          }
        },
        error: function () {
          //called when there is an error
        }   
      }
            );})}
                


  function getNewMessages(queue_id) {
    $.ajax({
      url: "queue/" + queue_id,
      type: "GET",
      dataType: "json",
      data: {},
      complete: function () {
        //called when complete
        
      },
      success: function (data) {
        //called when successful
        if (data) {
          $.each(data, function (i, msg) { 
            $('.queue_title').after($('<p/>').haml(message(msg)).html());
          });
        }
      },
      error: function () {
        //called when there is an error
       }   
           }
    );}

  function sendMessage() {
      $.ajax({
      url: "message",
      type: "POST",
      dataType: "json",
      data: {exchange: "amq.topic",
             'routing-key': "boo"},
      success: function (data) {
        //called when successful
     
      },
      error: function () {
        //called when there is an error
       }   
           }
    );
  }

  function initQueueButton(selector) {
    // Bind a button to poll the queue
    $(selector).live('click', function () {
      var queue = window.location.hash.split('/')[2];
        //console.log(queue);
      getNewMessages(queue);
    });
  }
    
  function startQueue(queue_name) {
      // Start monitoring one queue
    $('#main').html('').haml(entries(queue_name));
    current_queue = pollNewMessages(queue_name);
  }

  function stopQueue() {
    console.log(current_queue);
  }
    
    //write new sammy application
    function initSammy() {
      var app = $.sammy(function() {
      with(this) {
          //console.log('initing sammy');
          get('#/', function() {
              $('#main').html('');
              // $('#sidebar').haml(controls);
              });
        //corresponds to routes such as #/section/1	
        get('#/queue/:queue', function() {
                      with(this) {
            $(function() {
              // do something
                  startQueue(params['queue']);
                  $('title').html('Monitoring queue: ' + params['queue']);
                //console.log("queue: "+ params['queue']);
            });
          }
        });
      }
    });
    return app;
  }

  function init() {
    initQueueButton('#newmsgButton');
    $('#sendmsgButton').live('click', sendMessage);
    $('#sidebar').haml(controls);
  }

  // reveal all things private by assigning public pointers
  return {
    init: init,
    initSammy: initSammy,
    current_queue: stopQueue
  };
}();
