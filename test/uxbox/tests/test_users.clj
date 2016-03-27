(ns uxbox.tests.test-users
  (:require [clojure.test :as t]
            [promesa.core :as p]
            [clj-http.client :as http]
            [catacumba.testing :refer (with-server)]
            [buddy.hashers :as hashers]
            [uxbox.persistence :as up]
            [uxbox.frontend.routes :as urt]
            [uxbox.services.users :as usu]
            [uxbox.services :as usv]
            [uxbox.tests.helpers :as th]))

(t/use-fixtures :each th/database-reset)

(t/deftest test-http-update-and-retrieve-profile
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)]
      ;; Retrieve
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/profile/me")
              [status data] (th/http-get user uri)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 200 status))
          (t/is (= (:fullname data) "User 1"))
          (t/is (= (:username data) "user1"))
          (t/is (= (:metadata data) nil))
          (t/is (= (:email data) "user1@uxbox.io"))
          (t/is (not (contains? data :password)))))

      ;; Update
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/profile/me")
              data (assoc user
                          :fullname "Full Name"
                          :username "user222"
                          :metadata [1 2 3]
                          :email "user222@uxbox.io")
              [status data] (th/http-put user uri {:body data})]
          (println "RESPONSE:" status data)
          (t/is (= 200 status))
          (t/is (= (:fullname data) "Full Name"))
          (t/is (= (:username data) "user222"))
          (t/is (= (:metadata data) [1 2 3]))
          (t/is (= (:email data) "user222@uxbox.io"))
          (t/is (not (contains? data :password))))))))


