(ns elite-mfd.core-test
  (:require [clojure.test :refer :all]
            [clojure.zip :refer [xml-zip]]
            [clojure.data.zip.xml :refer [xml1-> attr=]]
            [elite-mfd.core :refer :all]))

(def missing-logging
  "<AppConfig>
      <Network
        Port=\"0\"
        upnpenabled=\"1\"
        LogFile=\"netLog\"
        DatestampLog=\"1\"
        >
      </Network>
    <GameObjects />
  </AppConfig>")

(def has-logging
  "<AppConfig>
      <Network
        Port=\"0\"
        upnpenabled=\"1\"
        LogFile=\"netLog\"
        DatestampLog=\"1\"
        VerboseLogging=\"1\"
        >
      </Network>
    <GameObjects />
  </AppConfig>")

(def missing-logging-reader
  (java.io.StringReader. missing-logging))
(def has-logging-reader
  (java.io.StringReader. has-logging))

(def system-log-line
  "{00:38:48} System:21(Eravate) Body:50 Pos:(-643.133,465.037,-608.375)")
(def system-log-line2
  "{00:38:48} System:21(Yakabugai) Body:50 Pos:(-643.133,465.037,-608.375)")

(deftest test-fix-config
  (testing "fix AppConfig.xml"
    (let [result (fix-config-reader missing-logging-reader)]
      (is (not (nil? result)))
      (is (not= nil (xml1-> 
                   (xml-zip result)
                   :Network
                   (attr= :VerboseLogging "1"))))))
  (testing "nop for fixed AppConfig.xml"
    (is (= nil (fix-config-reader has-logging-reader))))) 

(deftest system-extraction
  (testing "System name extractor"
    (is (= "Eravate" (extract-system-name system-log-line)))))

(defn- string-reader [in]
  (java.io.BufferedReader.
    (java.io.StringReader. in)))

(deftest test-system-poller-loop
  (testing "Absolutely nothing"
    (let [last-system (ref nil)
          [file stream] (system-poller-loop 
                          nil nil  ; no last-file/stream yet
                          :pick-log #(identity nil)
                          :open-log #(is (nil? %)) ; should not be called
                          :callback #(dosync (ref-set last-system %)))]
      (is (nil? file))
      (is (nil? stream))
      (is (nil? @last-system))))
  (testing "First run"
    (let [last-system (ref nil)
          [file stream] (system-poller-loop 
                          nil nil  ; no last-file/stream yet
                          :pick-log #(identity "file1")
                          :open-log (fn [&_] (string-reader system-log-line))
                          :callback #(dosync (ref-set last-system %)))]
      (is (= file "file1"))
      (is (not (nil? stream)))
      (is (= "Eravate" @last-system))))
  (testing "Keep Reading"
    (let [last-system (ref nil)
          last-stream (string-reader system-log-line)
          [file stream] (system-poller-loop
                          "file1" last-stream
                          :pick-log #(identity "file1")
                          :open-log #(is (nil? %)) ; should not be called
                          :callback #(dosync (ref-set last-system %))) ]
      (is (= "file1" file))
      (is (= last-stream stream)) ; same stream
      (is (= "Eravate" @last-system))))
  (testing "Do nothing on none left"
    (let [last-system (ref nil)
          last-stream (string-reader "")
          [file stream] (system-poller-loop
                          "file1" last-stream
                          :pick-log #(identity "file1")
                          :open-log #(is (nil? %)) ; should not be called
                          :callback #(dosync (ref-set last-system %)))]
      (is (= "file1" file)) ; same
      (is (= last-stream stream)) ; same
      (is (nil? @last-system))))
  (testing "Switch to new file"
    (let [last-system (ref nil)
          last-stream (string-reader system-log-line)
          [file stream] (system-poller-loop
                          "file1" last-stream
                          :pick-log #(identity "file2")
                          :open-log (fn [&_] (string-reader system-log-line2))
                          :callback #(dosync (ref-set last-system %)))]
      (is (= "file2" file)) ; new!
      (is (not= last-stream stream)) ; new!
      (is (= "Yakabugai" @last-system)))))
