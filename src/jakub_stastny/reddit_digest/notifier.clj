(ns jakub-stastny.reddit-digest.notifier
  (:require [clojure.string :as str]
            ;; [postal.core :refer [send-message]]
            ))

;; ;; Postfix is required.
;; ;;
;; ;; Linux:
;; ;; systemctl start postfix
;; ;; systemctl enable postfix
;; ;;
;; ;; macOS:
;; ;; sudo postfix start
;; ;; This doesn't work at the moment, but at least it doesn't fail.
;; (def smtp-opts
;;   {:host "localhost" :user nil :pass nil :port 25 :tls false :ssl false})

;; (def default-mail-opts
;;   {:from (str "noreply@" (ns-name *ns*))})

;; (defn send-mail [{:keys [to subject body] :as mail-opts}]
;;   (println "~ MAIL" to)
;;   (println "  Subject:" subject "\n")
;;   (println body "\n")
;;   (send-message smtp-opts (merge default-mail-opts mail-opts)))

;; (defn send-digest [now new-items]
;;   (when-not (empty? new-items)
;;     (send-mail
;;      {:to "jakub.stastny.pt+reddit@gmail.com"
;;       :subject (str "Reddit digest " now)
;;       :body "This is a test email sent from Clojure."})))

;; PushOver
;; MIGHT BE BETTER AS WE DON'T DEPEND ON AN EXTERNAL DEAMON.
(defn send-push-notifications [new-items]
  (doseq [item new-items]
    (prn :i item)))
