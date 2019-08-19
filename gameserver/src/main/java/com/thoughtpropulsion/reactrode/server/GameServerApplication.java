package com.thoughtpropulsion.reactrode.server;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class GameServerApplication {

  public static void main(String[] args) {
    new SpringApplicationBuilder()
        .sources(GameServerApplication.class)
        .run(args);
  }
}
