This is a [Conway's Game of Life](https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life) simulation implemented in a reactive style using [Project Reactor](https://projectreactor.io/).

## Status

Gameserver produces `allGenerations` flux which can be consumed by:

* browser-based, JavaScript react-app
* Java-based testclient app
* Java-based recorder app, which feeds geodeserver Geode server

## Build and Run

Works with OpenJDK 12 from Oracle and Adopt OpenJDK 12. 

## TODO

* enable BlockHound https://github.com/reactor/BlockHound
* upgrade RSocket to 1.0.0-RC2 or newer
* side project: figure out how to make `Mono<Cell> put(final Mono<Cell> cellMono)` work analogously to its Flux counterpart
* remove dependency on org.springframework.data:spring-data-commons in model module (necessary to make spring-data-geode serialization work)

## Questions

1. putting a null reference into a `Flux` (or `Mono`) results in a puzzling "circularity" exception
2. why won't RSocket for Spring Messaging support e.g. `Flux<String>` return type or a `String` arg to a message mapping&mdash;primitive types aren't supported.
3. why must I my RSocket client app include a dependency on webflux to avoid crashing the RSocket server?

## Run

After starting these in IntelliJ:

GeodeServerApplication `-Xmx1g -Xms1g -Xss144k`
GameServerApplication
RecorderApplication

use gfsh:

```
connect --locator=localhost[10334]
describe region --name=Cells
```