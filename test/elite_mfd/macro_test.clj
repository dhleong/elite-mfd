(ns elite-mfd.macro-test
  (:require [clojure.test :refer :all]
            [elite-mfd.macro :refer :all]))

(def vk-s 83)

(deftest vk-and-bindings
  (testing "vk"
    (is (= vk-s (vk "s")))
    (is (= vk-s (vk "s ")))) ; handle extra spaces
  (testing "binding-to-vk"
    ;; TODO make sure custom bindings don't mess this up
    (is (= vk-s (binding-to-vk :ui-down)))
    (is (= vk-s (binding-to-vk "ui-down")))
    (is (= vk-s (binding-to-vk "ui-down "))))
  (testing "quoted binding to vk")
    (is (= [vk-s (vk "o") vk-s]
           (binding-to-vk "\"sos\"")))) 
