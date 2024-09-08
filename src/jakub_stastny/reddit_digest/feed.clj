(ns jakub-stastny.reddit-digest.feed
  (:require [clojure.string :as str]
            [clj-http.client :as http]
            [clojure.data.xml :as xml]
            [jakub-stastny.reddit-digest.config :as config])
  (:import [java.time Instant]))

(defn one-or-many [data]
  (if (and (vector? data) (= (count data) 1))
    (get data 0)
    data))

;; (defn assoc-in-or-return [hash element attr]
;;   (prn :assoc-in-or-return) ;;;
;;   (let [data (get element attr)]
;;     (if (empty? data)
;;       hash
;;       (assoc-in hash [element] (one-or-many data)))))

(defn xml-element->map [element]
  (if (map? element)
    (into {} (for [[k v] element] [k (one-or-many (xml-element->map v))]))
    (if (coll? element)
      (vec (map xml-element->map element))
      element)))

(defn find-tag [items tag-name]
  (first (filter #(= (:tag %) tag-name) items)))

(defn process-entry [entry]
  (let [data (:content entry)
        author-tag (find-tag data :author)
        author-name (:content (find-tag (:content author-tag) :name))
        content (:content (find-tag data :content))
        published-date (:content (find-tag data :published))
        title (:content (find-tag data :title))
        link (:href (:attrs (find-tag data :link)))]
    {:title title :link link :author author-name :published-date (Instant/parse published-date) :content content}))

(defn return-entry-if-published-in-last-3-days [now entry]
  (when entry ;; the first return-entry-* function can return nil.
    (let [current-timestamp (.getEpochSecond now)
          entry-published-date-timestamp (.getEpochSecond (:published-date entry))]
      (when (< (- current-timestamp entry-published-date-timestamp) (* 3 24 60 60))
        entry))))

(defn return-entry-if-not-in-last-fetch [now last-fetch entry]
  (when-not (contains? (set (map :link last-fetch)) (:link entry))
    entry))

(defn process-new-entries [now entries last-fetch]
  (filter (comp (partial return-entry-if-published-in-last-3-days now)
                (partial return-entry-if-not-in-last-fetch now last-fetch))
          (map process-entry entries)))

;; Here though we need to return new AND current items.
(defn process-entries [now entries last-fetch]
  (let [new-entries (process-new-entries now entries last-fetch)]
    (prn :lf last-fetch)
    ;; TODO: Filter only if last ... 3(?) days.
    [new-entries, (into last-fetch new-entries)]))

;; This runs only once in this case (on the feed tag which is akin to the html tag).
;; We only filter entries now, other tags have more channel-specific info.
(defn xml->map [now node last-fetch]
  (vec (process-entries now (filter #(= (:tag %) :entry) (xml-element->map (:content node))) last-fetch)))

(defn parse-atom-feed [now xml-content last-fetch]
  (xml->map now (xml/parse-str xml-content) last-fetch))

(defn xml-to-edn [now xml-content last-fetch]
  (parse-atom-feed now xml-content last-fetch))

(defn fetch-atom-feed [url]
  (let [response (http/get url {:headers {"User-Agent" config/user-agent}})]
    (:body response)))

(defn fetch-and-parse-atom [now url last-fetch]
  (xml-to-edn now (fetch-atom-feed url) last-fetch))

(defn reddit-url [reddit]
  (str "https://www.reddit.com/" reddit "/.rss"))

(defn- process-reddit [now reddit last-fetch]
  [reddit (fetch-and-parse-atom now (reddit-url reddit) last-fetch)])

(defn get-reddits [now last-feed]
  (into {} (map #(process-reddit now % (or (get-in last-feed [:reddits %]) {})) config/reddits)))

(defn fetch-and-parse-reddits [now last-feed]
  (let [reddits (get-reddits now last-feed)
        new-items [{:author "JS" :title "Test" :content "Lorem ipsum"}] ;;;;
        current-items {:fetch-time now :reddits reddits}]
    [new-items current-items]))
