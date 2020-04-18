(ns beatthemarket.ui.root
  (:require
    [fulcro.client.dom :as dom :refer [div ul li p h3]]
    [fulcro.client.primitives :as prim :refer [defsc]]
    [beatthemarket.model.user :as user]
    [beatthemarket.ui.components :as comp]
    [beatthemarket.ui.stock-chart :as stock-chart]
    [taoensso.timbre :as log]))


(defsc Root [this _]
  {:initial-state (fn [_]
                    {:stock-chart (prim/get-initial-state stock-chart/StockChart {:label "StockChart"})})

   :componentDidMount (fn [_] (log/info (prim/get-initial-state Root {})))}

  (div
    (stock-chart/ui-stock-chart {})))
