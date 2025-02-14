(ns book.demos.pre-merge.post-mutation-countdown
  (:require
    [fulcro.client :as fc]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [book.demos.util :refer [now]]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc InitialAppState initial-state]]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [fulcro.server :as server]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SERVER:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def all-counters
  [{::counter-id 1 ::counter-label "A"}])

(server/defquery-entity ::counter-id
  (value [_ id _]
    (first (filter #(= id (::counter-id %)) all-counters))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; CLIENT:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(m/defmutation initialize-counter [{::keys [counter-id]}]
  (action [{:keys [state]}]
    (swap! state update-in [::counter-id counter-id] #(merge {:ui/count 5} %))))

(defsc Countdown [this {::keys   [counter-label]
                        :ui/keys [count]}]
  {:ident [::counter-id ::counter-id]
   :query [::counter-id ::counter-label :ui/count]}
  (dom/div
    (dom/h4 counter-label)
    (let [done? (zero? count)]
      (dom/button {:disabled done?
                   :onClick  #(m/set-value! this :ui/count (dec count))}
        (if done? "Done!" (str count))))))

(def ui-countdown (comp/factory Countdown {:keyfn ::counter-id}))

(defsc Root [this {:keys [counter]}]
  {:initial-state (fn [_] {})
   :query         [{:counter (comp/get-query Countdown)}]}
  (dom/div
    (dom/h3 "Counters")
    (if (seq counter)
      (ui-countdown counter)
      (dom/button {:onClick #(df/load this [::counter-id 1] Countdown
                               {:target               [:counter]
                                :post-mutation        `initialize-counter
                                :post-mutation-params {::counter-id 1}})}
        "Load one counter"))))

(defn initialize
  "To be used in :started-callback to pre-load things."
  [app])
