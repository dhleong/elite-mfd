(ns elite-mfd.trading-test
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
