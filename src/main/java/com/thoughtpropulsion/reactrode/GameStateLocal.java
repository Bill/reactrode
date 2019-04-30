package com.thoughtpropulsion.reactrode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

public class GameStateLocal implements GameState {

  // cells keyed on offset (see toOffset())
  private Map<Integer,Boolean> cells = new ConcurrentHashMap<>();

  // key is generation, value is list of sinks for fluxes subscribed for that generation
  private Map<Integer, List<FluxSink<Cell>>> queries = new ConcurrentHashMap<>();

  public final int columns; // x
  public final int rows;    // y

  public GameStateLocal(final int columns, final int rows) {
    this.columns = columns; this.rows = rows;
  }

  @Override
  public void put(final Mono<Cell> cellMono) {
    // TODO: age "old" entries out of cells map
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
        getQueriesForGeneration(generation).add(sink);
      });
    });
  }

  private int toOffset(final Coordinate coordinate) {
    return toOffset(coordinate.generation, coordinate.y, coordinate.x);
  }

  private int toOffset(final int generation, final int x, final int y) {
    return columns * (generation * rows  + y) + x;
  }

  private void distributeToQueries(final Cell cell) {
    final List<FluxSink<Cell>> queriesForGeneration = getQueriesForGeneration(cell.coordinate.generation);
    queriesForGeneration.forEach(sink -> sink.next(cell));
  }

  /**
   * Get the (possibly empty) list of query {@code List<FluxSink<Cell>>} for the {@param generation}
   *
   * @param generation is the generation of interest
   * @return not null {@code List<FluxSink<Cell>>} for the generation
   */
  private List<FluxSink<Cell>> getQueriesForGeneration(final Integer generation) {
    return queries.computeIfAbsent(generation, (k) -> new ArrayList<>());
  }

}
