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
(def commander-data-read (atom false))

(defn- ensure-data-read
  "Returns the commander-data atom for convenience"
  [] 
  (do
    (when (and (not @commander-data-read)
               (.exists (file commander-data-file)))
      (when-let [old-data (slurp commander-data-file)]
        (when-let [parsed (parse-string old-data true)]
          (swap! commander-data-read (constantly true))
          (swap! commander-data (constantly parsed))
          (log "Read commander prefs" @commander-data))))
    commander-data))

(defn get-commander-field
  "Public for testing; generally you want to just use get-field"
  [cmdr field]
  {:pre [(keyword? field)
         (not (nil? cmdr))]}
  (get @cmdr field))

(defn set-commander-field
  "Public for testing; generally you want to just use set-field"
  [cmdr field value]
  {:pre [(keyword? field)
         (not (nil? cmdr))]}
  (swap! cmdr #(assoc % field value))
  (when (= cmdr commander-data)
    ; updating the global state (IE: not a test env)
    ; so persist the change
    (spit commander-data-file (generate-string @commander-data {:pretty true}))))

(defn get-field [field]
  (get-commander-field (ensure-data-read) field))

(defn set-field [field value]
  (set-commander-field (ensure-data-read) field value))


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
  (ensure-data-read)
  ; okay, now associate
  (assoc handlers
         :commander on-commander))
