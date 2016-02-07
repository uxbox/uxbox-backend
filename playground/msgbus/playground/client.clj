(ns msgbus.playground.client
  (:require [clojure.core.async :as a]
            [msgbus.client :as c]))

(defn -main
  [& args]
  (let [client (c/async-client {:endpoint "tcp://127.0.0.1:4444"})
        response1 (a/<!! (c/-send client {:foo "bar"}))
        response2 (a/<!! (c/-send client {:foo "bar"}))]
    (println response1)))
