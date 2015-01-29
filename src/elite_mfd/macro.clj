(ns ^{:author "Daniel Leong"
      :doc "Macro support"} 
  elite-mfd.macro
  (:require [elite-mfd
             [commander :as cmdr]
             [util :refer [log]]])
  (:import [java.awt Robot AWTException]))

;; NB travis runs in a headless env, so the following would crash
(def robot (try 
             (Robot.)
             (catch AWTException e)))
;; hand-tuned. If it works in my VM, it should work for everyone
(def tap-delay 80)
(def multi-tap-delay 10)
;; this is enough for the default "request docking" macro
(def default-cmdr-bindings {:navigation "1"
                            :press-enter "enter" ;; eg: quick comms; shouldn't need remap
                            :tab-left "q"
                            :tab-right "e"
                            :ui-left "a"
                            :ui-up "w"
                            :ui-right "d"
                            :ui-down "s"
                            :ui-select "space"})
(def default-cmdr-macros [{:name "Request Docking"
                           :value [:navigation :tab-right :tab-right
                                   :ui-select :ui-down :ui-select
                                   :tab-left :tab-left :navigation]}])
;; TODO support more symbols, perhaps
(def vk-special-cases {\space "VK_SPACE"
                       \- "VK_MINUS"
                       \+ "VK_PLUS"
                       \_ "VK_UNDERSCORE"
                       \, "VK_COMMA"
                       \. "VK_PERIOD"
                       \! "VK_EXCLAMATION_MARK" ; FIXME this doesn't work on qwerty
                       \/ "VK_SLASH"
                       \\ "VK_BACK_SLASH"
                       \? "VK_SLASH"}) ; since there's no QUESTION_MARK key...

(defn- cmdr-bindings []
  (if-let [existing (cmdr/get-field :bindings)]
    existing
    default-cmdr-bindings))

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
        '(doto elite-mfd.macro/robot) 
        (map #(identity `(.keyPress ~%)) keyCodes))
     ;; work yo' body
     ~(cons 'do body)
     ;; release them all
     ~(concat 
        '(doto elite-mfd.macro/robot) 
        (map #(identity `(.keyRelease ~%)) keyCodes))))


(defmulti key-tap
  "Quickly tap the keyCode or sequence of keyCodes"
  (fn [arg & _]
    (cond 
      (map? arg) :map
      (coll? arg) :collection
      :else :code)))
(defmethod key-tap :code
  [keyCode & {:keys [with-delay] :or {with-delay tap-delay}}]
  (doto robot
    (.keyPress keyCode)
    (.delay with-delay)
    (.keyRelease keyCode)
    (.delay with-delay)))
(defmethod key-tap :collection
  [keyCodes & _]
  (doseq [code keyCodes]
    (key-tap code :with-delay multi-tap-delay)))
(defmethod key-tap :map
  [keyDef & args]
  (with-held [(:with keyDef)]
    (apply key-tap (cons (:vk keyDef) args)))) 

(defn vk
  "Given a string name like 'down', returns the
  value of its KeyEvent/VK_* constant"
  [keyName]
  (let [special-case (get vk-special-cases keyName nil)
        vk-name (some identity 
                      [special-case
                       (str "VK_" (-> keyName str .trim .toUpperCase))])]
    (-> java.awt.event.KeyEvent 
        (.getField vk-name)
        (.get nil))))

(defn char-vk
  "Given a single char from a quoted string,
  return either a VK_* constant (ala vk) or
  a dict indicating what combo of keys to press"
  [character]
  (let [codePoint (Character/codePointAt (str character) 0)]
    (cond
      ;; letters
      (Character/isAlphabetic codePoint)
      (if (= codePoint (Character/toUpperCase codePoint))
        {:vk (vk character) :with (vk "shift")}
        (vk character))
      ;; numbers
      (Character/isDigit codePoint)
      (vk character)
      ;; TODO symbols
      ;; for everything else...
      :else (vk character))))

(defn binding-to-vk
  [raw-binding]
  (let [bind (if (string? raw-binding)
               (.trim raw-binding)
               raw-binding)
        raw-key (get (cmdr-bindings) (keyword bind))]
    (cond 
      ;; is it a normal binding?
      (not (nil? raw-key)) (vk raw-key)
      ;; is it a quoted string?
      (-> bind (.startsWith "\"")) 
      (->> bind
           seq ; turn the string into a seq...
           rest ; so we can drop the first...
           drop-last ; ... and last quotes
           (map char-vk)) ; and get their vks
      ;; no idea.
      :else (throw (IllegalArgumentException. (str "No such binding:" bind))))))

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
