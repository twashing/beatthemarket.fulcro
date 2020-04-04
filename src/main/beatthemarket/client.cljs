(ns beatthemarket.client
  (:require [taoensso.timbre :as log]
            [fulcro.client :as fc]

            [fulcro.client.network :as net]
            [fulcro.client.data-fetch :as df]

            [com.fulcrologic.fulcro.networking.websockets :as fws]
            [com.fulcrologic.fulcro.application :as app]

            [com.fulcrologic.fulcro.mutations :refer [defmutation]]
            [com.fulcrologic.fulcro.components :as comp]

            [beatthemarket.ui.root :as root]
            [beatthemarket.ui.stock-chart :as stock-chart]))


(defonce ^:export app (atom nil))
(def secured-request-middleware
  ;; The CSRF token is embedded via server_components/html.clj
  (->
    (net/wrap-csrf-token (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!"))
    (net/wrap-fulcro-request)))

(defmutation ^:export connect-socket [_]
  (websocket [_] true))

(defn ^:export connect-websocket []
  (comp/transact! app `[(connect-socket {})]))


(defn get-chart-series []
  (first
    (.-series
      (first
        (.-charts js/Highcharts)))))

(defn push-handler [{:keys [topic msg] :as data}]

  (log/info "push-handler received /" data)
  (log/info "msg /" msg)

  ;; Highcharts.charts[0].series[0].addPoint(eval(e.data), true, false);
  (.addPoint (get-chart-series) (msg true false)))


;; optionally you can listen for websocket state changes
(defn state-callback [before after]
  (log/info "state-callback: " {:before before
                                :after after}))

(defn ^:export init []

  (reset! app (app/fulcro-app {;; :client-did-mount (fn [beatthemarket]
                               ;;                     (df/load beatthemarket :all-users stock-chart/StockChart))

                               :remotes {:remote (net/fulcro-http-remote
                                                   {:url                "/api"
                                                    :request-middleware secured-request-middleware})
                                         :websocket (fws/fulcro-websocket-remote
                                                      {:csrf-token js/fulcro_network_csrf_token
                                                       :push-handler push-handler
                                                       :state-callback state-callback})}}))
  (app/mount! @app root/Root "app"))

(.addEventListener
  js/window "DOMContentLoaded"
  (fn []
    (.log js/console "DOMContentLoaded callback")
    (init)))
