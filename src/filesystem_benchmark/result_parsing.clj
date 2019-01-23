(ns filesystem-benchmark.result-parsing
  (:require [clojure.edn :refer [read-string]]))

(def first-pooled-1MB (read-string (slurp "/mnt/DL_cluster_storage/benchmark/pooled-read-throughput-1MB-8192copies-2019-01-23T13:18:26.067.edn")))
(def second-pooled-1MB (read-string (slurp "/mnt/DL_cluster_storage/benchmark/pooled-read-throughput-1MB-8192copies-2019-01-23T13:58:19.062.edn")))
(def first-1MB (read-string (slurp "/mnt/DL_cluster_storage/benchmark/read-throughput-1MB-8192copies-2019-01-23T13:22:07.102.edn")))
(def second-1MB (read-string (slurp "/mnt/DL_cluster_storage/benchmark/read-throughput-1MB-8192copies-2019-01-23T14:44:27.252.edn")))
(def first-pooled-4MB (read-string (slurp "/mnt/DL_cluster_storage/benchmark/pooled-read-throughput-4MB-8192copies-2019-01-23T13:27:19.625.edn")))
(def second-pooled-4MB (read-string (slurp "/mnt/DL_cluster_storage/benchmark/pooled-read-throughput-4MB-8192copies-2019-01-23T14:28:27.029.edn")))
(def first-4MB (read-string (slurp "/mnt/DL_cluster_storage/benchmark/read-throughput-4MB-8192copies-2019-01-23T13:24:07.497.edn")))
(def second-4MB (read-string (slurp "/mnt/DL_cluster_storage/benchmark/read-throughput-4MB-8192copies-2019-01-23T14:42:25.288.edn")))


(defn quota
  [k first-run second-run]
  (map #(/ (k %1) (k %2)) first-run second-run))

(defn difference
  [k first-seq second-seq]
  (map #(- (k %1) (k %2)) first-seq second-seq))

(defn average
  ([coll]
   (/ (reduce + coll) (count coll)))
  ([k coll]
   (/ (reduce + (map k coll)) (count coll))))

(average :throughput-MBps first-pooled-1MB)
;; 2641.8121143469507
(average :throughput-MBps second-pooled-1MB)
;; 3249.382658299129
(average :throughput-MBps first-1MB)
;; 1366.879272852438
(average :throughput-MBps second-1MB)
;; 554.6649017755135

(average :throughput-MBps first-pooled-4MB)
;; 1130.6870530904528
(average :throughput-MBps second-pooled-4MB)
;; 1193.5973885297649
(average :throughput-MBps first-4MB)
;; 1089.1460433472628
(average :throughput-MBps second-4MB)
;; 1069.8763009406975


(average (difference :throughput-MBps first-pooled-1MB second-pooled-1MB))
;; -607.5705439521782
(average (difference :throughput-MBps first-1MB second-1MB))
;; 812.2143710769243

(average (difference :throughput-MBps first-pooled-4MB second-pooled-4MB))
;; -62.91033543931206
(average (difference :throughput-MBps first-4MB second-4MB))
;; 19.269742406565406
