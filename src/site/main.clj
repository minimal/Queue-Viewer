;; Copyright (c) Christopher McDevitt. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution. By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license. You must not remove this notice, or any other, from
;; this software.

(ns site.main
  (:gen-class)
  (:use
   [compojure]
   [clojure.contrib.json.write]
   [clojure.contrib.command-line]
   [clojure.contrib.duck-streams]
   [site.messaging :only [queues declare-queue publish-once basic-conn-map]]
   [site.websocket :only [do-run-websocket-server]]))

(def public-dir (atom "public"))

;; # AMQP live queue viewer

;; _Chris McDevitt_

;; ## Compojure part

;; ### Views

(defn html-doc 
  [title & body] 
  (html 
   (doctype :html4) 
   [:html 
    [:head 
     [:title title] 
     (include-css  "http://yui.yahooapis.com/2.7.0/build/reset-fonts-grids/reset-fonts-grids.css"
                   "/public/css/main.css"
                   "/public/css/form.css"
                   "/public/js/lib/prettify/prettify.css")
     (javascript-tag "if (!window.console) console = {log: function(){ }, error: function(){ }};")
     (include-js "http://ajax.googleapis.com/ajax/libs/jquery/1.4.4/jquery.min.js"
                 "/public/js/lib/jquery.haml-1.3.js"
                 "/public/js/lib/sammy-0.6.2.min.js"
                 "/public/js/lib/sammy.json-0.6.2.min.js"
                 "/public/js/lib/sammy.storage-0.6.2.min.js"
                 "/public/js/lib/prettify/prettify.js"
                 "/public/js/lib/json2.js"
                 "/public/js/lib/swfobject.js"
                 "/public/js/lib/FABridge.js"
                 "/public/js/lib/web_socket.js"
                 "/public/js/message.js"
                 "/public/js/viewer.js")
     (javascript-tag
      (str 
       "$(function() {"
       "WebSocket.__swfLocation = '/public/js/lib/WebSocketMain.swf';"
       "sammy_app = viewer.initSammy(); sammy_app.run('#/');"
       "viewer.init(sammy_app);"
       "});"))] 
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


;; ### Controllers

(defn setup-queue
  "Setup a queue for a client with a routing key  
   TODO: move to message.clj"
  [routing-key exchange queueName]
  (dosync (if-not (contains? @queues routing-key)
            (alter queues assoc routing-key queueName)))
  (declare-queue queueName exchange routing-key)
  (json-str {:msg (str "Setup queue with key: " routing-key
                       " and exchange: " exchange)
             :status "success"
             :queue-id queueName}))

;; ### Routes


(defroutes queue-viewer
  (GET "/"
       (html-doc "Welcome"
                 [:div#main]))
  (PUT "/queue" [{:headers {"Content-Type" "application/json"}}]
       (setup-queue
        (params :routing-key) (params :exchange) (params :queue-name)))
  (POST "/message"
        (publish-once (params :routing-key) (params :exchange)))
  (GET "/public/*" (or (serve-file @public-dir (params :*)) :next))
  (GET "/favicon.ico" 404)
  (ANY "*"
       (page-not-found)))


;; ### Server

(defn do-run-server
  [port]
  (run-server {:port port}
              "/*" (servlet queue-viewer)))

(defn do-run-both-servers []
  [(do-run-server 8080) (do-run-websocket-server)])

(defn -main [& args] 
  (with-command-line args
    "Queue Viewer"
    [[rabbithost "Rabbitmq host" "dev.rabbitmq.com"]
     [webport "Webserver port" 8080]
     [staticdir "Path to static files" "current-dir/public"]] 
    (let [webserver (if webport
                      (do-run-server (Integer. webport))
                       (do-run-server 8080))
          websock-server (do-run-websocket-server)]
      (if rabbithost (swap! basic-conn-map assoc :host rabbithost))
      (if staticdir
        (reset! public-dir staticdir)
        (reset! public-dir (str (pwd) "/public")))
      (println "rabbit: " (:host @basic-conn-map)
               "webport: " (or webport 8080)
               "public: " @public-dir)
      (println "Servers started: " webserver websock-server)
      [webserver websock-server])))
