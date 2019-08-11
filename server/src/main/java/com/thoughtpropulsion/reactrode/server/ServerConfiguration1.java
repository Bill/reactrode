package com.thoughtpropulsion.reactrode.server;

import static com.thoughtpropulsion.reactrode.model.Functional.returning;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.CoordinateSystem;
import com.thoughtpropulsion.reactrode.model.GameOfLife;
import com.thoughtpropulsion.reactrode.model.Patterns;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.rsocket.RSocketProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.boot.rsocket.netty.NettyRSocketServerFactory;
import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.boot.rsocket.server.RSocketServerFactory;
import org.springframework.boot.rsocket.server.ServerRSocketFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import reactor.core.publisher.Flux;

@Configuration
public class ServerConfiguration1 {

  @Bean
  public RSocketServerFactory rSocketServerFactory(RSocketProperties properties,
                                                   ReactorResourceFactory resourceFactory,
                                                   ObjectProvider<ServerRSocketFactoryCustomizer> customizers) {
    NettyRSocketServerFactory factory = new NettyRSocketServerFactory();
    factory.setResourceFactory(resourceFactory);
    PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    try {
      factory.setAddress(InetAddress.getLocalHost());
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }

// can't access getServer() because RSocketProperties.Server is package-protected
//    map.from(properties.getServer().getAddress()).to(factory::setAddress);
//    map.from(properties.getServer().getPort()).to(factory::setPort);

    factory.setPort(7000);
    factory.setTransport(RSocketServer.TRANSPORT.WEBSOCKET);

    factory.setServerCustomizers(
        customizers.orderedStream().collect(Collectors.toList()));
    return factory;
  }

  @Bean
  public CoordinateSystem getCoordinateSystem() {
    return mediumCoordinateSystem();
  }

  @Bean
  public int primordialGenerationNumber() {return -1;}

  @Bean
  public Publisher<Cell> primordialGeneration() {

    final List<Boolean> pattern = randomPattern(400,400 );

    return Flux.fromIterable(
        Patterns.cellsFromBits(pattern, primordialGenerationNumber(), getCoordinateSystem()));
  }

  private CoordinateSystem mediumCoordinateSystem() {
    return new CoordinateSystem(400,400);
  }

  private List<Boolean> randomPattern(final int columns, final int rows) {
    final Random random = createRandom(1L);
    return Stream.generate(random::nextBoolean).limit(columns * rows).collect(Collectors.toList());
  }

  private Random createRandom(final long seed) {
    try {
      return returning(SecureRandom.getInstance("SHA1PRNG"), random -> random.setSeed(seed));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
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
