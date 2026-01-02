package com.moona.productsmanager.moonaproductsmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moona.productsmanager.moonaproductsmanager.config.ErpProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ErpReportClient {

    private final WebClient erpWebClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> cookieJar = new ConcurrentHashMap<>();

    @Autowired
    public ErpReportClient(ErpProperties erpProperties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofMillis(erpProperties.getReadTimeoutMs()));

        this.erpWebClient = WebClient.builder()
            .baseUrl(erpProperties.getBaseUrl())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(erpProperties.getMaxInMemoryBytes()))
                .build())
            .filter((request, next) -> {
                ClientRequest.Builder builder = ClientRequest.from(request);
                resolveCookieHeader().ifPresent(cookie -> builder.header("Cookie", cookie));
                return next.exchange(builder.build())
                    .doOnNext(resp -> resp.cookies().forEach((name, values) -> {
                        if (!values.isEmpty()) {
                            cookieJar.put(name, values.get(0).getValue());
                        }
                    }));
            })
            .build();
    }

    public Mono<String> login(ErpProperties erpProperties) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("usr", erpProperties.getUsername());
        formData.add("pwd", erpProperties.getPassword());

        return erpWebClient.post()
            .uri(erpProperties.getLoginPath())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(formData))
            .exchangeToMono(resp -> handleResponse(resp, "ERP login failed"));
    }

    public Mono<String> fetchReport(ErpProperties erpProperties, String reportName) {
        return erpWebClient.post()
            .uri(uriBuilder -> uriBuilder
                .path(erpProperties.getReportPath())
                .queryParam("report_name", reportName)
                .build())
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Frappe-CSRF-Token", resolveCsrfToken().orElse(""))
            .bodyValue(buildReportPayload(reportName))
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(ex -> Mono.error(new IllegalStateException("ERP report fetch failed", ex)));
    }

    private Mono<String> handleResponse(ClientResponse resp, String errorPrefix) {
        if (resp.statusCode().is2xxSuccessful()) {
            return resp.bodyToMono(String.class);
        }
        return resp.bodyToMono(String.class)
            .flatMap(body -> Mono.error(new IllegalStateException(errorPrefix + ": status=" + resp.statusCode().value() + " body=" + body)));
    }

    public JsonNode normalize(String rawJson) throws Exception {
        JsonNode json = objectMapper.readTree(rawJson);
        JsonNode envelope = json;
        if (json.has("message") && json.get("message").isObject()) {
            envelope = json.get("message");
        }
        JsonNode rows = envelope.has("result") ? envelope.get("result") : envelope.get("message");
        return objectMapper.createObjectNode()
            .set("rows", rows == null ? objectMapper.createArrayNode() : rows);
    }

    public Optional<String> extractCsrfToken(String rawLoginResponse) {
        try {
            JsonNode node = objectMapper.readTree(rawLoginResponse);
            if (node.has("full_name")) {
                return Optional.of("noop");
            }
        } catch (Exception ignored) { }
        return Optional.empty();
    }

    private Optional<String> resolveCsrfToken() {
        return cookieJar.entrySet().stream()
            .filter(e -> e.getKey().equalsIgnoreCase("csrftoken") || e.getKey().equalsIgnoreCase("csrf_token") || e.getKey().toLowerCase().contains("csrf"))
            .map(Map.Entry::getValue)
            .findFirst();
    }

    private Optional<String> resolveCookieHeader() {
        if (cookieJar.isEmpty()) {
            return Optional.empty();
        }
        String cookieHeader = cookieJar.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .reduce((a, b) -> a + "; " + b)
            .orElse("");
        return cookieHeader.isBlank() ? Optional.empty() : Optional.of(cookieHeader);
    }

    private String buildReportPayload(String reportName) {
        return "{\"report_name\":\"" + reportName + "\",\"filters\":{},\"file_format_type\":\"JSON\"}";
    }
}
