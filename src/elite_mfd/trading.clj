(ns ^{:author "Daniel Leong"
      :doc "Trading module based on elitetradingtool.co.uk"}
  elite-mfd.trading
  (:require [cheshire.core :refer [generate-string parse-string]]
            [org.httpkit.client :as http]
            [elite-mfd.util :refer [log to-client client-error]]  
            [elite-mfd.core-api :refer [get-stations station-id]]))

;;
;; Constants
;;
(def calculate-url 
  "http://www.elitetradingtool.co.uk/api/EliteTradingTool/Calculator")
(def search-url 
  "http://www.elitetradingtool.co.uk/api/EliteTradingTool/Search")

;;
;; Util methods
;;
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

;;
;; API calls
;;
(defn calculate-trades
  "Call the trade calculator to request best trades
   starting at the given station and (optionally)
   ending at another station, constrained by several options.
   In particular, you should provide the :callback option if
   you hope to do anything useful with the results. The callback
   should accept a single argument which is either nil on error,
   or a vector containing suggested trades"
  [station-name-start &
   {:keys [callback cargo cash station-name-end min-profit pad-size search-range]
    :or {callback identity
         cargo 4
         cash 1000
         min-profit 500
         pad-size :Small
         search-range "15"}}]
  (let [request-body {:Cargo cargo
                      :Cash cash
                      :EndStationId (station-id station-name-end)
                      :MinProfit min-profit
                      :PadSize pad-size
                      :SearchRange search-range
                      :StartStationId (station-id station-name-start)}]
    (http/post 
      calculate-url
      {:headers {"Content-Type" "application/json"}
       :body (generate-string request-body)}
      (fn [{:keys [error body]}]
        (if error
          (do 
            (log "! Error calculating:" error "Request:" request-body)
            (callback nil))
          ; TODO parse results to a friendly format so any changes
          ;  to the service's format are transparent to clients
          (callback (:StationRoutes (parse-string body true))))))))

(defn search-stations
  "Call the station searcher with various filters"
  [system-name &
   {:keys [callback commodity-id pad-size search-type search-range]
    :or {callback identity
         commodity-id nil
         pad-size "Small"
         search-type "Station Selling" ; or "Station Buying"; req for commodity-id
         search-range "15"}}]
  (let [request-body {:CurrentLocation system-name
                      :Commodity (if commodity-id true false)
                      :CommodityId commodity-id
                      :PadSize pad-size
                      :SearchType search-type
                      :SearchRange search-range}]
    (println request-body)
    (http/post 
      search-url
      {:headers {"Content-Type" "application/json"}
       :body request-body}
      (fn [{:keys [error body]}]
        (println body)
        (if error
          (do 
            (log "! Error searching" error "Request:" request-body)
            (callback nil))
          (callback (:Results (parse-string body true))))))))

;;
;; Packet handlers and related
;;
(defn calculate-packet-to-seq
  "Take a packet map for on-calculate and turn it into a sequence"
  [packet]
  ; should be sufficient for now
  (flatten (seq (dissoc packet :type :station-name))))

(defn on-calculate
  "Packet handler for :calculate"
  [ch packet]
  (if-let [station (:station-name packet)]
    ; NB this seems overly complicated
    (apply calculate-trades 
           (flatten (conj [station] 
                          (calculate-packet-to-seq packet)
                          :callback #(to-client ch 
                                                {:type :calculate-result
                                                 :result %}))))
    (client-error ch "Must specify starting station")))

(defn on-search
  "Packet handler for :search"
  [ch packet]
  ; TODO implement
  (if-let [system (:system packet)]
    ; NB this seems overly complicated
    (apply search-stations
           (flatten (conj [system] 
                          (calculate-packet-to-seq packet)
                          :callback #(to-client ch %))))
    (client-error ch "Must specify system")))

;;
;; Registration
;;
(defn register-handlers
  "Interface used by server for registering websocket packet handlers"
  [handlers]
  (assoc handlers
         :calculate on-calculate
         :search on-search))
