(ns ^{:author "Daniel Leong"
      :doc "Macro support"} 
  elite-mfd.macro
  (:require [elite-mfd
             [commander :as cmdr]
             [util :refer [log]]])
  (:import  [java.awt Robot AWTException]))

;; Without this, the Robot init would create a dock icon on OSX
;;  Not a big deal, but may be nicer when OSX client is released
(System/setProperty "apple.awt.UIElement" "false")

;; NB travis runs in a headless env, so the following would crash
(def robot (try 
             (Robot.)
             (catch AWTException e)))
;; hand-tuned. If it works in my VM, it should work for everyone
(def tap-delay 80)
;; this is enough for a default "request docking" macro
(def default-cmdr-bindings {:navigation "1"
                            :tab-left "q"
                            :tab-right "e"
                            :ui-down "down"
                            :ui-right "right"
                            :ui-select "space"})
(def default-cmdr-macros [{:name "Request Docking"
                           :value [:navigation :tab-right :tab-right
                                   :ui-select :ui-down :ui-select
                                   :tab-left :tab-left :navigation]}])

(defn- cmdr-bindings []
  (if-let [existing (cmdr/get-field :bindings)]
    existing
    default-cmdr-bindings))

(defn key-tap
  "Quickly tap the keyCode"
  [keyCode]
  (doto robot
    (.keyPress keyCode)
    (.delay tap-delay)
    (.keyRelease keyCode)
    (.delay tap-delay)))

(defn vk
  "Given a string name like 'down', returns the
    value of its KeyEvent/VK_* constant"
  [keyName]
  (let [vk-name (str "VK_" (.toUpperCase keyName))]
    (-> java.awt.event.KeyEvent 
        (.getField vk-name)
        (.get nil))))

(defn binding-to-vk
  [bind]
  (if-let [raw-key (get (cmdr-bindings) (keyword bind))]
    (vk raw-key)
    (throw (IllegalArgumentException. (str "No such binding:" bind)))))

(defmacro with-held 
  "Perform some actions while the provided keyCodes 
   are held, then release the keyCodes. Probably
   not actually needed for elite macros, but it
   was interesting to write!
  Eg: (defn alt-tab []
        (with-held [(vk \"alt\")]
          (key-tap (vk \"tab\")))"
  [keyCodes & body]
  `(do 
     ;; press them all
     ~(concat 
        '(doto robot) 
        (map #(identity `(.keyPress ~%)) keyCodes))
     ;; work yo' body
     ~(cons 'do body)
     ;; release them all
     ~(concat 
        '(doto robot) 
        (map #(identity `(.keyRelease ~%)) keyCodes))))

(defn evaluate-macro
  "Evaluate a macro as a series of binding presses"
  [bindings]
  (doseq [bind bindings]
    (println (-> bind binding-to-vk))
    (-> bind
        binding-to-vk
        key-tap)))

;;
;; Handlers
;;
(defn on-macro
  [_ packet]
  (when-let [macro (:macro packet)]
    (log "Perform macro:" macro)
    (evaluate-macro macro)))

;;
;; Registration
;;
(defn register-handlers
  "Interface used by server for registering websocket packet handlers"
  [handlers]
  ; while we're here, ensure some default cmdr data is setup
  (let [bindings (cmdr/get-field :bindings)
        macros (cmdr/get-field :macros)]
    (when (empty? bindings)
      (cmdr/set-field :bindings default-cmdr-bindings))
    (when (empty? macros)
      (cmdr/set-field :macros default-cmdr-macros)))
  (assoc handlers
         :macro on-macro))
