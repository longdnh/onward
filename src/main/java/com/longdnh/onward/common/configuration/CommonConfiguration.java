package com.longdnh.donot_get_left_behind.common.configuration;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfiguration {

    /** Single injectable clock so services never call Instant.now() directly; tests can swap it. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
