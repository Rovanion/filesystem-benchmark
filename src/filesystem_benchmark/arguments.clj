(ns filesystem-benchmark.arguments
  (:require [clojure.java.io            :as io]
            [clojure.tools.cli          :as cli]
            [clojure.math.numeric-tower :as math]
            [clojure.string             :as string]
            [filesystem-benchmark.utilities :refer [tprn]]))

(def cli-options
  ;; An option with a required argument
  [["-p" "--path PATH" "Path to the file system being tested."
    :default  (io/file "./")
    :parse-fn io/file
    :validate [#(.exists %)      "Path must exist."
               #(.isDirectory %) "Path must be a directory."]]
   ["-s" "--file-size NUM_BYTES" "Size of the files to be generated in bytes."
    :default  (math/expt 2 22)
    :parse-fn (biginteger "1")
    :validate [#(> % 0)          "Size must be a positive integer."]]
   ["-c" "--concurrency NUM_FILES" "The number of files to write concurrently."
    :default  (math/expt 2 10)
    :parse-fn (biginteger "1")
    :validate [#(> % 0)          "The number of files must be a positive integer."]]
   ["-b" "--benchmarks (read-throughput|write-throughput|write-type)"
    "Which of the available benchmarks to run. Separate by comma if multiple."
    :default  [:write-throughput :write-type :read-throughput]
    :parse-fn (fn [benchmarks]
                (as-> benchmarks b
                    (string/split b #",")
                    (map keyword b)))
    :validate [#(every? #{:write-throughput :write-type :read-throughput} %)
               "Benchmark must be a list of either write-throughput, write-type or read-throughput"]]
   ;; Additional -v increases the verbosity level.
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :update-fn inc]
   ;; A boolean option defaulting to nil.
   ["-h" "--help"]])

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options) {:exit-message summary :ok? true}  ; help => exit 0 with usage summary.
      errors          {:exit-message errors}             ; errors => exit 1 with description of errors.
      (not (empty arguments)) {:exit-message "This program takes no positional arguments."}
      :else           {:options      options})))
