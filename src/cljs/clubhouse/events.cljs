(ns clubhouse.events
  (:require
    [re-frame.core :as re-frame]
    [clojure.string :as str]
    [clubhouse.db :as db]
    [ajax.core :as ajax]
    [cljsjs.moment]
    [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]))

(re-frame/reg-event-db
  ::initialize-db
  (fn-traced [_ _] db/default-db))

(re-frame/reg-event-fx
  ::change-day
  (fn-traced
    [{:keys [db]} [_ increment]]
    (let [new-day (-> db
                    :day
                    js/moment
                    (.add increment "days")
                    (.format "YYYY-MM-DD"))]
      (js/location.replace (str (-> js/location.href (str/split "#") first)
                                "#"
                                new-day))
      {:db (assoc db :day (max (:min-day db) new-day))
       :dispatch [::refresh-db]})))

(re-frame/reg-event-fx
  ::refresh-db
  (fn-traced
    [{:keys [db]} _]
    (let [date (:day db)]
      {:db (assoc db :pulling? true)
       :http-xhrio {:method :get
                    :uri (str "/api/" date ".json")
                    :timeout 8000
                    :response-format (ajax/json-response-format
                                       {:keywords? true})
                    :on-success [::refresh-success]
                    :on-failure [::refresh-failure]}})))

(re-frame/reg-event-db
  ::refresh-success
  (fn-traced
    [db [_ {:keys [events members projects stories]}]]
    (assoc db
           :events events
           :members members
           :projects projects
           :stories stories
           :pulling? false)))

(re-frame/reg-event-db
  ::refresh-failure
  (fn-traced
    [db [_ _]]
    (-> db
      (dissoc :events :members :projects :stories)
      (assoc :pulling? false))))
