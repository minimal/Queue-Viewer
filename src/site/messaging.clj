;; # AMQP

(ns site.messaging
  (:import (com.rabbitmq.client 
             Connection
             Channel
             AMQP
             ConnectionFactory
             Consumer
             QueueingConsumer)))

;; ## Config

(def basic-conn-map (atom {:username "guest"
                           :password "guest"
                           :host "dev.rabbitmq.com"
                           :port 5672
                           :virtual-host "/"
                           :exchange "amq.topic"
                           :routing-key "test"
                           :durable false}))

(defonce queues (ref {}))  ; map of all queues {queue-id queue}

(defn connect
  [{:keys [username password virtual-host port #^String host]}]
  (let [#^ConnectionFactory f (new ConnectionFactory)]
    (doto f
      (.setUsername username)
      (.setPassword password)
      (.setVirtualHost virtual-host)
      (.setHost host)
      (.setPort port))
    (let [#^Connection conn (.newConnection f)]
      [conn (.createChannel conn)])))

(defn disconnect
  [#^Channel ch #^Connection conn]
  (.close ch)
  (.close conn))

(defn publish
  ([{:keys [exchange routing-key]}
     #^Channel ch
     #^String m]
     (let [msg-bytes (.getBytes m)]
       (.basicPublish ch exchange routing-key nil msg-bytes)))
  ([{:keys [exchange routing-key]}
    mandatory
    immediate
     #^Channel ch
     #^String m]
     (let [msg-bytes (.getBytes m)]
       (.basicPublish ch exchange routing-key mandatory immediate nil msg-bytes))))

(def c (ref 0))
(defn publish-once
  ([]
     (publish-once (@basic-conn-map :routing-key) (@basic-conn-map :exchange)))
  ([routing-key exchange] 
     (let [[conn channel] (connect @basic-conn-map)]
       (try (dotimes [ n 1]
              (dosync (alter c inc)) 
              (println "rabbitmq publishing:" (format "message %d" @c) "to key: " routing-key)
              (publish {:routing-key routing-key
                        :exchange exchange}
                       channel (format "message %d" @c)))
            (finally  (disconnect channel conn))))))


(defn get-one-msg
  "Get one message with basicGet"
  [queueName]
  (let [noAck true
        [conn channel] (connect @basic-conn-map);;connection
        response (.basicGet channel queueName noAck) ]
    (disconnect channel conn)
    response))

(defn declare-passive-exchange
  [ch exchange]
  (.exchangeDeclarePassive ch exchange))

(defn bind-channel-mod
  [{:keys [exchange type queue routing-key durable]}
   #^Channel ch]
  (declare-passive-exchange ch exchange)
  (.queueDeclare ch queue false false true {})
  (.queueBind ch queue exchange routing-key))

(defn declare-queue
  "Easily create a queue."
  [queueName exchange routing-key]
  (try 
    (let [[conn channel] (connect @basic-conn-map)
          mappings (assoc @basic-conn-map
                     :routing-key routing-key
                     :exchange exchange
                     :queue queueName
                     :type "topic")]
      (try (do
             (bind-channel-mod mappings channel)
             channel) 
           (finally (disconnect channel conn))))))

(defn consume-queue
  "Start consuming a queue to feed back to a websocket. Create a
  connection, Declare the queue, Create a QueueingConsumer, start
  consuming in a loop"
  [{queue-name "name"
    exchange "exchange"
    routing-key "routing-key"}
   callback pred rchannel]
  (println "start consume" queue-name)
  (let [[conn channel] (connect @basic-conn-map)
        ;; temp values
        type "topic"
        durable false 
        qd (.queueDeclare channel queue-name false false true {})
        _ (declare-passive-exchange channel exchange)
        _ (.queueBind channel queue-name exchange routing-key)
        qconsumer (QueueingConsumer. channel)]
    (.basicConsume channel queue-name qconsumer)
    (try 
     (while (pred)
            (do        
              (let [delivery (.nextDelivery qconsumer)
                    msg (String. (.getBody delivery))
                    envelope (.getEnvelope delivery)
                    _ (.basicAck channel (.. delivery getEnvelope getDeliveryTag) false)]
                (callback {:msg msg
                           :envelope {:exchange (.getExchange envelope)
                                      :tag (.getDeliveryTag envelope)
                                      :isRedeliver (.isRedeliver envelope)
                                      :routing-key (String. (.getRoutingKey envelope))}
                           :props (.getProperties delivery)}
                          rchannel))))
     (println "Disconnection rabbit")
     (catch Exception ex 
       (println queue-name "Consume thread caught exception:" ex))
     (finally (disconnect channel conn)))))
