;; Without this, we create a dock icon on OSX.
;;  Not a big deal, but may be nicer when OSX client is released
;;  since we should be run in a terminal anyway...
(System/setProperty "apple.awt.UIElement" "true")

;; Normal namespace setup
(ns ^{:author "Daniel Leong"
      :doc "Config/Prep/Startup"} 
  elite-mfd.core
  (:require [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [clojure.data.xml :refer [parse emit]]
            [clojure.zip :refer [xml-zip root edit node]]
            [clojure.data.zip.xml :as c-d-z-xml
             :refer [xml-> xml1-> attr attr= text]]
            [clojure.java.io :refer [file reader writer]]
            [org.httpkit.server :refer [run-server]]
            [compojure
             [core :refer [defroutes GET]]
             [route :as route]]
            [elite-mfd
             [util :refer [log]] 
             [server :refer [create-server set-system]]])
  (:import  [java.io RandomAccessFile])
  (:gen-class))

;;
;; Config
;;
(def app-data (file (System/getProperty "user.home") "AppData"))
(def product-root (if (.exists app-data) 
                    ; actual windows machine
                    (file app-data "Local/Frontier_Developments/Products")
                    ; dev environment
                    (file (System/getProperty "user.home") "Desktop")))
(def http-port 9876)
(def websockets-port 9877)
(def nrepl-port 7888)

;;
;; Constants
;;
(def product-name "FORC-FDEV-D-1010")
(def product-dir (file product-root product-name))
(def app-config-path (file product-dir "AppConfig.xml"))
(def app-config-backup-path (file product-dir "AppConfig.xml.bak"))
(def logs-dir (file product-dir "Logs"))

(defn fix-config-reader [in]
  "Ensure the AppConfig.xml file is fixed.
   Returns nil if it's good, else the root Element with the attribute added"
  (let [z (xml-zip (parse in))
        network (xml1-> z :Network)
        verbose (attr network :VerboseLogging)]
    (when (or (nil? verbose) (= "0" verbose))
      (root
        (edit network #(assoc-in % [:attrs :VerboseLogging] "1"))))))

(defn- fix-config []
  "Ensure the AppConfig.xml file is fixed"
  (when (.exists app-config-path)
    (when-let [el (with-open [in (reader app-config-path)]
                    (fix-config-reader in))]
      ; backup the file...
      (spit app-config-backup-path (slurp app-config-path))
      ; ... because our replacement trashes the formatting
      (with-open [out (writer app-config-path)]
        (emit el out)))))

(defn pick-log-file []
  "Pick most recent netLog file"
  (let [netlogs (filter #(-> %
                             (.getName)
                             (.startsWith "netLog"))
                        (file-seq logs-dir))]
    (-> netlogs
        sort
        last)))

(defn- slurp-lines
  [stream]
  (if-let [line (.readLine stream)]
    (lazy-seq (cons line (slurp-lines stream)))))

;; is the Body useful?
(def system-matcher #"System:\d+\((?<System>[^\)]+)\) Body:")
(defn extract-system-name [line]
  (when-let [match (.matcher system-matcher line)]
    (when (.find match)
      (.group match "System"))))

(defn system-poller-loop
  "One loop of the system-poller"
  [last-file last-stream & {:keys [pick-log open-log callback] :as opts}]
  (when-let [new-file (pick-log)]
    (if (= new-file last-file)
      ; same file; keep reading
      (do
        (doseq [line (slurp-lines last-stream)]
          (if-let [system (extract-system-name line)]
            (callback system)))
        ; return file/stream as deconstructable vector
        [last-file last-stream])
      ; else, new file; open and start over
      (do
        (if-not (nil? last-stream)
          (.close last-stream))
        (println "Discovered new log " new-file)
        (recur new-file (open-log new-file) opts)))))

(defn system-poller [pred]
  "Polls for changes in the system 
  and calls the predicate when it changed"
  (let [opts {:pick-log pick-log-file
              :open-log #(RandomAccessFile. % "r")
              :callback pred }]
    (loop [last-file nil
           last-stream nil]
      ; is there a better way to do this?!
      (let [[new-file new-stream] (apply 
                                    system-poller-loop
                                    (flatten
                                      (conj [last-file last-stream]
                                            (seq opts))))]
        (Thread/sleep 1000)
        (recur new-file new-stream)))))

(defn- wrap-dir-index
  "Middleware to reroute / to /index.html"
  [handler]
  (fn [req]
    (handler
     (update-in req [:uri]
                #(if (= "/" %) "/index.html" %)))))

(defroutes http-routes
  (route/resources "/"))

(defn -main
  "Start up all the things"
  [& args]
  ; make sure we're config'd correctly
  (fix-config)
  (log "Starting")
  (defonce nrepl-server (start-server :port nrepl-port))
  (log "Repl available on" nrepl-port)
  (run-server (-> http-routes
                  wrap-dir-index)
              {:port http-port})
  (log "Http listening on " http-port)
  (if (.exists product-dir)
    (log "Found product dir" product-dir)
    (log "Expected product dir" product-dir "does not exists"))
  (let [server (create-server websockets-port)
        system-callback #(set-system server %)
        system-poll-future (future (system-poller system-callback))]
    system-poll-future
    ))
