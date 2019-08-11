package com.thoughtpropulsion.reactrode.client;

import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

@Configuration
public class LifeClientConfiguration {


  // TODO: this is defined in DefaultRSocketRequester---figure out right way to wrap() in M4
  private static final MimeType ROUTING = new MimeType("message", "x.rsocket.routing.v0");
  private static final MimeType COMPOSITE_METADATA = new MimeType("message", "x.rsocket.composite-metadata.v0");

  @Bean
  public RSocket rSocket() {
//    return RSocketFactory
//        .connect()
//        .mimeType(
//            COMPOSITE_METADATA.toString(),
//            MimeTypeUtils.TEXT_PLAIN_VALUE) // payload MIME type
//        .frameDecoder(PayloadDecoder.ZERO_COPY)
//        // TODO: DRY from application.properties
//        .transport(TcpClientTransport.create(7000))
//        .start()
//        .block();

    return RSocketFactory.connect()
        .dataMimeType(MimeTypeUtils.APPLICATION_JSON_VALUE)
        .frameDecoder(PayloadDecoder.ZERO_COPY)
//        .transport(TcpClientTransport.create(7000))
        .transport(WebsocketClientTransport.create(50979))
        .start()
        .block();
  }

  @Bean
  RSocketRequester rSocketRequester(RSocketStrategies rSocketStrategies) {
//    return RSocketRequester.wrap(
//        rSocket(),
//        MimeTypeUtils.TEXT_PLAIN, // payload MIME type
//        COMPOSITE_METADATA,
//        rSocketStrategies);
    return RSocketRequester.wrap(
        rSocket(),
        MimeTypeUtils.APPLICATION_JSON,
        rSocketStrategies);
  }

}
