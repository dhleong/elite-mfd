(ns ^{:author "Daniel Leong"
      :doc "API calls/utils, etc. that are used in the core app"}
  elite-mfd.core-api
  (:require [cheshire.core :refer [parse-string]]
            [org.httpkit.client :as http]
            [elite-mfd.util :refer [log]]))

;;
;; Constants
;;
(def stations-url 
  "http://www.elitetradingtool.co.uk/api/EliteTradingTool/Stations?marketsOnly=true")

;;
;; Globals
;;
(defonce cached-stations (ref nil))
(defonce cached-stations-map (ref nil))

(defn parse-stations-map
  [array]
  (reduce
    (fn [smap info]
      (let [station (:Station info)
            system-name (:System info) ]
        (assoc smap 
               system-name
               (conj (get smap system-name [])
                     station))))
    {} array))

(defn ensure-stations-cached []
  (when (nil? @cached-stations)
    (let [{:keys [status body error] :as resp} @(http/get stations-url)]
      (if error
        (log "Could not get stations:" error)
        (when-let [array (parse-string body true)]
          (dosync 
            (ref-set cached-stations array)
            (ref-set cached-stations-map (parse-stations-map array))))))))

(defn get-system-stations
  "Get the names of stations within a given system"
  [system]
  (ensure-stations-cached)
  (if-let [cached @cached-stations-map]
    (get cached system []) 
    [])) ; network issue? be graceful

(defn get-stations
  "Returns a raw array of Station info maps"
  []
  (ensure-stations-cached)
  (if-let [cached @cached-stations]
    cached 
    [])) ; network issue? be graceful 

(defn station-id
  "Returns the id of a station from its name"
  [station-name]
  ; NB if performance becomes a problem, we can certainly cache this as well...
  (if (nil? station-name)
    nil ; quick shortcut
    (:StationId
      (first 
        (filter 
          #(= station-name (:Station %))
          (get-stations))))))
