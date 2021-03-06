clj-newrelic
======

[![Build Status](https://travis-ci.org/TheClimateCorporation/clj-newrelic.png?branch=master)](https://travis-ci.org/TheClimateCorporation/clj-newrelic)
[![Dependencies Status](http://jarkeeper.com/TheClimateCorporation/clj-newrelic/status.svg)](http://jarkeeper.com/TheClimateCorporation/clj-newrelic)

This project exports one macro, `com.climate.newrelic.trace/defn-traced`. Use it as a
drop-in replacement for defn if you want calls to a function to show up in
New Relic transaction tracing.

To include in your project, add

[![Clojars Project](http://clojars.org/com.climate/clj-newrelic/latest-version.svg)](http://clojars.org/com.climate/clj-newrelic)

to your lein dependencies.

General strategy adapted from [Sean Corfield](http://corfield.org/blog/post.cfm/instrumenting-clojure-for-new-relic-monitoring).
