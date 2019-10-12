(ns watchdog.core
  (:require [clojure.java.jdbc :as j]
            [clojure.java.io :as io])
  (:gen-class))

(def exceeded-file "/tmp/exceeded")

(def ^:dynamic *db* nil)

(defn- db [sqlite-db-file]
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     sqlite-db-file})

(defn create-table []
  (j/execute! *db* "create table heartbeat (dt date)"))

(defn heartbeat []
  (j/insert! *db* :heartbeat {:dt (new java.util.Date)}))

(defn midnight []
  (let [calendar (new java.util.GregorianCalendar)]
    (.set calendar java.util.Calendar/HOUR_OF_DAY 0)
    (.set calendar java.util.Calendar/MINUTE 0)
    (.set calendar java.util.Calendar/SECOND 0)
    (.getTime calendar)))

(defn todays-limit-exceeded [ limit]
  (let [from (midnight)
        cnt (:cnt (first (j/query *db* ["select count(*) as cnt from heartbeat where dt > ?" (midnight)])))
        f (io/file exceeded-file)]
    (if (> cnt limit)
      (.createNewFile f)
      (io/delete-file f true))))

(defn cleanup []
  (let [to (midnight)]
    (j/execute! *db* ["delete from heartbeat where dt < ?" to])))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [sqlite-db-file (first args)]
    (binding [*db* (db sqlite-db-file)]
      (when-not (.exists (io/file sqlite-db-file))
        (create-table))
      (heartbeat)
      (cleanup)
      (todays-limit-exceeded 15))))
