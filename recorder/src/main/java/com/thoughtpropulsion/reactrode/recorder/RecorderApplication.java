package com.thoughtpropulsion.reactrode.recorder;

import com.thoughtpropulsion.reactrode.model.Cell;
import com.thoughtpropulsion.reactrode.model.CoordinateSystem;
import com.thoughtpropulsion.reactrode.recorder.config.RecordingConfiguration;
import com.thoughtpropulsion.reactrode.recorder.gemfireTemplate.CellGemfireTemplate;
import com.thoughtpropulsion.reactrode.recorder.subscriber.RecordingSubscriber;
import org.reactivestreams.Publisher;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.EnableContinuousQueries;
import reactor.core.publisher.Flux;

@SpringBootApplication(scanBasePackageClasses = RecordingConfiguration.class)
@ClientCacheApplication(subscriptionEnabled = true)
@EnableContinuousQueries
public class RecorderApplication {

  private static final int PARALLELISM = 4;
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
      final CellGemfireTemplate cellGemfireTemplate,
      final CoordinateSystem coordinateSystem) throws Exception {

    final Publisher<Cell> source = recordingSubscriber.allGenerations();

    return args -> {
      Flux.from(
          CellOperations.createSerialBulkPutPublisher(
              cellGemfireTemplate,
              coordinateSystem,
              source, CellOperations.LIMIT_REQUEST))
          .blockLast();
    };
  }

}
