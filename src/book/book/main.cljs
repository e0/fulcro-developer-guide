(ns book.main
  (:require
    [com.fulcrologic.fulcro.networking.mock-server-remote :refer [mock-http-server]]
    [book.macros :refer [defexample]]
    [book.ui.d3-example :as d3-example]
    [book.ui.focus-example :as focus-example]
    [book.ui.hover-example :as hover-example]
    [book.ui.victory-example :as victory-example]
    [book.queries.union-example-1 :as union-example-1]
    book.queries.dynamic-queries
    book.queries.dynamic-query-parameters
    book.queries.recursive-demo-1
    book.queries.recursive-demo-2
    book.queries.recursive-demo-3
    book.queries.recursive-demo-bullets
    book.forms.form-state-demo-1
    book.forms.form-state-demo-2
    [book.demos.autocomplete :as autocomplete]
    book.ui-routing
    book.simple-router-1
    book.simple-router-2
    book.tree-to-db
    book.merge-component
    book.html-converter
    book.server.morphing-example
    book.demos.cascading-dropdowns
    book.demos.declarative-mutation-refresh
    book.demos.dynamic-ui-routing
    book.demos.initial-app-state
    book.demos.loading-data-basics
    book.demos.loading-data-targeting-entities
    ;book.demos.loading-in-response-to-UI-routing
    ;book.demos.loading-indicators
    ;book.demos.paginating-large-lists-from-server
    ;book.demos.parallel-vs-sequential-loading
    book.demos.parent-child-ownership-relations
    ;book.demos.pre-merge.post-mutation-countdown
    ;book.demos.pre-merge.post-mutation-countdown-many
    ;book.demos.pre-merge.countdown
    ;book.demos.pre-merge.countdown-many
    ;book.demos.pre-merge.countdown-with-initial
    ;book.demos.pre-merge.countdown-initial-state
    ;book.demos.pre-merge.countdown-extracted
    ;book.demos.pre-merge.countdown-mutation
    ;book.demos.server-error-handling
    ;book.demos.server-query-security
    ;book.demos.server-return-values-as-data-driven-mutation-joins
    ;book.demos.server-targeting-return-values-into-app-state
    ;book.demos.server-return-values-manually-merging
    [book.server.ui-blocking-example :as ui-blocking]

    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro-css.css-injection :as css]
    [com.wsscode.pathom.connect :as pc]
    [com.wsscode.pathom.core :as p]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    [book.example-1 :as ex1]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.application :as app]
    [taoensso.encore :as encore]))

(defonce latency (atom 100))

(def my-resolvers [autocomplete/list-resolver
                   book.demos.declarative-mutation-refresh/resolvers
                   book.forms.form-state-demo-2/resolvers
                   book.demos.loading-data-basics/resolvers
                   #_book.demos.cascading-dropdowns/model-resolver])

(def parser
  (p/parallel-parser
    {::p/env     {::p/reader [p/map-reader
                              pc/parallel-reader
                              pc/open-ident-reader
                              p/env-placeholder-reader]}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register my-resolvers})
                  (p/post-process-parser-plugin p/elide-not-found)
                  p/error-handler-plugin]}))

(defmutation set-server-latency [{:keys [delay]}]
  (action [{:keys [app state]}]
    (js/console.log "Latency set to" delay)
    (reset! latency delay)
    (swap! state assoc-in [:server-control/by-id :server :server-control/delay] delay)))

(defsc ServerControl [this {:keys [:server-control/delay ui/hidden?]}]
  {:query         [:server-control/delay :ui/hidden?]
   :initial-state {:ui/hidden? true}
   :ident         (fn [] [:server-control/by-id :server])}
  (dom/div (clj->js {:style {:position        :fixed
                             :width           "180px"
                             :height          "130px"
                             :fontSize        "10px"
                             :backgroundColor :white
                             :zIndex          60000
                             :opacity         1.0
                             :padding         "3px"
                             :border          "3px groove white"
                             :top             0
                             :right           (if hidden? "-179px" "-1px")}})
    (dom/div nil "Latency: " (dom/span nil delay))
    (dom/br nil)
    (dom/button #js {:disabled (> delay 2000) :onClick #(comp/transact! this `[(set-server-latency {:delay ~(+ delay 500)})])} "Slower")
    (dom/button #js {:disabled (< delay 500) :onClick #(comp/transact! this `[(set-server-latency {:delay ~(- delay 500)})])} "Faster")
    (dom/div #js {:onClick #(m/toggle! this :ui/hidden?)
                  :style   #js {:color           "grey"
                                :backgroundColor "lightgray"
                                :padding         "5px"
                                :paddingLeft     "10px"
                                :fontSize        "14px"
                                :position        :relative
                                :opacity         1.0
                                :transform       "rotate(-90deg) translate(12px, -100px)"}}
      "Server Controls")))

(def ui-server-control (comp/factory ServerControl))

(defsc ServerControlRoot [this {:keys [ui/react-key server-control]}]
  {:query         [:ui/react-key {:server-control (comp/get-query ServerControl)}]
   :initial-state {:server-control {}}}
  (dom/div #js {:key react-key}
    (ui-server-control server-control)))

(defonce example-server
  (let [transmit! (:transmit! (mock-http-server {:parser (fn [req] (parser {} req))}))]
    {:remote {:transmit! (fn [this send-node]
                           (js/setTimeout
                             #(transmit! this send-node)
                             @latency))}}))

(css/upsert-css "example-css" {:component     book.macros/ExampleRoot
                               :auto-include? false})
(defexample "Sample Example" ex1/Root "example-1")
(defexample "D3" d3-example/Root "ui-d3")
(defexample "Input Focus and React Refs/Lifecycle" focus-example/Root "focus-example")
(defexample "Drawing in a Canvas" hover-example/Root "hover-example")
(defexample "Using External React Libraries" victory-example/Root "victory-example")
(defexample "Unions to Select Type" union-example-1/Root "union-example-1")
(defexample "UI Blocking" ui-blocking/Root "ui-blocking-example" :remotes book.main/example-server)
;
;;; Dynamic queries
(defexample "Dynamic Query" book.queries.dynamic-queries/Root "dynamic-queries")
(defexample "Dyanmic Query Parameters" book.queries.dynamic-query-parameters/Root "dynamic-query-parameters")
;
(defexample "Routing Demo" book.ui-routing/Root "ui-routing" :remotes book.main/example-server)
(defexample "Simple Router" book.simple-router-1/Root "simple-router-1")
(defexample "Nested Router" book.simple-router-2/Root "simple-router-2")
(defexample "Tree to DB with Queries" book.tree-to-db/Root "tree-to-db" :remotes book.main/example-server)
(defexample "Merging with a Component" book.merge-component/Root "merge-component" :remotes book.main/example-server)
(defexample "HTML Converter" book.html-converter/Root "html-converter")
;
;;; Forms
(defexample "Editing Existing Data" book.forms.form-state-demo-1/Root "form-state-demo-1" :remotes book.main/example-server)
(defexample "Network Interactions and Forms" book.forms.form-state-demo-2/Root "form-state-demo-2" :remotes book.main/example-server)

(defexample "Autocomplete" autocomplete/AutocompleteRoot "autocomplete-demo" :remotes book.main/example-server)
;(defexample "Cascading Dropdowns" book.demos.cascading-dropdowns/Root "cascading-dropdowns" :remotes book.main/example-server)
(defexample "Dynamic UI Routing" book.demos.dynamic-ui-routing/Root "dynamic-ui-routing"
  :client-did-mount book.demos.dynamic-ui-routing/application-loaded
  :remotes book.main/example-server)
(defexample "Recursive Demo 1" book.queries.recursive-demo-1/Root "recursive-demo-1")
(defexample "Recursive Demo 2" book.queries.recursive-demo-2/Root "recursive-demo-2")
(defexample "Recursive Demo 3" book.queries.recursive-demo-3/Root "recursive-demo-3")
(defexample "Recursive Demo 4" book.queries.recursive-demo-bullets/Root "recursive-demo-bullets")
(defexample "Declarative Mutation Refresh" book.demos.declarative-mutation-refresh/Root "declarative-mutation-refresh" :remotes book.main/example-server)
;
;#?(:cljs (defexample "Loading Data Basics" book.demos.loading-data-basics/Root "loading-data-basics" :remotes book.main/example-server :started-callback book.demos.loading-data-basics/initialize))
;#?(:cljs (defexample "Loading Data and Targeting Entities" book.demos.loading-data-targeting-entities/Root "loading-data-targeting-entities" :remotes book.main/example-server))
;#?(:cljs (defexample "Loading In Response To UI Routing" book.demos.loading-in-response-to-UI-routing/Root "loading-in-response-to-UI-routing" :remotes book.main/example-server))
;#?(:cljs (defexample "Loading Indicators" book.demos.loading-indicators/Root "loading-indicators" :remotes book.main/example-server))
(defexample "Initial State" book.demos.initial-app-state/Root "initial-app-state" :remotes book.main/example-server)
;#?(:cljs (defexample "Legacy Load Indicators" book.demos.legacy-load-indicators/Root "legacy-load-indicators" :remotes book.main/example-server))
;#?(:cljs (defexample "Paginating Lists From Server" book.demos.paginating-large-lists-from-server/Root "paginating-large-lists-from-server"
;           :started-callback book.demos.paginating-large-lists-from-server/initialize
;           :remotes book.main/example-server))
;
;#?(:cljs (defexample "Parallel vs. Sequential Loading" book.demos.parallel-vs-sequential-loading/Root "parallel-vs-sequential-loading" :remotes book.main/example-server))
(defexample "Parent-Child Ownership" book.demos.parent-child-ownership-relations/Root "parent-child-ownership-relations" :remotes book.main/example-server)

;
;#?(:cljs (defexample "Pre merge - using post mutations" book.demos.pre-merge.post-mutation-countdown/Root "pre-merge-postmutations" :remotes book.main/example-server))
;#?(:cljs (defexample "Pre merge - using post mutations to many" book.demos.pre-merge.post-mutation-countdown-many/Root "pre-merge-postmutations-many" :remotes book.main/example-server))
;#?(:cljs (defexample "Pre merge" book.demos.pre-merge.countdown/Root "postmutations-single" :remotes book.main/example-server))
;#?(:cljs (defexample "Pre merge - to many" book.demos.pre-merge.countdown-many/Root "postmutations-many" :remotes book.main/example-server))
;#?(:cljs (defexample "Pre merge - with initial" book.demos.pre-merge.countdown-with-initial/Root "postmutations-with-initial" :remotes book.main/example-server))
;#?(:cljs (defexample "Pre merge - extracted ui" book.demos.pre-merge.countdown-extracted/Root "postmutations-extracted" :remotes book.main/example-server))
;#?(:cljs (defexample "Pre merge - initial state" book.demos.pre-merge.countdown-initial-state/Root "postmutations-initial-state" :remotes book.main/example-server))
;#?(:cljs (defexample "Pre merge - mutation" book.demos.pre-merge.countdown-mutation/Root "postmutations-mutation" :remotes book.main/example-server))
;
;#?(:cljs (defexample "Error Handling" book.demos.server-error-handling/Root "server-error-handling"
;           :remotes book.main/example-server))
;#?(:cljs (defexample "Query Security" book.demos.server-query-security/Root "server-query-security"
;           :remotes book.main/example-server))
;#?(:cljs (defexample "Return Values and Mutation Joins" book.demos.server-return-values-as-data-driven-mutation-joins/Root "server-return-values-as-data-driven-mutation-joins"
;
;           :remotes book.main/example-server))
;#?(:cljs (defexample "Manually Merging Server Mutation Return Values" book.demos.server-return-values-manually-merging/Root "server-return-values-manually-merging"
;           :mutation-merge book.demos.server-return-values-manually-merging/merge-return-value
;           :remotes book.main/example-server))
;#?(:cljs (defexample "Targeting Mutation Return Values" book.demos.server-targeting-return-values-into-app-state/Root "server-targeting-return-values-into-app-state" :remotes book.main/example-server))

(defonce server-control-app (app/fulcro-app
                              {:client-did-mount (fn [app]
                                                   (comp/transact! app [(set-server-latency {:delay 100})]))}))

(defn ^:export init []
  (js/console.log "Init")
  (app/mount! server-control-app ServerControlRoot "server-controls"))

(defn ^:export focus [app-id]
  (encore/when-let [app        (get @book.macros/app-registry app-id)
                    state-map  (app/current-state app)
                    inspect-id (get state-map :fulcro.inspect.core/app-uuid)]
    (com.fulcrologic.fulcro.inspect.preload/set-active-app inspect-id)))
