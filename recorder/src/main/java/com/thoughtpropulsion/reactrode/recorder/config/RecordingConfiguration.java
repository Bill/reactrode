package com.thoughtpropulsion.reactrode.recorder.config;

import static org.springframework.messaging.rsocket.MetadataExtractor.ROUTING;

import java.net.URI;

import com.thoughtpropulsion.reactrode.recorder.server.RecorderServer;
import com.thoughtpropulsion.reactrode.recorder.subscriber.RecordingSubscriber;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeTypeUtils;

@Configuration
@ComponentScan(basePackageClasses = {RecordingSubscriber.class, RecorderServer.class})
public class RecordingConfiguration {

  @Bean
  public RSocket rSocket() {

    return RSocketFactory.connect()
//        .keepAlive(Duration.ofSeconds(20),Duration.ofSeconds(150),3)
        .mimeType(ROUTING.toString(), MimeTypeUtils.APPLICATION_JSON.toString())
        .frameDecoder(PayloadDecoder.ZERO_COPY)
        .transport(WebsocketClientTransport.create(URI.create("ws://localhost:7000/rsocket")))
        .start()
        .block();
  }

  @Bean
  RSocketRequester rSocketRequester(RSocketStrategies rSocketStrategies) {
    return RSocketRequester.wrap(
        rSocket(),
        MimeTypeUtils.APPLICATION_JSON,
        ROUTING,
        rSocketStrategies);
  }

}
