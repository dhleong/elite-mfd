(ns ^{:author "Daniel Leong"
      :doc "API calls/utils, etc. that are used in the core app"}
  elite-mfd.core-api
  (:require [cheshire.core :refer [parse-string]]
            [org.httpkit.client :as http]))

;;
;; Constants
;;
(def stations-url 
  "http://www.elitetradingtool.co.uk/api/EliteTradingTool/Stations?marketsOnly=true")

;;
;; Globals
;;
(defonce cached-stations-map (ref nil))

(defn- log [& msg]
  (apply println msg))


(defn parse-stations-map
  [raw]
  (when-let [array (parse-string raw true)]
    (reduce
      (fn [smap info]
        (let [station (:Station info)
              system-name (:System info) ]
          (assoc smap 
                 system-name
                 (conj (get smap system-name [])
                       station))))
      {} array)))

(defn ensure-stations-cached []
  (when (nil? @cached-stations-map)
    (let [{:keys [status body error] :as resp} @(http/get stations-url)]
      (if error
        (log "Could not get stations:" error)
        (dosync (ref-set cached-stations-map (parse-stations-map body)))))))

(defn get-system-stations
  "Get the names of stations within a given system"
  [system]
  (ensure-stations-cached)
  (if-let [cached @cached-stations-map]
    (get @cached-stations-map system []) 
    [])) ; network issue? be graceful
