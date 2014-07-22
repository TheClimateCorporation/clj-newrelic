clj-newrelic
======

[![Build Status](https://travis-ci.org/TheClimateCorporation/clj-newrelic.png?branch=master)](https://travis-ci.org/TheClimateCorporation/clj-newrelic)
[![Dependencies Status](http://jarkeeper.com/TheClimateCorporation/clj-newrelic/status.png)](http://jarkeeper.com/TheClimateCorporation/clj-newrelic)

This project exports one macro, `com.climate.newrelic.trace/defn-traced`. Use it as a
drop-in replacement for defn if you want calls to a function to show up in
New Relic transaction tracing.

General strategy adapted from [Sean Corfield](http://corfield.org/blog/post.cfm/instrumenting-clojure-for-new-relic-monitoring).
