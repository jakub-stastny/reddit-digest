(ns jakub-stastny.reddit-digest.feed
  (:require [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clj-http.client :as http]
            [clojure.data.xml :as xml]
            [clojure.zip :as zip]
            [clojure.xml :as xml-parser]
            [clojure.data.zip.xml :as dz]
            [clojure.edn :as edn])
  (:import [java.time Instant]
           [java.time.format DateTimeFormatter]))

;; Custom print-method for java.time.Instant
(defmethod print-method java.time.Instant [inst ^java.io.Writer w]
  (.write w (str "#inst \"" (.toString inst) "\"")))

(def user-agent (str (ns-name *ns*) "/1.0"))

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

(defn process-entry [entry]
  (let [data (:content entry)
        author-tag (find-tag data :author)
        author-name (:content (find-tag (:content author-tag) :name))
        content (:content (find-tag data :content))
        truncated-content (truncate-text (html-to-text content) 350)
        published-date (:content (find-tag data :published))
        title (:content (find-tag data :title))
        link (:href (:attrs (find-tag data :link)))]
    {:title title :link link :author author-name :published-date (Instant/parse published-date) :content truncated-content}))

(defn process-entries [now entries]
  ;; Filter only items published in the last 3 days.
  (filter #(< (- (.getEpochSecond now) (.getEpochSecond (:published-date %))) (* 3 24 60 60))
          (map process-entry entries)))

;; This runs only once in this case (on the feed tag which is akin to the html tag).
;; We only filter entries now, other tags have more channel-specific info.
(defn xml->map [now node]
  (vec (process-entries now (filter #(= (:tag %) :entry) (xml-element->map (:content node))))))

(defn parse-atom-feed [now xml-content]
  (xml->map now (xml/parse-str xml-content)))

(defn xml-to-edn [now xml-content]
  (parse-atom-feed now xml-content))

(defn fetch-atom-feed [url]
  (let [response (http/get url {:headers {"User-Agent" user-agent}})]
    (:body response)))

(defn fetch-and-parse-atom [now url]
  (xml-to-edn now (fetch-atom-feed url)))

(defn reddit-url [reddit]
  (str "https://www.reddit.com/" reddit "/.rss"))

(defn- process-reddit [now reddit]
  [reddit (fetch-and-parse-atom now (reddit-url reddit))])

;; One top-level file.
(defn fetch-and-parse-reddits [now reddits]
  {:fetch-time now :reddits (into {} (map #(process-reddit now %) reddits))})
