(ns beatthemarket.client
  (:require [fulcro.client :as fc]
            [beatthemarket.ui.root :as root]
            [fulcro.client.network :as net]
            [fulcro.client.data-fetch :as df]

            [com.fulcrologic.fulcro.networking.websockets :as fws]
            [com.fulcrologic.fulcro.application :as app]

            [com.fulcrologic.fulcro.mutations :refer [defmutation]]
            [com.fulcrologic.fulcro.components :as comp]))


;; (defonce app (app/fulcro-app {:remotes {:remote (fws/fulcro-websocket-remote {})}}))
(defonce SPA (atom nil))

(defn mount []
  (reset! SPA (fc/mount @SPA root/Root "app")))

(defn start []
  (mount))

(def secured-request-middleware
  ;; The CSRF token is embedded via server_components/html.clj
  (->
    (net/wrap-csrf-token (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!"))
    (net/wrap-fulcro-request)))


(defmutation connect-socket [_]
  (websocket [_] true))

(defn ^:export connect-websocket []
  (comp/transact! SPA `[(connect-socket {})]))


(defn ^:export init []
  (reset! SPA (fc/make-fulcro-client
                {:client-did-mount (fn [beatthemarket]
                                     (df/load beatthemarket :all-users root/User))
                 ;; This ensures your client can talk to a CSRF-protected server.
                 ;; See middleware.clj to see how the token is embedded into the HTML
                 :networking       {:remote (net/fulcro-http-remote
                                              {:url                "/api"
                                               :request-middleware secured-request-middleware})
                                    :websocket (fws/fulcro-websocket-remote {})}}))
  (start))
