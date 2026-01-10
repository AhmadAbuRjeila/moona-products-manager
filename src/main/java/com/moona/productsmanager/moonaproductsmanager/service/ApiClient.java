package com.moona.productsmanager.moonaproductsmanager.service;

import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ApiClient {

    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);
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
                .exchangeToMono(resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new AbstractMap.SimpleEntry<>(resp.statusCode().value(), body)))
                .flatMap(entry -> {
                    int status = entry.getKey();
                    String body = entry.getValue();
                    if (status >= 400) {
                        log.error("GraphQL error status={} body={}", status, body);
                        return Mono.error(new GraphqlRequestException(status));
                    }
                    log.debug("GraphQL success status={} body={}", status, body);
                    return Mono.just(body);
                })
                .timeout(Duration.ofSeconds(30))
                .retryWhen(retrySpec());
    }

    private RetryBackoffSpec retrySpec() {
        return Retry.backoff(5, Duration.ofSeconds(2))
                .maxBackoff(Duration.ofSeconds(20))
                .jitter(0.5)
                .filter(this::isRetriable)
                .doBeforeRetry(signal -> log.warn(
                        "Retrying GraphQL call attempt={}",
                        signal.totalRetries() + 1
                ));
    }

    private boolean isRetriable(Throwable ex) {
        if (ex instanceof GraphqlRequestException gre) {
            return gre.status >= 500; // retry only server-side GraphQL errors
        }
        return ex instanceof ReadTimeoutException
                || ex instanceof TimeoutException
                || (ex instanceof WebClientRequestException && ex.getCause() instanceof ReadTimeoutException);
    }

    private static final class GraphqlRequestException extends IllegalStateException {
        private final int status;

        GraphqlRequestException(int status) {
            super("GraphQL request failed status=" + status);
            this.status = status;
        }
    }
}
