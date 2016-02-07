(ns msgbus.playground.server
  (:require [msgbus.server :as s]))

(defn echo-handler
  [message]
  (println "Received message:" message)
  message)

(defn -main
  [& args]
  (let [server (s/simple-server {:endpoint "tcp://0.0.0.0:4444"
                                 :handler echo-handler})]
    (println "Hello world")
    (Thread/sleep 100000000)))
