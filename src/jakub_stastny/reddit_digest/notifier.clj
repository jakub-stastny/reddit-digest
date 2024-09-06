(ns jakub-stastny.reddit-digest.notifier
  (:require [clojure.string :as str]
            [clj-http.client :as http]))

;; truncated-content (truncate-text (html-to-text content) 350)
(defn html-to-text [html]
  (-> html
      (str/replace #"<!--.*?-->" "") ;; Remove comments
      (str/replace #"<[^>]+>" "")))  ;; Remove HTML tags

(defn truncate-text [text max-length]
  (if (<= (count text) max-length)
    text
    (let [truncated (subs text 0 max-length)
          last-space (or (str/last-index-of truncated " ") max-length)]
      (subs text 0 last-space))))

(def pushover-api-endpoint "https://api.pushover.net/1/messages.json")

(defn get-pushover-creds []
  (if-let [creds-str (System/getenv "PUSHOVER_CREDS")]
    (apply hash-map (interleave [:user :token] (str/split creds-str #":")))
    (throw (ex-info "PUSHOVER_CREDS env var missing" {}))))

(defn send-pushover-notification [title message]
  (println "~ [PushOver]" title "→" message)
  (let [params (merge (get-creds) {:title title :message message})]
    (http/post pushover-api-endpoint {:form-params params})))

(defn send-pushover-notifications [new-items]
  (doseq [item new-items]
    (let [title (str (:author item) ": " (:title item))
          message (:content item)]
      (send-pushover-notification title message))))
