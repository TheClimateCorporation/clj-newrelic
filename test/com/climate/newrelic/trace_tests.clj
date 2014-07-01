(ns com.climate.newrelic.trace-tests
  (:require [clojure.test :refer :all]
            [clojure.test-helper :as util]
            [com.climate.newrelic.trace :as nr]))

(nr/defn-traced adds "adds some numbers"
  ([] 0)
  ([a] a)
  ([a b] (+ a b))
  ([a b c & ds]
    (apply + a b c ds)))

(deftest test-adds
  (is (= (adds) 0))
  (is (= (adds 1) 1))
  (is (= (adds 1 2) 3))
  (is (= (adds 1 2 3) 6))
  (is (= (adds 1 2 3 4) 10))
  (is (= (adds 1 2 3 4 5) 15)))


(deftest test-reflection
  (util/should-not-reflect
    (com.climate.newrelic.trace/defn-traced get-length
      [^java.util.List l]
      (.size l))))

(nr/defn-traced destructures [[foo bar] {:keys [baz qux]}]
  {:foo foo
   :bar bar
   :baz baz
   :qux qux})

(deftest test-destructure
  (is (= (destructures [1 2] {:baz 3 :qux 4})
         {:foo 1 :bar 2 :baz 3 :qux 4})))
