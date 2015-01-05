(ns elite-mfd.server-test
  (:require [clojure.test :refer :all]
            [elite-mfd.server :refer :all]))

(deftest test-server-modify
  (testing "Add"
    (let [server (ref {:clients []})]
      (is (empty? (:clients @server)))
      (add-client server "hi")
      (is (= "hi" (-> @server :clients first)))))
  (testing "Remove")
    (let [server (ref {:clients ["hi"]})]
      (is (= 1 (count (:clients @server))))
      (remove-client server "not in it")
      (is (= 1 (count (:clients @server))))
      (remove-client server "hi")
      (is (empty? (:clients @server))))) 
 
