(ns beatthemarket.server-components.middleware
  (:require
   [beatthemarket.server-components.config :refer [config]]
   [beatthemarket.server-components.pathom :refer [parser]]
   [mount.core :refer [defstate]]
   [fulcro.server :as server]
   [ring.middleware.defaults :refer [wrap-defaults]]
   [ring.middleware.gzip :refer [wrap-gzip]]
   [ring.util.response :refer [response file-response resource-response]]
   [ring.util.response :as resp]
   [hiccup.page :refer [html5]]
   [taoensso.timbre :as log]
   [clj-time.core :as t]
   [clj-time.coerce :as c]
   [clojure.core.async :as async :refer [chan go-loop >!! alts! timeout]]

   [com.fulcrologic.fulcro.server.api-middleware :refer [not-found-handler]]
   [com.fulcrologic.fulcro.networking.websockets :as fws]
   [com.fulcrologic.fulcro.networking.websocket-protocols :as fwsp :refer [WSListener]]
   ;; [immutant.web :as web]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.not-modified :refer [wrap-not-modified]]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   ;; [ring.util.response :refer [response file-response resource-response]]
   ;; [taoensso.sente.server-adapters.immutant :refer [get-sch-adapter]]
   [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]))


(defn wrap-api [handler uri]
  (fn [request]
    (if (= uri (:uri request))
      (server/handle-api-request
        parser
        ;; this map is `env`. Put other defstate things in this map and they'll be
        ;; added to the resolver/mutation env.
        {:ring/request request}
        (:transit-params request))
      (handler request))))

;; ================================================================================
;; Dynamically generated HTML. We do this so we can safely embed the CSRF token
;; in a js var for use by the client.
;; ================================================================================
(defn index [csrf-token]
  (log/debug "Serving index.html")
  (html5
    [:html {:lang "en"}
     [:head {:lang "en"}
      [:title "Application"]
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"}]
      [:link {:href "https://cdnjs.cloudflare.com/ajax/libs/semantic-ui/2.4.1/semantic.min.css"
              :rel  "stylesheet"}]
      [:link {:rel "shortcut icon" :href "data:image/x-icon;," :type "image/x-icon"}]
      [:script (str "var fulcro_network_csrf_token = '" csrf-token "';")]]
     [:body
      [:div#app]
      [:script {:src "js/main/main.js"}]
      [:script "beatthemarket.client.init();"]]]))

(defn highstock [csrf-token]
  (log/debug "Serving highstock.html")
  (html5
    [:html {:lang "en"}
     [:head {:lang "en"}

      [:title "Beat The Market"]
      [:link {:rel "icon" :href "data:;base64,iVBORw0KGgo="}]
      [:link {:href "https://maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css"
              :rel "stylesheet"}]
      [:style ".colleague {
                  font-style: italic;
                  color: #999; }"]

      ;; Typekit
      [:script {:src "https://use.typekit.net/ktm4usi.js"}]
      [:script "try{Typekit.load({ async: true });}catch(e){}"]

      [:script {:src "https://code.jquery.com/jquery-3.1.1.min.js"}]
      [:script {:src "https://code.highcharts.com/stock/highstock.js"}]
      [:script {:src "https://code.highcharts.com/highcharts-more.js"}]
      [:script {:src "https://code.highcharts.com/stock/modules/exporting.js"}]
      [:script {:src "https://code.highcharts.com/stock/modules/export-data.js"}]



      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"}]
      ;; [:link {:href "https://cdnjs.cloudflare.com/ajax/libs/semantic-ui/2.4.1/semantic.min.css"
      ;;         :rel  "stylesheet"}]
      ;; [:link {:rel "shortcut icon" :href "data:image/x-icon;," :type "image/x-icon"}]
      [:script (str "var fulcro_network_csrf_token = '" csrf-token "';")]]
     [:body {:fullbleed "" :vertical "" :layout "" :style "width:100%; height:100%;"}

      [:div {:class "tk-league-gothic" :style "font-size:3em"} "Beat The Market"]
      [:div {:class "tk-open-sans"} "Lorem ipsum dolor sit amet"]

      ;; Ensuring users of IE can't use Compatibility Mode, as this will break Persona.
      ;; (https://developer.mozilla.org/en-US/Persona/Quick_setup)
      [:meta {:http-equiv "X-UA-Compatible" :content "IE=Edge"}]

      [:div#app]
      [:script {:src "js/main/main.js"}]
      #_[:script "beatthemarket.client.init();"]]]))

;; ================================================================================
;; Workspaces can be accessed via shadow's http server on http://localhost:8023/workspaces.html
;; but that will not allow full-stack fulcro cards to talk to your server. This
;; page embeds the CSRF token, and is at `/wslive.html` on your server (i.e. port 3000).
;; ================================================================================
(defn wslive [csrf-token]
  (log/debug "Serving wslive.html")
  (html5
    [:html {:lang "en"}
     [:head {:lang "en"}
      [:title "devcards"]
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"}]
      [:link {:href "https://cdnjs.cloudflare.com/ajax/libs/semantic-ui/2.4.1/semantic.min.css"
              :rel  "stylesheet"}]
      [:link {:rel "shortcut icon" :href "data:image/x-icon;," :type "image/x-icon"}]
      [:script (str "var fulcro_network_csrf_token = '" csrf-token "';")]]
     [:body
      [:div#app]
      [:script {:src "workspaces/js/main.js"}]]]))

(defn wrap-html-routes [ring-handler]
  (fn [{:keys [uri anti-forgery-token] :as req}]
    (cond
      (#{"/" "/index.html"} uri)
      (-> (resp/response (index anti-forgery-token))
          (resp/content-type "text/html"))

      (#{"/highstock" "/highstock.html"} uri)
      (-> (resp/response (highstock anti-forgery-token))
          (resp/content-type "text/html"))

      ;; See note above on the `wslive` function.
      (#{"/wslive.html"} uri)
      (-> (resp/response (wslive anti-forgery-token))
          (resp/content-type "text/html"))

      :else
      (ring-handler req))))

(defrecord BeatthemarketWSListener []
  WSListener
  (client-added [this ws-net cid]
    (println (str "Listener for dealing with client added events." [ws-net cid])))
  (client-dropped [this ws-net cid]
    (println (str "listener for dealing with client dropped events." [ws-net cid]))))

(def beatthemarket-ws-listener (->BeatthemarketWSListener))

(def websockets' (atom nil))

(defn query-parser
  ""
  [env query]
  ;; call out to something like a pathom parser. See Fulcro Developers Guide
  )

(defstate middleware
  :start
  (let [websockets (fws/start! (fws/make-websockets
                                 query-parser
                                 {:http-server-adapter (get-sch-adapter)
                                  :parser-accepts-env? true
                                  ;; See Sente for CSRF instructions
                                  ;; :sente-options       {:csrf-token-fn nil}
                                  }))
        defaults-config (:ring.middleware/defaults-config config)
        legal-origins   (get config :legal-origins #{"localhost"})]

    (fwsp/add-listener websockets beatthemarket-ws-listener)
    (reset! websockets' websockets)

    (-> not-found-handler
        (wrap-api "/api")
        (fws/wrap-api websockets)
        server/wrap-transit-params
        server/wrap-transit-response
        (wrap-html-routes)
        (wrap-defaults defaults-config)
        wrap-gzip)))

(defn wave-length
  ([] (wave-length 64))
  ([length]
   (->> (constantly (range 1 length))
        repeatedly
        (apply concat))))

(defn sine-wave
  ([] (sine-wave 64))
  ([length]
   (let [xmultiplier (wave-length length)
         yaxis (->> xmultiplier
                    (map #(/ % 10))
                    (map #(Math/sin %)))
         xaxis (range)]
     (->> (interleave xaxis yaxis)
          (partition 2)))))

(defn sine-wave-with-datetime
  ([start-time sine-wave] (sine-wave-with-datetime start-time 0 sine-wave))
  ([start-time y-offset sine-wave]
   (let [time-seq (iterate #(t/plus % (t/seconds 1)) start-time)]
     (map (fn [t [_ y]] [t (+ y-offset y)])
          time-seq
          sine-wave))))

(defn stream-to-client! [push-fn control-chan sine-wave-seq]

  (go-loop [tick-list sine-wave-seq
            timeout-chan (timeout 500)]

    (let [tick (first tick-list)
          [v ch] (alts! [control-chan timeout-chan])]

      (when-not (= :exit v)

        (println tick)
        (push-fn tick)
        (recur (rest tick-list) (timeout 500))))))


(comment


  (require '[com.fulcrologic.fulcro.networking.websocket-protocols :refer [push]])


  ;; A Pushing a piece of data over Websocket
  (let [client-uid (-> @(:connected-uids @websockets')
                       :any
                       first)]
    (push @websockets' client-uid :foo-topic {:foo "bar"}))


  ;; B Streaming sequence data over Websocket
  (let [client-uid (-> @(:connected-uids @websockets')
                       :any
                       first)
        nums (take 1000 (repeatedly #(rand-int 10)))]

    (doseq [n nums]
      (push @websockets' client-uid :foo-topic n)))


  ;; C Generate SINE Wave
  (require '[clojure.data.csv :as csv]
           '[clojure.java.io :as io])

  (let [length 128
        records (sine-wave length)]
    (with-open [writer (io/writer "out-file.2.csv")]
      (csv/write-csv writer (take length records))))


  ;; D Push to Client

  ;; ok convert x values to time stamps
  ;; ok bump y values by 50
  ;; ok push new value to client, every 1/2 second
  ;; TODO put new time values to Highstock graph
  ;;   A. trnasform clj-time values -> epoch time
  ;;   B. Try with initData if the graph does appear at first

  (def sine-wave-seq
    (->> (sine-wave)
         (sine-wave-with-datetime (t/date-time 2020 01 01) 20)))
  (def control-chan (chan))


  (def push-fn
    (fn [[x y]]
      (let [client-uid (-> @(:connected-uids @websockets')
                           :any
                           first)]
        (push @websockets' client-uid :tick-topic [(c/to-long x) y]))))


  (stream-to-client! identity control-chan sine-wave-seq)
  (stream-to-client! push-fn control-chan sine-wave-seq)

  (>!! control-chan :exit)
  )
