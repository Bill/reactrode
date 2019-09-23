package com.thoughtpropulsion.reactrode.gameserver;

import static com.thoughtpropulsion.reactrode.model.Patterns.pufferfishSpaceshipPattern;

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
public class ServerConfiguration {

  @Bean
  public CoordinateSystem getCoordinateSystem() {
    return smallCoordinateSystem();
  }

  @Bean
  public int primordialGenerationNumber() {return -1;}

  @Bean
  public Pattern getPattern(final CoordinateSystem coordinateSystem) {
    return pufferfishSpaceshipPattern(coordinateSystem);
//    return randomPattern(coordinateSystem);
  }

  @Bean
  public Publisher<Cell> primordialGeneration(
      final Pattern pattern,
      final CoordinateSystem coordinateSystem,
      final int primordialGenerationNumber) {
    return Flux.fromIterable(
        Patterns.cellsFromBits(pattern, primordialGenerationNumber, coordinateSystem));
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
