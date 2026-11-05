package it.somnia.dreamscape.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DreamscapeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DreamscapeApiApplication.class, args);
    }

}
