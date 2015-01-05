(ns elite-mfd.trading-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]
            [cheshire.core :refer [parse-string]]
            [elite-mfd.trading :refer :all]))

(defn- filter-stations-search
  "Returns a set containing the results
  of (filter-stations) with the given input,
  and extracting the given field"
  [input & {:keys [field] :or {field :Station}}]
  (set 
    (map #(get % field)
         (filter-stations input))))

(deftest test-filter-stations
  (testing "Partial station name finds"
    (is (contains? 
          (filter-stations-search "aachen") 
          "Aachen Town")))
  (testing "System name finds"
    (is (contains? 
          (filter-stations-search "lalande") 
          "4A504D"))))

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

(defn- do-test [test-callback]
  (let [called (ref false)
        result @(calculate-trades
                  "Ackerman Market"
                  :callback (fn [result]
                              (dosync (ref-set called true))
                              (test-callback result)))]
    (is (not (nil? result)))
    (is (= true @called))) 
  )

(deftest test-calculate
  (testing "Simple request"
    ; stub the http... wherever it's going, return the expected response
    (with-fake-http [#".*" dummy-calculation-result]
      (do-test 
        (fn [result]
          (is (vector? result))
          (is (not (empty? result)))
          (is (= 2104 (-> result first :Total))) 
          ))))
  (testing "Request Error"
    (with-fake-http [#".*" 400] ; server didn't like it
      (do-test
        (fn [result]
          (is (nil? result)))))))
