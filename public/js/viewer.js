
var viewer = function() {
  ws = null;
  ws_host = "ws://192.168.1.17:8090/";
    //ws_host = "ws://10.137.16.6:8090/";
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
             'routing-key': "earth.europe.england.london"},
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
      // TODO: handle other type of message no just amqp for appending
      // console.log(msg.data);
    appendMessages([JSON.parse(msg.data)]);
  }

  function onWsClose(msg) {
    $('#ws_status').html('Websocket closed').css("background-color", "#FFC4C7");
    if (window.location.hash.split('/')[1] === "queue") {
        // only reconnect if viewing queue
        // TODO: put the ws connection in a module var
      delete viewer.ws;
      viewer.ws = new WebSocket(ws_host);
      viewer.ws.onmessage = onWsMessage;
      viewer.ws.onclose= onWsClose;
      viewer.ws.onopen = onWsOpen;
    }
  }

  function onWsOpen() {
    $('#ws_status').html('Websocket Opened').css("background-color", "#DCFFD6");
    var hash = window.location.hash.split('/').slice(1);    
    this.send(JSON.stringify({
        _action: "queue",
        args: {
            name: hash[1],
            exchange: $('#entries div:first').attr("exchange"),
            "routing-key": $('#entries div:first').attr("routing-key")}}));
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
          return {queues: {}};
        })
        app.log(store);
        store['queues'][queue_name] = {
          exchange: exchange,
          "routing-key": routing_key};
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
    
  function startQueue(queue_name, app) {
      // Start monitoring one queue
    viewer.ws = new WebSocket(ws_host);
    viewer.ws.onmessage = onWsMessage;
    viewer.ws.onclose= onWsClose;
    viewer.ws.onopen = onWsOpen;
    var queue = app.session("store") ["queues"][queue_name];
    $('#main').html('').haml(entries(queue_name, queue["routing-key"], queue["exchange"]));
    return ws
      //current_queue = pollNewMessages(queue_name);
  }
    /* Switch to monitoring a different queue without killing the
    websocket */
    function switchQueue(ws, queue_name, app) {
        var queue = app.session("store") ["queues"][queue_name];
        $('#main').html('').haml(entries(queue_name, queue["routing-key"], queue["exchange"]));
        $('title').html('Monitoring queue: ' + queue_name);
        $('#ws_status').html('Websocket Opened').css("background-color", "#DCFFD6");
        app.trigger("changed");
                    console.log("queue: "+ queue_name);
        ws.send(JSON.stringify({
            _action: "queue",
            args: {name: queue_name,
                   exchange: queue["exchange"],
                   "routing-key": queue["routing-key"]}}));
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
                /* if no websocket is open start new queue with new websocket */
                if (viewer.ws === null ||  viewer.ws.readyState != 1 ) {
                    app.ws = startQueue(params['queue'], app);
                    $('title').html('Monitoring queue: ' + params['queue']);
                    trigger("changed");
                    console.log("queue: "+ params['queue']);
                }
                else {
                    console.log("switching queue");
                    switchQueue(viewer.ws, params['queue'], app);
                }
                
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

        // bind(name, callback)
        bind('run-route', function(e, data) {
          /* Clean up if required */
            if (window.location.hash.split('/')[1] !== "queue") {
                console.log("closing ws", ws, viewer.ws);
                if (viewer.ws) {viewer.ws.close();}
             }
            //this.redirect('#/');
        });


        
    } } );
    return app;
  }

  function init(app) {
    initQueueButton('#newmsgButton');
    $('#sendmsgButton').live('click', sendMessage);
    $('#flushQueuesButton').live('click', function() {
        var store  = app.session('store');
        store['queues'] = {};
        app.session('store', store);
        $("#queue_list").html("");
    });
      $('#trimView').live('click', function() {
          $('#entries .message').slice(10).remove();
      });
    $('#sidebar').haml(controls);
    // Show previous queues
    $.each(app.session('store')['queues'], function (queue_name, queue) {
      $('#queue_list').haml(queue_button({queue_name: queue_name,
                                          exchange: queue['exchange'],
                                          "routing-key": queue["routing-key"]}));
    }  );
  }

  // reveal all things private by assigning public pointers
  return {
    init: init,
    initSammy: initSammy,
    current_queue: stopQueue,
    ws: ws
  };
}();
