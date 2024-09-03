(ns jakub-stastny.reddit-digest.core
  (:require [clojure.pprint :refer [pprint]]
            [jakub-stastny.reddit-digest.feed :as feed])
  (:import [java.time Instant])
  (:gen-class))

(def reddits
  [:energy_work])

(defn save [now reddits]
  (pprint (feed/fetch-and-parse-reddits now reddits)))

(defn help []
  (println "No arguments expected, all the configuration is hard-coded.")
  (println "You are currently monitoring the following Reddits:")
  (prn reddits)
  (System/exit 1))

(defn -main [& args]
  (if (seq args)
    (help)
    (save (Instant/now) reddits)))
