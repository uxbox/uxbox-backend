(ns uxbox.tests.test-projects
  (:require [clojure.test :as t]
            [promesa.core :as p]
            [suricatta.core :as sc]
            [clj-uuid :as uuid]
            [catacumba.testing :refer (with-server)]
            [catacumba.serializers :as sz]
            [uxbox.persistence :as up]
            [uxbox.frontend.routes :as urt]
            [uxbox.services.projects :as uspr]
            [uxbox.services.pages :as uspg]
            [uxbox.services :as usv]
            [uxbox.tests.helpers :as th]))

(t/use-fixtures :each th/database-reset)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend Test
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(t/deftest test-http-project-list
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)
          proj (uspr/create-project conn {:user (:id user) :name "proj1"})]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/projects")
              [status data] (th/http-get user uri)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 200 status))
          (t/is (= 1 (count data))))))))

(t/deftest test-http-project-create
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/projects")
              params {:body {:name "proj1"}}
              [status data] (th/http-post user uri params)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 201 status))
          (t/is (= (:user data) (:id user)))
          (t/is (= (:name data) "proj1")))))))

(t/deftest test-http-project-update
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)
          proj (uspr/create-project conn {:user (:id user) :name "proj1"})]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/projects/" (:id proj))
              params {:body (assoc proj :name "proj2")}
              [status data] (th/http-put user uri params)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 200 status))
          (t/is (= (:user data) (:id user)))
          (t/is (= (:name data) "proj2")))))))

(t/deftest test-http-project-delete
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)
          proj (uspr/create-project conn {:user (:id user) :name "proj1"})]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/projects/" (:id proj))
              [status data] (th/http-delete user uri)]
          (t/is (= 204 status))
          (let [sqlv ["SELECT * FROM projects WHERE \"user\"=? AND deleted=false"
                      (:id user)]
                result (sc/fetch conn sqlv)]
            (t/is (empty? result))))))))
