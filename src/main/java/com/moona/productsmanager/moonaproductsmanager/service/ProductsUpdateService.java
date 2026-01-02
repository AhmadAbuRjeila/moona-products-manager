package com.moona.productsmanager.moonaproductsmanager.service;

import com.moona.productsmanager.moonaproductsmanager.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ProductsUpdateService {
    private static final Logger log = LoggerFactory.getLogger(ProductsUpdateService.class);

    /**
     * Stub upsert: update if exists, otherwise create. Replace with real implementation later.
     */
    public Mono<Void> upsertProducts(List<Product> products) {
        log.info("Stub upsertProducts called with {} products (no-op)", products.size());
        return Mono.empty();
    }
}

