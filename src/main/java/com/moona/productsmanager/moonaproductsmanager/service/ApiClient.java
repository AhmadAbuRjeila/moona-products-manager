package com.moona.productsmanager.moonaproductsmanager.service;

import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

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
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(30))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                .filter(ex -> ex instanceof ReadTimeoutException
                    || ex instanceof TimeoutException
                    || (ex instanceof WebClientRequestException && ex.getCause() instanceof ReadTimeoutException)))
            ;
    }
}
