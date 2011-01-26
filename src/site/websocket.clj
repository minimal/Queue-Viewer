(ns site.websocket
  (:use [site.messaging]
        [clojure.stacktrace]
        [compojure.server.jetty :only [run-server]]
        [clojure.contrib.json read write]
        [matchure])
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
(def heartbeat (atom nil))

(defmacro contained?
  "Swapped argument order for contains? So can use one collection with
  condp"
  [a b] 
  `(contains? ~b ~a))

(defn send-message
  "Easily send a message

  TODO: don't assume amqp"
  [msg outbound]
  (println "To send msg" msg "to " outbound)
  (try 
   (.sendMessage outbound (byte 0) 
                 (json-str msg)) 
   (catch Exception ex
     (println "Exception while sending: " ex " Outbound: " outbound)
     (swap! outbounds dissoc outbound))))


(defn send-heartbeat
  "Send tic to stop websocket disconnects"
  [socket]
  (let [msg (json-str {:tic 60})] 
    (.sendMessage socket (byte 0) msg)))

(defn callp []
  (prn (count @futures) " futures")
  (doseq [outbound @outbounds] (send-heartbeat (last outbound))))

(defn send-periodic-tics []
  (let [myfuture (future
                  (loop []
                    (callp)
                    (Thread/sleep 60000)
                    (recur)))]
    myfuture))

;; ## RPC funcs

(defn stop-queue
  "Stop listening to a queue"
  [name websocket]
  (if (contains? @futures websocket)
    (future-cancel (@futures websocket))))

(defn send-amqp-to-ws
  [msg websocket]
   ;; remove BasicProperties before json
  (let [smsg  (dissoc msg :props)]
    (send-message
     {:method :amqp-msg
      :jsonrpc 2.0
      :params {:msg (try
                      (read-json (:msg smsg)) ;; assume json
                      (catch Exception ex
                        (:msg smsg))) 
               :envelope (:envelope smsg)}}
     (@outbounds websocket))))

(defn start-queue
  "Start consuming a queue in a future  
   TODO: use agent instead of future"
  ; `[{:keys [name exchange routing-key] :as args} websocket]`
  [args websocket]
  (do (stop-queue nil websocket)
      (prn "in start-queue" args)
      (swap! futures assoc websocket
             (future
              (try  (consume-queue
                     args 
                     send-amqp-to-ws
                     #(contains? @outbounds websocket)
                     websocket)
                    (catch Exception ex (print-stack-trace ex)))))))

(defn publish-amqp
  [{:keys [routing-key exchange msg]}] nil)

(defn-match run-rpc
  "Use matchure to handle actions

  * Try to follow json-rpc-2.0 spec
  * XXX: on clojure 1.2 read-json uses :keywords instead of strings"
  ([{"method" "start-queue", "params" ?params} ?socket]
     (start-queue params socket))
  ([{"method" "stop-queue", "params" ?params} ?socket]
     (stop-queue params socket))
  ([{"method" "publish-amqp", "params" ?params} & ?_]
     (publish-amqp params))
  ([{"msg" "ack"} ?_] nil)
  ([{"method" ?method "id" ?id} & ?_] (prn "no method for" method))
  ([?action & _] (prn "no method for" action)))

(defn handle-websocket-message
  [frame data websocket]
  (let [msg (try (read-json data)
                 (catch Exception _ (str data))) 
        _ (println "recieved: " msg (type frame))]
    (if (map? msg) 
        (run-rpc msg websocket)
      (println "no rpc cmds"))))

(defn on-websocket-disconnect
  [socket]
  (println "onDisconnect WS")
  (swap! outbounds dissoc socket)
  ;; Stop consuming the queue
  (future-cancel (@futures socket))
  (swap! futures dissoc socket))


;; ## Jetty websockets servers

(defn make-amqp-websock []
  (let [state (atom 0)
        obj (proxy [WebSocket] []
              (onConnect
               [outbound]
               (swap! outbounds assoc this outbound))
              (onMessage
               [frame data]
               (handle-websocket-message frame data this))
              (onDisconnect [] (on-websocket-disconnect this)))]
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
  (reset! heartbeat (send-periodic-tics))
  (run-server {:port 8090}
              "/*" (web-sock-serv)))
 
