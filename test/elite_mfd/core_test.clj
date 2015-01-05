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
