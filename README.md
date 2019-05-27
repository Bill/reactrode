This is a [Conway's Game of Life](https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life) simulation implemented in a reactive style using [Project Reactor](https://projectreactor.io/).

## Status

Just getting started here. See the tests.

## TODO

* ~~backpressure on `GameStateHotChanges.putAll(Flux)` and `GameStateHotChanges.put(Mono)`~~
* ~~`GameStateHotChanges.changes()` ages out old cells~~
* make `GameStateColdChanges.changes()` produce a cold flux so no buffering is needed
* make a visualization: maybe an RSocket client for JavaScript or something
* enable BlockHound https://github.com/reactor/BlockHound
* implement `GameState` with Apache Geode
* &hellip; profit!
* compose-in a `CoordinateSystem` class to encapsulate dimensions (columns, rows) and wrap computations that need them

## Questions

1. how can I create a `Flux` that delays providing items&mdash;seems like I have to write to the captured `sink` from my own thread(s) since I can't get the subscribing thread (I think I want a mutable flux that's a subscriber and lets me swap out the publisher)
2. &hellip;tried with `GameStateColdChanges.feedQuery()` but concerned about `sink.next()` call
3. when I run all my tests, one (`processFluxWithCustomBackpressure()`) fails to terminate. when I run them individually they all terminate&mdash;do I have to dispose?
4. debugging works sometimes but other times I fail to see the summarized stack traces
5. putting a null reference into a `Flux` (or `Mono`) results in a puzzling "circularity" exception see `GameStateHotChanges.get()`
