(ns beatthemarket.client
  (:require [taoensso.timbre :as log]
            [fulcro.client :as fc]
            [beatthemarket.ui.root :as root]
            [fulcro.client.network :as net]
            [fulcro.client.data-fetch :as df]

            [com.fulcrologic.fulcro.networking.websockets :as fws]
            [com.fulcrologic.fulcro.application :as app]

            [com.fulcrologic.fulcro.mutations :refer [defmutation]]
            [com.fulcrologic.fulcro.components :as comp]))


(defonce ^:export app (atom nil))

(defn mount []
  (reset! app (app/mount! @app root/Root "app")))

(defn start []
  (mount))

(def secured-request-middleware
  ;; The CSRF token is embedded via server_components/html.clj
  (->
    (net/wrap-csrf-token (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!"))
    (net/wrap-fulcro-request)))

(defmutation ^:export connect-socket [_]
  (websocket [_] true))

(defn ^:export connect-websocket []
  (comp/transact! app `[(connect-socket {})]))

(defn push-handler [{:keys [topic msg] :as data}]
  (log/info "push-handler received: " data))

;; optionally you can listen for websocket state changes
(defn state-callback [before after]
  (log/info "state-callback: " {:before before
                                :after after}))

(defn ^:export init []

  (reset! app (app/fulcro-app {:client-did-mount (fn [beatthemarket]
                                                   (df/load beatthemarket :all-users root/User))

                               :remotes {:remote (net/fulcro-http-remote
                                                   {:url                "/api"
                                                    :request-middleware secured-request-middleware})
                                         :websocket (fws/fulcro-websocket-remote
                                                      {:csrf-token js/fulcro_network_csrf_token
                                                       :push-handler push-handler
                                                       :state-callback state-callback})}}))

  (start))
