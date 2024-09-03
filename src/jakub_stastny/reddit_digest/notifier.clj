(ns jakub-stastny.reddit-digest.notifier
  (:require [clojure.string :as str]
            [clj-http.client :as http]))

(def pushover-api-endpoint "https://api.pushover.net/1/messages.json")

(defn get-creds []
  (if-let [creds-str (System/getenv "PUSHOVER_CREDS")]
    (apply hash-map (interleave [:user :token] (str/split creds-str #":")))
    (throw (ex-info "PUSHOVER_CREDS env var missing" {}))))

(defn send-pushover-notification [title message]
  (println "~ [PushOver]" title "→" message)
  (let [params (merge (get-creds) {:title title :message message})]
    (http/post pushover-api-endpoint {:form-params params})))

;; TODO: This is where we should strip tags and limit length,
;; not before.
(defn send-pushover-notifications [new-items]
  (doseq [item new-items]
    (let [title (str (:author item) ": " (:title item))
          message (:content item)]
      (send-pushover-notification title message))))
