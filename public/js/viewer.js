
var viewer = function() {
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
            $('#entries').haml(message(msg));
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
    $(selector).click(function () {
      var queue = window.location.hash.split('/')[2];                       console.log(queue);
      getNewMessages(queue);
    });
  }
    
  function startQueue(routing_key) {
      // Start monitoring one queue
      $('#main').html('').haml(entries);

  }
    
    //write new sammy application
    function initSammy() {
      var app = $.sammy(function() {
      with(this) {
          console.log('initing sammy');
          get('#/', function() {
              $('#main').html('');    
              });
        //corresponds to routes such as #/section/1	
        get('#/queue/:queue', function() {
                      with(this) {
            $(function() {
              // do something
                  startQueue(params['queue']);
                  $('title').html('Monitoring queue: ' + params['queue']);
              console.log("queue: "+ params['queue']);
            });
          }
        });
      }
    });
    return app;
  }

  function init() {
    initQueueButton('#newmsgButton');
    $('#sendmsgButton').click(sendMessage);  
  }

  // reveal all things private by assigning public pointers
  return {
    init: init,
    initSammy: initSammy
  };
}();