package com.thoughtpropulsion.reactrode.recorder;

import static com.thoughtpropulsion.reactrode.model.Patterns.randomList;

import java.util.List;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.GameOfLifeSystem;
import com.thoughtpropulsion.reactrode.model.Patterns;
import org.reactivestreams.Publisher;
import org.springframework.data.gemfire.GemfireTemplate;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class CellTesting {

  private CellTesting() {}


  static void recordCellsAndVerifySerialPut(final GemfireTemplate template, final int n) {

    final List<Boolean> pattern = randomList(CellOperations.coordinateSystem);

    final GameOfLifeSystem gameOfLifeSystem = GameOfLifeSystem.create(
        Flux.fromIterable(
            Patterns.cellsFromBits(pattern, CellOperations.PRIMORDIAL_GENERATION, CellOperations.coordinateSystem)),
        CellOperations.coordinateSystem);

    final Publisher<Cell> cells =
        CellOperations.createSerialPutPublisher(template, CellOperations.coordinateSystem,
            gameOfLifeSystem.getAllGenerations(), n);

    StepVerifier.create(cells)
        .expectNextCount(n)
        .verifyComplete();
  }

  static void recordCellsAndVerifyParallelPut(final GemfireTemplate template,
                                              final int n, final int parallelism) {

    final List<Boolean> pattern = randomList(CellOperations.coordinateSystem);

    final GameOfLifeSystem gameOfLifeSystem = GameOfLifeSystem.create(
        Flux.fromIterable(
            Patterns.cellsFromBits(pattern, CellOperations.PRIMORDIAL_GENERATION, CellOperations.coordinateSystem)),
        CellOperations.coordinateSystem);

    final Publisher<Cell> cells =
        CellOperations.createParallelPutPublisher(template, CellOperations.coordinateSystem,
            gameOfLifeSystem.getAllGenerations(), n, parallelism);

    StepVerifier.create(cells)
        .expectNextCount(n)
        .verifyComplete();
  }

  static void recordCellsAndVerifySerialBulkPut(final GemfireTemplate template, final int generations) {

    final List<Boolean> pattern = randomList(CellOperations.coordinateSystem);

    final GameOfLifeSystem gameOfLifeSystem = GameOfLifeSystem.create(
        Flux.fromIterable(
            Patterns.cellsFromBits(pattern, CellOperations.PRIMORDIAL_GENERATION, CellOperations.coordinateSystem)),
        CellOperations.coordinateSystem);

    final Publisher<List<Cell>> cells =
        CellOperations.createSerialBulkPutPublisher(template, CellOperations.coordinateSystem,
            gameOfLifeSystem.getAllGenerations(), generations);

    StepVerifier.create(cells)
        .expectNextCount(generations)
        .verifyComplete();
  }

  static void recordCellsAndVerifyParallelBulkPut(final GemfireTemplate template,
                                                  final int generations, final int parallelism) {

    final List<Boolean> pattern = randomList(CellOperations.coordinateSystem);

    final GameOfLifeSystem gameOfLifeSystem = GameOfLifeSystem.create(
        Flux.fromIterable(
            Patterns.cellsFromBits(pattern, CellOperations.PRIMORDIAL_GENERATION, CellOperations.coordinateSystem)),
        CellOperations.coordinateSystem);

    final Publisher<List<Cell>> cells =
        CellOperations.createParallelBulkPutPublisher(template, CellOperations.coordinateSystem,
            gameOfLifeSystem.getAllGenerations(), generations, parallelism);

    StepVerifier.create(cells)
        .expectNextCount(generations)
        .verifyComplete();
  }
}
