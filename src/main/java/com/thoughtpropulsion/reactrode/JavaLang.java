package com.thoughtpropulsion.reactrode;

import java.util.function.Consumer;

public class JavaLang {
  private JavaLang(){}

  /**
   * Safely unbox a {@link Boolean} reference. (Convert to {@code boolean}, a {@link Boolean}
   * (reference) which may, in general, be {@null}.)
   *
   * Boxing of return value (at call site) never generates garbage because there are at most
   * two boolean objects per JVM: ({@code Boolean.TRUE} and {@code Boolean.FALSE}).
   *
   * The result can be passed to methods expecting a {@link Boolean} reference
   *
   * @param value
   * @return {@code true} or {@code false}
   */
  public static boolean toBooleanNotNull(final Boolean value) {
    return Boolean.TRUE.equals(value);
  }

  public static <T> T returning(final T val, final Consumer<T> f) {
    f.accept(val);
    return val;
  }
}
