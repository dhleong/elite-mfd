(ns ^{:author "Daniel Leong"
      :doc "The websocket server"}
  elite-mfd.server
  (:require [cheshire.core :refer [generate-string parse-string]]
            [elite-mfd
             [util :refer [log each-client to-client]]
             [commander :as commander]
             [core-api :as api]
             [macro :as macro]
             [trading :as trading]
             [navigate :as navigate]  
             [narrate :as narrate]])
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
              :stations (api/get-system-stations system)}))

(defn- client-handler
  [server handlers request]
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
    ;; give them known commander values
    (commander/on-connect channel)
    ;; pass incoming packets to registered handlers
    (on-receive channel 
                (fn [data]
                  (if-let [json (parse-string data true)]
                    (if-let [handler (-> json :type keyword handlers)]
                      (handler channel json)
                      (log "Unhandled packet:" data))
                    (log "Invalid json:" data))))))

(defn create-server
  "Initialize a server"
  [port]
  (let [server (ref {:clients [] :system nil})
        handlers (-> {}
                     api/register-handlers
                     commander/register-handlers
                     macro/register-handlers
                     trading/register-handlers
                     navigate/register-handlers  
                     narrate/register-handlers)]
    (run-server (partial client-handler server handlers) {:port port})
    (println "Websockets listening on" port)
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
