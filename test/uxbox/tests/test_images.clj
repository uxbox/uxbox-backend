(ns uxbox.tests.test-images
  (:require [clojure.test :as t]
            [promesa.core :as p]
            [suricatta.core :as sc]
            [clj-uuid :as uuid]
            [clojure.java.io :as io]
            [catacumba.testing :refer (with-server)]
            [buddy.core.codecs :as codecs]
            [uxbox.persistence :as up]
            [uxbox.sql :as sql]
            [uxbox.frontend.routes :as urt]
            [uxbox.services.images :as images]
            [uxbox.services :as usv]
            [uxbox.tests.helpers :as th]))

(t/use-fixtures :each th/database-reset)

(t/deftest test-http-list-image-collections
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)
          data {:user (:id user)
                :name "coll1"}
          coll (images/create-collection conn data)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/library/image-collections")
              [status data] (th/http-get user uri)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 200 status))
          (t/is (= 1 (count data))))))))

(t/deftest test-http-create-image-collection
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/library/image-collections")
              data {:user (:id user)
                    :name "coll1"}
              params {:body data}
              [status data] (th/http-post user uri params)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 201 status))
          (t/is (= (:user data) (:id user)))
          (t/is (= (:name data) "coll1")))))))

(t/deftest test-http-update-image-collection
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)
          data {:user (:id user)
                :name "coll1"}
          coll (images/create-collection conn data)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/library/image-collections/" (:id coll))
              params {:body (assoc coll :name "coll2")}
              [status data] (th/http-put user uri params)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 200 status))
          (t/is (= (:user data) (:id user)))
          (t/is (= (:name data) "coll2")))))))

(t/deftest test-http-image-collection-delete
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)
          data {:user (:id user)
                :name "coll1"
                :data #{1}}
          coll (images/create-collection conn data)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/library/image-collections/" (:id coll))
              [status data] (th/http-delete user uri)]
          (t/is (= 204 status))
          (let [sqlv (sql/get-image-collections {:user (:id user)})
                result (sc/fetch conn sqlv)]
            (t/is (empty? result))))))))

(t/deftest test-http-create-image
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/library/images")
              params [{:name "sample.jpg"
                       :part-name "file"
                       :content (io/input-stream
                                 (io/resource "uxbox/tests/_files/sample.jpg"))}]
              [status data] (th/http-multipart user uri params)]
          (println "RESPONSE:" status data)
          (t/is (= 201 status))
          (t/is (= (:user data) (:id user)))
          (t/is (= (:name data) "sample.jpg")))))))

