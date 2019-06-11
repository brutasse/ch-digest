(ns clubhouse.db)

(defn valid-hash
  []
  (let [h (.substring js/location.hash 1)
        date (-> h js/moment (.format "YYYY-MM-DD"))]
    (when (= h date) h)))

(def min-day "2019-06-01")
(def max-day (-> (js/moment) (.subtract 1 "days") (.format "YYYY-MM-DD") str))
(def initial-day (or (valid-hash) max-day))

(def default-db
  {:pulling? false
   :min-day min-day
   :max-day max-day
   :day initial-day})
