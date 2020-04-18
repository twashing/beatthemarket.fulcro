(ns beatthemarket.ui.stock-chart
  (:require
   [cljs.pprint :refer [pprint]]
   [fulcro.client.dom :as dom :refer [div ul li p h3]]
   [fulcro.client.primitives :as prim :refer [defsc]]
   [beatthemarket.model.user :as user]
   [beatthemarket.ui.components :as comp]
   [taoensso.timbre :as log]))


;; TODO namespace db keywords
;; TODO put query and tick updates locally in StockChart?
;;   don't rerender when we get new ticks

(defsc StockChart [this _]
  {:initial-state (fn [_] {:tick []})

   :componentDidMount (fn [_]
                        (let [data []
                              chartOptions (clj->js {:rangeSelector {:selected 1}
                                                     :title {:text "Sine Wave Stock Price"}
                                                     :series [{:name "Sine Wave"
                                                               :data data
                                                               :tooltip { :valueDecimals 2 }}]})]

                          (log/info "chartOptions / " chartOptions)
                          (.. js/Highcharts (stockChart "stock-chart" chartOptions))))}

  (div :#stock-chart.panel.callout.radius
       "StockChart Component"))

(def ui-stock-chart (prim/factory StockChart))
