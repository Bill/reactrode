package com.thoughtpropulsion.reactrode.recorder;

import static com.thoughtpropulsion.reactrode.model.Patterns.randomList;

import java.util.List;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.GameOfLifeSystem;
import com.thoughtpropulsion.reactrode.model.Patterns;
import com.thoughtpropulsion.reactrode.recorder.gemfireTemplate.CellGemfireTemplate;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class CellTesting {

  private CellTesting() {}


  static void recordCellsAndVerify(final CellGemfireTemplate cellsTemplate, final int generations) {

    final List<Boolean> pattern = randomList(CellOperations.coordinateSystem);

    final GameOfLifeSystem gameOfLifeSystem = GameOfLifeSystem.create(
        Flux.fromIterable(
            Patterns.cellsFromBits(pattern, CellOperations.PRIMORDIAL_GENERATION, CellOperations.coordinateSystem)),
        CellOperations.coordinateSystem);

    final Publisher<List<Cell>> cells = Flux.from(
        CellOperations.createSerialBulkPutPublisher(cellsTemplate, CellOperations.coordinateSystem,
            gameOfLifeSystem.getAllGenerations(), generations));

    StepVerifier.create(cells)
        .expectNextCount(generations)
        .verifyComplete();
  }
}
