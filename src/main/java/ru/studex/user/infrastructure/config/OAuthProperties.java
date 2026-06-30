package ru.studex.user.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

    private String redirectBaseUrl = "http://localhost:8080";
    private String frontendUrl = "http://localhost:5173";

    private ProviderConfig sber  = new ProviderConfig();
    private ProviderConfig tbank = new ProviderConfig();
    private ProviderConfig alfa  = new ProviderConfig();

    @Data
    public static class ProviderConfig {
        private String clientId     = "";
        private String clientSecret = "";
        private String authUrl      = "";
        private String tokenUrl     = "";
        private String userinfoUrl  = "";
        private String scope        = "openid profile email";
        private boolean pkce        = false;
    }
}
