package com.thoughtpropulsion.reactrode.model;

import static com.thoughtpropulsion.reactrode.model.Functional.returning;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

public class GameOfLife {

  final CoordinateSystem coordinateSystem;
  private final Publisher<Cell> allGenerations;

  public GameOfLife(
      final CoordinateSystem coordinateSystem,
      final Publisher<Cell> primordialGenerationPublisher) {

    this.coordinateSystem = coordinateSystem;

    final Flux<Cell> futureGenerations =
        Flux.from(primordialGenerationPublisher)
            .buffer(coordinateSystem.size())
            // this flatMap converts a single (primordial) generation to many (future) ones
            .flatMap(primordialGeneration ->
                Flux.generate(
                    () -> primordialGeneration,
                    (List<Cell> oldGeneration, SynchronousSink<List<Cell>> sink) ->
                        returning(
                            nextGenerationList(oldGeneration),
                            newGeneration -> sink.next(newGeneration))))
            // this flatMap expands each generation to its constituent cells
            .flatMap(generation -> Flux.fromIterable(generation));

    allGenerations = Flux.concat(primordialGenerationPublisher,futureGenerations);
  }

  public Publisher<Cell> getAllGenerations() {
    return allGenerations;
  }

  private List<Cell> nextGenerationList(
      final List<Cell> oldGeneration) {
    return nextGenerationStream(oldGeneration).collect(Collectors.toList());
  }

  private Stream<Cell> nextGenerationStream(
      final Collection<Cell> previousGeneration) {

    final int generationSize = coordinateSystem.size();
    final Cell[] board = new Cell[generationSize];
    int cellCount = 0;

    for (final Cell cell : previousGeneration) {
      board[getBoardOffset(cell.coordinates)] = cell;
      cellCount++;
    }

    // Flux.buffer() can produce a partial buffer so we have to check the count
    if (cellCount < generationSize)
      return Stream.empty();
    else {
      // defensive coding here: ensure we filled the board
      for (final Cell cell : board)
        assert null != cell;

      return previousGeneration
          .stream()
          .map(cell -> nextGenerationCell(cell.coordinates, board));
    }
  }

  private Cell nextGenerationCell(
      final Coordinates coordinates,
      final Cell[] board) {
    return Cell.create(
        coordinateSystem.timeShifted(coordinates,coordinates.generation + 1),
        isAlive(coordinates,board));
  }

  /**
   * Calculate liveness (in next generation) for one cell.
   *
   * @param c is the cell's coordinates
   * @return true iff cell should be alive in next generation
   */
  private Boolean isAlive(
      final Coordinates c,
      final Cell[] board) {
    
    final CoordinateSystem cs = this.coordinateSystem;
    
    final Integer liveNeighborsCount = Stream.of(
        cs.nw(c),
        cs.n(c),
        cs.ne(c),
        cs.w(c),
        cs.e(c),
        cs.sw(c),
        cs.s(c),
        cs.se(c))
        .map(coordinate -> wasAliveCount(coordinate, board))
        .reduce(0, Integer::sum);

    final boolean wasAlive = wasAlive(c, board);

    if (wasAlive) {
      if (liveNeighborsCount < 2) {
        return false; // underpopulation
      } else if (liveNeighborsCount > 3) {
        return false; // overpopulation
      } else {
        return true;  // survival
      }
    } else {
      if (liveNeighborsCount == 3) {
        return true;  // reproduction
      } else {
        return false; // status quo
      }
    }
  }

  private int wasAliveCount(final Coordinates coordinates, final Cell[] board) {
    return wasAlive(coordinates,board) ? 1 : 0;
  }

  private boolean wasAlive(final Coordinates coordinates, final Cell[] board) {
    return board[getBoardOffset(coordinates)].isAlive;
  }

  private int getBoardOffset(final Coordinates coordinates) {
    final Coordinates genZeroCoordinate =
        coordinateSystem.timeShifted(coordinates, 0);
    return coordinateSystem.toOffset(genZeroCoordinate) % coordinateSystem.size();
  }

}
