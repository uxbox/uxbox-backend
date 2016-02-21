(ns uxbox.tests.test-auth
  "A txlog and services abstraction generic tests."
  (:require [clojure.test :as t]
            [promesa.core :as p]
            [clj-http.client :as http]
            [catacumba.testing :refer (with-server)]
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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend Test
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:const +base-url
  "http://localhost:5050")

(t/deftest test-http-success-auth
  (let [data {:username "user1"
              :password "user1"
              :email "user1@uxbox.io"}
        user (with-open [conn (up/get-conn)]
               (usa/create-user conn data))]
    (with-server {:handler urt/app}
      (let [frame {:dest :auth/login
                   :type :novelty
                   :data {:username "user1"
                          :password "user1"
                          :scope "foobar"}}
            response (th/send-frame (str +base-url "/api") frame)]
        ;; (println "RESPONSE:" response)
        (t/is (= (:type response) :response))
        (t/is (contains? response :data))
        (t/is (contains? (:data response) :token))))))


(t/deftest test-http-failed-auth
  (let [data {:username "user1"
              :password "user1"
              :email "user1@uxbox.io"}
        user (with-open [conn (up/get-conn)]
               (usa/create-user conn data))]
    (with-server {:handler urt/app}
      (let [frame {:dest :auth/login
                   :type :novelty
                   :data {:username "user1"
                          :password "user2"
                          :scope "foobar"}}
            response (th/send-frame (str +base-url "/api") frame)]
        ;; (println "RESPONSE:" response)
        (t/is (= (:type response) :error))
        (t/is (= (:message response) "Invalid credentials"))))))

