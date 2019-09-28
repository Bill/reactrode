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


  static void recordCellsAndVerify(final GemfireTemplate template, final int generations) {

    final Publisher<List<Cell>> cells = getCellsPublisher(template, generations);

    StepVerifier.create(cells)
        .expectNextCount(generations)
        .verifyComplete();
  }

  private static Publisher<List<Cell>> getCellsPublisher(final GemfireTemplate template,
                                                         final int generations) {
    final List<Boolean> pattern = randomList(CellOperations.coordinateSystem);

    final GameOfLifeSystem gameOfLifeSystem = GameOfLifeSystem.create(
        Flux.fromIterable(
            Patterns.cellsFromBits(pattern, CellOperations.PRIMORDIAL_GENERATION, CellOperations.coordinateSystem)),
        CellOperations.coordinateSystem);

    return CellOperations.createSerialBulkPutPublisher(template, CellOperations.coordinateSystem,
            gameOfLifeSystem.getAllGenerations(), generations);
  }


}
