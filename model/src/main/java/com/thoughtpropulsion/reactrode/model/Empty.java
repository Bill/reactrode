package com.thoughtpropulsion.reactrode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// this class is for benchmarking RSocket
// since it has no properties, we have to give it some Jackson anno to make Jackson serialization work
@JsonIgnoreProperties(ignoreUnknown=true)
public class Empty {
  public static Empty create() { return new Empty();}
  // don't call this. It's here to make RSocket serialization via Jackson work
  private Empty() {}
}
