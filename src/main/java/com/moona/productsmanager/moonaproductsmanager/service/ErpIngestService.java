package com.moona.productsmanager.moonaproductsmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.moona.productsmanager.moonaproductsmanager.config.ErpProperties;
import com.moona.productsmanager.moonaproductsmanager.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ErpIngestService {
    private static final Logger log = LoggerFactory.getLogger(ErpIngestService.class);

    private final ErpReportClient erpReportClient;
    private final ErpProductMapper erpProductMapper;
    private final ProductsUpdateService productsUpdateService;
    private final ErpProperties erpProperties;

    public ErpIngestService(ErpReportClient erpReportClient,
                            ErpProductMapper erpProductMapper,
                            ProductsUpdateService productsUpdateService,
                            ErpProperties erpProperties) {
        this.erpReportClient = erpReportClient;
        this.erpProductMapper = erpProductMapper;
        this.productsUpdateService = productsUpdateService;
        this.erpProperties = erpProperties;
    }

    public Mono<String> importFromErp(String reportName, boolean dryRun) {
        log.info("ERP ingest started (report={}, dryRun={})", reportName, dryRun);
        return erpReportClient.login(erpProperties)
            .flatMap(loginResp -> erpReportClient.fetchReport(erpProperties, reportName))
            .flatMap(rawReport -> {
                JsonNode normalized;
                try {
                    normalized = erpReportClient.normalize(rawReport).path("rows");
                } catch (Exception e) {
                    return Mono.error(e);
                }
                List<Product> products = erpProductMapper.mapRows(normalized);
                log.info("ERP ingest mapped {} products", products.size());
                if (dryRun) {
                    return Mono.just("Dry run: mapped " + products.size() + " products");
                }
                return productsUpdateService.upsertProducts(products, ProductsUpdateService.UpdateMode.SKIP_PRODUCT_MASTER_DATA)
                    .thenReturn("Ingested " + products.size() + " products");
            });
    }
}
