package com.thoughtpropulsion.reactrode.geodeserver;

import com.thoughtpropulsion.reactrode.geodeserver.config.GeodeConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication(scanBasePackageClasses = GeodeConfiguration.class)
public class GeodeServerApplication {
  public static void main(String[] args) {
    new SpringApplicationBuilder()
        .web(WebApplicationType.NONE)
        .sources(GeodeServerApplication.class)
        .run(args);
  }
}
