(ns site.main
  (:use [compojure])
  (:use [clojure.contrib.json.write])
  (:use [com.github.icylisper.rabbitmq])
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
;; ========= amqp part =========


;; amqp config

(defonce conn-map {:username "guest"
                   :password "guest"
                   :host "localhost"
                   :port 5672
                   :virtual-host "/"
                   ;:type "direct"
                   :exchange "sorting-room"
                   :queue "apo-box"
                   :durable false
                   :routing-key "atata"})

(defonce basic-conn-map {:username "guest"
                         :password "guest"
                         :host "localhost"
                         :port 5672
                         :virtual-host "/"
                         :durable false
                         })

(defonce queues (ref {}))  ; map of all queues {queue-id queue}

(def connection (connect conn-map))
(def c (ref 0))
(defn publish-once
  ([]
     (publish-once (conn-map :routing-key) (conn-map :exchange)))
  ([routing-key exchange] 
     (let [[_ channel] connection]
       (dotimes [ n 1]
         (dosync (alter c inc))
                                        ;(bind-channel conn-map channel)
         (println "rabbitmq publishing:" (format "message %d" @c) "to key: " routing-key)
         (publish {:routing-key routing-key
                   :exchange exchange}
                  channel (format "message %d" @c))))))


(defn get-one-msg
  "Get one message with basicGet"
  [queueName]
  (let [noAck true
        [conn channel] (connect basic-conn-map);;connection
        response (.basicGet channel queueName noAck) ]
    (disconnect channel conn)
    response))


(defn declare-queue
  "Easily create a queue."
  [queueName exchange routing-key]
  (let [[conn channel] (connect basic-conn-map)
        mappings (assoc basic-conn-map
                   :routing-key routing-key
                   :exchange exchange
                   :queue queueName
                   :type "topic")]
    (bind-channel mappings channel)
    (disconnect channel conn)
    channel))

(defn do-consume-poll
  [queueName exchange routing-key]
  (let [channel (declare-queue queueName exchange routing-key)]
      (consume-poll basic-conn-map channel)))

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
     (include-js "/public/js/jquery-1.3.2.min.js"
                 "/public/js/jquery.haml-1.3.js"
                 "/public/js/sammy.js"
                 "/public/js/prettify/prettify.js"
                 "/public/js/json2.js"
                 "/public/js/message.js"
                 "/public/js/viewer.js")
     (javascript-tag " $(document).ready(function() {viewer.init(); var app = viewer.initSammy(); app.run('#/');});")] 
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

