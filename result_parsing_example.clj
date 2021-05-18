(ns filesystem-benchmark.result-parsing
  (:require [clojure.edn :refer [read-string]])
  (:refer-clojure :exclude [future-call]))

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


;;; It was decided that the pooled strategy managed to saturate the
;;; line better than the threaded execution strategy.

(def fourMB-16k-files (read-string (slurp "/mnt/DL_cluster_storage/benchmark/read-throughput-4MB-16384copies-2019-01-23T16:35:33.484.edn")))

(average :throughput-MBps fourMB-16k-files)
;; 1167.7008237425032

;;; Genom htop kan man observera hur den trådpoolen består av 64
;;; trådar. Med tanke på att maskinen i sig har 80st exekveringskärnor
;;; skulle man kunna tänka sig att det vore fördelaktigt att öka
;;; storleken på trådpoolen till 80.

;;; Dock noteras det att pooledExecutor som används initieras med
;;; 2 + (.availableProcessors (Runtime/getRuntime)) trådar i poolen.
(.availableProcessors (Runtime/getRuntime))
;; => 80

;;; Mycket riktigt
(.getPoolSize clojure.lang.Agent/pooledExecutor)
;; => 82

;;; En ny slutsats dras: Jag räknade helt enkelt fel i htop. Totalt
;;; har javaprocessen 170st trådar vilket inte säger oss någonting.

(def second-fourMB-16k-files (read-string (slurp "/mnt/DL_cluster_storage/benchmark/read-throughput-4MB-16384copies-2019-01-23T16:51:53.317.edn")))


(average :throughput-MBps second-fourMB-16k-files)
;; 1129.0135030572744

(average (difference :throughput-MBps fourMB-16k-files second-fourMB-16k-files))
;; 38.68732068522851

;;; Inte jättestor skillnad mellan de två körningarna. Och om de
;;; rapporterade siffrorna är korrekta så är vi ganska nära att
;;; saturera linan.

(* 8 (average :throughput-MBps second-fourMB-16k-files))
;; 9032.108024458195

;;; Inte riktigt 10Gbps men nära nog.


;;;; Så hur var det med skrivning då?

(def first-pooled-1MB-write (read-string (slurp "/mnt/DL_cluster_storage/benchmark/write-throughput-1MB-8192copies-2019-01-23T13:21:49.055.edn")))
(def second-pooled-1MB-write (read-string (slurp "/mnt/DL_cluster_storage/benchmark/write-throughput-1MB-8192copies-2019-01-23T14:43:53.533.edn")))
(def first-pooled-4MB-write (read-string (slurp "/mnt/DL_cluster_storage/benchmark/write-throughput-4MB-8192copies-2019-01-23T13:22:43.410.edn")))
(def second-pooled-4MB-write (read-string (slurp "/mnt/DL_cluster_storage/benchmark/write-throughput-4MB-8192copies-2019-01-23T14:41:01.855.edn")))

(average :throughput-MBps first-pooled-1MB-write)
;; 733.0983773484977
(average :throughput-MBps second-pooled-1MB-write)
;; 416.2128015389157

(average :throughput-MBps first-pooled-4MB-write)
;; 695.4825063497987
(average :throughput-MBps second-pooled-4MB-write)
;; 607.5201550961763

;;; Även här ser vi att det är först vid 4MB som vi blir stabila
;;; resultatmässigt. En observation jag gjorde under tiden skrivtesten
;;; körde var att det både lästes och skrevs över gränssnittet.


(def first-pooled-4MB-write-16k-files (read-string (slurp "/mnt/DL_cluster_storage/benchmark/write-throughput-4MB-16384copies-2019-01-23T16:32:18.856.edn")))
(def second-pooled-4MB-write-16k-files (read-string (slurp "/mnt/DL_cluster_storage/benchmark/write-throughput-4MB-16384copies-2019-01-23T16:49:09.211.edn")))

(average :throughput-MBps first-pooled-4MB-write-16k-files)
;; 518.5092360140801
(average :throughput-MBps second-pooled-4MB-write-16k-files)
;; 714.1648191698366

;;; Här fick vi ganska stora skillnader mellan första och andra körningen, vilket är spännande.
