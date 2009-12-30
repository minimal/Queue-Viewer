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
                       $('#entries').haml(message(data[0]));
                   },
                   
                   error: function () {
                       //called when there is an error
                       
                   }
               }); 
    }

    function initQueueButton(selector) {
        // Bind a button to poll the queue
        $(selector).click(function () {
                              getNewMessages(1);
                          });
    }

    function init() {
        initQueueButton('#newmsgButton');
    }

    // reveal all things private by assigning public pointers
    return {
        init: init
    };
}();