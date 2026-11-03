package it.gruppo5.tcg;

import org.springframework.boot.SpringApplication;

public class TestDreamscapeApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(DreamscapeApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
