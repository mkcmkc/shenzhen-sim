(ns shenzhen-sim.util.spec
  (:require [clojure.spec.alpha :as s]))

(defn non-empty-vec [pred]
  {:pre [(fn? pred)]}
  (s/coll-of pred :kind vector? :min-count 1))

(defn complement [spec]
  (fn [x] (not (s/valid? spec x))))

(defn isa? [spec]
  (fn [x] (s/valid? spec x)))

(defmacro map-of [m]
  (let [preds (map (fn [[k p]] `(fn [x#] (~p (~k x#))))
                   m)]
    `(s/and ~@preds)))
