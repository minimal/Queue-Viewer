(defproject queue-viewer "0.1"
  :description "AMQP Queue Viewer"
  :url "http://github.com/minimal/Queue-Viewer"
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]
                 [org.apache.commons.codec "1.4"]
                 [org.apache.commons/commons-io "1.3.2"]
                 [org.apache.commons.fileupload "1.2.1"]
                 [org.eclipse.jetty/jetty-server "7.3.0-SNAPSHOT"]
                 [org.eclipse.jetty/jetty-servlet "7.3.0-SNAPSHOT"]
                 [org.eclipse.jetty/jetty-websocket "7.3.0-SNAPSHOT"]
                 [com.rabbitmq/amqp-client "1.7.2"]]
  ;;:dev-dependencies [[swank-clojure "1.1.0"]]
  :main site.main
  )
