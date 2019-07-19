package com.thoughtpropulsion.reactrode.client;

public class GreetingsRequest {
  private final String name;
  public GreetingsRequest() {
    name = null;
  }
  public GreetingsRequest(final String name) {
    this.name = name;
  }
  public String getName() {
    return name;
  }
}
