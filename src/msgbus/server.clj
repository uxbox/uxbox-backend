;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>
;; All rights reserved.

(ns msgbus.server
  (:require [clojure.core.async :as a]
            [buddy.core.codecs :as codecs]
            [msgbus.serializers :as sz]
            [msgbus.atomic :as atomic]
            [msgbus.executor :as exec]
            [msgbus.zeromq :as zmq]
            [msgbus.util :as util]))

(defn- receive-multipart
  [socket]
  (loop [data (transient [])]
    (let [frame (zmq/recv! socket)]
      (if (zmq/more? socket)
        (recur (conj! data frame))
        (-> (conj! data frame)
            (persistent!))))))

(defn- handle-request
  [request handler]
  (when-let [{:keys [socket readable? errored?]} request]
    (if readable?
      (let [[msg] (receive-multipart socket)
            msg (sz/decode msg :transit+msgpack)
            response (handler msg)
            response (sz/encode response :transit+msgpack)]
        (zmq/send! socket response))
      (println "still not readable:" request))))

(defn- schedule
  [server]
  (let [active (.-active server)]
    (when (atomic/compare-and-set! active false true)
      (exec/execute! server))))

(deftype SimpleServer [context poller stoped active handler]
  Runnable
  (run [this]
    (println "SimpleServer$run")
    (when-not @stoped
      (try
        (when-let [requests (zmq/poll! poller 1000)]
          (handle-request (first requests) handler))
        (finally
          (atomic/set! active false)
          (when-not (zmq/empty? poller)
            (schedule this))))))

  java.lang.AutoCloseable
  (close [_]
    (atomic/set! stoped true)
    (.term context)))

(defn simple-server
  [{:keys [endpoint io-threads handler]
    :or {io-threads 1}}]
  (let [context (zmq/context io-threads)
        socket (zmq/socket context :rep)
        poller (zmq/poller context 1)
        stoped (atomic/boolean false)
        active (atomic/boolean false)]
    (zmq/bind! socket endpoint)
    (zmq/register! poller socket #{:poll-in :poll-err})
    (let [server (SimpleServer. context poller stoped active handler)]
      (exec/execute! server)
      server)))
