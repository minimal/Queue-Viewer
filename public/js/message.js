// jquery haml templates

message = function (data) {
  var d = new Date();
  return ["%div.message",
          ["%table",
           ["%tbody",
            ["%tr",
             ["%td", data.msg],
             ["%td.date", d.getHours() +":"+ d.getMinutes() +"."+ d.getSeconds()]]]]]
};

entries = function(title){
  return ["%div#entries",
          ["%div.queue_title",
           ["%h2", title]]]
}

controls = ["%div#controls",
            ["%h2", 'Actions'],
            ["%ul",
             // ["%li#newmsgButton.action", {style: "cursor: pointer;"},
             //  "Get new messages"],
             ["%li#sendmsgButton.action", {style: "cursor: pointer;"},
              "Send new message"],
             ["%li.action",
              ["%a", {href: "#/new_queue"},
               "New Queue"]]
            ],
            ["%h2", "Availible Queues"],
            ["%ul#queue_list"]]

queue_button = function(data) {
  return ["%li.action",
          ["%a#newqButton.action", {href: "#/queue/" + data.queue_name},
           data.queue_name]]
}

new_queue = ["%h2", "Create a new queue",
             ["%form", {method: 'put', action: '#/new_queue'},
              ["%fieldset",
               ["%legend", "Main Information"],
               ["%div.fm-req",
                ["%label", {"for": "routing_key"}, "Routing Key"],
                ["%input", {name: "routing_key"}]],
               ["%div.fm-req",
                ["%label", {"for": "exchange"}, "Exchange"],
                ["%input", {name: "exchange", value: "amq.topic"}]],
               ["%div.fm-req",
                ["%label", {"for": "queue_name"}, "Queue Name"],
                ["%input", {name: "queue_name"}]],
               ["%div.fm-multi",
                ["%div.fm-opt", {style: "display: block;"},
                 ["%p", "Durable?"],
                 ["%label", {"for": "durable-yes"},
                  ["%input", {type: 'radio', value: "yes", checked: "checked", name: "durable"}],
                  "Yes"],
                 ["%label", {"for": "durable-no"},
                  ["%input", {type: 'radio', value: "no", name: "durable"}],
                  "No"]]],
               ["%br"],["%br"],
               ["%div.fm-multi",
                ["%div.fm-opt", {style: "display: block;"},
                 ["%p", "Auto_Delete?"],
                 ["%label", {"for": "auto_delete-yes"},
                  ["%input", {type: 'radio', value: "yes", name: "auto_delete"}],
                  "Yes"],
                 ["%label", {"for": "auto_delete-no"},
                  ["%input", {type: 'radio', value: "no", checked: "checked", name: "auto_delete"}],
                  "No"]]]],
              ["%div#fm-submit.fm-req",
               ["%input", {type: "submit", value: "Create Queue"}]]]]
