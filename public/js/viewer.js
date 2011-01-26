
var viewer = function() {
  ws = null;
  ws_host = "ws://" + window.location.hostname + ":8090/"
  var current_queue = null;

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
    addmsgs(msgs);
    prettyPrint();
    $(".prettyprint").removeClass('prettyprint')
  }
  function onWsMessage (msg) {
      // TODO: handle other type of message no just amqp for appending
    var parsed_msg = JSON.parse(msg.data);
    console.log(msg);
    if (parsed_msg.method === "amqp-msg") {
      appendMessages([parsed_msg.params]);
    }
    else {
      console.log(msg.data);
      this.send(JSON.stringify({msg: "ack"}));
    }
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
        "jsonrpc": "2.0",
        method: "start-queue",
        params: {
            name: hash[1],
            exchange: $('#entries div:first').attr("exchange"),
            "routing-key": $('#entries div:first').attr("routing-key")}}));
  }
  
  
  function setupQueue(routing_key, exchange, queue_name, app, callback) {
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
        if (callback) {callback();};
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
  }
    // Switch to monitoring a different queue without killing the websocket
    function switchQueue(ws, queue_name, app) {
        var queue = app.session("store") ["queues"][queue_name];
        $('#main').html('').haml(entries(queue_name, queue["routing-key"], queue["exchange"]));
        $('title').html('Monitoring queue: ' + queue_name);
        $('#ws_status').html('Websocket Opened').css("background-color", "#DCFFD6");
        app.trigger("changed");
                    console.log("queue: "+ queue_name);
        ws.send(JSON.stringify({
            method: "start-queue",
            params: {name: queue_name,
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
        use(Sammy.Session); 
          get('#/', function() {
              $('#main').html(''); 
              });

        
        //corresponds to routes such as #/section/1	
        get('#/queue/:queue', function() {
                      with(this) {
            $(function() {
                // if no websocket is open start new queue with new websocket 
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
            setupQueue(params["routing_key"], params["exchange"], params["queue_name"], app,
                       function() {redirect('#/');} ); 
          }
        });

        get('#/new_queue/:exchange/:key', function(context) {
          with(this) {
              var queue_name = "queue" + Math.floor(Math.random()*1000000);
              setupQueue(params["key"], params["exchange"], queue_name, app,
                         function() {redirect('#/queue/' + queue_name);} ); 
          }
        })

        var store  = this.session('store', function() {
          return {queues: {}};
        })

        after( function(e, data) {
          // Clean up if required 
            if (window.location.hash.split('/')[1] !== "queue") {
                console.log("closing ws", ws, viewer.ws);
                if (viewer.ws) {viewer.ws.close();}
             } 
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

    $(".trafficlights span").live('click', function() {
      $(this).toggleClass('running');
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
