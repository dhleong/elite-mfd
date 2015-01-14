(ns ^{:author "Daniel Leong"
      :doc "Navigation module using cmdr.club"}
  elite-mfd.navigate
  (:require [cheshire.core :refer [generate-string parse-string]]
            [org.httpkit.client :as http]
            [elite-mfd.util :refer [log to-client client-error]]))

(def base-url "https://cmdr.club/routeapi/")

(defn- clean-name [system]
  (-> system
      (.trim)
      (.replaceAll " " "+")))

(defn plot-route
  "Attempt to plot a route between two systems"
  [& {:keys [start end jump-range callback] :as opts}]
  {:pre (every? identity opts)}
  (let [route-url (str base-url 
                 jump-range "/" 
                 (clean-name start) "/" 
                 (clean-name end))]
    (println route-url)
    (http/get
      route-url
      {:timeout 7500}
      (fn [{:keys [error body]}]
        (if error
          (do 
            (log "Error plotting route:" error)
            (callback nil))
          (callback (parse-string body true)))))))

;;
;; Handlers
;;
(defn on-navigate
  [ch packet]
  (apply plot-route 
         (flatten (conj (vec packet)
                        :callback #(to-client ch {:type :navigate-result
                                                  :result % })))))

;;
;; Registration
;;
(defn register-handlers
  "Interface used by server for registering websocket packet handlers"
  [handlers]
  (assoc handlers
         :navigate on-navigate))
