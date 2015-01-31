(ns elite-mfd.macro-test
  (:require [clojure.test :refer :all]
            [elite-mfd.macro :refer :all]))

(def vk-s 83)
(def vk-shift (vk "shift"))

(deftest vk-and-bindings
  (testing "vk"
    (is (= vk-s (vk "s")))
    (is (= vk-s (vk "s ")))) ; handle extra spaces
  (testing "binding-to-vk"
    ;; TODO make sure custom bindings don't mess this up
    (is (= vk-s (binding-to-vk :ui-down)))
    (is (= vk-s (binding-to-vk "ui-down")))
    (is (= vk-s (binding-to-vk "ui-down "))))
  (testing "char-vk"
    (is (= {:vk vk-s, :with vk-shift}
           (char-vk "S"))))
  (testing "quoted binding to vk"
    (is (= [vk-s (vk "o") vk-s]
           (binding-to-vk "\"sos\"")))
    (is (= [(vk "minus") (vk "space") (vk "period") (vk "comma") ]
           (binding-to-vk "\"- .,\"")))
    (is (= [{:vk vk-s :with vk-shift} vk-s]
           (binding-to-vk "\"Ss\"")))
    ;; using qwerty default
    (is (= [{:vk (vk "1") :with vk-shift} {:vk (vk "slash") :with vk-shift}]
           (binding-to-vk "\"!?\""))))
  (testing "macro-wait"
    (is (= {:delay 850} (binding-to-vk "macro-wait")))))
