package com.thoughtpropulsion.reactrode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

public class GameStateLocal implements GameState {

  private Map<Integer,Boolean> cells = new ConcurrentHashMap<>();

  // key is generation, value is list of sinks for fluxes subscribed for that generation
  private ConcurrentNavigableMap<Integer, List<FluxSink<Cell>>> queries =
      new ConcurrentSkipListMap<>();

  public final int columns; // x
  public final int rows;    // y

  public GameStateLocal(final int columns, final int rows) {
    this.columns = columns; this.rows = rows;
  }

  // TODO: age "old" entries out of cells map
  @Override
  public void put(final Mono<Cell> cellMono) {
    cellMono.subscribe( cell -> {
      cells.put(toOffset(cell.coordinate), cell.isAlive);
      distributeToQueries(cell);
    });
  }

  @Override
  public Mono<Boolean> get(final Mono<Coordinate> coordinateMono) {
    return Mono.create( sink ->
      coordinateMono.subscribe( coordinate -> {
        final Boolean value = cells.get(toOffset(coordinate));
        // get() method contract dictates that we never produce a null Boolean reference
        sink.success(JavaLang.toBooleanNotNull(value));
      }));
  }

  @Override
  public Flux<Cell> changes(final Mono<Integer> generationMono) {
    return Flux.create(sink -> {
      generationMono.subscribe( generation -> {
        queries.computeIfAbsent(generation, (k) -> new ArrayList<>()).add(sink);
      });
    });
  }

  private int toOffset(final Coordinate coordinate) {
    return toOffset(coordinate.generation, coordinate.y, coordinate.x);
  }

  private int toOffset(final int generation, final int x, final int y) {
    return rows * (generation * columns  + y) + x;
  }

  private void distributeToQueries(final Cell cell) {
    queries.tailMap(cell.coordinate.generation, true)
        .forEach((generation, sinks) -> sinks.forEach(sink -> sink.next(cell)));
  }
}
