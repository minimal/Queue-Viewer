(ns site.websocket
  (:use [site.messaging])
  (:use [compojure])
  (:use [com.github.icylisper.rabbitmq])
  (:use [clojure.contrib.json read write])
  (:import java.io.IOException
           javax.servlet.RequestDispatcher
           (javax.servlet.http
            HttpServletRequest
            HttpServletResponse)
           org.eclipse.jetty.util.TypeUtil
           org.eclipse.jetty.util.log.Log
           (org.eclipse.jetty.websocket
            WebSocket
            WebSocketServlet)))


(def outbounds (atom {}))

(def futures (atom {}))

(defn contained?
  "Swapped argument order for contains? So can use one collection with
  condp"
  [key coll] (contains? coll key))

(defn send-message
  "Easily send a message"
  [msg outbound]
  (println "To send msg" msg "to " outbound)
  (try 
   (.sendMessage outbound (byte 0) 
                 (json-str {:_action :msg ;; change to amqp-msg
                            :args {:msg (try
                                         (read-json (:msg msg)) ;; assume json
                                         (catch Exception ex
                                           (:msg msg))) 
                                   :envelope (:envelope msg)}})) ;; XXX don't assume amqp
   (catch Exception ex
     (println "Exception while sending: " ex " Outbound: " outbound)
     (swap! outbounds dissoc outbound))))



;; rpc funcs go here

(defn start-queue
  "Start consuming a queue in a future"
  ;;[{:keys [name exchange routing-key] :as args} websocket]
  [args websocket]
  (do (stop-queue nil websocket) 
      (swap! futures assoc websocket
             (future
              (consume-queue
               args 
               #(send-message  ;; remove BasicProperties before json
                 (dissoc  % :props) (@outbounds websocket))
               #(contains? @outbounds websocket))))))

(defn stop-queue
  "Stop listening to a queue"
  [name websocket]
  (if (contains? @futures websocket)
    (future-cancel (@futures websocket))))

(def rpc-methods
     {:start-queue start-queue
      :stop-queue stop-queue})

(defn handle-rpc
  "Dispatch a websocket message to the correct method"
  [action args websocket]
  ((rpc-methods (keyword action)) args websocket))

;; end rpc

(defn make-amqp-websock []
  (let [state (atom 0)
        obj (proxy [WebSocket] []
              (onConnect [outbound]
                         (swap! outbounds assoc this outbound))
              (onMessage [frame data]
                         (let [msg (try (read-json data)
                                            (catch Exception _ (str data))) 
                               _ (println "recieved: " msg (type frame))
                               action (msg "_action")
                               args (msg "args")]
                           (handle-rpc action args this) 
                           (println (count @futures) " futures")))
              (onDisconnect []
                            (println "onDisconnect WS")
                            (swap! outbounds dissoc this)
                            ;; Stop consuming the queue
                            (future-cancel (@futures this))
                            (swap! futures dissoc this)))]
    obj))

(defn web-sock-serv []
  (proxy [WebSocketServlet] [] 
    (doGet [request response]
           (.. (proxy-super getServletContext)
               (getNamedDispatcher (proxy-super getServletName))
               (forward request response)))
    (doWebSocketConnect [request response]
                           (make-amqp-websock))))

(defn do-run-websocket-server []
  (run-server {:port 8090}
              "/*" (web-sock-serv)))
 
