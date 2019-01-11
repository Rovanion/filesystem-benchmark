(ns filesystem-benchmark.arguments
  (:require [clojure.java.io   :as io]
            [clojure.tools.cli :as cli]))

(def cli-options
  ;; An option with a required argument
  [["-p" "--path PATH" "Path to the file system being tested."
    :default (io/file "./")
    :parse-fn io/file
    :validate [#(.exists %)      "Path must exist."
               #(.isDirectory %) "Path must be a directory."]]
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :update-fn inc]
   ;; A boolean option defaulting to nil
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
      arguments       {:exit-message "This program takes no arguments."}
      :else           {:options      options})))
