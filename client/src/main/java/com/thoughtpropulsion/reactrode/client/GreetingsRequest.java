package com.thoughtpropulsion.reactrode.client;

import java.io.Serializable;

public class GreetingsRequest implements Serializable {
  private String name;
  public GreetingsRequest() {
    name = null;
  }
  public GreetingsRequest(final String name) {
    this.name = name;
  }
  public void setName(final String name) {
    this.name = name;
  }
  public String getName() {
    return name;
  }
}
