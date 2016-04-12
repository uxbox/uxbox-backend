(ns uxbox.tests.test-library
  (:require [clojure.test :as t]
            [suricatta.core :as sc]
            [catacumba.testing :refer (with-server)]
            [catacumba.serializers :as sz]
            [uxbox.persistence :as up]
            [uxbox.frontend.routes :as urt]
            [uxbox.services.library :as uslb]
            [uxbox.tests.helpers :as th]))

(t/use-fixtures :each th/database-reset)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend Test
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(t/deftest test-http-list-color-collection
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)
          data {:user (:id user)
                :name "coll1"
                :data #{1}}
          coll (uslb/create-color-collection conn data)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/library/colors")
              [status data] (th/http-get user uri)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 200 status))
          (t/is (= 1 (count data))))))))

(t/deftest test-http-create-color-collection
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/library/colors")
              data {:user (:id user)
                    :name "coll1"
                    :data #{1}}
              params {:body data}
              [status data] (th/http-post user uri params)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 201 status))
          (t/is (= (:user data) (:id user)))
          (t/is (= (:data data) #{1}))
          (t/is (= (:name data) "coll1")))))))

(t/deftest test-http-update-color-collection
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)
          data {:user (:id user)
                :name "coll1"
                :data #{1}}
          coll (uslb/create-color-collection conn data)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/library/colors/" (:id coll))
              params {:body (assoc coll :name "coll2" :data #{2})}
              [status data] (th/http-put user uri params)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 200 status))
          (t/is (= (:user data) (:id user)))
          (t/is (= (:data data) #{2}))
          (t/is (= (:name data) "coll2")))))))

(t/deftest test-http-color-collection-delete
  (with-open [conn (up/get-conn)]
    (let [user (th/create-user conn 1)
          data {:user (:id user)
                :name "coll1"
                :data #{1}}
          coll (uslb/create-color-collection conn data)]
      (with-server {:handler (urt/app)}
        (let [uri (str th/+base-url+ "/api/library/colors/" (:id coll))
              [status data] (th/http-delete user uri)]
          (t/is (= 204 status))
          (let [sqlv ["SELECT * FROM color_collections WHERE \"user\"=?" (:id user)]
                result (sc/fetch conn sqlv)]
            (t/is (empty? result))))))))
