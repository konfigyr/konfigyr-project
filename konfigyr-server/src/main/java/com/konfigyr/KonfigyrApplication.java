package com.konfigyr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Konfigyr Spring server application main class.
 *
 * @author : vladimir.spasic@ebf.com
 * @since : 26.03.24, Tue
 **/
@SpringBootApplication
public class KonfigyrApplication {

    public static void main(String[] args) {
        SpringApplication.run(KonfigyrApplication.class, args);
    }

}
