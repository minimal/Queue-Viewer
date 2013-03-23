AMQP Queue Viewer
=================

A realtime in browser viewer for AMQP messages


How?
----

- Clojure
- Javascript

- Websockets
- sammy.js

Features
--------

- Create temporary queues to view messages on your chosen exchange and
  routing key.
- New messages are displayed in real-time and json is nicely formatted
  and highlighted.
- Remembers previous queues.
- Uses web-socket-js for browsers without native websockets but with flash

![screenshot](https://github.com/downloads/minimal/Queue-Viewer/queue-viewer.png "Screenshot")

Requirements
------------

*Run*

- Download and extract [queue-viewer-0.2-SNAPSHOT.tar.bz2](//github.com/downloads/minimal/Queue-Viewer/queue-viewer-0.2-SNAPSHOT-2011-01-26.tar.bz2)
- cd to the directory
- type:

        java -server -jar queue-viewer-0.2-SNAPSHOT-standalone.jar --rabbithost myrabbitserver --webport 9090 --staticdir /path-to-app/public
- go to http://localhost:9090 in  a browser
- click "All test"
- click "Send test message" to test
 
*Dev*

- Clojure 1.1
- Jetty 7.2
- compojure with [this patch](http://github.com/minimal/compojure/commit/4ea5dc56f6be0a4345141dc45896b3f12cb6e131) applied to make it work with Jetty 7
- jquery 1.4+
- [sammy.js](http://github.com/quirkey/sammy)
- prettify.js
- [web-socket-js](http://github.com/gimite/web-socket-js)

Why?
----

I needed a nice way to view messages from various places and thought
it would be a good way to try a proper clojure app and to experiment
with websockets, instead of using python.


Author
------

Chris McDevitt


License
-------

EPL
