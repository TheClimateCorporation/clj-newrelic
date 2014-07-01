(ns com.climate.newrelic.trace)

(defn- traced-meta [fname]
  {'com.newrelic.api.agent.Trace {:metricName (str *ns* \. fname)}})

(defn- make-traced [tname fname arg-list body]
  (let [i-args (repeatedly (count arg-list) gensym)]
    `(do
       (definterface iname# (~'invoke [~@i-args]))
       (deftype ~tname []
         iname#
         (~(with-meta 'invoke (traced-meta fname)) [~'_ ~@i-args]
           (let [~@(interleave arg-list i-args)]
             ~@body))))))

(defn- is-varargs
  "checks whether an args list uses varargs"
  [arg-list]
  (let [l (count arg-list)]
    (when (> l 1)
      (let [sym (nth arg-list (- l 2))]
        (when (instance? clojure.lang.Named sym)
          (= (name sym) "&"))))))

(defn- insert-amp [arg-list]
  (concat (drop-last arg-list) ['& (last arg-list)]))

(defn- remove-amp [arg-list]
  (concat (drop-last 2 arg-list) [(last arg-list)]))

(defn- preproc-decl [fdecl]
  ; mostly stolen from clojure.core/defn
  (let [; get docstring
        m (if (string? (first fdecl))
            {:doc (first fdecl)}
            {})
        fdecl (if (string? (first fdecl))
                (next fdecl)
                fdecl)
        ; look for meta at beginning
        m (if (map? (first fdecl))
            (conj m (first fdecl))
            m)
        fdecl (if (map? (first fdecl))
                (next fdecl)
                fdecl)
        ; allow for single-arity functions
        fdecl (if (vector? (first fdecl))
                (list fdecl)
                fdecl)
        ; look for meta at end
        m (if (map? (last fdecl))
            (conj m (last fdecl))
            m)
        fdecl (if (map? (last fdecl))
                (butlast fdecl)
                fdecl)
        ; track what's var-args and what's not
        ann-fdecl (for [[args & body] fdecl]
                    (let [var-args (is-varargs args)]
                      {:var-args var-args
                       :args (if var-args (remove-amp args) args)
                       :body body}))]
    [m ann-fdecl]))

(defmacro defn-traced
  "Drop-in replacement for clojure.core/defn.

  Tells Newrelic that entry/exit to this function should be traced.
  Allows time spent in this function to be tracked as a % of total
  request time.

  If one function defined with defn-traced calls another defined with
  defn-traced, Newrelic will correctly show both in its transaction
  trace and show how much time was spent inside/outside the inner
  function."
  [fname & fdecl]
  (let [[m ann-fdecl] (preproc-decl fdecl)
        num-cases (count ann-fdecl)
        tnames (repeatedly num-cases #(gensym fname))
        onames (repeatedly num-cases gensym)]
    `(do
       ~@(for [[{:keys [args body]} tname] (map vector ann-fdecl tnames)]
           (make-traced tname fname args body))
       (let [~@(apply concat (for [[tname oname] (map vector tnames onames)]
                               `(~oname (new ~tname))))]
         (defn
           ~fname ~m
           ~@(for [[{:keys [var-args args]} oname] (map vector ann-fdecl onames)]
               (let [dumb-args (repeatedly (count args) gensym)]
                 `([~@(if var-args (insert-amp dumb-args) dumb-args)]
                   (.invoke ~oname ~@dumb-args)))))))))

