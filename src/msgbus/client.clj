;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>
;; All rights reserved.

(ns msgbus.client
  (:require [clojure.core.async :as a]
            [buddy.core.codecs :as codecs]
            [uxbox.msgbus.serializers :as sz]
            [uxbox.msgbus.atomic :as atomic]
            [uxbox.msgbus.executor :as exec]
            [uxbox.msgbus.zeromq :as zmq]
            [uxbox.msgbus.util :as util])
  (:import java.util.UUID))

(defn- receive-multipart
  [socket]
  (loop [data (transient [])]
    (let [frame (zmq/recv socket)]
      (if (zmq/more? socket)
        (recur (conj! data frame))
        (-> (conj! data frame)
            (persistent!))))))

(defn- schedule
  [^AsyncClient client]
  (let [active (.-active client)]
    (when (atomic/compare-and-set active false true)
      (exec/execute! client))))

(defn- handle-items
  [state queue items]
  (loop [items (rest items)
         item (first item)]
    (if-let [{:keys [socket readable? errored?]} item]
      (cond
        readable?
        (let [frames (receive-multipart socket)]
          (when (and (= (count frames) 2)
                     (empty? (first frames)))
            (let [msg (nth frames 1)
                  out (get @state socket)]
              (a/offer! out msg)
              (a/close! out)
              (swap! state dissoc socket))
            (recur items (assoc item :readable? false))))

        ;; TODO: properly handle errors
        errored?
        (recur (rest items) (first items))

        :else
        (recur (rest items) (first items))))))

(deftype AsyncClient [context state endpoint poller active]
  Runnable
  (run [this]
    (try
      (let [items (zmq/poll! poller 1000)]
        (handle-items items))
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
        (zmq/connect! socket endpoints)
        (zmq/register! poller socket #{:poll-in :poll-err})

        (swap! state assoc socket out)
        (zmq/send! socket msg)

        (schedule this)

        (if-let [response (a/<! rsp)]
          (sz/decode response :transit+msgpack)
          response)))))

  java.lang.AutoCloseable
  (close [_]
    (.destroy ctx)))

(defn async-client
  [{:keys [endpoint io-threads]
    :or {io-threads 1}}]
  (let [context (zmq/context io-threads)
        poller (zmq/poller context 32)
        active (atomic/boolean false)
        state (atom {})]
    (AsyncClient. context state endpoints poller active)))
