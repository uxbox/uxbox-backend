(ns uxbox.msgbus
  (:require [clojure.core.async :as a]
            [uxbox.util.zmq :as zmq]
            [promesa.core :as p]
            [buddy.core.codecs :as codecs]
            [catacumba.serializers :as cs])
  (:import java.util.UUID))

(defn- interrumpted?
  []
  (.isInterrupted (Thread/currentThread)))

(defn- message-id
  [msg]
  (str (UUID/randomUUID)))

(defn- receive!
  [socket]
  (loop [data (transient! [])]
    (let [frame (zmq/recv socket)]
      (if (zmq/more? socket)
        (recur (conj! data frame))
        (-> (conj! data frame)
            (persistent))))))

(deftype AsyncClient [ctx queue state]
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

  Runnable
  (run [this]
    (let [socket (zmq/socket ctx)
          poller (zmq/poller 1)]
      (zmq/register! poller socket)
      (loop []
        (when-not (interrumpted?)
          (zmq/poll! poller)
          (cond
            (zmq/readable? poller 1)
            (let [frames (receive socket)]
              (if (and (= (count frames) 3)
                       (empty? (first frames)))
                (let [msgid (codecs/bytes->str (nth frames 1))
                      msg (sc/decode (nth frames 2))
                      out (get @state msgid)]
                  (a/offer! out msg)
                  (vswap! state dissoc msgid)
                  (recur))))

            (zmq/writable? poller 1)
            (if-let [{:keys [msg out]} (a/poll! queue)]
              (let [id (message-id msg)]
                (vswap! state assoc id msg)
                (zmq/send! socket "")
                (zmq/send! socket id)
                (zmq/send! socket msg)
                (recur))
              (recur)))))))

  java.lang.AutoCloseable
  (close [_]
    (.destroy ctx)))

(defn async-client
  [{:keys [endpoint io-threads buffer]
    :or {io-threads 1 buffer 1}}]
  (let [ctx (zmq/context io-threads)
        queue (a/chan buffer)
        state (volatile! {})]
    (AsyncClient. ctx queue state)))
