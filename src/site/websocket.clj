(ns site.websocket
  (:use [compojure])
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

(defn make-chat-websock []
  (let [state (atom 0)
        obj (proxy [WebSocket] []
              (onConnect [outbound]
                         (swap! outbounds assoc this outbound))
              (onMessage [frame data]
                         (let [decdata (try (read-json data)
                                            (catch Exception _ (str data)))
                               msg (json-str {"routing-key" "foo"
                                              "msg" decdata})
                               _ (println "recieved: " decdata data)]
                           (doseq [[_ member] @outbounds]
                             (.sendMessage member frame msg))))
              (onDisconnect []
                            (swap! outbounds dissoc this)))]
    obj))

(defn web-sock-serv []
  (proxy [WebSocketServlet] [] 
    (doGet [request response]
           (.. (proxy-super getServletContext)
               (getNamedDispatcher (proxy-super getServletName))
               (forward request response)))
    (doWebSocketConnect [request response]
                           (make-chat-websock))))

(defn do-run-websocket-server []
  (run-server {:port 8090}
              "/*" (web-sock-serv)))
 
