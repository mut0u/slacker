# slacker

**"Superman is a slacker."**

slacker is a simple RPC framework for Clojure based on
[aleph](https://github.com/ztellman/aleph) and
[carbonite](https://github.com/sunng87/carbonite/). I forked carbonite
because slacker requires it to work on clojure 1.2.

slacker is growing.

### RPC vs Remote Eval

Before slacker, the clojure world uses a *remote eval* approach for
remoting ([nREPL](https://github.com/clojure/tools.nrepl),
[portal](https://github.com/flatland/portal)).  Comparing to remote
eval, RPC (especially slacker) has some pros and cons:

#### pros

* slacker uses direct function call, which is much faster than eval
  (about *100x*)
* with slacker, only selected functions are exposed, instead of the
  whole java environment when using eval. So it's much securer and
  generally you don't need a sandbox (like clojail) for slacker.

#### cons

* Eval approach provides full features of clojure, you can use
  high-order functions and lazy arguments. Due to the limitation of
  serialization, slacker has its difficulty to support these features.

## Usage

### Leiningen

    :dependencies [[info.sunng/slacker "0.2.0-SNAPSHOT"]]

### Getting Started

Slacker will expose all your public functions under a given
namespace. 

``` clojure
(ns slapi)
(defn timestamp []
  (System/currentTimeMillis))

;; ...more functions
```             

To expose `slapi`, use:

``` clojure
(use 'slacker.server)
(start-slacker-server (the-ns 'slapi) 2104)
```

On the client side, define a facade for the remote function:

``` clojure
(use 'slacker.client)
(def sc (slackerc "localhost" 2104))
(defremote sc timestamp)
(timestamp)
```

### Client Connection Pool

Slacker also supports connection pool in client API, which enables
high concurrent communication. 

To create a connection pool, use `slackerc-pool` instead of `slackerc`.

### Options in defremote

You are specify the remote function name when the name is occupied in
current namespace

``` clojure
(defremote sc remote-time
  :remote-name "timestamp")
```

If you add an `:async` flag to `defremote`, then the facade will be
asynchronous which returns a *promise* when you call it. You should
deref it by yourself to get the return value.

``` clojure
(defremote timestamp :async true)
@(timestamp)
```

You can also assign a callback for an async facade.

``` clojure
(defremote timestamp :callback #(println %))
(timestamp)
```

### Serializing custom types

By default, most clojure data types are registered in carbonite. (As
kryo requires you to **register** a class before you can serialize
its instances.) However, you may have additional types to
transport between client and server. To add your own types, you should
register custom serializers on *both server side and client side*. Run
this before you start server or client:

``` clojure
(use '[slacker.serialization])
(register-serializers some-serializers)
```
[Carbonite](https://github.com/revelytix/carbonite "carbonite") has
some detailed docs on how to create your own serializers.

## Performance

Some performance tests was executed while I'm developing
slacker. Currently, I have some rough results:

* Tested on HP DL360 (dual 6 core X5650, 2.66GHz): A single slacker
server has **10000+** TPS with cpu usage only about 35%.

Some formal performance benchmarks are coming soon.

## License

Copyright (C) 2011 Sun Ning

Distributed under the Eclipse Public License, the same as Clojure.
