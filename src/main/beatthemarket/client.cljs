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


;; (defonce app (app/fulcro-app {:remotes {:remote (fws/fulcro-websocket-remote {})}}))
(defonce ^:export app (atom nil))
(defonce ^:export SPA (atom nil))

#_(defn mount []
    (reset! SPA (fc/mount @SPA root/Root "app")))

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

(defn ^:export connect-websocket1 []
  (comp/transact! SPA `[(connect-socket {})]))

(defn ^:export connect-websocket2 []
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

  #_(reset! SPA (fc/make-fulcro-client
                  {:client-did-mount (fn [beatthemarket]
                                     (df/load beatthemarket :all-users root/User))
                 ;; This ensures your client can talk to a CSRF-protected server.
                 ;; See middleware.clj to see how the token is embedded into the HTML
                 :networking       {:remote (net/fulcro-http-remote
                                              {:url                "/api"
                                               :request-middleware secured-request-middleware})
                                    :websocket (fws/fulcro-websocket-remote {:csrf-token js/fulcro_network_csrf_token})}}))
  (start))
