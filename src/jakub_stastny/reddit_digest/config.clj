(ns jakub-stastny.reddit-digest.config
  (:require [clojure.string :as str]
            [babashka.fs :as fs]))

(def reddits ["r/energy_work"])

(defn app-name []
  (str/join "." (butlast (str/split (str (ns-name *ns*)) #"\."))))

(defn user-agent []
  (str (app-name) "/1.0"))

;; ;; ~/.cache/jakub-stastny.reddit-digest
(defn app-data-home []
  (fs/xdg-cache-home (app-name)))
