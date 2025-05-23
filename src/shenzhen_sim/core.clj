(ns shenzhen-sim.core
  (:require [clojure.core.match :refer [match]])
  (:require [com.rpl.specter :as sp])
  (:require [clojure.spec.alpha :as s]))

;; TODO(mkcmkc): Implement a main function.
;; (defn -main [args]
;;   (println "Hello, world!"))

(s/check-asserts true)

;; TODO: best spec for vectors?
(s/def ::op.mov #(match [%] [[:mov _ :guard int? _ :guard string?]] true :else false))
(s/def ::op.slp #(match [%] [[:slp _ :guard int?]] true :else false))
(s/def ::op (s/or :mov ::op.mov
                  :slp ::op.slp))
(s/def ::ops (s/coll-of #(s/valid? ::op %) :kind vector? :min-count 1))

(s/def ::dev.proc (s/and map? #(= (:type %) :proc) #(->> % :ops (s/valid? ::ops))))
(s/def ::dev (s/or :proc ::dev.proc))
(s/def ::devs (s/coll-of #(s/valid? ::dev %) :kind vector? :min-count 1))

(s/def ::ch (s/and map? #(-> % :name string?) #(->> % :type (contains? #{:signal}))))
(s/def ::in-chs (s/coll-of ::ch :kind vector?))
(s/def ::out-chs (s/coll-of #(s/valid? ::ch %) :kind vector? :min-count 1))

(s/def ::circ (s/and map?
                     #(->> % :in-chs (s/valid? ::in-chs))
                     #(->> % :out-chs (s/valid? ::out-chs))
                     #(->> % :devs (s/valid? ::devs))))

(def hi-lo-circ
  (s/assert
   ::circ
   {:in-chs
    []
    :out-chs
    [{:name "out" :type :signal}]
    :devs
    [{:type :proc
      :ops
      [[:mov 100 "out"]
       [:slp 50]
       [:mov 0 "out"]
       [:slp 50]]}]}))

;; TODO: set up instructions into a running program
;; (defn init-instructions [implementation]
;;   (sp/transform [:components sp/ALL (sp/pred #(= (:type %) :microcontroller)) :instructions sp/ALL]
;;                 #(match [%]
;;                         [[op & args]] {:op op :args args :attributes #{}}
;;                         :else (throw (ex-info "Instruction structure not recognized" {:instruction %})))
;;                 implementation))

;; (let [implementation (-> hi-lo-implementation
;;                          init-instructions)]
;;   implementation)
