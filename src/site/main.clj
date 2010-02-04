;; Copyright (c) Christopher McDevitt. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution. By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license. You must not remove this notice, or any other, from
;; this software.

(ns site.main
  (:use [compojure])
  (:use [clojure.contrib.json.write])
  (:use [com.github.icylisper.rabbitmq])
  (:use [site.messaging])
  (:use [site.websocket])
  (:import (com.rabbitmq.client
             ConnectionParameters
             Connection
             Channel
             AMQP
             ConnectionFactory
             Consumer
             QueueingConsumer)))


;; "AMQP live queue viewer

;; Chris McDevitt
;; "
 ;; (consume-poll mymap ((connect mymap) 1) {:prefetch 1}) 
;; compojure part

;; ========= Views =========


(defn html-doc 
  [title & body] 
  (html 
   (doctype :html4) 
   [:html 
    [:head 
     [:title title]
     ;[:link {:type "text/css" :href "http://yui.yahooapis.com/2.7.0/build/reset-fonts-grids/reset-fonts-grids.css" :rel "stylesheet"}]
     (include-css  "http://yui.yahooapis.com/2.7.0/build/reset-fonts-grids/reset-fonts-grids.css"
                   "/public/css/main.css"
                   "/public/css/form.css"
                   "/public/js/prettify/prettify.css")
     (include-js "/public/js/jquery-1.4.min.js"
                 "/public/js/jquery.haml-1.3.js"
                 "/public/js/sammy.js"
                 "/public/js/sammy.json-0.4.1.min.js"
                 "/public/js/sammy.storage-0.4.1.min.js"
                 "/public/js/prettify/prettify.js"
                 "/public/js/json2.js"
                 "/public/js/swfobject.js"
                 "/public/js/FABridge.js"
                 "/public/js/web_socket.js"
                 "/public/js/message.js"
                 "/public/js/viewer.js")
     (javascript-tag "$(document).ready(function() {WebSocket.__swfLocation = 'WebSocketMain.swf'; var app = viewer.initSammy(); app.run('#/'); viewer.init(app);});")] 
      [:body#doc.yui-t4
       [:div#hd 
        [:a {:href "/#/"} [:h1 "AMQP Message Viewer"]]]
       [:div#bd
        [:div#yui-main
         [:div.yui-b
          [:div.yui-g
           body]]] 
        [:div.yui-b
         [:div#sidebar ""]]]]]))


;; ========= Controllers =========

(defn get-waiting-messages
  "get a list of the current messages on the queue"
  [queueName]
  (loop [msgs []]
    (let [msg (get-one-msg queueName)]
      (if-not msg
        msgs
        (recur (conj msgs msg))))))


(defn poll-queue-for-messages
  "Return any new messages as a json array"
  [queueName]
  (let [msgs (get-waiting-messages queueName)]
    (json-str (for [msg msgs]
                {:msg (String. (. msg getBody))
                 :routing-key (String. (.. msg getEnvelope getRoutingKey))
                 :id queueName}))))

(defn setup-queue
  "Setup a queue for a client with a routing key"
  [routing-key exchange queueName]
  (dosync (if-not (contains? @queues routing-key) ; add queue description
            (alter queues assoc routing-key queueName)))
                                       ; declare queue
    
  (declare-queue queueName exchange routing-key)
  (json-str {:msg (str "Setup queue with key: " routing-key
                       " and exchange: " exchange)
             :status "success"
             :queue-id queueName}))

;; ========= Routes =========


(defroutes queue-viewer
  (GET "/"
       (html-doc "Welcome"
                 [:div#main]))
  
  (GET "/queue/:id" [{:headers {"Content-Type" "application/json"}}]
       (poll-queue-for-messages (params :id)))
  (PUT "/queue" [{:headers {"Content-Type" "application/json"}}]
       (setup-queue (params :routing-key) (params :exchange) (params :queue-name)))
  (POST "/message"
        (publish-once (params :routing-key) (params :exchange)))
  (GET "/public/*" (or (serve-file "/home/chris/devel/git/queue-viewer/public/" (params :*)) :next))
  (GET "/favicon.ico" 404)
  (ANY "*"
       (page-not-found)))


;; ========= server =========

(defn do-run-server []
  (run-server {:port 8080}
              "/*" (servlet queue-viewer)))

