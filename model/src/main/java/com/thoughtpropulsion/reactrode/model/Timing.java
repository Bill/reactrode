package com.thoughtpropulsion.reactrode.model;

public class Timing {
  private Timing() {}

  public static long elapsed(final Runnable f) {
    final long start = System.nanoTime();
    f.run();
    final long complete = System.nanoTime();
    return complete - start;
  }
}
