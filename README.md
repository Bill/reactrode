This is a [Conway's Game of Life](https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life) simulation implemented in a reactive style using [Project Reactor](https://projectreactor.io/).

## Status

Model is in place. `GameOfLife` works!

## TODO

* ~make server serve websocket (not tcp)~
* make server listen on a well-known port (not an ephemeral one!)
* make browser visualization using rsocket-js + d3
* connect Geode client (recorder) downstream
* start up from generation stored in Geode 
* upgrade to latest Spring Boot (2.2.0.M5 as of 8/6/2019)
* &hellip; profit!
* enable BlockHound https://github.com/reactor/BlockHound
* side project: figure out how to make `Mono<Cell> put(final Mono<Cell> cellMono)` work analogously to its Flux counterpart

## Questions

1. putting a null reference into a `Flux` (or `Mono`) results in a puzzling "circularity" exception
2. why won't RSocket for Spring Messaging support e.g. `Flux<String>` return type or a `String` arg to a message mapping&mdash;primitive types aren't supported.
3. why must I my RSocket client app include a dependency on webflux to avoid crashing the RSocket server?

## Run

`./gradlew -D bootRun `