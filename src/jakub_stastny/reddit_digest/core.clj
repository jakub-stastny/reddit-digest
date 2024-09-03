(ns jakub-stastny.reddit-digest.core
  (:require [clj-http.client :as http]
            [clojure.data.xml :as xml]
            [clojure.zip :as zip]
            [clojure.xml :as xml-parser]
            [clojure.data.zip.xml :as dz]))

(def user-agent (str (ns-name *ns*) "/1.0"))

(defn fetch-atom-feed [url]
  (let [response (http/get url {:headers {"User-Agent" user-agent}})
        feed-content (:body response)]
    (-> feed-content xml/parse-str zip/xml-zip)))

(defn extract-entries [atom-feed]
  (dz/xml-> atom-feed :entry
            (fn [entry]
              {:title   (dz/xml1-> entry :title dz/text)
               :summary (dz/xml1-> entry :summary dz/text)
               :link    (dz/xml1-> entry :link (dz/attr :href))})))

(defn fetch-and-parse-atom [url]
  (let [atom-feed (fetch-atom-feed url)]
    (extract-entries atom-feed)))

(defn -main [& args]
  (println (fetch-and-parse-atom "https://www.reddit.com/r/energy_work/.rss")))
