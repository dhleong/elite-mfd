(ns ^{:author "Daniel Leong"
      :doc "Trading module based on elitetradingtool.co.uk"}
  elite-mfd.trading
  (:require [elite-mfd.core-api :refer [get-stations]]))

(defn filter-stations
  "Return an array of stations matching the given name
  The system name is also matched"
  [raw-input]
  (let [input (.toLowerCase raw-input)]
    (filter 
      (fn [info]
        (let [station-name (.toLowerCase (:Station info))
              system-name (.toLowerCase (:System info))]
          (or 
            (.contains station-name input)
            (.contains system-name input))))
      (get-stations))))
