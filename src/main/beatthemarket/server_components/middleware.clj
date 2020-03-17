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

#_(def ^:private not-found-handler
  (fn [req]
    {:status  404
     :headers {"Content-Type" "text/plain"}
     :body    "NOPE"}))


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
                                  :sente-options       {:csrf-token-fn nil}}))
        defaults-config (:ring.middleware/defaults-config config)
        legal-origins   (get config :legal-origins #{"localhost"})]

    (fwsp/add-listener websockets beatthemarket-ws-listener)
    (reset! websockets' websockets)

    #_(-> not-found-handler
          (wrap-api "/api")
          (fws/wrap-api websockets)
          server/wrap-transit-params
          server/wrap-transit-response
          (wrap-html-routes)
          ;; If you want to set something like session store, you'd do it against
          ;; the defaults-config here (which comes from an EDN file, so it can't have
          ;; code initialized).
          ;; E.g. (wrap-defaults (assoc-in defaults-config [:session :store] (my-store)))
          (wrap-defaults defaults-config)
          wrap-gzip)

    #_(-> not-found-handler
          (fws/wrap-api websockets)
          wrap-keyword-params
          wrap-params
          (wrap-resource "public")
          wrap-content-type
          wrap-not-modified)

    (-> not-found-handler
        (wrap-api "/api")
        (fws/wrap-api websockets)
        ;; wrap-keyword-params
        ;; wrap-params
        ;; (wrap-resource "public")
        ;; wrap-content-type
        ;; wrap-not-modified

        server/wrap-transit-params
        server/wrap-transit-response
        (wrap-html-routes)
        ;; If you want to set something like session store, you'd do it against
        ;; the defaults-config here (which comes from an EDN file, so it can't have
        ;; code initialized).
        ;; E.g. (wrap-defaults (assoc-in defaults-config [:session :store] (my-store)))
        (wrap-defaults defaults-config)
        wrap-gzip)))
