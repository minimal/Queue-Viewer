(defproject queue-viewer "0.2-SNAPSHOT"
  :description "AMQP Queue Viewer"
  :url "http://github.com/minimal/Queue-Viewer"
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]
                 [matchure "0.10.1"]
                 [org.apache.commons.codec "1.4"]
                 [org.apache.commons/commons-io "1.3.2"]
                 [org.apache.commons.fileupload "1.2.1"]
                 [org.eclipse.jetty/jetty-server "7.2.2.v20101205"]
                 [org.eclipse.jetty/jetty-servlet "7.2.2.v20101205"]
                 [org.eclipse.jetty/jetty-websocket "7.2.2.v20101205"]
                 [com.rabbitmq/amqp-client "2.2.0"]]
  :dev-dependencies [[swank-clojure "1.2.1"
                      marginalia "0.3.0"]]
  :main site.main)
