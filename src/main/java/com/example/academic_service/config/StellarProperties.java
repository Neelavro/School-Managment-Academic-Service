package com.example.academic_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "stellar")
@Getter
@Setter
public class StellarProperties {

    private boolean enabled = false;
    private String authUser;
    private String authCode;
    private String baseUrl = "https://rumytechnologies.com/rams/json_api";
    private String timezone = "Asia/Dhaka";

    // Minimum seconds between any two Stellar calls (their rate-limit floor is 5 minutes).
    private int minCallIntervalSeconds = 300;
    // Dashboard read-only endpoint reuses cached count if last call is newer than this.
    private int dashboardCacheSeconds = 3600;
}
