;;; Reimplement future in order to use a different executor.
(ns filesystem-benchmark.thread-pool
  (:import [java.util.concurrent Executor Executors ThreadFactory])
  (:refer-clojure :exclude [future-call]))

(def ^:private thread-pool nil)

(defn counted-thread-factory
    "Create a ThreadFactory that maintains a counter for naming Threads.
     name-format specifies thread names - use %d to include counter
     daemon is a flag for whether threads are daemons or not
     opts is an options map:
       init-fn - function to run when thread is created"
  ([name-format daemon]
   (counted-thread-factory name-format daemon nil))
  ([name-format daemon {:keys [init-fn] :as opts}]
   (let [counter (atom 0)]
     (reify
       ThreadFactory
       (newThread [this runnable]
         (let [body (if init-fn
                      (fn [] (init-fn) (.run ^Runnable runnable))
                      runnable)
               t (Thread. ^Runnable body)]
           (doto t
             (.setName (format name-format (swap! counter inc)))
             (.setDaemon daemon))))))))

(defn start-thread-pool
  [number-threads]
  (if thread-pool
    thread-pool
    (def thread-pool (Executors/newFixedThreadPool
                      number-threads
                      (counted-thread-factory "filesystem-benchmark-thread-pool-%d" true)))))

(defn binding-conveyor-fn
  {:private true}
  [f]
  (let [frame (clojure.lang.Var/cloneThreadBindingFrame)]
    (fn
      ([]
         (clojure.lang.Var/resetThreadBindingFrame frame)
         (f))
      ([x]
         (clojure.lang.Var/resetThreadBindingFrame frame)
         (f x))
      ([x y]
         (clojure.lang.Var/resetThreadBindingFrame frame)
         (f x y))
      ([x y z]
         (clojure.lang.Var/resetThreadBindingFrame frame)
         (f x y z))
      ([x y z & args]
         (clojure.lang.Var/resetThreadBindingFrame frame)
         (apply f x y z args)))))

(defn ^:private deref-future
  ([^java.util.concurrent.Future fut]
     (.get fut))
  ([^java.util.concurrent.Future fut timeout-ms timeout-val]
     (try (.get fut timeout-ms java.util.concurrent.TimeUnit/MILLISECONDS)
          (catch java.util.concurrent.TimeoutException e
            timeout-val))))

(defn future-call
  "Takes a function of no args and yields a future object that will
  invoke the function in another thread, and will cache the result and
  return it on all subsequent calls to deref/@. If the computation has
  not yet finished, calls to deref/@ will block, unless the variant
  of deref with timeout is used. See also - realized?."
  {:static true}
  [f]
  (if (not thread-pool)
    (throw "Thread pool not started, unable to submit jobs to it.")
    (let [f   (binding-conveyor-fn f)
          fut (.submit thread-pool ^Callable f)]
      (reify
        clojure.lang.IDeref
        (deref [_] (deref-future fut))
        clojure.lang.IBlockingDeref
        (deref
            [_ timeout-ms timeout-val]
          (deref-future fut timeout-ms timeout-val))
        clojure.lang.IPending
        (isRealized [_] (.isDone fut))
        java.util.concurrent.Future
        (get [_] (.get fut))
        (get [_ timeout unit] (.get fut timeout unit))
        (isCancelled [_] (.isCancelled fut))
        (isDone [_] (.isDone fut))
        (cancel [_ interrupt?] (.cancel fut interrupt?))))))

(defmacro pooled-future
  "Takes a body of expressions and yields a future object that will
  invoke the body in another thread, and will cache the result and
  return it on all subsequent calls to deref/@. If the computation has
  not yet finished, calls to deref/@ will block, unless the variant of
  deref with timeout is used. See also - realized?."
  [& body]
  `(future-call (^{:once true} fn* [] ~@body)))
