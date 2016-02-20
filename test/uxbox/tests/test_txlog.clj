(ns uxbox.tests.test-txlog
  "A txlog and services abstraction generic tests."
  (:require [clojure.test :as t]
            [uxbox.tests.helpers :as th]))

(t/use-fixtures :each th/database-reset)

(t/deftest experiment-spec1
  (t/is (= 1 1)))

(t/deftest experiment-spec2
  (t/is (= 1 1)))
