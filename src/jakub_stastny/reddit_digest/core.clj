(ns jakub-stastny.reddit-digest.core
  (:require [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.edn :as edn]
            [babashka.fs :as fs]
            [clojure.java.io :as io]
            [jakub-stastny.reddit-digest.feed :as feed]
            [jakub-stastny.reddit-digest.notifier :as notifier])
  (:import [java.time Instant])
  (:gen-class))

(def reddits ["r/energy_work"])

(def app-name
  (str/join "." (butlast (str/split (str (ns-name *ns*)) #"\."))))

;; ~/.cache/jakub-stastny.reddit-digest
(def app-data-home
  (fs/xdg-cache-home app-name))

(defn get-user-data-path [relative-path]
  (str app-data-home "/" relative-path))

(defn get-feeds []
  (sort (fs/glob app-data-home "feed.*.edn")))

(defn manage-feeds []
  (let [feeds-for-deletion (butlast (get-feeds))]
    (doseq [feed feeds-for-deletion]
      (fs/delete feed))))

(defn get-last-feed []
  (last (get-feeds)))

(defn save [now reddits]
  (fs/create-dirs app-data-home)
  (manage-feeds)
  (let [last-feed (edn/read-string (slurp (str (get-last-feed))))
        base (str "feed." (.getEpochSecond now) ".edn")
        path (get-user-data-path base)]
    (println "~ Writing" path)
    (let [[new-items current-items] (feed/fetch-and-parse-reddits now reddits last-feed)]
      (notifier/send-push-notifications new-items)

      (with-open [writter (io/writer (io/file path))]
        (pprint current-items writter)))))

(defn help []
  (println app-name)
  (println)
  (println "No arguments expected, all the configuration is hard-coded.")
  (println "You are currently monitoring the following Reddits:")
  (prn reddits)
  (System/exit 1))

(defn -main [& args]
  (if (seq args)
    (help)
    (save (Instant/now) reddits)))
