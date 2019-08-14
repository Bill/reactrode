package com.thoughtpropulsion.reactrode.jacksontest;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtpropulsion.reactrode.model.Coordinates;
import com.thoughtpropulsion.reactrode.model.Empty;
import org.junit.jupiter.api.Test;

public class JacksonTest {
  @Test
  public void foo() throws IOException {
    final ObjectMapper objectMapper = new ObjectMapper();
    final Empty empty = Empty.create();
//    final Coordinates coordinates = Coordinates.create(0, 0, 0);
    objectMapper.writeValue(new File("build/empty.json"), empty);
  }
}
