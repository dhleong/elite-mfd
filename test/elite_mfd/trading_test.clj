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

;; brace yourself...
(def dummy-search-result
  "{\"Results\":[{
  \"SystemName\":\"Eravate\",\"Buy\":0,\"Sell\":176,\"LastUpdate\":\"15h 38m ago\",
  \"Station\":{\"Id\":1008,\"Name\":\"Sylvester City\",\"SystemId\":1048,\"EconomyId\":4,
  \"Economy\":{\"Id\":4,\"Name\":\"Industrial\"},
  \"SecondaryEconomyId\":null,\"SecondaryEconomy\":null,\"GovernmentId\":5,\"Government\":null,
  \"AllegianceId\":4,\"Allegiance\":null,\"HasBlackmarket\":false,
  \"HasMarket\":true,\"HasOutfitting\":false,\"HasShipyard\":false,\"HasRepairs\":true,
  \"DistanceFromJumpIn\":496.0,\"Version\":1.0,\"StationTypeId\":6,
  \"StationType\":{\"Id\":6,\"Name\":\"Industrial Outpost\",\"PrimaryType\":2,
  \"PadSmall\":true,\"PadMedium\":true,\"PadLarge\":false,\"Pads\":\"Small, Medium\"},
  \"StationCommodities\":[{\"Id\":46592,\"StationId\":1008,\"CommodityId\":59,\"Commodity\":null,\"Buy\":0,\"Sell\":176,\"LastUpdate\":\"2015-01-07T02:12:41.643\",\"UpdatedBy\":\"BearOverlord\",\"Version\":1.0}],
  \"Services\":\"Commodities Market, Repairs\",\"EconomyString\":\"Industrial\"},
  \"System\":{\"Id\":1048,\"Name\":\"Eravate\",\"GovernmentId\":5,
  \"Government\":{\"Id\":5,\"Name\":\"Democracy\"},\"AllegianceId\":4,
  \"Allegiance\":{\"Id\":4,\"Name\":\"Independent\"},
  \"X\":-42.4375,\"Y\":-3.15625,\"Z\":59.65625,\"Version\":1.0,\"DevData\":false,
  \"Checked\":false,\"CheckedDate\":null,
  \"CheckedBy\":\"\",\"Economy\":\"Industrial\"},
  \"Distance\":0.0}]}")


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

(defn- do-test [func test-callback]
  (let [called (ref false)
        result @(func
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
      (do-test calculate-trades
        (fn [result]
          (is (vector? result))
          (is (not (empty? result)))
          (is (= 2104 (-> result first :Total)))))))
  (testing "Request Error"
    (with-fake-http [#".*" 400] ; server didn't like it
      (do-test calculate-trades
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

(deftest test-search
  (testing "Simple request"
    ; stub the http... wherever it's going, return the expected response
    (with-fake-http [#".*" dummy-search-result]
      (do-test search-stations
        (fn [result]
          (is (vector? result))
          (is (not (empty? result)))
          (is (= "Eravate" (-> result first :SystemName)))))))
  (testing "Request Error"
    (with-fake-http [#".*" 400] ; server didn't like it
      (do-test search-stations
        (fn [result]
          (is (nil? result)))))))

