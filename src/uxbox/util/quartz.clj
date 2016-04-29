;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.quartz
  "A lightweight abstraction layer for quartz job scheduling library."
  (:import org.quartz.Scheduler
           org.quartz.SchedulerException
           org.quartz.impl.StdSchedulerFactory
           org.quartz.Job
           org.quartz.JobBuilder
           org.quartz.TriggerBuilder
           org.quartz.SimpleScheduleBuilder))

(defn- map->props
  [data]
  (let [p (Properties.)]
    (run! (fn [[k v]] (.setProperty p (name k) (str v))) (seq data))
    p))

(defn scheduler
  "Create a new scheduler instance."
  [{:keys [name daemon? threads]
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
    (.getScheduler factory)))

(defn start!
  ([scheduler]
   (.start ^Scheduler scheduler))
  ([scheduler ms]
   (.startDelayed ^Scheduler scheduler (int ms))))

(defn stop!
  [scheduler]
  (.shutdown ^Scheduler scheduler true))

(defn- make-simple-trigger-schedule
  [{:keys [repeat? interval name group]
    :or {repeat? true group "uxbox"}}]
  (assert (pos? interval) "interval should be specified")
  (let [schd (doto (SimpleScheduleBuilder/simpleSchedule)
               (.withIdentity ("job1", "group1")
        schd (if (number? repeat?)
               (.withRepeatCount schd repeat?)
               (.repeatForever schd))]
    (.withIntervalInMilliseconds schd interval)))

(defn- make-cron-trigger-schedule
  [expr]
  (CronScheduleBuilder/cronSchedule expr))

(defn- make-trigger
  [{:keys [cron group name]
    :as opts}]
  (let [schedule (if cron
                   (make-cron-trigger-schedule cron)
                   (make-simple-trigger-schedule opts))
        name (str name "-trigger")
        trigger (doto (TriggerBuilder/newTrigger)
                  (.withIdentity name group))]
    (.build trigger)))

(defn- make-job
  [f {:keys [group name] :or {group "uxbox"} :as opts}]
  (let [instance (reify Job (execute [_ _] (f)))
        name (or name (str (gensym "uxbox-job")))
        detail (doto (JobBuilder/newJob (class instance))
                 (.withIdentity name group))]
    (.build detail)))

(defn schedule!
  ([schd f] (schedule! sched f nil))
  ([schd f {:keys [name group] :as opts}]
   (let [name (or name (str (gensym "uxbox")))
         opts (assoc opts :name name :group "uxbox")
         job (make-job f opts)
         trigger (make-trigger opts)]
     (.scheduleJob ^Scheduler schd job trigger))))
