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
