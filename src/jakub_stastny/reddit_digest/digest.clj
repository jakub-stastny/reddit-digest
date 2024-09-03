(ns jakub-stastny.reddit-digest.digest
  (:require [clojure.string :as str]
            [postal.core :refer [send-message]]))

;; Use TLS. SSL is false since we're using TLS.
(def default-smtp-opts
  {:host "smtp.gmail.com" :port 587 :tls true :ssl false})

;; GMAIL_CREDS=jakub.stastny.pt@gmail.com:password
(defn get-smtp-opts []
  (let [gmail-creds-str (System/getenv "GMAIL_CREDS")]
    (if gmail-creds-str
      (let [[user pass] (str/split gmail-creds-str #":")]
        (merge default-smtp-opts {:user user :pass pass}))
      (throw (ex-info "Gmail credentials (GMAIL_CREDS env var) missing" {})))))

(defn send-digest [now new-items]
  (when-not (empty? new-items)
    (send-message
     (get-smtp-opts)
     {:from (str "noreply@" (ns-name *ns*))
      :to "jakub.stastny.pt+reddit@gmail.com"
      :subject (str "Reddit digest " now)
      :body "This is a test email sent from Clojure using Gmail SMTP."})))
