(use 'compojure)
(use 'clojure.contrib.json.write)
(use 'com.github.icylisper.rabbitmq)
;; (use 'rabbit)


;; "AMQP live queue viewer

;; Chris McDevitt
;; "
;; ;; amqp part

;; (defn producer [producer-num cnt]
;;   (rabbit/with-amqp
;;    {}
;;    (let [start-time now]
;;      (dotimes [ii cnt]
;;        (object-publish [ii (Date.)]))
;;      (log-producer-stat producer-num cnt start-time (now)))))



;; compojure part


(defn html-doc 
  [title & body] 
  (html 
   (doctype :html4) 
   [:html 
    [:head 
     [:title title]
     ;[:link {:type "text/css" :href "http://yui.yahooapis.com/2.7.0/build/reset-fonts-grids/reset-fonts-grids.css" :rel "stylesheet"}]
     (include-css  "http://yui.yahooapis.com/2.7.0/build/reset-fonts-grids/reset-fonts-grids.css"
                   "/public/css/main.css")
     (include-js "/public/js/jquery-1.3.2.min.js"
                 "/public/js/jquery.haml-1.3.js"
                 "/public/js/message.js"
                 "/public/js/viewer.js")
     (javascript-tag " $(document).ready(function() {viewer.init();});")] 
      [:body#doc.yui-t4
       [:div#hd 
        [:h1 "AMQP Message Viewer"]]
       [:div#bd
        [:div#yui-main
         [:div.yui-b
          [:div.yui-g
           [:h2 
            ;; Pass a map as the first argument to be set as attributes of the element
            [:a {:href "/"} "Home"]]
           body]]]
        [:div.yui-b
         [:p "Navigation"]]]]]))


(defn poll-queue-for-messages
  "Return any new messages as a json array"
  [id]
  (json-str [{:msg (str "I am message: " (rand-int 50)) :id id}]))

(defroutes queue-viewer
  (GET "/"
       (html-doc "Welcome"
                 [:div#controls 
                  [:p
                   "this is the start page of my amazin app"]
                  [:p [:a#newmsgButton {:href "#"} "Get new messages"]]]
                 [:div#entries [:p.message "messages go here"]]))

  (GET "/queue/:id" [{:headers {"Content-Type" "application/json"}}]
       (poll-queue-for-messages (Integer. (params :id))))

  (GET "/public/*" (or (serve-file "c:/Users/chris/Documents/code/clojure/queue-viewer/public" (params :*)) :next))
  (GET "/favicon.ico" 404)
  (ANY "*"
       (page-not-found)))

(run-server {:port 8080}
            "/*" (servlet queue-viewer))

