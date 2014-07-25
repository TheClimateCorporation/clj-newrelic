(ns com.climate.newrelic.trace)

(defn- traced-meta [fname]
  {'com.newrelic.api.agent.Trace {:metricName (str *ns* \. fname)}})

(defn- make-traced [tname fname arg-list body]
  (let [i-args (for [_ arg-list] (gensym))]
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
  (some #{'&} arg-list))

(defn- insert-amp [arg-list]
  (concat (drop-last arg-list) ['& (last arg-list)]))

(defn- remove-amp [arg-list]
  (remove #{'&} arg-list))

(defn- preproc-decl [fname fdecl]
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
                       :oname (gensym)
                       :tname (gensym fname)
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
  (let [[m ann-fdecl] (preproc-decl fname fdecl)]
    `(do
       (def ~fname)
       ~@(for [{:keys [args body tname]}  ann-fdecl]
           (make-traced tname fname args body))
       (let [~@(apply concat (for [{:keys [tname oname]} ann-fdecl]
                               `(~oname (new ~tname))))]
         (defn
           ~fname ~m
           ~@(for [{:keys [var-args args oname]} ann-fdecl]
               (let [dumb-args (for [_ args] (gensym))]
                 `([~@(if var-args (insert-amp dumb-args) dumb-args)]
                   (.invoke ~oname ~@dumb-args)))))))))
