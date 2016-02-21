(ns uxbox.tests.test-auth
  "A txlog and services abstraction generic tests."
  (:require [clojure.test :as t]
            [promesa.core :as p]
            [uxbox.persistence :as up]
            [uxbox.services.auth :as usa]
            [uxbox.services :as usv]
            [uxbox.tests.helpers :as th]))

(t/use-fixtures :each th/database-reset)

(t/deftest test-success-auth
  (let [user-data {:username "user1"
                   :password "user1"
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
                   :password "user1"
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
                   :password "user2"
                   :email "user2@uxbox.io"}
        user (with-open [conn (up/get-conn)]
               (usa/create-user conn user-data))
        data {:type :auth/login
              :username "user1"
              :password "user1"}
        result (th/await (usv/novelty data))]
    (t/is (th/ex-info? result))))
