(ns uxbox.tests.test-auth
  "A txlog and services abstraction generic tests."
  (:require [clojure.test :as t]
            [promesa.core :as p]
            [clj-http.client :as http]
            [catacumba.testing :refer (with-server)]
            [buddy.hashers :as hashers]
            [uxbox.persistence :as up]
            [uxbox.frontend.routes :as urt]
            [uxbox.services.auth :as usa]
            [uxbox.services :as usv]
            [uxbox.tests.helpers :as th]))

(t/use-fixtures :each th/database-reset)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Services Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(t/deftest test-success-auth
  (let [user-data {:username "user1"
                   :password  (hashers/encrypt "user1")
                   :email "user1@uxbox.io"}
        user (with-open [conn (up/get-conn)]
               (usa/create-user conn user-data))
        data {:type :auth/login
              :username "user1"
              :password "user1"}
        result @(usv/novelty data)]
    (t/is (contains? result :token))
    (t/is (string? (:token result)))))

(t/deftest test-success-by-email
  (let [user-data {:username "user1"
                   :password  (hashers/encrypt "user1")
                   :email "user1@uxbox.io"}
        user (with-open [conn (up/get-conn)]
               (usa/create-user conn user-data))
        data {:type :auth/login
              :username "user1@uxbox.io"
              :password "user1"}
        result @(usv/novelty data)]
    (t/is (contains? result :token))
    (t/is (string? (:token result)))))

(t/deftest test-failed-auth
  (let [user-data {:username "user2"
                   :password  (hashers/encrypt "user2")
                   :email "user2@uxbox.io"}
        user (with-open [conn (up/get-conn)]
               (usa/create-user conn user-data))
        data {:type :auth/login
              :username "user1"
              :password "user1"}
        result (th/await (usv/novelty data))]
    (t/is (th/ex-info? result))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend Test
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:const +base-url
  "http://localhost:5050")

(t/deftest test-http-success-auth
  (let [data {:username "user1"
              :password  (hashers/encrypt "user1")
              :email "user1@uxbox.io"}
        user (with-open [conn (up/get-conn)]
               (usa/create-user conn data))]
    (with-server {:handler (urt/app)}
      (let [data {:username "user1"
                  :password "user1"
                  :scope "foobar"}
            uri (str +base-url "/api/auth/token")
            [status data] (th/post uri data)]
        ;; (println "RESPONSE:" response)
        (t/is (= status 200))
        (t/is (contains? data :token))))))

(t/deftest test-http-failed-auth
  (let [data {:username "user1"
              :password  (hashers/encrypt "user1")
              :email "user1@uxbox.io"}
        user (with-open [conn (up/get-conn)]
               (usa/create-user conn data))]
    (with-server {:handler (urt/app)}
      (let [data {:username "user1"
                  :password "user2"
                  :scope "foobar"}
            uri (str +base-url "/api/auth/token")
            [status data] (th/post uri data)]
        ;; (println "RESPONSE:" status data)
        (t/is (= 400 status))
        (t/is (= (:message data) "Invalid credentials"))))))

