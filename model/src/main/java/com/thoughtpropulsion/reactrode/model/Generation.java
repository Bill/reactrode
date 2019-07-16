package com.thoughtpropulsion.reactrode.model;

import java.util.Objects;

import reactor.core.publisher.Flux;

public class Generation {
  public final int number;
  public final Flux<Cell> cells;

  public static Generation create(final int number, final Flux<Cell> cells) {
    return new Generation(number,cells);
  }

  private Generation(final int number, final Flux<Cell> cells) {
    this.number = number;
    this.cells = cells;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Generation that = (Generation) o;
    return number == that.number;
  }

  @Override
  public int hashCode() {
    return Objects.hash(number);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Generation{");
    sb.append("number=").append(number);
    sb.append('}');
    return sb.toString();
  }
}
