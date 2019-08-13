package com.thoughtpropulsion.reactrode.server;

import static com.thoughtpropulsion.reactrode.model.Patterns.pufferfishSpaceshipPattern;

import java.util.List;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.CoordinateSystem;
import com.thoughtpropulsion.reactrode.model.GameOfLife;
import com.thoughtpropulsion.reactrode.model.Pattern;
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
    return smallCoordinateSystem();
  }

  @Bean
  public int primordialGenerationNumber() {return -1;}

  @Bean
  public Pattern getPattern() {
    return pufferfishSpaceshipPattern();
  }

  @Bean
  public Publisher<Cell> getPrimordialGeneration(final Pattern pattern) {
    return Flux.fromIterable(
        Patterns.cellsFromBits(pattern, primordialGenerationNumber(), getCoordinateSystem()));
  }

  private Publisher<Cell> smallPrimordialGeneration() {

    final List<Boolean> pattern = Patterns.randomPattern(100,100 );

    return Flux.fromIterable(
        Patterns.cellsFromBits(pattern, primordialGenerationNumber(), getCoordinateSystem()));
  }

  private CoordinateSystem mediumCoordinateSystem() {
    return new CoordinateSystem(400,400);
  }

  private CoordinateSystem smallCoordinateSystem() {
    return new CoordinateSystem(100,100);
  }

  private CoordinateSystem tinyCoordinateSystem() {
    return new CoordinateSystem(4,5);
  }

  @Bean
  public Publisher<Cell> allGenerations(final CoordinateSystem coordinateSystem, final Publisher<Cell> primordialGeneration) {
    return new GameOfLife(coordinateSystem, primordialGeneration).getAllGenerations();
  }

  @Bean
  public RSocketStrategiesCustomizer rSocketStrategiesCustomizer() {
    return strategies -> {
      strategies.encoder(CharSequenceEncoder.allMimeTypes());
      strategies.decoder(StringDecoder.allMimeTypes());
    };
  }
}
