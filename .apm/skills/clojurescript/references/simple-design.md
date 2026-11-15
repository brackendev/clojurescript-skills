# Simple Design in ClojureScript

Part of the `clojurescript` skill. It applies to ClojureScript on JavaScript hosts and gives the host constructs for Key Rule 20 of the host-neutral `clojure` skill. Read `clojure/references/simple-design.md` in the `clojure` skill first. Its rules and its exceptions apply here unchanged. This file adds what that reference leaves to the host: state across callbacks, queues between components, and protocols extended to JavaScript types.

## State: one atom read per callback, and values that arrive later

Atoms are the only built-in reference type (`references/host-semantics.md`). The baseline rule holds: dereference once at the edge and pass the value inward.

- JavaScript runs one callback at a time, so a value read from an atom stays current until the callback returns or reaches an `await`. After an `await`, the earlier read is a snapshot of the past. Make the update in one `swap!` that reads the current value (`references/async-and-errors.md`).
- A Promise becomes a value once. `await` it in an `^:async` function at the edge, and pass the result to pure functions. A function that takes a Promise also takes on its waiting and its rejection.

## Queues between components

The baseline says to put a queue between a producer and a consumer that should not know each other, or that run at different rates or times. For work that completes in one callback, keep the direct call.

| Construct | Use it when |
|-----------|-------------|
| A `cljs.core.async` channel | Events or work items pass from a producer to a consumer across callbacks, with a bounded buffer or a pipeline of steps |
| `#queue []` (`cljs.core/PersistentQueue`) | The queue is part of the state. It is an immutable first-in, first-out collection, a value and not a way to pass work. Keep it in an atom. |
| A Promise | One result arrives later. It is not a queue. Use `^:async` and `await` for it (`references/async-and-errors.md`). |

- ClojureScript has no blocking channel operations: `<!!`, `>!!`, and `thread` do not exist. Take and put with `<!` and `>!` inside `go`, or with `take!` and `put!` from a callback.
- `put!` does not wait for room. When the buffer is full, the channel holds the put as pending, and core.async throws after 1024 pending puts on one channel. When the producer can be faster than the consumer, use `(chan (async/dropping-buffer n))` to discard new items or `sliding-buffer` to discard the oldest ones, or put with `>!` inside `go`, which parks until there is room.
- Put plain data on the channel, such as maps with namespaced keys, and never `nil`, which a channel rejects.
- Mark the end with `close!`. Takes return `nil` after the buffered items are taken.
- Pass the channel to each side as an argument, so each side knows only the channel and the shape of the data on it.

```clojure
(ns my-app.indexing
  (:require [cljs.core.async :as async :refer [<! chan go put!]]))

;; good: an event handler knows only the channel and the shape of the message
(defn import-record! [index-chan record]
  (put! index-chan {:index/op :add :index/record record}))

;; good: the consumer takes until the channel is closed and its buffer is empty
(defn run-indexer! [index-chan index!]
  (go
    (loop []
      (when-some [message (<! index-chan)]
        (index! (:index/record message))
        (recur)))))

(def index-chan (chan (async/sliding-buffer 100)))
```

## Extend protocols to JavaScript types

`extend-protocol` connects a protocol to a JavaScript type that the program does not own, so no wrapper type is needed. The built-in JavaScript types have lowercase names of their own: `string`, `number`, `boolean`, `array`, `function`, `object`, and `nil`. A constructor is named through `js`, as in `js/Date`.

```clojure
(defprotocol Summary
  (summarize [x]))

(extend-protocol Summary
  string
  (summarize [s] (subs s 0 (min 20 (count s))))
  array
  (summarize [a] (str (alength a) " items"))
  js/Date
  (summarize [d] (.toISOString d))
  nil
  (summarize [_] "nothing"))
```

- Extend to `nil` to give absence an explicit answer.
- Avoid the catch-all names `object` and `default` unless one answer is correct for every value. `object` matches more than plain JavaScript objects. In a test with ClojureScript 1.12.145, a keyword and a map with no implementation of their own both used the `object` implementation. Without a catch-all, an unsupported value throws "No protocol method Summary.summarize defined for type", which reports the gap.
- Use `reify` for a single implementation that needs no named type.
- A protocol declared with `:extend-via-metadata true` also accepts implementations in a value's metadata, so a plain map can carry one.
