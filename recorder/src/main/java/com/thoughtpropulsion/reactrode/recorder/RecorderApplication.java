package com.thoughtpropulsion.reactrode.recorder;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.CoordinateSystem;
import com.thoughtpropulsion.reactrode.recorder.config.RecordingConfiguration;
import com.thoughtpropulsion.reactrode.recorder.geodeclient.GeodeClientConfiguration;
import com.thoughtpropulsion.reactrode.recorder.subscriber.RecordingSubscriber;
import org.reactivestreams.Publisher;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.EnableContinuousQueries;
import reactor.core.publisher.Flux;

@SpringBootApplication(scanBasePackageClasses = {RecordingConfiguration.class, GeodeClientConfiguration.class})
@ClientCacheApplication(subscriptionEnabled = true)
@EnableContinuousQueries
public class RecorderApplication {

  private static volatile ConfigurableApplicationContext applicationContext;

  final RecordingSubscriber recordingSubscriber;

  public RecorderApplication(final RecordingSubscriber recordingSubscriber) {
    this.recordingSubscriber = recordingSubscriber;
  }

  public static void main(String[] args) {
    applicationContext = new SpringApplicationBuilder()
        .sources(RecorderApplication.class)
        .run(args);
  }

  @Bean
  public CoordinateSystem getCoordinateSystem() {
    return new CoordinateSystem(100, 100);
  }

  @Bean
  public ApplicationRunner getRunner(
      final GemfireTemplate gemfireTemplate,
      final CoordinateSystem coordinateSystem) {

    final Publisher<Cell> source = recordingSubscriber.allGenerations();

    return args ->
      Flux.from(
          CellOperations.createSerialBulkPutPublisher(
              gemfireTemplate,
              coordinateSystem,
              source, 100))
          .blockLast();
  }

}
