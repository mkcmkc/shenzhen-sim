(ns shenzhen-sim.core
  (:require [clojure.core.match :refer [match]])
  (:require [com.rpl.specter :as sp])
  (:require [clojure.spec.alpha :as s])
  (:require [shenzhen-sim.util.spec :as su]))

(s/check-asserts true)

(s/def ::non-neg-int (s/and int? (complement neg?)))

(s/def ::op (s/or :mov (s/tuple #{:mov} int? string?)
                  :slp (s/tuple #{:slp} ::non-neg-int)))

(s/def ::dev (s/or :proc (su/map-of {:type #{:proc}
                                     :op (su/isa? (su/non-empty-vec (su/isa? ::op)))})))


(s/def ::channel (su/map-of {:name string? :type #{:signal}}))

(s/def ::circ (su/map-of {:out-ch (su/isa? (su/non-empty-vec (su/isa? ::channel)))
                          :dev (su/isa? (su/non-empty-vec (su/isa? ::dev)))}))

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

;; ;; TODO: set up instructions into a running program
;; ;; (defn init-instructions [implementation]
;; ;;   (sp/transform [:components sp/ALL (sp/pred #(= (:type %) :microcontroller)) :instructions sp/ALL]
;; ;;                 #(match [%]
;; ;;                         [[op & args]] {:op op :args args :attributes #{}}
;; ;;                         :else (throw (ex-info "Instruction structure not recognized" {:instruction %})))
;; ;;                 implementation))

;; ;; (let [implementation (-> hi-lo-implementation
;; ;;                          init-instructions)]
;; ;;   implementation)
