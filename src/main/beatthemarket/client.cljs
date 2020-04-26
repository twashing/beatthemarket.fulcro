(ns beatthemarket.client
  (:require [clojure.string :as str]
            [taoensso.timbre :as log]
            [pushy.core :as pushy]
            [edn-query-language.core :as eql]
            [fulcro.client :as fc]

            [fulcro.client.network :as net]
            [fulcro.client.data-fetch :as df]

            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [com.fulcrologic.fulcro.networking.websockets :as fws]
            [com.fulcrologic.fulcro.application :as app]

            [com.fulcrologic.fulcro.mutations :refer [defmutation]]
            [com.fulcrologic.fulcro.components :as comp]

            [beatthemarket.ui.root :as root]
            [beatthemarket.ui.stock-chart :as stock-chart]))


(defonce ^:export app (atom nil))
;; (def secured-request-middleware
;;   ;; The CSRF token is embedded via server_components/html.clj
;;   (->
;;     (net/wrap-csrf-token (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!"))
;;     (net/wrap-fulcro-request)))

(defmutation ^:export connect-socket [_]
  (websocket [_] true))

(defn ^:export connect-websocket []
  (comp/transact! app `[(connect-socket {})]))


;; ====>

(defn url->path
  "Given a url of the form \"/gift/123/edit?code=abcdef\", returns a
  path vector of the form [\"gift\" \"123\" \"edit\"]. Assumes the url
  starts with a forward slash. An empty url yields the path [\"home\"]
  instead of []."
  [url]
  (-> url (str/split "?") first (str/split "/") rest vec))

(defn path->url
  "Given a path vector of the form [\"gift\" \"123\" \"edit\"],
  returns a url of the form \"/gift/123/edit\"."
  [path]
  (str/join (interleave (repeat "/") path)))

(defn routable-path?
  "True if there exists a router target for the given path."
  [app path]
  (let [state-map  (app/current-state app)
        root-class (app/root-class @app)
        root-query (comp/get-query root-class state-map)
        ast        (eql/query->ast root-query)]
    (some? (dr/ast-node-for-route ast path))))

(def default-route ["index"])

(defonce history (pushy/pushy
                   (fn [path]
                     (log/info "pushy/pushy A /" path)
                     (dr/change-route app path))
                   (fn [url]
                     (log/info "pushy/pushy B /" url)
                     (let [path (url->path url)]
                       (log/info "pushy/pushy B.i /" path)
                       (if (routable-path? app path)
                         path
                         default-route)))))

;; ====>

(defn get-chart-series []
  (first
    (.-series
      (first
        (.-charts js/Highcharts)))))

(defn push-handler [{:keys [topic msg] :as data}]

  (log/info "push-handler received /" data)
  (log/info "msg /" msg)

  (.addPoint (get-chart-series) (msg true false)))


;; optionally you can listen for websocket state changes
(defn state-callback [before after]
  (log/info "state-callback: " {:before before
                                :after after}))

(defn ^:export init []

  (reset! app (app/fulcro-app {;; :client-did-mount (fn [beatthemarket]
                               ;;                     (df/load beatthemarket :all-users stock-chart/StockChart))

                               ;; :remotes {:remote (net/fulcro-http-remote
                               ;;                     {:url                "/api"
                               ;;                      ;; :request-middleware secured-request-middleware
                               ;;                      })
                               ;;           :websocket (fws/fulcro-websocket-remote
                               ;;                        {;; :csrf-token js/fulcro_network_csrf_token
                               ;;                         :push-handler push-handler
                               ;;                         :state-callback state-callback})}
                               }))
  (app/mount! @app root/Root "app" {:initialize-state? true})
  (dr/initialize! @app)
  (pushy/start! history)
  (dr/change-route app (dr/path-to root/Index)))

(defn route-to! [path]

  ;; (log/info "route-to! /" path)
  (pushy/set-token! history (path->url path)))

;; TODO On initial load, use URI route
;; TODO Fix route-to mutation
(defmutation route-to
  "Mutation to go to a specific route"
  [{:keys [path]}]
  (action [_]
          (log/info "route-to / path /" path)
          (route-to! path)))

(comment

  ;; shadow-cljs clj-repl
  (shadow/watch :main)
  (shadow/repl :main)

  (shadow/repl :main)


  (ns beatthemarket.client)
  (require '[com.fulcrologic.fulcro.algorithms.merge :as merge]
           '[com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
           '[com.fulcrologic.fulcro.application :as app]
           '[com.fulcrologic.fulcro.components :as comp]
           '[com.fulcrologic.fulcro.mutations :as m :refer [defmutation]])

  (app/current-state app)
  (comp/get-initial-state root/Root)
  (comp/get-query root/Root)

  (merge/merge-component!
    app root/Root {:index/id 2
                   :index/text "Foobar"})

  (dr/change-route app (dr/path-to root/Index))
  (dr/change-route app ["index"])

  (route-to! ["settings"])
  (route-to! ["game"])
  (route-to {:path (dr/path-to root/Settings)}))
