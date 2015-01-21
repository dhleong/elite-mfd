(ns elite-mfd.macro-test
  (:require [clojure.test :refer :all]
            [elite-mfd.macro :refer :all]))

(deftest vk-and-bindings
  (testing "vk"
    (is (= 40 (vk "down"))))
  (testing "binding-to-vk"
    ;; TODO make sure custom bindings don't mess this up
    (is (= 40 (binding-to-vk :ui-down)))
    (is (= 40 (binding-to-vk "ui-down")))))
