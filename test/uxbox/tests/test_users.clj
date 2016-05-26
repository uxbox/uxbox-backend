(ns uxbox.tests.test-users
  (:require [clojure.test :as t]
            [clojure.java.io :as io]
            [promesa.core :as p]
            [clj-http.client :as http]
            [catacumba.testing :refer (with-server)]
            [buddy.hashers :as hashers]
            [uxbox.db :as db]
            [uxbox.frontend.routes :as urt]
            [uxbox.services.users :as usu]
            [uxbox.services :as usv]
            [uxbox.tests.helpers :as th]))

(t/use-fixtures :each th/database-reset)

(t/deftest test-http-retrieve-profile
  (with-open [conn (db/connection)]
    (let [user (th/create-user conn 1)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/profile/me")
              [status data] (th/http-get user uri)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 200 status))
          (t/is (= (:fullname data) "User 1"))
          (t/is (= (:username data) "user1"))
          (t/is (= (:metadata data) "1"))
          (t/is (= (:email data) "user1@uxbox.io"))
          (t/is (not (contains? data :password))))))))

(t/deftest test-http-update-profile
  (with-open [conn (db/connection)]
    (let [user (th/create-user conn 1)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/profile/me")
              data (assoc user
                          :fullname "Full Name"
                          :username "user222"
                          :metadata "222"
                          :email "user222@uxbox.io")
              [status data] (th/http-put user uri {:body data})]
          ;; (println "RESPONSE:" status data)
          (t/is (= 200 status))
          (t/is (= (:fullname data) "Full Name"))
          (t/is (= (:username data) "user222"))
          (t/is (= (:metadata data) "222"))
          (t/is (= (:email data) "user222@uxbox.io"))
          (t/is (not (contains? data :password))))))))

(t/deftest test-http-update-profile-photo
  (with-open [conn (db/connection)]
    (let [user (th/create-user conn 1)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/profile/me/photo")
              params [{:name "sample.jpg"
                       :part-name "file"
                       :content (io/input-stream
                                 (io/resource "uxbox/tests/_files/sample.jpg"))}]
              [status data] (th/http-multipart user uri params)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 204 status)))))))

