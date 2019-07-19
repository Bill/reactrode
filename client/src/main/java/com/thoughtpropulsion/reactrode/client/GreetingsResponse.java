package com.thoughtpropulsion.reactrode.client;

import java.io.Serializable;

class GreetingsResponse implements Serializable {
  private String name;
  public GreetingsResponse() {
    name = null;
  }
  public GreetingsResponse(final String name) {
    this.name = name;
  }
  public void setName(final String name) {
    this.name = name;
  }
  public String getName() {
    return name;
  }
}
