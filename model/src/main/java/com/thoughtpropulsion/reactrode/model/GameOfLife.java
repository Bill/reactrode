package com.thoughtpropulsion.reactrode.model;

import static com.thoughtpropulsion.reactrode.model.Functional.returning;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vavr.CheckedFunction1;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

public class GameOfLife {

  final CoordinateSystem coordinateSystem;
  private final Publisher<Cell> allGenerations;

  public GameOfLife(
      final CoordinateSystem coordinateSystem,
      final Publisher<Cell> primordialGenerationPublisher) {

    this.coordinateSystem = coordinateSystem;

    final Flux<Cell> futureGenerations =

        enforceGenerationFraming(
          Flux.from(primordialGenerationPublisher)
              .buffer(coordinateSystem.size()),
            coordinateSystem)

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

  /*
   * It's important that we store exactly one frame/generation at a time. No less. No more!
   * The GameOfLife is not able to start from a generation that is not exactly the right size.
   * This function is suitable for use with Flux.transform(). It transforms the flux so that
   * if we see a buffer (List<Cell>) that is the wrong size, or which has cells from more than
   * one generation, we generate an error.
   *
   * This method is public because, not only is it useful in our own constructor (where it's used
   * to ensure the primordial generation is valid)---it's also useful in other places e.g.
   * anywhere we are recording or saving cell data for replay.
   */
  public static Flux<List<Cell>> enforceGenerationFraming(
      final Flux<List<Cell>> flux,
      final CoordinateSystem coordinateSystem) {

    return flux
        .concatMap(primordialGeneration -> {
          if (primordialGeneration.size() == coordinateSystem.size()) {
            return Mono.just(primordialGeneration);
          } else {
            return Mono.error(new IllegalArgumentException(String.format(
                "Expected generation of size %d but got %d",
                coordinateSystem.size(),
                primordialGeneration.size()
            )));
          }
        })
        .concatMap(primordialGeneration -> {
          final AtomicReference<Integer> generation = new AtomicReference<>();
          final Predicate<Cell> isInGeneration =
              (final Cell cell) -> {
                if (generation.get() == null) {
                  generation.set(cell.coordinates.generation);
                  return true;
                } else {
                  return cell.coordinates.generation == generation.get();
                }
              };
          if (primordialGeneration.stream().allMatch(isInGeneration)) {
            return Mono.just(primordialGeneration);
          } else {
            return Mono.error(new IllegalArgumentException(String.format(
                "Started with generation %d but changed mid-frame: %s",
                generation.get(),
                primordialGeneration
            )));
          }
        });
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

  /**
   * Calculate one cell's successor.
   *
   * @param c is the cell's coordinates
   * @return the cell's successor
   */
  private Cell nextGenerationCell(
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

    final Coordinates newCoordinates = cs.timeShifted(c, c.generation + 1);

    final boolean wasAlive = wasAlive(c, board);

    if (wasAlive) {
      if (liveNeighborsCount < 2) {
        return Cell.createDead(newCoordinates); // underpopulation
      } else if (liveNeighborsCount > 3) {
        return Cell.createDead(newCoordinates); // overpopulation
      } else {
        return Cell.createAlive(newCoordinates,false); // survival
      }
    } else {
      if (liveNeighborsCount == 3) {
        return Cell.createAlive(newCoordinates,true); // reproduction
      } else {
        return Cell.createDead(newCoordinates); // status quo
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
