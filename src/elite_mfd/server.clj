(ns ^{:author "Daniel Leong"
      :doc "The websocket server"}
  elite-mfd.server
  (:require [cheshire.core :refer [generate-string]]
            [elite-mfd.core-api :refer [get-system-stations]])
  (:use org.httpkit.server)
  (:gen-class))

(defn- log [& msg]
  (apply println msg))

(defn add-client
  [server client]
  (dosync
    (alter server
      assoc-in [:clients] 
      (conj (:clients @server) client))))

(defn remove-client
  [server client]
  (dosync
    (alter server
      assoc-in [:clients] 
      (remove #(= % client) (:clients @server)))))

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

(defn notify-system
  [client system]
  (to-client client
             {:type :on-system
              :system system
              :stations (get-system-stations system)}))

(defn- create-handler
  [server]
  (fn [request]
    (with-channel request channel
      (log "* new client: " channel)
      (on-close channel
                (fn [status]
                  (log "# lost client: " channel)
                  (remove-client server channel)))

      ;; handle the client
      (add-client server channel)
      ;; if we know the system, tell them
      (if-let [system (:system @server)]
        (notify-system channel system))

      (on-receive channel 
                  (fn [data]
                    ;; TODO handle
                    ;; echo for now
                    (send! channel data)))
      )))

(defn create-server
  "Initialize a server"
  [port]
  (let [server (ref {:clients [] :system nil})]
    (run-server (create-handler server) {:port port})
    server))

(defn set-system
  "Notify the server that the active system has changed"
  [server system]
  (log " - Entered System: " system)
  (dosync
    (alter server assoc :system system))
  (if-not (nil? system)
    ;; notify clients of system change
    (each-client server #(notify-system % system))))
