package com.thoughtpropulsion.reactrode.webapp;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    new SpringApplicationBuilder()
        .main(Application.class)
        .sources(Application.class)
        .profiles("webapp")
        .run(args);
  }
}
