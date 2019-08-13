package com.thoughtpropulsion.reactrode.server;

import java.util.List;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.CoordinateSystem;
import com.thoughtpropulsion.reactrode.model.GameOfLife;
import com.thoughtpropulsion.reactrode.model.Patterns;
import org.reactivestreams.Publisher;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import reactor.core.publisher.Flux;

@Configuration
public class ServerConfiguration1 {

  @Bean
  public CoordinateSystem getCoordinateSystem() {
    return mediumCoordinateSystem();
  }

  @Bean
  public int primordialGenerationNumber() {return -1;}

  @Bean
  public Publisher<Cell> primordialGeneration() {

    final List<Boolean> pattern = Patterns.randomPattern(400,400 );

    return Flux.fromIterable(
        Patterns.cellsFromBits(pattern, primordialGenerationNumber(), getCoordinateSystem()));
  }

  private CoordinateSystem mediumCoordinateSystem() {
    return new CoordinateSystem(400,400);
  }

  private CoordinateSystem tinyCoordinateSystem() {
    return new CoordinateSystem(4,5);
  }

  private List<Boolean> stableBlockPattern() {
    return Patterns.toPattern(0, 0, 0, 0,
        0, 1, 1, 0,
        0, 1, 1, 0,
        0, 0, 0, 0,
        0, 0, 0, 0);
  }

  @Bean
  public Publisher<Cell> allGenerations() {
    return new GameOfLife(getCoordinateSystem(), primordialGeneration()).getAllGenerations();
  }

  @Bean
  public RSocketStrategiesCustomizer rSocketStrategiesCustomizer() {
    return strategies -> {
      strategies.encoder(CharSequenceEncoder.allMimeTypes());
      strategies.decoder(StringDecoder.allMimeTypes());
    };
  }
}
