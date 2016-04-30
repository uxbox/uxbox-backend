;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.quartz
  "A lightweight abstraction layer for quartz job scheduling library."
  (:import java.util.Properties
           org.quartz.Scheduler
           org.quartz.SchedulerException
           org.quartz.impl.StdSchedulerFactory
           org.quartz.Job
           org.quartz.JobBuilder
           org.quartz.JobDataMap
           org.quartz.JobExecutionContext
           org.quartz.TriggerBuilder
           org.quartz.CronScheduleBuilder
           org.quartz.SimpleScheduleBuilder
           org.quartz.PersistJobDataAfterExecution
           org.quartz.DisallowConcurrentExecution))

;; --- Implementation

(defn- map->props
  [data]
  (let [p (Properties.)]
    (run! (fn [[k v]] (.setProperty p (name k) (str v))) (seq data))
    p))

(deftype ^{org.quartz.PersistJobDataAfterExecution true
           org.quartz.DisallowConcurrentExecution true}
    PersistentJobImpl []
  Job
  (execute [_ context]
    (let [^JobDataMap data (.. context getJobDetail getJobDataMap)
          callable (.get data "callable")
          state (.get data "state")
          args (.get data "args")
          result (apply callable state args)]
      (.put ^JobDataMap data "state" result))))

(deftype JobImpl []
  Job
  (execute [_ context]
    (let [^JobDataMap data (.. context getJobDetail getJobDataMap)
          args (.get data "args")
          callable (.get data "callable")]
      (apply callable args))))

(defn- build-trigger
  [{:keys [cron group name repeat? interval]
    :or {repeat? true}
    :as opts}]
  (let [schedule (if cron
                   (CronScheduleBuilder/cronSchedule cron)
                   (let [schd (SimpleScheduleBuilder/simpleSchedule)
                         schd (if (number? repeat?)
                                (.withRepeatCount schd repeat?)
                                (.repeatForever schd))]
                     (.withIntervalInMilliseconds schd interval)))
        name (str name "-trigger")
        builder (doto (TriggerBuilder/newTrigger)
                  (.startNow)
                  (.withIdentity name group)
                  (.withSchedule schedule))]
    (.build builder)))

(defn- build-job-detail
  [f {:keys [group name state args] :as opts}]
  (let [data (JobDataMap. {"callable" f
                           "state" state
                           "args" args})
        builder (doto (JobBuilder/newJob (if state PersistentJobImpl JobImpl))
                  (.storeDurably true)
                  (.usingJobData data)
                  (.withIdentity name group))]
    (.build builder)))

(defn- resolve-fn
  [func opts]
  (cond
    (symbol? func)
    (let [ns (symbol (namespace func))
          _  (require ns)
          var (resolve func)
          opts (assoc (merge (meta var) opts)
                      :name (name (or (:name opts) (gensym "uxbox")))
                      :group (name (:group opts "uxbox")))]
      [@var opts])

    (fn? func)
    (let [opts (assoc opts
                      :name (name (:name opts) (gensym "uxbox"))
                      :group (name (:group opts "uxbox")))]
      [func opts])))

(defn- next-name
  []
  (str (gensym "uxbox")))

;; --- Public Api

(defn scheduler
  "Create a new scheduler instance."
  ([] (scheduler nil))
  ([{:keys [name daemon? threads thread-priority]
     :or {name "uxbox-scheduler"
          daemon? true
          threads 1
          thread-priority Thread/MIN_PRIORITY}}]
   (let [params {"org.quartz.threadPool.threadCount" threads
                 "org.quartz.threadPool.threadPriority" thread-priority
                 "org.quartz.threadPool.makeThreadsDaemons" (if daemon? "true" "false")
                 "org.quartz.scheduler.instanceName" name
                 "org.quartz.scheduler.makeSchedulerThreadDaemon" (if daemon? "true" "false")}
         props (map->props params)
         factory (StdSchedulerFactory. props)]
     (.getScheduler factory))))

(defn start!
  ([scheduler]
   (.start ^Scheduler scheduler))
  ([scheduler ms]
   (.startDelayed ^Scheduler scheduler (int ms))))

(defn stop!
  [scheduler]
  (.shutdown ^Scheduler scheduler true))

(defn schedule!
  ([schd f] (schedule! schd f nil))
  ([schd f opts]
   (let [[f opts] (resolve-fn f opts)
         job (build-job-detail f opts)
         trigger (build-trigger opts)]
     (.scheduleJob ^Scheduler schd job trigger))))
