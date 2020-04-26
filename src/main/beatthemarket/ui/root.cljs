(ns beatthemarket.ui.root
  (:require
   [fulcro.client.dom :as dom :refer [div ul li p h3]]
   [fulcro.client.primitives :as prim]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]

   [beatthemarket.model.user :as user]
   [beatthemarket.ui.stock-chart :as stock-chart]
   [taoensso.timbre :as log]))


(defsc Index [this {:index/keys [id text]}]
  {:query [:index/id :index/text]
   :ident (fn [] [:component/id :index])
   :route-segment ["index"]
   :initial-state {:index/id 1
                   :index/text :param/text}}

  (h3 text))

(def ui-index (comp/factory Index))

(defsc Landing [this {:landing/keys [id text]}]
  {:query [:landing/id :landing/text]
   :ident (fn [] [:component/id :landing])
   :route-segment ["landing"]
   :initial-state {:landing/id 1
                   :landing/text :param/text}}

  (h3 text))

(def ui-landing (comp/factory Landing))

(defsc Settings [this {:settings/keys [id text]}]
  {:query [:settings/id :settings/text]
   :ident (fn [] [:component/id :settings])
   :route-segment ["settings"]
   :initial-state {:settings/id 1
                   :settings/text :param/text}}

  (h3 text))

(def ui-settings (comp/factory Settings))

(defsc Game [this {:game/keys [id text]}]
  {:query [:game/id :game/text]
   :ident (fn [] [:component/id :game])
   :route-segment ["game"]
   :initial-state {:game/id 1
                   :game/text :param/text}}

  (h3 text))

(def ui-game (comp/factory Game))


(dr/defrouter TopRouter [this {:keys [current-state] :as props}]
  {:router-targets [Index Landing Game Settings]}

  (log/info "TopRouter Props /" props)
  (case current-state
    :pending (dom/div "Loading...")
    :failed (dom/div "Failed!")

    (dom/div "No route selected.")))

(def ui-top-router (comp/factory TopRouter))


;; (defsc Root [this {:root/keys [index landing game settings] :as props}]
;;   {:query [{:root/index (comp/get-query Index)}
;;            {:root/landing (comp/get-query Landing)}
;;            {:root/game (comp/get-query Game)}
;;            {:root/settings (comp/get-query Settings)}]
;;
;;    :initial-state {:root/index {:text "Index Text"}
;;                    :root/landing {:text "Landing Text"}
;;                    :root/game {:text "Game Text"}
;;                    :root/settings {:text "Settings Text"}}}
;;
;;   :query [{:root/router (comp/get-query TopRouter)}]
;;
;;
;;   (log/info "Root Props /" this props)
;;   (when index
;;     (dom/div (ui-index index))))

(defsc Root [this {:root/keys [router index landing game settings]}]
  {:query [{:root/router (comp/get-query TopRouter)}
           {:root/index (comp/get-query Index)}
           {:root/landing (comp/get-query Landing)}
           {:root/game (comp/get-query Game)}
           {:root/settings (comp/get-query Settings)}]

   :initial-state {:root/router {}
                   :root/index {:text "Index Text"}
                   :root/landing {:text "Landing Text"}
                   :root/game {:text "Game Text"}
                   :root/settings {:text "Settings Text"}}}

  (when router
    (dom/div (ui-top-router router))))
