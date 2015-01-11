(ns ^{:author "Daniel Leong"
      :doc "Narration module for TTS feedback"}
  elite-mfd.narrate
  (:use [speech-synthesis.say :as say])
  (:import [java.util.concurrent Executors]))

(def narrate-queue (Executors/newFixedThreadPool 1))

(defn on-narrate
  "Speak the given text (asynchronously). 
   Successive requests are queued and executed sequentially"
  [ch packet]
  (-> narrate-queue
      (.execute 
        #(say/say (:text packet)))))

;;
;; Registration
;;
(defn register-handlers
  "Interface used by server for registering websocket packet handlers"
  [handlers]
  (assoc handlers
         :narrate on-narrate))
