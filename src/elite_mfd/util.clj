(ns ^{:author "Daniel Leong"
      :doc "Various utilities"}
  elite-mfd.util
  (:require [cheshire.core :refer [generate-string]]
            [org.httpkit.server :refer [send!]]
            ))

(defn each-client
  "Perform (pred client) for all clients connected"
  [server pred]
  (doall (map pred (:clients @server))))

(defn to-client
  [client obj]
  "Send a clj map as JSON to the client"
  ;; right now, just a wrapper; we may want to
  ;;  do some post-processing, however...
  (send! client (generate-string obj)))

(defn to-all
  "Send a message to all connected clients of the server"
  [server message]
  (each-client server 
    #(to-client % message)))

(defn log [& msg]
  "Global log utility"
  (apply println msg))

