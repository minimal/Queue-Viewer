// jquery haml templates

message = function (data) {
  var d = new Date();
  return ["%div.message",
          ["%table",
           ["%tbody",
            ["%tr",
             ["%td", data.msg],
             ["%td.date", d.toTimeString()]]]]]
};

entries = function(title){
  return ["%div#entries",
          ["%div.queue_title",
           ["%h2", title]]]
}

controls = ["%div#controls",
            ["%h2", 'Actions'],
            ["%ul",
             ["%li#newmsgButton.action", {style: "cursor: pointer;"},
              "Get new messages"],
             ["%li#sendmsgButton.action", {style: "cursor: pointer;"},
              "Send new message"],
            ],
            ["%h2", "Availible Queues"],
            ["%ul",
             ["%li.action",
              ["%a#newqButton.action", {href: "#/queue/boo47"},
               "boo47"]],
             ["%li.action", 
              ["%a#newqButton.action", {href: "#/queue/boo"},
               "boo"]]]]


