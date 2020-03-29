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
(defonce ^:export stockChart (atom nil))

(defn mount []
  #_(reset! app (app/mount! @app root/Root "app")))

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

  ;; (log/info "push-handler received /" data)
  ;; (log/info "msg /" msg)

  ;; Highcharts.charts[0].series[0].addPoint(eval(e.data), true, false);
  (.addPoint @stockChart (msg true false)))


;; optionally you can listen for websocket state changes
(defn state-callback [before after]
  (log/info "state-callback: " {:before before
                                :after after}))

(defn ^:export setup-chart []

  (let [data [[1524063124446 295.63] [1524063136649 295.68] [1524063167106 295.72] [1524063193400 296.05] [1524063206587 296.09] [1524063215645 296.06] [1524063265087 295.92] [1524063311409 295.49] [1524063338332 295.26] [1524063344853 295.26] [1524063351681 295.12] [1524063355750 295.13] [1524063379607 295.28] [1524063391067 295.02] [1524063395580 294.94] [1524063398087 294.91] [1524063402099 294.91] [1524063403604 294.89] [1524063404857 294.8] [1524063411376 294.81] [1524063415960 294.78] [1524063424467 294.86] [1524063442026 294.82] [1524063460622 294.79] [1524063463438 294.53] [1524063470708 294.62] [1524063480986 294.72] [1524063487156 294.63] [1524063497936 294.85] [1524063507520 294.86] [1524063511470 294.95] [1524063514479 295.01] [1524063525510 295.19] [1524063528019 295.26] [1524063530526 295.16] [1524063536041 295.26] [1524063547390 295.16] [1524063550816 295.16] [1524063565854 295.25] [1524063569364 295.0] [1524063572715 294.96] [1524063599450 294.94] [1524063618791 294.93] [1524063632748 295.05] [1524063663732 295.41] [1524063680732 295.54] [1524063689507 295.53] [1524063697030 295.56] [1524063699036 295.63] [1524063700290 295.6] [1524063710651 295.57] [1524063739685 295.27] [1524063744516 295.01] [1524063749526 295.11] [1524063758625 295.16] [1524063768875 295.05] [1524063790225 294.82] [1524063792232 294.83] [1524063804067 295.21] [1524063811114 295.11] [1524063821395 294.88] [1524063831417 294.65] [1524063849317 294.75] [1524063857375 294.79] [1524063871893 294.78] [1524063881921 294.87] [1524063888940 294.82] [1524063914023 295.03] [1524063926447 294.83] [1524063948251 294.85] [1524063970327 294.73] [1524063973085 294.77] [1524063974087 294.81] [1524063984170 295.01] [1524064013345 295.01] [1524064019213 295.01] [1524064025097 295.09] [1524064030112 295.29] [1524064034375 295.3] [1524064041897 295.29] [1524064071990 295.3] [1524064084773 295.22] [1524064105247 295.31] [1524064117713 295.28] [1524064124908 295.36] [1524064130424 294.9] [1524064156770 295.11] [1524064168054 294.94] [1524064174072 295.0] [1524064178084 295.07] [1524064193497 294.96] [1524064198766 294.82] [1524064206310 294.81] [1524064208761 294.87] [1524064212077 294.9] [1524064221653 294.77] [1524064237231 295.24] [1524064240671 295.34] [1524064241923 295.35] [1524064245576 295.24]]
        data []
        chartOptions (clj->js {:rangeSelector {:selected 1}
                               :title {:text "Sine Wave Stock Price"}
                               :series [{:name "Sine Wave"
                                         :data data
                                         :tooltip { :valueDecimals 2 }}]})]

    (log/info "chartOptions / " chartOptions)
    (. js/Highcharts (stockChart "container" chartOptions))

    (let [a (.-charts js/Highcharts)
          _ (log/info "a /" a)

          achart (first a)
          _ (log/info "achart /" achart)

          b (.-series achart)
          _ (log/info "b /" b)

          bseries (first b)
          _ (log/info "bseries /" bseries)]

      (reset! stockChart bseries))))

(defn ^:export asdf []

  (.log js/console (.-charts js/Highcharts))

  (let [a (.-charts js/Highcharts)
        _ (log/info "a /" a)

        achart (first a)
        _ (log/info "achart /" achart)

        b (.-series achart)
        _ (log/info "b /" b)

        bseries (first b)
        _ (log/info "bseries /" bseries)]

    ;; (.addPoint bseries (data true false))

    )

  #_(let [a (.-charts js/Highcharts)
        achart (aget a 0)

        b (.-series a)
        bseries (aget b 0)]

    (log/info "a /" a)
    (log/info "achart /" achart)
    (log/info "b /" b)
    (log/info "bseries /" bseries)))

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

  ;; A
  ;; (start)

  ;; B
  (setup-chart))

(.addEventListener
  js/window "DOMContentLoaded"
  (fn []
    (.log js/console "DOMContentLoaded callback")
    (init)))
