package com.thoughtpropulsion.reactrode.client;

class GreetingsResponse {
  private final String name;
  public GreetingsResponse() {
    name = null;
  }
  public GreetingsResponse(final String name) {
    this.name = name;
  }
  public String getName() {
    return name;
  }
}
