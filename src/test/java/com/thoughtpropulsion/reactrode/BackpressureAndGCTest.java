package com.thoughtpropulsion.reactrode;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class BackpressureAndGCTest {

  // make the board non-square to catch bugs where the row/column sense is inconsistent
  private static final int PRIMORDIAL_GENERATION = -1;
  private static final int COLUMNS = 8;
  private static final int ROWS = 4;
  private static final int GENERATIONS_CACHED = 3;
  private GameOfLifeSystem gameOfLifeSystem;

  @BeforeAll
  static void beforeAll() { Hooks.onOperatorDebug();}

  @BeforeEach
  void beforeEach() {
    gameOfLifeSystem = GameOfLifeSystem.create(COLUMNS, ROWS, GENERATIONS_CACHED,
        PRIMORDIAL_GENERATION);
  }

  // before any coordinates have arrived...

  // ...it's not possible to read coordinates
  @Test
  void getBeforeAnyCoordinates() {
    final Coordinates coord = gameOfLifeSystem.getCoordinateSystem().createCoordinate(0);

    StepVerifier.create(gameOfLifeSystem.getGameState().get(coord))
        .expectComplete() // nothing to see here--move along
        .verify();
  }

  //... it's not possible to subscribe to passed generations
  @Test
  void subscribeToPassedGeneration() {

    final Flux<Cell> changes = gameOfLifeSystem.getGameState().changes(-2);

    StepVerifier.create(changes)
        .expectError(SubscriptionException.class)
        .verify(Duration.ofSeconds(1));
  }

  // ...it's possible to subscribe to generation PRIMORDIAL_GENERATION
  @Test
  void subscribeBeforeAnyCellsHaveArrived() {

    final Flux<Cell> changes = gameOfLifeSystem.getGameState().changes(PRIMORDIAL_GENERATION);

    generate(PRIMORDIAL_GENERATION,1);

    verifyOneGeneration(changes, "cells arriving after subscription are seen by query");
  }

  // ...it's not possible to subscribe beyond the end of the window
  @Test
  void noSubscribeBeyondWindow() {
    final int generation = PRIMORDIAL_GENERATION +
        GENERATIONS_CACHED;

    final Flux<Cell> changes = gameOfLifeSystem.getGameState().changes(generation);

    StepVerifier.create(changes)
        .expectError(SubscriptionException.class)
        .verify(Duration.ofSeconds(1));
  }

  // ...it's never possible to subscribe to a generation before PRIMORDIAL_GENERATION

  // first coordinates generated must be to generation PRIMORDIAL_GENERATION

  // won't accept coordinates for generation N until generation N-1 is full

  // after some coordinates have arrived...

  //...it's possible to read the coordinates
  @Test
  void subscribeAfterCellsHaveArrived() {

    generate(PRIMORDIAL_GENERATION,1);

    final Flux<Cell> changes = gameOfLifeSystem.getGameState().changes(PRIMORDIAL_GENERATION);

    verifyOneGeneration(changes, "cells arriving before subscription are seen by query");
  }

  @Test
  void withoutSubscribersWindowAdvances() {

    final int range = GENERATIONS_CACHED + 1;

    final Flux<Generation> generations =
        gameOfLifeSystem.getGameState().generations().take(range);

    // we'll first verify range generations, then we'll verify one more
    generate(PRIMORDIAL_GENERATION, range + 1);

    StepVerifier.create(generations)
        .expectNextCount(range)
        .expectComplete()
        .verify();

    final Flux<Generation> generations2 =
        gameOfLifeSystem.getGameState().generations().take(1);

    StepVerifier.create(generations2)
        .assertNext(generation ->{
          assertThat(generation.number).isEqualTo(PRIMORDIAL_GENERATION + range);
        })
        .expectComplete()
        .verify();

  }

  @Test
  void withoutSubscribersWindowAdvancesAndAllCellsAreProduced() {

    final int range = GENERATIONS_CACHED + 1;

    final Flux<Generation> generations =
        gameOfLifeSystem.getGameState().generations().take(range);

    generate(PRIMORDIAL_GENERATION, range);

    final Consumer<Generation> validateGeneration = generation ->
        StepVerifier.create(generation.cells)
            .expectNextCount(gameOfLifeSystem.getCoordinateSystem().size())
            .as("a full generation-worth of cells")
            .expectComplete()
            .verify();

    /*
     We've hard-coded for 4 generations in our verification below. If range is not 4
     it is not the fault of the UUT (unit-under-test), hence the bare assert here
     */
    assert range == 4;

    StepVerifier.create(generations)
        .consumeNextWith(validateGeneration)
        .consumeNextWith(validateGeneration)
        .consumeNextWith(validateGeneration)
        .consumeNextWith(validateGeneration)
        .expectComplete()
        .verify();
  }

  @Disabled
  @Test
  void gameIsThrottled() {
  }

  @Disabled
  @Test
  void gameResumesAfterThrottling() {
    // after producer (GameOfLife) is throttled, destroying subscription to oldest generation
    // causes resumption
  }

  @Disabled
  @Test
  void fullWindowIsAccessible() {
    final CoordinateSystem coordinateSystem = gameOfLifeSystem.getCoordinateSystem();
    final int lastOffset = coordinateSystem.size() * GENERATIONS_CACHED - 1;
    final Coordinates lastCoordinates = coordinateSystem.createCoordinate(lastOffset);

    /*
     Subscribe to generation PRIMORDIAL_GENERATION changes but don't process them. This should cause
     GameStateColdChanges to hold on to all generations starting at generation PRIMORDIAL_GENERATION.
     */
    final Flux<Cell> primordialGenerationChanges =
        gameOfLifeSystem.getGameState().changes(PRIMORDIAL_GENERATION);

    final Flux<Cell> lastGenerationChanges = gameOfLifeSystem.getGameState().changes(lastCoordinates.generation);

    final AtomicReference<Cell> took = new AtomicReference<>();
    lastGenerationChanges
        .skip(coordinateSystem.size()-1).take(1).doOnNext(took::set)
        .publishOn(Schedulers.parallel()).subscribe();

    gameOfLifeSystem.startGame();

    while (null == took.get()) {
      try {
        Thread.sleep(10);
      } catch (final InterruptedException e) {
      }
    }
    assertThat(took.get()).isNotNull();
    assertThat(took.get().coordinates).isEqualTo(lastCoordinates);

    // now that we've seen the last coordinates, verify we can still access the first one
    took.set(null);
    primordialGenerationChanges
        .take(1).doOnNext(took::set)
        .publishOn(Schedulers.parallel()).subscribe();
    while (null == took.get()) {
      try {
        Thread.sleep(10);
      } catch (final InterruptedException e) {
      }
    }
    assertThat(took.get()).isNotNull();
    assertThat(took.get().coordinates).isEqualTo(coordinateSystem.createCoordinate(0));
  }


  private void generate(final int startingGeneration, final int generations) {
    final CoordinateSystem cs = gameOfLifeSystem.getCoordinateSystem();
    final int startingOffset = startingGeneration * cs.size();
    final int endingOffset = startingOffset + cs.size() * generations;
    gameOfLifeSystem.getGameState().putAll(
        Flux.generate(
            () -> startingOffset,
            (i, sink) -> {
              if (i < endingOffset) {
                System.out.println(String.format("producing cell %d of (%d,+%d)",
                    i, startingGeneration, generations));
                sink.next(
                    Cell.create(
                        cs.createCoordinate(i),
                        true));
                System.out.println(String.format("produced cell %d of (%d,+%d)",
                    i, startingGeneration, generations));
              } else {
                System.out.println(String.format("producing last cell of generation (%d,+%d)",
                    startingGeneration, generations));
                sink.complete();
                System.out.println(String.format("produced last cell of generation (%d,+%d)",
                    startingGeneration, generations));
              }
              return i + 1;
            }));
  }

  private void verifyOneGeneration(final Publisher<Cell> changes, final String s) {
    StepVerifier.create(changes)
        .expectNextCount(gameOfLifeSystem.getCoordinateSystem().size())
        .as(s)
        .expectComplete()
        .verify(Duration.ofSeconds(1));
  }
}