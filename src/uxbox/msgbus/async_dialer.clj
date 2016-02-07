;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>
;; All rights reserved.

(ns uxbox.msgbus.async-dialer
  (:require [clojure.core.async :as a]
            [buddy.core.codecs :as codecs]
            [catacumba.serializers :as cs]
            [uxbox.msgbus.executor :as exec]
            [uxbox.msgbus.zmq :as zmq]
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

(defn- connect
  [context endpoints]
  (let [poller (zmq/poller context (count endpoints))]
    (doseq [endpoint endpoints]
      (let [socket (zmq/socket context :dealer)]
        (zmq/connect! socket endpoint)
        (zmq/register! poller socket)))
    poller))

(defn- handle-items
  [state queue items]
  (loop [items (rest items)
         item (first item)]
    (if-let [{:keys [socket readable? writable? errored?]} item]
      (cond
        readable?
        (let [frames (receive-multipart socket)]
          (when (and (= (count frames) 3)
                     (empty? (first frames)))
            (let [msgid (codecs/bytes->str (nth frames 1))
                  msg (sc/decode (nth frames 2))
                  out (get @state msgid)]
              (a/offer! out msg)
              (vswap! state dissoc msgid))
            (recur items (assoc item :readable? false))))

        writable?
        (let [item (a/poll! queue)]
          (when-let [{:keys [msg out]} item]
            (let [id (message-id msg)]
              (vswap! state assoc id msg)
              (zmq/send! socket "")
              (zmq/send! socket id)
              (zmq/send! socket msg)))
          (recur items (assoc item :writable? false)))

        ;; TODO: properly handle errors
        errored?
        (recur (rest items) (first items))

        :else
        (recur (rest items) (first items))))))

(deftype AsyncClient [context queue state endpoints]
  Runnable
  (run [this]
    (let [poller (connect context endpoints)]
      (loop []
        (when-not (interrumpted?)
          (let [items (zmq/poll! poller)]
            (handle-items state queue items)
            (recur))))))

  IAsyncClient
  (-send [_ msg]
    (a/go
      (let [out (a/chan 1)
            item {:msg (cs/encode msg :transit+msgpack)
                  :out out}]
        (a/>! queue item)
        (if-let [rsp (a/<! out)]
          (cs/decode rsp :transit+msgpack)
          rsp))))

  java.lang.AutoCloseable
  (close [_]
    (.destroy ctx)))

(defn async-client
  [{:keys [endpoints io-threads buffer]
    :or {io-threads 1 buffer 1}}]
  {:pre [(coll? endpoints)]}
  (let [ctx (zmq/context io-threads)
        queue (a/chan buffer)
        state (volatile! {})
        client (AsyncClient. ctx queue state endpoints)]
    (exec/execute! client)
    client))
