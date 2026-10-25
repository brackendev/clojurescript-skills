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

`cljs.test` loads its own macros, so `(:require [cljs.test :refer [deftest is testing]])` works as well as the `:refer-macros` form above.

Since 1.12.145, `deftest ^:async` defines a test whose body can `await` Promises. The test runner waits for the returned Promise before moving on:

```clojure
(deftest ^:async fetch-json-test
  (let [result (await (api/fetch-json "/fixtures/user.json"))]
    (is (= "Bruce" (:name result)))))
```

On older compilers, use `cljs.test/async` with a `done` callback.

For test seams, see the `with-redefs` entry under "Vars" in `references/host-semantics.md`.
