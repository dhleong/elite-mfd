(ns elite-mfd.server
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

(defn- create-handler
  [server]
  (fn [request]
    (with-channel request channel
      (log "* new client: " channel)
      (on-close channel
                (fn [status]
                  (log "# lost client: " channel)
                  (remove-client server channel)))

      ; handle the client
      (add-client server channel)
      ; TODO if we know the system, tell them

      (on-receive channel 
                  (fn [data]
                    ; TODO handle
                    ; echo
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
  (dosync
    (alter server assoc :system system))
  ; TODO notify clients of system change
  )

(defn send-all
  "Send a message to all connected clients of the server"
  [server message]
  (map #(send! % message)
       (:clients @server)))
