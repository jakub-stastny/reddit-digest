(ns jakub-stastny.reddit-digest.core
  (:require [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.edn :as edn]
            [babashka.fs :as fs]
            [clojure.java.io :as io]
            [jakub-stastny.reddit-digest.config :as config]
            [jakub-stastny.reddit-digest.feed :as feed]
            [jakub-stastny.reddit-digest.notifier :as notifier])
  (:import [java.time Instant])
  (:gen-class))

;; Custom print-method for java.time.Instant
(defmethod print-method java.time.Instant [inst ^java.io.Writer w]
  (.write w (str "#inst \"" (.toString inst) "\"")))

(defn get-user-data-path [relative-path]
  (str (config/app-data-home) "/" relative-path))

(defn get-feeds []
  (sort (fs/glob (config/app-data-home) "feed.*.edn")))

(defn delete-all-but-last-feed []
  (let [feeds-for-deletion (butlast (get-feeds))]
    (doseq [feed feeds-for-deletion]
      (fs/delete feed))))

;; ;; Use feed1+feed2+feed3.
;; ;; Use spec?
(defn process [now]
  (fs/create-dirs (config/app-data-home))
  (delete-all-but-last-feed)

  (let [last-feed-path (str (last (get-feeds)))
        last-feed-data (when last-feed-path (edn/read-string (slurp last-feed-path)))
        base (str "feed." (.getEpochSecond now) ".edn")
        path (get-user-data-path base)]
    (let [[new-items current-items] (feed/fetch-and-parse-reddits now last-feed-data)]
      (notifier/send-pushover-notifications new-items)

      (println "~ Writing" path)
      (with-open [writter (io/writer (io/file path))]
        (pprint current-items writter)))))

(defn help []
  (println (config/app-name))
  (println)
  (println "No arguments expected, all the configuration is hard-coded.")
  (println "You are currently monitoring the following Reddits:")
  (prn config/reddits)
  (System/exit 1))

(defn -main [& args]
  (if (seq args) (help) (process (Instant/now))))
