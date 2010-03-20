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
                 (json-str {"msg" (try
                                   (read-json (msg "msg")) ;; assume json
                                   (catch Exception ex
                                     (msg "msg")))
                            "routing-key" (msg "routing-key")}))
   (catch Exception ex
     (println "Exception while sending: " ex " Outbound: " outbound)
     (swap! outbounds dissoc outbound))))



(defn make-amqp-websock []
  (let [state (atom 0)
        obj (proxy [WebSocket] []
              (onConnect [outbound]
                         (swap! outbounds assoc this outbound))
              (onMessage [frame data]
                         ;; TODO: handle different actions eg
                         ;; listening to a queue
                         (let [decdata (try (read-json data)
                                            (catch Exception _ (str data)))
                               msg (json-str {"routing-key" "foo"
                                              "msg" decdata})
                               _ (println "recieved: " decdata (type frame))]
                           (condp = (first (decdata "hash"))
                               ;; Put this in it's own thread
                               ;; to stop it blocking -future
                             "queue" (do (if (contains? @futures this)
                                           (future-cancel (@futures this)))
                                         (swap! futures assoc this
                                                (future
                                                 (consume-queue
                                                  (last (decdata "hash"))
                                                  (@outbounds this)
                                                  send-message
                                                  #(contains? @outbounds this)
                                                  (decdata "exchange")
                                                  (decdata "routing-key"))))))
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
 
