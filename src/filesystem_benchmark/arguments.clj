(ns filesystem-benchmark.arguments
  (:require [clojure.java.io            :as io]
            [clojure.tools.cli          :as cli]
            [clojure.math.numeric-tower :as math]
            [clojure.string             :as string]
            [filesystem-benchmark.utilities :refer [tprn]]))

(defn parse-int
  "Takes a string and tries to parse it as an integer."
  [number-string]
  (Integer/parseInt number-string))

(def cli-options
  ;; An option with a required argument
  [["-p" "--path PATH" "Path to the file system being tested."
    :default  (io/file "./")
    :parse-fn io/file
    :validate [#(.exists %)      "Path must exist."
               #(.isDirectory %) "Path must be a directory."]]
   ["-f" "--file-size NUM_BYTES" "Size of the files to be generated in bytes."
    :default  (math/expt 2 22)
    :parse-fn parse-int
    :validate [#(> % 0)          "Size must be a positive integer."]]
   ["-m" "--max-files NUM_FILES" "The largest number of files to write concurrently."
    :default  (math/expt 2 10)
    :parse-fn parse-int
    :validate [#(> % 0)          "The number of files must be a positive integer."]]
   ["-s" "--step-size NUM_FILES" "The size of the steps in how many files are concurrently read."
    :default  8
    :parse-fn parse-int
    :validate [#(> % 0)          "The step size must be at least 1, lest we do nothing for a really long time."]]
   ["-t" "--threads NUM_THREADS" "Number of threads reading or writing files in parallel."
    :default (.availableProcessors (Runtime/getRuntime))
    :parse-fn parse-int
    :validate [#(> % 0)          "The number of threads must be at least 1."]]
   ["-b" "--benchmarks (read-throughput|write-throughput|write-type)"
    "Which of the available benchmarks to run. Separate by comma if multiple like write-throughput,read-throughput."
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
      (:help options)         {:exit-message summary :ok? true}  ; help => exit 0 with usage summary.
      errors                  {:exit-message errors}             ; errors => exit 1 with description of errors.
      (not (empty arguments)) {:exit-message "This program takes no positional arguments."}
      :else                   {:options      options})))
