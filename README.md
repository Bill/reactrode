This is a [Conway's Game of Life](https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life) simulation implemented in a reactive style using [Project Reactor](https://projectreactor.io/).

## Status

Just getting started here. See the tests.

## TODO

* fix `GameOfLifeFramingTest`: `GamStateWithBackpressure.put()` needs to advance window
* delete `GameStateColdChanges`&mdash;it's superseded by `GameStateWithBackpressure`
* make `GameStateColdChanges.changes()` produce a cold flux so no buffering is needed
* make a visualization: maybe an RSocket client for JavaScript or something
* enable BlockHound https://github.com/reactor/BlockHound
* implement `GameState` with Apache Geode
* &hellip; profit!
* Deterministic testing: hook the Reactor scheduler to control choices, dial down to a single thread, eliminate Thread.sleep()s
* side project: figure out how to make `Mono<Cell> put(final Mono<Cell> cellMono)` work analogously to its Flux counterpart

## Questions

1. how can I create a `Flux` that delays providing items&mdash;seems like I have to write to the captured `sink` from my own thread(s) since I can't get the subscribing thread (I think I want a mutable flux that's a subscriber and lets me swap out the publisher)
2. &hellip;tried with `GameStateColdChanges.feedQuery()` but concerned about `sink.next()` call
3. when I run all my tests, one (`processFluxWithCustomBackpressure()`) fails to terminate. when I run them individually they all terminate&mdash;do I have to dispose?
4. debugging works sometimes but other times I fail to see the summarized stack traces
5. putting a null reference into a `Flux` (or `Mono`) results in a puzzling "circularity" exception see `GameStateHotChanges.get()`
