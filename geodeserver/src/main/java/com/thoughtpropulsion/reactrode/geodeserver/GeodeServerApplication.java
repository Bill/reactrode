package com.thoughtpropulsion.reactrode.geodeserver;

import com.thoughtpropulsion.reactrode.geodeconfig.GeodeServerConfigurationAutoEvictionAndCQ;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication(scanBasePackageClasses = GeodeServerConfigurationAutoEvictionAndCQ.class)
public class GeodeServerApplication {

  /*
    While OpenJDK 12 defaults to G1GC now, Geode 1.9 doc says use CMS

    Run with:

    -Xmx100m -Xms100m -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=70
   */
  public static void main(String[] args) {
    new SpringApplicationBuilder()
        .web(WebApplicationType.NONE)
        .sources(GeodeServerApplication.class)
        .run(args);
  }
}
