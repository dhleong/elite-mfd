(ns elite-mfd.trading-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]
            [cheshire.core :refer [parse-string]]
            [org.httpkit.server :refer [Channel send!]]
            [elite-mfd.trading :refer :all]))

;;
;; Constants
;;
(def dummy-calculation-result
  "{\"StationRoutes\":
  [{\"StartingStationId\":1046,\"StartingStationName\":\"Ackerman Market\",
  \"StartingSystemName\":\"Eravate\",\"DestinationStationId\":5792,
  \"DestinationStationName\":\"Ray Dock\",\"DestinationSystemName\":\"Eta Serpentis\",
  \"Distance\":18.01,
  \"CommodityId\":14,\"CommodityName\":\"Animal Meat\",\"Buy\":1054,\"Sell\":1580,
  \"GalacticAveragePrice\":1454,\"Profit\":526,\"Qty\":4,\"Total\":2104,
  \"LastUpdate\":\"1h 45m ago\",\"UpdatedBy\":\"kbclint\",
  \"DistanceFromJumpIn\":662.0}]}")

(def basic-calculate-packet
  {:type "on-calculate"
   :cash 1234
   :station-name "Foo"
   :station-name-end "Bar"
   })

;;
;; Util methods
;;
(defn- filter-stations-search
  "Returns a set containing the results
  of (filter-stations) with the given input,
  and extracting the given field"
  [input & {:keys [field] :or {field :Station}}]
  (set 
    (map #(get % field)
         (filter-stations input))))

(defn- do-test
  "Test the async calculate-trades method"
  [test-callback]
  (let [called (ref false)
        result @(calculate-trades
                  "Ackerman Market"
                  :callback (fn [result]
                              (dosync (ref-set called true))
                              (test-callback result)))]
    (is (not (nil? result)))
    (is (= true @called))))

(deftype DummyChannel [sent]
  Channel
  (send! [ch data]
    (swap! sent #(conj % data))))
(defn- get-sent
  "Get a vector of things sent to the client,
   parsed into clj data structures for convenience."
  [ch]
  (map #(parse-string % true) @(. ch sent)))

;;
;; Tests proper
;;
(deftest test-filter-stations
  (testing "Partial station name finds"
    (is (contains? 
          (filter-stations-search "aachen") 
          "Aachen Town")))
  (testing "System name finds"
    (is (contains? 
          (filter-stations-search "lalande") 
          "4A504D"))))

(deftest test-calculate
  (testing "Simple request"
    ; stub the http... wherever it's going, return the expected response
    (with-fake-http [#".*" dummy-calculation-result]
      (do-test 
        (fn [result]
          (is (vector? result))
          (is (not (empty? result)))
          (is (= 2104 (-> result first :Total)))))))
  (testing "Request Error"
    (with-fake-http [#".*" 400] ; server didn't like it
      (do-test
        (fn [result]
          (is (nil? result)))))))

(deftest test-handler
  (testing "calculate-packet-to-seq"
    ; should strip type and station-name and convert to seq
    (is (= [:station-name-end "Bar" :cash 1234] 
           (calculate-packet-to-seq basic-calculate-packet))))
  (testing "DummyChannel"
    (let [ch (DummyChannel. (atom []))]
      (send! ch "\"hi!\"")
      (is (= "hi!" (first (get-sent ch))))))
  (testing "handler"
    (with-fake-http [#".*" dummy-calculation-result]
      (let [ch (DummyChannel. (atom []))]
        ; run the packet handler
        @(on-calculate ch basic-calculate-packet)
        (let [sent (get-sent ch)]
          (is (= 1 (count sent))) ; only one sent packet
          (is (= "calculate-result" (-> sent first :type)))
          (is (= 1 (count (-> sent
                              first
                              :result))))
          (is (= 2104 (-> sent 
                          first ; the packet
                          :result
                          first :Total))))))))
