(ns elite-mfd.commander-test
  (:require [clojure.test :refer :all]
            [elite-mfd.commander :refer :all]))

(deftest test-read-write
  (testing "Set and get"
    (let [cmdr (atom {})]
      (is (nil? (get-commander-field cmdr :cash)))
      (set-commander-field cmdr :cash 42)
      (is (= 42 (get-commander-field cmdr :cash)))))
  (testing "Handler"
    (let [cmdr (atom {})]
      (is (nil? (get-commander-field cmdr :name)))
      (on-commander nil
                    {:field "name" :value "Malcom Reynolds"}
                    :cmdr cmdr) ; use our test atom
      (is (= "Malcom Reynolds" (get-commander-field cmdr :name))))))
