This is a [Conway's Game of Life](https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life) simulation implemented in a reactive style using [Project Reactor](https://projectreactor.io/). I developed this code for my 2019 Apache Geode Summit Talk: [_Reactive Event Processing with Apache Geode_](https://www.youtube.com/watch?v=KX9GgZK8744).

The goal of the talk was to explore how Apache Geode fit into a reactive system. In the talk, and in this repository, you'll see how Geode fits into a distributed reactive system as an event producer, and as an event consumer. [RSocket](https://rsocket.io/) is used for network communication, and there's even a fun little browser-based visualizer based on [Luca Sbardella's Gist](https://gist.github.com/lsbardel/c0edf5f88ca5af118599844b7d2dcdf8).

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

To run just the reactive toy app: game server -> browser:

```
./gradlew :gameserver:bootRun
```

then, in another terminal:

```
cd react-app
npm start
```

After starting these in IntelliJ:

GeodeServerApplication `-Xmx1g -Xms1g -Xss144k`
GameServerApplication
RecorderApplication

use gfsh:

```
connect --locator=localhost[10334]
describe region --name=Cells
```
