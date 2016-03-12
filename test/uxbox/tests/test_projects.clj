(ns uxbox.tests.test-projects
  (:require [clojure.test :as t]
            [promesa.core :as p]
            [clj-uuid :as uuid]
            [clj-http.client :as http]
            [catacumba.testing :refer (with-server)]
            [buddy.hashers :as hashers]
            [uxbox.persistence :as up]
            [uxbox.frontend.routes :as urt]
            [uxbox.services.auth :as usa]
            [uxbox.services.projects :as usp]
            [uxbox.services :as usv]
            [uxbox.tests.helpers :as th]))

(t/use-fixtures :each th/database-reset)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Services Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-user
  "Helper for create users"
  [conn i]
  (let [data {:username (str "user" i)
              :password  (hashers/encrypt (str "user" i))
              :email (str "user" i "@uxbox.io")}]
    (usa/create-user conn data)))

(t/deftest test-create-project
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)
          data {:id (uuid/v4)
                :type :project/create
                :user (:id user)
                :name "test"}
          proj @(usv/novelty data)]
      (t/is (= (:id data) (:id proj)))
      (t/is (= (:name data) (:name proj)))
      (t/is (= (:user data) (:user proj))))))

(t/deftest test-list-project
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)
          proj1 (usp/create-project conn {:user (:id user) :name "proj1"})
          proj2 (usp/create-project conn {:user (:id user) :name "proj2"})
          data {:type :project/list :user (:id user)}
          result @(usv/query data)]
      (t/is (= 2 (count result)))
      (t/is (= "proj2" (:name (first result))))
      (t/is (= (:user data) (:user (first result))))
      (t/is (= "proj1" (:name (second result))))
      (t/is (= (:user data) (:user (second result)))))))

(t/deftest test-page-crud
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)
          proj (usp/create-project conn {:id (uuid/v4)
                                         :user (:id user)
                                         :name "proj1"})
          data {:id (uuid/v4)
                :type :page/create
                :user (:id user)
                :project (:id proj)
                :name "test"
                :width 200
                :height 200
                :layout "mobil"}
          page @(usv/novelty data)]

      (t/is (= (:id data) (:id page)))
      (t/is (= (:name data) (:name page)))
      (t/is (= (:user data) (:user page)))
      (t/is (= (:width data) (:width page)))
      (t/is (= (:height data) (:height page)))
      (t/is (= 0 (:version page)))

      ;; query
      (let [result @(usv/query {:type :page/list
                                :project (:id proj)
                                :user (:id user)})]
        (t/is (= 1 (count result)))
        (t/is (= (:id data) (:id (first result))))
        (t/is (= (:name data) (:name (first result))))
        (t/is (= (:user data) (:user (first result))))
        (t/is (= (:width data) (:width (first result))))
        (t/is (= (:height data) (:height (first result))))
        (t/is (= 0 (:version (first result)))))

      (let [page @(usv/novelty (assoc data
                                      :data "test"
                                      :type :page/update))]
        (t/is (= (:id data) (:id page)))
        (t/is (= 1 (:version page)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend Test
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:const +base-url
  "http://localhost:5050")

(t/deftest test-http-project-list
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)
          proj (usp/create-project conn {:user (:id user) :name "proj1"})]
      (with-server {:handler (urt/app)}
        (let [uri (str +base-url "/api/projects")
              [status data] (th/http-get user uri)]
          (println "RESPONSE:" status data)
          (t/is (= 200 status))
          (t/is (= 1 (count data))))))))

(t/deftest test-http-project-create
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)]
      (with-server {:handler (urt/app)}
        (let [uri (str +base-url "/api/projects")
              params {:body {:name "proj1"}}
              [status data] (th/http-post user uri params)]
          (println "RESPONSE:" status data)
          (t/is (= 201 status))
          (t/is (= (:user data) (:id user)))
          (t/is (= (:name data) "proj1")))))))

(t/deftest test-http-project-update
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)
          proj (usp/create-project conn {:user (:id user) :name "proj1"})]
      (with-server {:handler (urt/app)}
        (let [uri (str +base-url "/api/projects/" (:id proj))
              params {:body {:name "proj2"}}
              [status data] (th/http-put user uri params)]
          (println "RESPONSE:" status data)
          (t/is (= 200 status))
          (t/is (= (:user data) (:id user)))
          (t/is (= (:name data) "proj2")))))))

(t/deftest test-http-project-delete
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)
          proj (usp/create-project conn {:user (:id user) :name "proj1"})]
      (with-server {:handler (urt/app)}
        (let [uri (str +base-url "/api/projects/" (:id proj))
              [status data] (th/http-delete user uri)]
          (println "RESPONSE:" status data)
          (t/is (= 204 status)))))))

;; (t/deftest test-http-failed-auth
;;   (let [data {:username "user1"
;;               :password  (hashers/encrypt "user1")
;;               :email "user1@uxbox.io"}
;;         user (with-open [conn (up/get-conn)]
;;                (usa/create-user conn data))]
;;     (with-server {:handler (urt/app)}
;;       (let [data {:username "user1"
;;                   :password "user2"
;;                   :scope "foobar"}
;;             uri (str +base-url "/api/auth/token")
;;             [status data] (th/post uri data)]
;;         ;; (println "RESPONSE:" status data)
;;         (t/is (= 400 status))
;;         (t/is (= (:message data) "Invalid credentials"))))))

