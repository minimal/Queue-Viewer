// jquery haml templates

function truncate(text, max) {
  if (text.length > max) {
    return text.slice(0, max) + "..."
  }
  else {
    return text
  }
}

function make_pretty(text) {
  try {
    return JSON.stringify(JSON.parse(text), null, 4)
  } catch (e) { // if invalid or not JSON
      //console.log("error: " + e + "msg: " + text);
    return text
  }
}

message = function (data) {
  var d = new Date();
  minutes = d.getMinutes();
  seconds = d.getSeconds();
  if (minutes < 10) {
    minutes = "0" + minutes;
  }
  if (seconds < 10) {
    seconds = "0" + seconds;
  }
  return ["%.message",
          ["%table",
           ["%tbody",
            ["%tr",
             ["%td.key", {"colspan":"3", 
                          "title": data['routing-key']},
              truncate(data.envelope['routing-key'], 40), " | ", data.envelope.exchange,
              " | Tag: ", data.envelope.tag, data.envelope.isRedeliver && ", RD" || "",
              ["%span.trafficlights", {title: "Mark Message"}, ["%span.unchecked"]],
              ["%span.date", d.getHours() +":"+ minutes +"."+ seconds]]],
            ["%tr", 
             ["%td",
              ["%pre.prettyprint",
               make_pretty(JSON.stringify(data.msg))]], 
             ]]]]
};

entries = function(title, routing_key, exchange){
  return ["%#entries",
          ["%.queue_title", {"routing-key": routing_key, exchange: exchange},
           ["%h2", title, " key: ", routing_key, " exchange: ", exchange],
           ["%p#ws_status", {style: "padding: 5px"}]]]
}

controls = ["%#controls",
            ["%h2", 'Actions'],
            ["%ul",
             ["%li#sendmsgButton.action", 
              "Send test message"],
             ["%li#flushQueuesButton.action",
              "Remove queues from session"],
             ["%li.action",
              ["%a", {href: "#/new_queue"},
               "New Queue"]],
             ["%li#trimView.action", { title: "Trim to most recent 10 messages"},
             "Trim View"]
            ],
            ["%h2", "Examples"],
            ["%ul",
             ["%li.action",
              ["%a", {href: "#/new_queue/pirc.topic/admin.publishing.#"},
              "Publishing"]],
            ["%li.action",
              ["%a", {href: "#/new_queue/pirc.topic/admin.workplanner.#"},
              "Workplanner"]],
            ["%li.action",
              ["%a", {href: "#/new_queue/pirc.topic/admin.#"},
              "All Admin"]],
            ["%li.action",
              ["%a", {href: "#/new_queue/amq.topic/#"},
              "All Test"]]],
            ["%h2", "Available Queues"],
            ["%ul#queue_list"]]

queue_button = function(data) {
  return ["%li.action",
          ["%a#newqButton.action", {href: "#/queue/" + data.queue_name,
                                    exchange: data.exchange,
                                    "routing-key": data['routing-key']},
           data.queue_name, ]]
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
                ["%.fm-opt", {style: "display: none;"},
                 ["%p", "Durable?"],
                 ["%label", {"for": "durable-yes"},
                  ["%input", {type: 'radio', value: "yes", checked: "checked", name: "durable"}],
                  "Yes"],
                 ["%label", {"for": "durable-no"},
                  ["%input", {type: 'radio', value: "no", name: "durable"}],
                  "No"]]],
               ["%br"],["%br"],
               ["%.fm-multi",
                ["%.fm-opt", {style: "display: none;"},
                 ["%p", "Auto_Delete?"],
                 ["%label", {"for": "auto_delete-yes"},
                  ["%input", {type: 'radio', value: "yes", name: "auto_delete"}],
                  "Yes"],
                 ["%label", {"for": "auto_delete-no"},
                  ["%input", {type: 'radio', value: "no", checked: "checked", name: "auto_delete"}],
                  "No"]]]],
              ["%#fm-submit.fm-req",
               ["%input", {type: "submit", value: "Create Queue"}]]]]
