(use 'compojure)
(use 'clojure.contrib.json.write)

"AMQP live queue viewer

Chris McDevitt
"

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
        [:h1 "Header"]]
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
  (json-str [{:msg "I am the message" :id id}]))

(defroutes queue-viewer
  (GET "/"
       (html-doc "Welcome"
                 [:p
                  "this is the start page of my amazin app"]
                 [:p [:a#newmsgButton {:href "#"} "Get new messages"]]
                 [:div#entries [:ul "messages go here"]]))
  (GET "/queue/:id" [{:headers {"Content-Type" "application/json"}}]
       (poll-queue-for-messages (Integer. (params :id))))
  (GET "/foo/:moo"
       (html [:title "hello from foo with " (params :moo)]
             [:h1#foo.moo "what the hell is this?"]
             [:ul 
              [:li "an item"]
              [:li "another item"]
              [:li  ( params :moo)]
              [:li [:a {:href "/"} "go back"]]
              (map (fn [x] [:li x])
                   ["hello" "what is this" "when will" "it end?"])]))
  (GET "/public/*" (or (serve-file "c:/Users/chris/Documents/code/clojure/queue-viewer/public" (params :*)) :next))
  (ANY "*"
       (page-not-found)))

(run-server {:port 8080}
            "/*" (servlet queue-viewer))

