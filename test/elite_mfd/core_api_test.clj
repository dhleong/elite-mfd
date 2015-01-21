(ns elite-mfd.core-api-test
  (:require [clojure.test :refer :all]
            [cheshire.core :refer [parse-string]]
            [elite-mfd.core-api :refer :all]))

;; somewhat bogus values for laziness
(def simple-stations-array
  (parse-string 
    "[
    {\"StationId\": 1794, \"Station\": \" Joule Relay\",
    \"System\": \"Ross 41\", \"SystemId\": 48174},
    {\"StationId\": 1881, \"Station\": \"4A504D\",
    \"System\": \"Lalande 4141\", \"SystemId\": 43803 },
    {\"StationId\": 3667, \"Station\": \"Aachen Town\",
    \"System\": \"Lalande 4141\", \"SystemId\": 43803 },
    {\"StationId\": 5200, \"Station\": \"Aaronson Landing\",
    \"System\": \"Hehebeche\", \"SystemId\": 38226},
    {\"StationId\": 1279, \"Station\": \"Aristotle Gateway\",
    \"System\": \"Ross 780\", \"SystemId\": 1131},
    {\"StationId\": 1397, \"Station\": \"Aristotle Gateway\",
    \"System\": \"Yakabugai\", \"SystemId\": 51290}
    ]" 
    true))

(deftest stations-map-handling
  (testing "Parse stations map json"
   (let [smap (parse-stations-map simple-stations-array)]
     (is (= 1 (count (get smap "Ross 41"))))
     (is (= 2 (count (get smap "Lalande 4141"))))
     (is (= "Aaronson Landing" (first (get smap "Hehebeche"))))))
  (testing "station-id works"
    (is (nil? (station-id nil)))
    (is (= 5200 (station-id "Aaronson Landing")))
    ;; without system, return (any) match
    (is (not (nil? (station-id "Aristotle Gateway"))))
    ;; return the right one if we include system
    (is (= 1397 (station-id "Aristotle Gateway (Yakabugai)")))  
    (is (= 1279 (station-id "Aristotle Gateway (Ross 780)")))))

(deftest systems-filter
  (testing "case insenstive"
    (is (contains?
         (set (filter-systems "era"))
         "Eravate"))))
