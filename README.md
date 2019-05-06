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