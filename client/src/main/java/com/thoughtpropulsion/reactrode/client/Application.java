package com.thoughtpropulsion.reactrode.client;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class Application {

  final LifeClient lifeClient;

  public Application(final LifeClient lifeClient) {
    this.lifeClient = lifeClient;
  }

  public static void main(String[] args) {

    new SpringApplicationBuilder()
        .main(Application.class)
        .sources(Application.class)
        .properties("spring.devtools.livereload.enabled=false")
        .profiles("client")
        .run(args);
  }

  @Bean
  public ApplicationRunner getRunner() throws Exception {
    return args -> {
      System.out.println("ran!");
      // TODO: figure out why this flux never terminates (app hangs)
      Flux.from(lifeClient.allGenerations()).take(5).subscribe(cell -> System.out.println("got: " + cell));
    };
  }

}