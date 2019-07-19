package com.thoughtpropulsion.reactrode.server;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    new SpringApplicationBuilder()
//        .main(Application.class)
        .sources(Application.class)
        .properties("spring.rsocket.server.port=7000")
        .properties("spring.main.lazy-initialization=true")
//        .profiles("server")
        .run(args);
  }
}
