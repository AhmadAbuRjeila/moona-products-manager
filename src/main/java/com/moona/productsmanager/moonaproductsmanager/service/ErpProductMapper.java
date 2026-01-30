package com.moona.productsmanager.moonaproductsmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moona.productsmanager.moonaproductsmanager.config.ErpProperties;
import com.moona.productsmanager.moonaproductsmanager.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ErpProductMapper {
    private static final Logger log = LoggerFactory.getLogger(ErpProductMapper.class);

    private final ErpProperties erpProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ErpProductMapper(ErpProperties erpProperties) {
        this.erpProperties = erpProperties;
    }

    public List<Product> mapRows(JsonNode normalizedRows) {
        List<Product> products = new ArrayList<>();
        if (normalizedRows == null || !normalizedRows.isArray()) {
            return products;
        }
        for (JsonNode rowNode : normalizedRows) {
            if (rowNode.isArray()) {
                Product p = mapArrayRow(rowNode);
                if (p != null) {
                    products.add(p);
                }
                continue;
            }
            LinkedHashMap<String, Object> row = objectMapper.convertValue(rowNode, LinkedHashMap.class);

            String name = coerceString(findValue(row, "item name"));
            if (name != null && name.trim().equals("عربي بكيت ونستون")) {
                log.info("Skipping (blocked name): {} | sku={} ", name, coerceString(findValue(row, "barcode")));
                continue;
            }

            Integer qty = coerceInteger(firstNonNull(
                findValue(row, "qty"),
                findValue(row, "quantity"),
                findValue(row, "Qty")
            ));

            Double price = coerceDouble(findValue(row, "standard selling price"));
            if (price == null) {
                log.info("Skipping (no price): {} | sku={}", coerceString(findValue(row, "item name")), coerceString(findValue(row, "barcode")));
                continue;
            }
            if (price < 1.0d) {
                log.info("Adjusting qty to zero because price<1: {} | sku={} | price={}", name, coerceString(findValue(row, "barcode")), price);
                qty = 0;
            }

            String category = coerceString(findValue(row, "item group"));
            if (category != null && category.trim().equals("دخان و معسل")) {
                log.info("Skipping (category 'دخان و معسل'): {} | sku={}", coerceString(findValue(row, "item name")), coerceString(findValue(row, "barcode")));
                continue;
            }

            String barcode = coerceString(findValue(row, "barcode"));
            if (barcode == null || barcode.isEmpty()) {
                log.info("Skipping (no barcode): {}", name);
                continue;
            }

            Product p = new Product();
            p.setName(name);
            p.setCategoryName(category);
            p.setCategoryId(erpProperties.getDefaultCategoryId());
            p.setSku(barcode);
            p.setPrice(price);
            p.setCostPrice(price);
            p.setAvailableQuantity(qty);
            p.setTrackInventory(erpProperties.isDefaultTrackInventory());
            p.setWeighted(erpProperties.getDefaultWeighted());
            p.setMinAmount(erpProperties.getDefaultMinAmount());
            p.setChannelId(erpProperties.getDefaultChannelId());
            p.setWarehouseId(erpProperties.getDefaultWarehouseId());
            p.setPublished(erpProperties.isDefaultPublished());
            products.add(p);
        }
        return products;
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private Object findValue(Map<String, Object> row, String headerName) {
        if (headerName == null) {
            return null;
        }
        String target = headerName.toLowerCase(Locale.ROOT).trim();
        for (String key : row.keySet()) {
            if (key != null && key.toLowerCase(Locale.ROOT).trim().equals(target)) {
                return row.get(key);
            }
        }
        return null;
    }

    private String coerceString(Object v) {
        return v == null ? null : String.valueOf(v).trim();
    }

    private Double coerceDouble(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer coerceInteger(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Product mapArrayRow(JsonNode array) {
        // Positions shifted by +1: [2]=Name, [4]=Barcode, [6]=Price, [7]=Quantity, [3]=Category (optional)
        String name = getStringAt(array, 2);
        String barcode = getStringAt(array, 4);
        Double price = getDoubleAt(array, 6);
        Integer qty = getIntegerAt(array, 7);
        String category = getStringAt(array, 3); // optional

        if (name != null && name.trim().equals("عربي بكيت ونستون")) {
            log.info("Skipping (blocked name array row): {} | sku={}", name, barcode);
            return null;
        }

        if (price == null) {
            log.info("Skipping (no price array row): {} | sku={}", name, barcode);
            return null;
        }
        if (price < 1.0d) {
            log.info("Adjusting qty to zero because price<1 array row: {} | sku={} | price={}", name, barcode, price);
            qty = 0;
        }
        if (barcode == null || barcode.isBlank()) {
            log.info("Skipping (no barcode array row): {}", name);
            return null;
        }

        Product p = new Product();
        p.setName(name != null && !name.isBlank() ? name : barcode);
        p.setCategoryName(category);
        p.setCategoryId(erpProperties.getDefaultCategoryId());
        p.setSku(barcode);
        p.setPrice(price);
        p.setCostPrice(price);
        p.setAvailableQuantity(qty);
        p.setTrackInventory(erpProperties.isDefaultTrackInventory());
        p.setWeighted(erpProperties.getDefaultWeighted());
        p.setMinAmount(erpProperties.getDefaultMinAmount());
        p.setChannelId(erpProperties.getDefaultChannelId());
        p.setWarehouseId(erpProperties.getDefaultWarehouseId());
        p.setPublished(erpProperties.isDefaultPublished());
        return p;
    }

    private String getStringAt(JsonNode array, int indexOneBased) {
        int idx = indexOneBased - 1;
        if (array == null || idx < 0 || idx >= array.size()) {
            return null;
        }
        return coerceStringNode(array.get(idx));
    }

    private Double getDoubleAt(JsonNode array, int indexOneBased) {
        int idx = indexOneBased - 1;
        if (array == null || idx < 0 || idx >= array.size()) {
            return null;
        }
        return coerceDoubleNode(array.get(idx));
    }

    private Integer getIntegerAt(JsonNode array, int indexOneBased) {
        Double val = getDoubleAt(array, indexOneBased);
        return val == null ? null : val.intValue();
    }

    private String coerceStringNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText().trim();
        }
        return node.toString().trim();
    }

    private Double coerceDoubleNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.doubleValue();
        }
        try {
            return Double.parseDouble(node.asText().trim());
        } catch (Exception ex) {
            return null;
        }
    }
}
