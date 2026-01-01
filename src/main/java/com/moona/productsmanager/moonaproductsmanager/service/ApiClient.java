package com.moona.productsmanager.moonaproductsmanager.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class ApiClient {

    private final WebClient webClient;

    public ApiClient(WebClient apiWebClient) {
        this.webClient = apiWebClient;
    }

    public Mono<String> mutation(String query, Map<String, Object> variables) {
        return webClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "query", query,
                "variables", variables
            ))
            .retrieve()
            .bodyToMono(String.class);
    }
}
