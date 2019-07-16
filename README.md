This is a [Conway's Game of Life](https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life) simulation implemented in a reactive style using [Project Reactor](https://projectreactor.io/).

## Status

Model is in place. `GameOfLife` works!

## TODO

* fix RSocket client error: "No decoder for java.lang.String"
* enable zero copy frame decoder in client and server
* make browser visualization using rsocket-js + d3
* connect Geode client (recorder) downstream
* start up from generation stored in Geode 
* &hellip; profit!
* enable BlockHound https://github.com/reactor/BlockHound
* side project: figure out how to make `Mono<Cell> put(final Mono<Cell> cellMono)` work analogously to its Flux counterpart

## Questions

1. putting a null reference into a `Flux` (or `Mono`) results in a puzzling "circularity" exception

## Run

`./gradlew -D bootRun `