package com.moona.productsmanager.moonaproductsmanager.service;

import com.moona.productsmanager.moonaproductsmanager.config.ApiProperties;
import com.moona.productsmanager.moonaproductsmanager.config.FileProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Map;

@Component
public class ImageUploadClient {

    private final WebClient apiWebClient;
    private final FileProperties fileProperties;
    private final ApiProperties apiProperties;

    public ImageUploadClient(WebClient apiWebClient, FileProperties fileProperties, ApiProperties apiProperties) {
        this.apiWebClient = apiWebClient;
        this.fileProperties = fileProperties;
        this.apiProperties = apiProperties;
    }

    public Mono<String> uploadImage(Map<String, Object> operations, Map<String, Object> mapField, File imageFile) {
        if (imageFile == null || !imageFile.exists() || !imageFile.isFile()) {
            return Mono.error(new IllegalArgumentException("Image file not found: " + imageFile));
        }

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("operations", operations)
            .contentType(MediaType.APPLICATION_JSON);
        builder.part("map", mapField)
            .contentType(MediaType.APPLICATION_JSON);
        builder.part("0", new FileSystemResource(imageFile))
            .contentType(MediaType.parseMediaType(fileProperties.getImageDefaultContentType()))
            .filename(imageFile.getName());

        MultiValueMap<String, org.springframework.http.HttpEntity<?>> multipartBody = builder.build();

        return apiWebClient.post()
            .uri(apiProperties.getBaseUrl())
            .body(BodyInserters.fromMultipartData(multipartBody))
            .retrieve()
            .bodyToMono(String.class);
    }
}

