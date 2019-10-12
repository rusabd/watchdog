(ns watchdog.core
  (:require [clojure.java.jdbc :as j]
            [clojure.java.io :as io])
  (:gen-class))

(def exceeded-file "/tmp/exceeded")

(defn db [sqlite-db-file]
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     sqlite-db-file})

(defn create-table [sqlite-db-file]
  (j/execute! (db sqlite-db-file) "create table heartbeat (dt date)"))

(defn heartbeat [sqlite-db-file]
  (j/insert! (db sqlite-db-file) :heartbeat {:dt (new java.util.Date)}))

(defn midnight []
  (let [calendar (new java.util.GregorianCalendar)]
    (.set calendar java.util.Calendar/HOUR_OF_DAY 0)
    (.set calendar java.util.Calendar/MINUTE 0)
    (.set calendar java.util.Calendar/SECOND 0)
    (.getTime calendar)))

(defn todays-limit-exceeded [sqlite-db-file limit]
  (let [from (midnight)
        cnt (:cnt (first (j/query (db sqlite-db-file) ["select count(*) as cnt from heartbeat where dt > ?" (midnight)])))
        f (io/file exceeded-file)]
    (if (> cnt limit)
      (.createNewFile f)
      (io/delete-file f true))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [sqlite-db-file (first args)]
    (when-not (.exists (io/file sqlite-db-file))
      (create-table))
    (heartbeat sqlite-db-file)
    (todays-limit-exceeded sqlite-db-file 15)))
