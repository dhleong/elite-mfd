(ns ^{:author "Daniel Leong"
      :doc "Startup/shutdown Elite client"} 
  elite-mfd.startup
  (:require [clojure.java.shell :refer [sh]]  
            [clojure.string :as str]  
            [elite-mfd
             [commander :as cmdr]
             [macro :refer [with-held vk binding-to-vk key-tap]]
             [util :refer [log to-client]]])
  (:import [java.lang Runtime]))

;;
;; Config
;;
(def launcher-wait-sleep 2000)
(def menu-open-wait-sleep 1000)
(def os-name (if (-> (System/getProperty "os.name")
                     (.startsWith "Windows"))
               :windows
               :mac))
(def launcher-path 
  (os-name {:windows "C:\\Program Files (x86)\\Frontier\\EDLaunch\\EDLaunch.exe"
            :mac nil })) ;; dunno yet

;;
;; Util
;;
(defn send-shift-tab []
  (with-held [(vk "shift")]
    (key-tap (vk "tab"))))

(defmulti list-procs (constantly os-name))
(defmethod list-procs :windows []
  (->> (:out (sh "tasklist.exe" "/fo" "csv" "/nh"))
      str/split-lines
      (map (comp #(subs % 1 (- (count %) 1)) ; gross way to trim off quotes
                 first 
                 #(str/split % #",")))))
(defmethod list-procs :mac []
  (str/split-lines (:out (sh "ps" "-ax" "-o" "command"))))

(defmulti kill-proc (constantly os-name))
(defmethod kill-proc :windows [proc-name]
  (sh "taskkill.exe" 
      "/f" ; forcefully
      "/fi" (str "IMAGENAME eq " proc-name "*")))
(defmethod kill-proc :mac [proc-name]
  (sh "killall" proc-name))

(defn proc-running [proc-name]
  (not (empty? (filter #(.contains % proc-name)
                       (list-procs)))))

;;
;; Domain/convenience
;;

(defn launcher-running []
  (proc-running "EDLaunch"))

(defn client-running []
  (proc-running "EliteDangerous"))

(defn launcher-kill []
  (kill-proc "EDLaunch"))

(defn client-kill []
  (kill-proc "EliteDangerous"))

;;
;; Public interface
;;

(defn start-elite
  "A macro/command for starting Elite from desktop
  Assumes you have chosen 'remember my password.'
  This is BLOCKING (for now). You probably won't
  need to do anything else anyway, so that should
  not be a problem. Would be trivial to wrap in a
  (future) if needed...."
  []
  (when (launcher-running)
    (log "Killing old launcher")
    (launcher-kill)
    (Thread/sleep launcher-wait-sleep))
  (let [launcher (-> (Runtime/getRuntime) 
                     (.exec launcher-path))]
    (loop []
      (log "Waiting for launcher...")
      (Thread/sleep launcher-wait-sleep)
      (when (not (launcher-running))
        (recur)))
    ; launcher started; macro
    (send-shift-tab)
    (key-tap (vk "space"))
    ; wait for login screen...
    (Thread/sleep launcher-wait-sleep)
    (send-shift-tab)
    (send-shift-tab)
    (key-tap (vk "space"))
    ;; wait for login...
    (Thread/sleep launcher-wait-sleep)
    (send-shift-tab)
    (key-tap (vk "home")) ;; select E:D
    (send-shift-tab)
    (send-shift-tab)
    (key-tap (vk "space")))) ;; gogogo!

(defn open-menu
  "Open the game menu. Assumes you're in-game already.
  Basically, this is a friendly alias for
    (key-tap (vk \"escape\"))
  since it cannot currently be re-bound"
  []
  (key-tap (vk "escape"))
  ;; opening the menu takes a little bit
  (Thread/sleep menu-open-wait-sleep))

(defn save-and-quit
  "A macro/command for saving and quitting Elite to desktop.
  Does a forceful termination, since Elite does not close
  cleanly on my system."
  []
  ;; go to the menu...
  (open-menu)
  ;; ... save and exit
  (key-tap (binding-to-vk :ui-up))
  (key-tap (binding-to-vk :ui-select))
  ;; confirm
  (Thread/sleep launcher-wait-sleep)
  (key-tap (binding-to-vk :ui-right))
  (key-tap (binding-to-vk :ui-select))
  ;; give it plenty of time to exit cleanly
  (Thread/sleep (* 2 launcher-wait-sleep))
  ;; make sure it's dead (rarely exits cleanly for me)
  (client-kill))

;;
;; Handlers
;;
(defn on-connect
  [ch]
  ; let them know if we're running or not
  (to-client ch {:type :startup-data
                 :client-running (client-running)}))

(defn on-startup [_ packet] (start-elite))
(defn on-shutdown [_ packet] (save-and-quit))
(defn on-open-menu [_ packet] (open-menu))

;;
;; Registration
;;
(defn register-handlers
  "Interface used by server for registering websocket packet handlers"
  [handlers]
  (assoc handlers
         :open-menu on-open-menu
         :startup on-startup
         :shutdown on-shutdown))
