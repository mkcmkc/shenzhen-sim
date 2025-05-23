(ns shenzhen-sim.core
  (:require [clojure.core.match :refer [match]])
  (:require [clojure.set :as set])
  (:require [com.rpl.specter :as sp])
  (:require [clojure.spec.alpha :as s])
  (:require [shenzhen-sim.util.spec :as su]))

(s/check-asserts true)

;; TODO: move to util package
(defmacro dbg [expr]
  `(doto ~expr prn))

(s/def ::non-neg-int (s/and int? (complement neg?)))

(s/def ::op (s/or :mov (s/tuple #{:mov} int? string?)
                  :slp (s/tuple #{:slp} ::non-neg-int)))

(defn unannotate-op [op]
  (vec (flatten [(:op op) (:args op)])))

(s/def ::ann-op (s/and #(->> % unannotate-op (s/valid? ::op))
                       (su/map-of {:run-count (su/isa? ::non-neg-int)})))

(s/def ::dev (s/or :proc (su/map-of {:type #{:proc}
                                     :op (su/isa? (su/non-empty-vec (su/isa? ::op)))})))


(s/def ::channel (su/map-of {:name string? :type #{:signal}}))

(defn defined-channels [circuit]
  {:post [(s/valid? (su/non-empty-vec (su/isa? ::channel)) %)]}
  (sp/select [:out-ch sp/ALL] circuit))

(defn annotate-op [op]
  {:pre [(s/valid? ::op op)]
   :post [(s/valid? ::ann-op %)]}
  (let [op-code (first op)
        args (vec (rest op))]
    {:op op-code :args args :run-count 0}))

(defn used-channels? [circuit]
  {:post [(s/valid? (s/coll-of string? :type set?) %)]}
  (->> circuit
       (sp/transform [:dev sp/ALL :op sp/ALL] annotate-op)
       (sp/select [:dev sp/ALL :op sp/ALL #(->> % :op #{:mov}) :args sp/ALL string?])
       (into #{})))

(defn valid-channels? [circuit]
  {:post [(boolean? %)]}
  (let [defined (sp/select [sp/ALL :name] (defined-channels circuit))
        used (used-channels? circuit)]
    (let [defined-uniq (s/assert set? (into #{} defined))]
      (and (== (count defined) (count defined-uniq))
           (set/subset? used defined-uniq)))))

(s/def ::circ (s/and (su/map-of {:out-ch (su/isa? (su/non-empty-vec (su/isa? ::channel)))
                                 :dev (su/isa? (su/non-empty-vec (su/isa? ::dev)))})
                     valid-channels?))

(def hi-lo-circ
  (s/assert
   ::circ
   {:out-ch
    [{:name "out" :type :signal}]
    :dev
    [{:type :proc
      :op
      [[:mov 100 "out"]
       [:slp 50]
       [:mov 0 "out"]
       [:slp 50]]}]}))

;; TODO: HERE: add post condition
(defn init-circ [circ]
  {:pre [(s/valid? ::circ circ)]}
  (->> circ
       (sp/transform [:dev sp/ALL :op sp/ALL] annotate-op)
       (#(assoc % :state {:op-ptr 0}))
       (sp/transform [:dev sp/ALL] #(assoc % :state {:sleep-for nil :signal-out {}}))
       (sp/transform [:out-ch sp/ALL] #(assoc % :values []))))

;; TODO: start running the program
(init-circ hi-lo-circ)

{:out-ch [{:name "out", :type :signal, :values []}],
 :dev [{:type :proc,
        :op [{:op :mov, :args [100 "out"], :run-count 0}
             {:op :slp, :args [50], :run-count 0}
             {:op :mov, :args [0 "out"], :run-count 0}
             {:op :slp, :args [50], :run-count 0}],
        :state {:sleep-for nil, :signal-out {}}}],
 :state {:op-ptr 0}}




;; ;; (defn init-instructions [implementation]
;; ;;   (sp/transform [:components sp/ALL (sp/pred #(= (:type %) :microcontroller)) :instructions sp/ALL]
;; ;;                 #(match [%]
;; ;;                         [[op & args]] {:op op :args args :attributes #{}}
;; ;;                         :else (throw (ex-info "Instruction structure not recognized" {:instruction %})))
;; ;;                 implementation))

;; ;; (let [implementation (-> hi-lo-implementation
;; ;;                          init-instructions)]
;; ;;   implementation)
