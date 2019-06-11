(ns ^:figwheel-hooks clubhouse.main
  (:require
    [reagent.core :as reagent]
    [re-frame.core :as re-frame]
    [day8.re-frame.http-fx]
    [clubhouse.events :as events]
    [clubhouse.views :as views]
    [clubhouse.config :as config]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)))

(defonce loaded? (atom false))

(defn mount-root []
  (let [can-mount? @loaded?]
    (when can-mount?
      (re-frame/clear-subscription-cache!)
      (reagent/render [#'views/main-panel]
                      (js/document.getElementById "app")))))

; Mount on reload
(mount-root)

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch-sync [::events/refresh-db])
  (dev-setup)
  (swap! loaded? (partial not))
  (mount-root))
