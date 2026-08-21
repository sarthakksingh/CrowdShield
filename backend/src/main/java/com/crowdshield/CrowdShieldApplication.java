package com.crowdshield;

import com.crowdshield.config.CrowdShieldProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CrowdShieldProperties.class)
public class CrowdShieldApplication {
    public static void main(String[] args) {
        SpringApplication.run(CrowdShieldApplication.class, args);
    }
}
