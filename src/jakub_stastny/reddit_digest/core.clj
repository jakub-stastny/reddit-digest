(ns jakub-stastny.reddit-digest.core
  (:require [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [jakub-stastny.reddit-digest.feed :as feed])
  (:import [java.time Instant])
  (:gen-class))

(def reddits ["r/energy_work"])

(def app-name
  (str/join "." (butlast (str/split (str (ns-name *ns*)) #"\."))))

;; $XDG_DATA_HOME defaults to ~/.local/share.
(def xdg-data-home
  (or (System/getenv "XDG_DATA_HOME")
      (str (System/getProperty "user.home") "/.local/share")))

(def app-data-home
  (str xdg-data-home "/" app-name))

(defn get-user-data-path [relative-path]
  (str app-data-home "/" relative-path))

(defn ensure-directory-exists [dir]
  (let [file (io/file dir)]
    (when-not (.exists file)
      (io/make-parents file)
      (.mkdir file))))

(defn save [now reddits]
  (ensure-directory-exists app-data-home)
  (let [base (str "feeds." (.getEpochSecond now) ".edn")
        path (get-user-data-path base)]
    (println "~ Writing" path)
    (with-open [w (io/writer (io/file path))]
      (pprint (feed/fetch-and-parse-reddits now reddits) w))))

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
