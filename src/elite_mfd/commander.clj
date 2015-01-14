(ns ^{:author "Daniel Leong"
      :doc "Automatic storage of arbitrary commander's prefs"}
  elite-mfd.commander
  (:require [clojure.java.io :refer [file]]
            [cheshire.core :refer [generate-string parse-string]]
            [elite-mfd.util :refer [log to-client client-error]]))

;;
;; Constants
;;
(def commander-data-file "commander.json")

;;
;; global state :O
;;
(def commander-data (atom {}))

(defn get-commander-field
  [cmdr field]
  {:pre [(keyword? field)
         (not (nil? cmdr))]}
  (get @cmdr field))

(defn set-commander-field
  [cmdr field value]
  {:pre [(keyword? field)
         (not (nil? cmdr))]}
  (swap! cmdr #(assoc % field value))
  (when (= cmdr commander-data)
    ; updating the global state (IE: not a test env)
    ; so persist the change
    (spit commander-data-file (generate-string @commander-data {:pretty true}))))

;;
;; Handlers
;;
(defn on-connect
  [ch]
  ; let them know what we have
  (to-client ch {:type :commander-data
                 :data @commander-data }))

(defn on-commander
  [_ packet & {:keys [cmdr] :or {cmdr commander-data}}]
  (let [field (-> packet :field keyword)]
    (set-commander-field cmdr field (:value packet))))

;;
;; Registration
;;
(defn register-handlers
  "Interface used by server for registering websocket packet handlers"
  [handlers]
  ; let's use this chance to initialize our values
  (when (.exists (file commander-data-file))
    (when-let [old-data (slurp commander-data-file)]
      (when-let [parsed (parse-string old-data true)]
        (swap! commander-data (fn [_] identity parsed))
        (log "Read commander prefs" @commander-data))))
  ; okay, now associate
  (assoc handlers
         :commander on-commander))
