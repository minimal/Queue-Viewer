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

(def basic-conn-map (atom {:username "guest"
                           :password "guest"
                           :host "dev.rabbitmq.com"
                           :port 5672
                           :virtual-host "/"
                           :durable false
                           }))

(defonce queues (ref {}))  ; map of all queues {queue-id queue}

;;(def connection (connect conn-map))
(def c (ref 0))
(defn publish-once
  ([]
     (publish-once (conn-map :routing-key) (conn-map :exchange)))
  ([routing-key exchange] 
     (let [[conn channel] (connect @basic-conn-map)]
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
        [conn channel] (connect @basic-conn-map);;connection
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
  (let [[conn channel] (connect @basic-conn-map)
        mappings (assoc @basic-conn-map
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
      (consume-poll @basic-conn-map channel)))

(defn consume-queue
  "Start consuming a queue to feed back to a websocket. Create a
  connection, Declare the queue, Create a QueueingConsumer, start
  consuming in a loop"
  [{queue-name "name"
    exchange "exchange"
    routing-key "routing-key"}
   callback pred]
  (println "start consume" queue-name)
  (let [[conn channel] (connect @basic-conn-map)
        ;; temp values
        type "topic"
        durable false 
        qd (.queueDeclare channel queue-name false false false true {})
        _ (.exchangeDeclare channel exchange type durable)
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
                           :props (.getProperties delivery)
                           }))))
     (println "Disconnection rabbit")
     (catch Exception ex 
       (println queue-name "Consume thread caught exception:" ex))
     (finally (disconnect channel conn)))))
