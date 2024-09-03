(ns jakub-stastny.reddit-digest.notifier
  (:require [clojure.string :as str]
            [clj-http.client :as http]))

(defn send-push-notifications [new-items]
  (doseq [item new-items]
    (prn :i item)))

;; (defn send-pushover-notification [title message]
;;   (http/post "https://api.pushover.net/1/messages.json"
;;                {:form-params {:token "your-api-token"
;;                               :user "your-user-key"
;;                               :title title
;;                               :message message}}))

;; ;; Example usage
;; (send-pushover-notification "Test Notification" "This is a test message sent to your Mac/iOS device.")
