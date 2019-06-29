package com.thoughtpropulsion.reactrode;

import java.util.function.Consumer;

public class Functional {
  private Functional(){}

  /**
   * Return {@param val} after invoking {@param f} for side-effects.
   *
   * @param val is the value to return
   * @param f is invoked for side-effects
   * @param <T> is any class
   * @return {@param val}
   */
  public static <T> T returning(final T val, final Consumer<T> f) {
    f.accept(val);
    return val;
  }

}
