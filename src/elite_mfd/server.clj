(ns ^{:author "Daniel Leong"
      :doc "The websocket server"}
  elite-mfd.server
  (:require [cheshire.core :refer [generate-string parse-string]]
            [elite-mfd.util :refer [log each-client to-client]]
            [elite-mfd.core-api :refer [get-system-stations]]
            [elite-mfd.trading :as trading])
  (:use org.httpkit.server))

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

(defn notify-system
  [client system]
  (to-client client
             {:type :on-system
              :system system
              :stations (get-system-stations system)}))

(defn- create-handler
  [server handlers]
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
      ;; pass incoming packets to registered handlers
      (on-receive channel 
                  (fn [data]
                    (if-let [json (parse-string data true)]
                      (if-let [handler (-> json :type keyword handlers)]
                        (handler channel json)
                        (log "Unhandled packet:" json))
                      (log "Invalid json:" data)))))))

(defn create-server
  "Initialize a server"
  [port]
  (let [server (ref {:clients [] :system nil})
        handlers (-> {}
                     trading/register-handlers)]
    (run-server (create-handler server handlers) {:port port})
    (println "Server listening on" port)
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
