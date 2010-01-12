// jquery haml templates

message = function (data) {
  var d = new Date();
  return ["%.message",
          ["%table",
           ["%tbody",
            ["%tr",
             ["%td", {"colspan":"3", "style":"font-weight:bold; padding-bottom:5px;"},
              data['routing-key']]],
            ["%tr", 
             ["%td",
              ["%pre.prettyprint",
               JSON.stringify(JSON.parse(data.msg), null, 4)]], 
             ["%td.date", d.getHours() +":"+ d.getMinutes() +"."+ d.getSeconds()]]]]]
};

entries = function(title){
  return ["%#entries",
          ["%.queue_title",
           ["%h2", title]]]
}

controls = ["%#controls",
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
               ["%.fm-req",
                ["%label", {"for": "routing_key"}, "Routing Key"],
                ["%input", {name: "routing_key"}]],
               ["%.fm-req",
                ["%label", {"for": "exchange"}, "Exchange"],
                ["%input", {name: "exchange", value: "amq.topic"}]],
               ["%.fm-req",
                ["%label", {"for": "queue_name"}, "Queue Name"],
                ["%input", {name: "queue_name"}]],
               ["%.fm-multi",
                ["%.fm-opt", {style: "display: block;"},
                 ["%p", "Durable?"],
                 ["%label", {"for": "durable-yes"},
                  ["%input", {type: 'radio', value: "yes", checked: "checked", name: "durable"}],
                  "Yes"],
                 ["%label", {"for": "durable-no"},
                  ["%input", {type: 'radio', value: "no", name: "durable"}],
                  "No"]]],
               ["%br"],["%br"],
               ["%.fm-multi",
                ["%.fm-opt", {style: "display: block;"},
                 ["%p", "Auto_Delete?"],
                 ["%label", {"for": "auto_delete-yes"},
                  ["%input", {type: 'radio', value: "yes", name: "auto_delete"}],
                  "Yes"],
                 ["%label", {"for": "auto_delete-no"},
                  ["%input", {type: 'radio', value: "no", checked: "checked", name: "auto_delete"}],
                  "No"]]]],
              ["%#fm-submit.fm-req",
               ["%input", {type: "submit", value: "Create Queue"}]]]]
