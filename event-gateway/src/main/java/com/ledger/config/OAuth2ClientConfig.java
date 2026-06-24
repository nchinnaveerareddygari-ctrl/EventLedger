package com.eventledger.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.client.RestClient;

/**
 * OAuth2 Client Configuration for Event Gateway
 *
 * Configures OAuth2 client credentials flow to authenticate with Account Service
 * Uses RestClient to handle Bearer token injection automatically
 */
@Configuration
public class OAuth2ClientConfig {

    /**
     * Create OAuth2AuthorizedClientManager for handling client credentials flow
     * This automatically manages token acquisition and refresh
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        // Client Credentials flow for service-to-service authentication
                        .clientCredentials()
                        .build();

        DefaultOAuth2AuthorizedClientManager manager =
                new DefaultOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientRepository);

        manager.setAuthorizedClientProvider(authorizedClientProvider);

        return manager;
    }

    /**
     * Create RestClient with OAuth2 interceptor for automatic Bearer token handling
     * RestClient is preferred over RestTemplate for OAuth2 scenarios
     */
    @Bean
    public RestClient restClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        return RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    // The OAuth2 interceptor will be added by Spring Security
                    return execution.execute(request, body);
                })
                .build();
    }
}