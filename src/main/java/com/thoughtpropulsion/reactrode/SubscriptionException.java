package com.thoughtpropulsion.reactrode;

public class SubscriptionException extends RuntimeException {
  public SubscriptionException() {
    super();
  }

  public SubscriptionException(final String message) {
    super(message);
  }

  public SubscriptionException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public SubscriptionException(final Throwable cause) {
    super(cause);
  }

  protected SubscriptionException(final String message, final Throwable cause,
                                  final boolean enableSuppression,
                                  final boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
