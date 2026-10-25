# Testing

Part of the `clojurescript` skill. It applies to ClojureScript on JavaScript hosts and overrides the host-neutral `clojure` skill in this domain.

## Testing

`cljs.test` provides the testing macros. Catch JS-typed exceptions in `thrown?` assertions:

```clojure
(ns my-app.core-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [my-app.core :as core]))

(deftest parse-int-test
  (testing "parses decimal strings"
    (is (= 42 (core/parse-int "42"))))
  (testing "throws on garbage"
    (is (thrown? js/Error (core/parse-int! "not-a-number")))))
```

For `.cljc` tests that run on both hosts, use a reader conditional in the require and in the thrown-type:

```clojure
(ns my-app.core-test
  (:require
   #?(:clj  [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer-macros [deftest is testing]])
   [my-app.core :as core]))

(deftest parse-int-test
  (is (thrown? #?(:clj Exception :cljs js/Error) (core/parse-int! "not-a-number"))))
```

`thrown?` uses its type as the catch type. `js/Error` asserts that the code threw an `Error` (including `ex-info`), so a thrown string escapes the assertion and the test reports an error. `(thrown? :default ...)` passes for any thrown value, so use it only when the test accepts a value that is not an `Error`.

`cljs.test` loads its own macros, so `(:require [cljs.test :refer [deftest is testing]])` works as well as the `:refer-macros` form above.

`deftest ^:async` defines a test whose body can `await` Promises. The test runner waits for the returned Promise before moving on:

```clojure
(deftest ^:async fetch-json-test
  (let [result (await (api/fetch-json "/fixtures/user.json"))]
    (is (= "Bruce" (:name result)))))
```

For an API that reports through a callback instead of a Promise, wrap the test body in `cljs.test/async`, referred alongside `deftest` and `is`, and call `done` from the callback:

```clojure
(deftest callback-test
  (async done
    (api/fetch-json-cb "/fixtures/user.json"
                       (fn [result]
                         (is (= "Bruce" (:name result)))
                         (done)))))
```

For test seams, see the `with-redefs` entry under "Vars" in `references/host-semantics.md`.
