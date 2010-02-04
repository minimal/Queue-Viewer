
var viewer = function() {
  ws = null;
  
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
            // $.each(data, function (i, msg) { 
            //   msg = $('<p/>').haml(message(msg)).html()
            //   $(msg).insertAfter('.queue_title')
            //     .animate({height: "toggle"}, 0).animate({height: "toggle"}, {queue: true});
            // });

            function make_caller_with_tail(msgs) {
              var tail = msgs.slice(1);
              return function() {
                addmsgs(tail);
              }
            }
            function addmsgs(msgs) {
              if (msgs.length === 0) {
                return
              }
              msg = $('<p/>').haml(message(msgs[0])).html()
              $(msg).insertAfter('.queue_title')
                .animate({height: "toggle"}, 0).animate({height: "toggle"},
                                                        make_caller_with_tail(msgs));
            }
            addmsgs(data);
            prettyPrint();
              //console.log("got data");
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
  function appendMessages(msgs) {
    function make_caller_with_tail(msgs) {
      var tail = msgs.slice(1);
      return function() {
        addmsgs(tail);
      }
    }
    function addmsgs(msgs) {
      if (msgs.length === 0) {
        return
      }
      msg = $('<p/>').haml(message(msgs[0])).html()
      $(msg).insertAfter('.queue_title')
        .animate({height: "toggle"}, 0).animate({height: "toggle"},
                                                make_caller_with_tail(msgs));
    }
      //console.log(msgs);
    addmsgs(msgs);
    prettyPrint();
  }
  function onWsMessage (msg) {
    console.log(msg.data);
    appendMessages([JSON.parse(msg.data)]);
  }

  function onWsClose(msg) {
    $('#ws_status').html('Websocket closed').css("background-color", "#FFC4C7");
    if (window.location.hash.split('/')[1] === "queue") {
        // only reconnect if viewing queue
        // TODO: put the ws connection in a module var
      ws = new WebSocket("ws://192.168.1.17:8090/");
      ws.onmessage = onWsMessage;
      ws.onclose= onWsClose;
      ws.onopen = onWsOpen;
    }
  }

  function onWsOpen() {
    $('#ws_status').html('Websocket Opened').css("background-color", "#DCFFD6");
     this.send(JSON.stringify({hash:window.location.hash.split('/').slice(1)}));
  }
  
  
  function setupQueue(routing_key, exchange, queue_name, app) {
    $.ajax({
      url: "queue",
      type: "PUT",
      dataType: "json",
      data: {exchange: exchange,
             'routing-key': routing_key,
             "queue-name": queue_name},
      success: function (data) {
        //called when successful
        console.log(data);
        app.setLocation("#/");
        $('#queue_list').haml(queue_button({queue_name: queue_name}));
        var store  = app.session('store', function() {
          return {queues: []};
        })
        app.log(store);
        store['queues'].push(queue_name);
        app.session('store', store);
        app.log("The current cart: ", store);
        
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
    ws = new WebSocket("ws://192.168.1.17:8090/");
    ws.onmessage = onWsMessage;
    ws.onclose= onWsClose;
    ws.onopen = onWsOpen;
    $('#main').html('').haml(entries(queue_name));
    return ws
      //current_queue = pollNewMessages(queue_name);
  }

  function stopQueue() {
    console.log(current_queue);
  }
    
    //write new sammy application
    function initSammy() {
      var app = $.sammy(function() {
      with(this) {
        element_selector = '#main';
          //use(Sammy.Template);
        use(Sammy.Session);
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
                  app.ws = startQueue(params['queue']);
                  $('title').html('Monitoring queue: ' + params['queue']);
                   trigger("changed");
                //console.log("queue: "+ params['queue']);
              log(app.getLocation());              
            });
          }
        });

        get('#/new_queue', function(context) {
          with(this) {
            $('#main').html('');
            $('title').html('New queue');
            $('#main').haml(new_queue);
            trigger("changed"); // bind form to app
          }
        });
        
        put('#/new_queue', function(context) {
          with(this) {
            log("posting:" + context);
            setupQueue(params["routing_key"], params["exchange"], params["queue_name"], app);
            
            //app.setLocation("#/");
            //return false
          }
        });

        var store  = this.session('store', function() {
          return {queues: []};
        })
          
        
    } } );
    return app;
  }

  function init(app) {
    initQueueButton('#newmsgButton');
    $('#sendmsgButton').live('click', sendMessage);
    $('#flushQueuesButton').live('click', function() {
        var store  = app.session('store');
        store['queues'] = [];
        app.session('store', store);
        $("#queue_list").html("");
    });
    $('#sidebar').haml(controls);
    // Show previous queues
    $.each(app.session('store')['queues'], function (i, queue) {
      $('#queue_list').haml(queue_button({queue_name: queue}));
    }  );
  }

  // reveal all things private by assigning public pointers
  return {
    init: init,
    initSammy: initSammy,
    current_queue: stopQueue
  };
}();
