(ns elite-mfd.navigate-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]
            [cheshire.core :refer [parse-string]]
            [org.httpkit.server :refer [Channel send!]]
            [elite-mfd.navigate :refer :all]))

(def dummy-plot-result
  "[{\"jump\":0,\"name\":\"Yakabugai\",\"distance\":\"0\"},{\"jump\":1,\"name\":\"Chamunda\",\"distance\":\"4.3770084675495\"},{\"jump\":2,\"name\":\"LTT 18486\",\"distance\":\"7.9636012778453\"}]")

(defn- do-test [func & {:keys [start end jump-distance then]} ]
  (let [called (ref false)
        result @(func
                  :start start
                  :end end
                  :jump-distance jump-distance
                  :callback (fn [result]
                              (dosync (ref-set called true))
                              (then result)))]
    (is (not (nil? result)))
    (is (= true @called))))

;;
;; Tests proper
;;
(deftest unit-plot
  (testing "Unit test plot-route"
    (with-fake-http [#"/10/Yakabugai/LTT\+18486$" dummy-plot-result]
      (do-test 
        plot-route
        :start "Yakabugai " ; sic: just in case, we trim off whitespace
        :end "LTT 18486"
        :jump-distance 10
        :then (fn [result]
                (is (seq? result))
                (is (= 3 (count result)))
                (is (= "Chamunda" (:name (second result)))))))))
