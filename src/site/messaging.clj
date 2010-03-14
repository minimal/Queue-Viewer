;; ========= amqp part =========

(ns site.messaging
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

;; amqp config

(defonce conn-map {:username "guest"
                   :password "guest"
                   :host "vsuse"
                   :port 5672
                   :virtual-host "/"
                   ;:type "direct"
                   :exchange "sorting-room"
                   :queue "apo-box"
                   :durable false
                   :routing-key "atata"})

(defonce basic-conn-map {:username "guest"
                         :password "guest"
                         :host "vsuse"
                         :port 5672
                         :virtual-host "/"
                         :durable false
                         })

(defonce queues (ref {}))  ; map of all queues {queue-id queue}

;;(def connection (connect conn-map))
(def c (ref 0))
(defn publish-once
  ([]
     (publish-once (conn-map :routing-key) (conn-map :exchange)))
  ([routing-key exchange] 
     (let [[conn channel] (connect basic-conn-map)]
       (dotimes [ n 1]
         (dosync (alter c inc))
                                        ;(bind-channel conn-map channel)
         (println "rabbitmq publishing:" (format "message %d" @c) "to key: " routing-key)
         (publish {:routing-key routing-key
                   :exchange exchange}
                  channel (format "message %d" @c)))
       (disconnect channel conn))))


(defn get-one-msg
  "Get one message with basicGet"
  [queueName]
  (let [noAck true
        [conn channel] (connect basic-conn-map);;connection
        response (.basicGet channel queueName noAck) ]
    (disconnect channel conn)
    response))

(defn bind-channel-mod
  [{:keys [exchange type queue routing-key durable]}
   #^Channel ch]
  (.exchangeDeclare ch exchange type durable)
  ;;  (.queueDeclare ch queue durable)
  (.queueDeclare ch queue false false false true {})
  (.queueBind ch queue exchange routing-key))

(defn declare-queue
  "Easily create a queue."
  [queueName exchange routing-key]
  (let [[conn channel] (connect basic-conn-map)
        mappings (assoc basic-conn-map
                   :routing-key routing-key
                   :exchange exchange
                   :queue queueName
                   :type "topic")]
    (bind-channel-mod mappings channel)
    (disconnect channel conn)
    channel))

(defn do-consume-poll
  [queueName exchange routing-key]
  (let [channel (declare-queue queueName exchange routing-key)]
      (consume-poll basic-conn-map channel)))

(defn consume-queue
  "Start consuming a queue to feed back to a websocket. Create a
  connection, Declare the queue, Create a QueueingConsumer, start
  consuming in a loop"
  [queue-name wsconn callback pred]
  (println "start consume" queue-name wsconn)
  (let [[conn channel] (connect basic-conn-map)
        ;; temp values
        exchange "amq.topic"
        type "topic"
        durable false
        routing-key "#"
        ;; qd (.queueDeclare channel queue-name)
        qd (.queueDeclare channel queue-name false false false true {})
        _ (.exchangeDeclare channel exchange type durable)
        ;;  (.queueDeclare ch queue durable)
        ;; (.queueDeclare ch queue false durable false true {})
        _ (.queueBind channel queue-name exchange routing-key)
        qconsumer (QueueingConsumer. channel)]
    (.basicConsume channel queue-name qconsumer)
    (try 
     (while (pred)
            (do        
              (let [delivery (.nextDelivery qconsumer)
                    msg (String. (.getBody delivery))
                    _ (.basicAck channel (.. delivery getEnvelope getDeliveryTag) false)]
                (callback {"msg" msg
                           "routing-key" "test"}
                          wsconn))
        
              #_(disconnect channel conn)))
     (disconnect channel conn)
     (println "Disconnection rabbit")
     (catch Exception ex
       (try 
        (disconnect channel conn)
        (catch Exception _ (println "disco failed")))
       (println queue-name "Consume thread caught exception:" ex))))
)
