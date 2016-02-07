;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>
;; All rights reserved.

(ns msgbus.client
  (:require [clojure.core.async :as a]
            [buddy.core.codecs :as codecs]
            [msgbus.serializers :as sz]
            [msgbus.atomic :as atomic]
            [msgbus.executor :as exec]
            [msgbus.zeromq :as zmq]
            [msgbus.util :as util])
  (:import java.util.UUID))

(defn- receive-multipart
  [socket]
  (loop [data (transient [])]
    (let [frame (zmq/recv! socket)]
      (if (zmq/more? socket)
        (recur (conj! data frame))
        (-> (conj! data frame)
            (persistent!))))))

(defn- schedule
  [client]
  (println "schedule")
  (let [active (.-active client)]
    (when (atomic/compare-and-set! active false true)
      (exec/execute! client))))

(defn- handle-items
  [state poller items]
  (loop [item (first items)
         items (rest items)]
    (when item
      (let [socket (:socket item)]
        (cond
          (:readable? item)
          (let [[msg] (receive-multipart socket)
                out (get @state socket)]
            (a/offer! out msg)
            (a/close! out)
            (swap! state dissoc socket)
            (zmq/unregister! poller socket)
            (recur (assoc item :readable? false) items))

          ;; TODO: properly handle errors
          (:errored? item)
          (recur (first items) (rest items))

          :else
          (recur (first items) (rest items)))))))

(defprotocol IAsyncClient
  (-send [_ msg]))

(deftype AsyncClient [context state endpoint poller active]
  Runnable
  (run [this]
    (println "AsyncClient$run")
    (try
      (let [items (zmq/poll! poller 3000)]
        (when-not (empty? items)
          (println "AsyncClient$run$1" items)
          (handle-items state poller items)))
      (finally
        (atomic/set! active false)
        (when-not (zmq/empty? poller)
          (schedule this)))))

  IAsyncClient
  (-send [this msg]
    (a/go
      (let [socket (zmq/socket context :req)
            out (a/chan 1)
            msg (sz/encode msg :transit+msgpack)]
        (zmq/connect! socket endpoint)
        (zmq/register! poller socket #{:poll-in :poll-err})

        (swap! state assoc socket out)
        (zmq/send! socket msg)

        (schedule this)
        (when-let [response (a/<! out)]
          (sz/decode response :transit+msgpack))))))

(defn async-client
  [{:keys [endpoint io-threads]
    :or {io-threads 1}}]
  (let [context (zmq/context io-threads)
        poller (zmq/poller context 32)
        active (atomic/boolean false)
        state (atom {})]
    (AsyncClient. context state endpoint poller active)))

