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
          (callback (:StationRoutes (parse-string body true))))))))

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

(defn register-handlers
  "Interface used by server for registering websocket packet handlers"
  [handlers]
  (assoc handlers
         :calculate on-calculate))