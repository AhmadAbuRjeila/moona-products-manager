package com.moona.productsmanager.moonaproductsmanager.service;

import com.moona.productsmanager.moonaproductsmanager.model.Product;
import com.moona.productsmanager.moonaproductsmanager.model.ProductRefill;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
public class ExcelReader {

    public static final String ID_COLUMN = "ID";
    public static final String SKU_COLUMN = "SKU";
    public static final String NAME_COLUMN = "NAME";
    public static final String PRICE_COLUMN = "PRICE";
    public static final String COST_PRICE_COLUMN = "COST_PRICE";
    public static final String AVAILABLE_QUANTITY_COLUMN = "AVAILABLE_QUANTITY";
    public static final String NEEDED_QUANTITY_COLUMN = "NEEDED_QUANTITY";
    public static final String IMAGE_COLUMN = "IMAGE";
    public static final String CATEGORY_ID_COLUMN = "CATEGORY_ID";
    public static final String CATEGORY_NAME_COLUMN = "CATEGORY";
    public static final String PARENT_CATEGORY_NAME_COLUMN = "PARENT_CATEGORY";
    public static final String MIN_AMOUNT_ATTRIBUTE_COLUMN = "MIN_AMOUNT";
    public static final String WEIGHTED_ATTRIBUTE_COLUMN = "WEIGHTED";
    public static final String BOYCOTT_ATTRIBUTE_COLUMN = "BOYCOTT";
    public static final String WEIGHT_COLUMN = "WEIGHT";
    public static final String TRACK_INVENTORY_COLUMN = "TRACK_INVENTORY";
    public static final String PUBLISHED_COLUMN = "PUBLISHED";
    public static final String CHANNEL_ID_COLUMN = "CHANNEL_ID";
    public static final String WAREHOUSE_ID_COLUMN = "WAREHOUSE_ID";
    public static final String RATING_COLUMN = "RATING";
    public static final String BOX_SIZE_COLUMN = "BOX_SIZE";
    public static final String ITEM_BARCODE = "ITEM_BARCODE";
    public static final String MIN_ORDER_QUANTITY_COLUMN = "MIN_ORDER_QUANTITY";
    public static final String PROVIDER_COLUMN = "PROVIDER";
    public static final String PROVIDER1_COLUMN = "PROVIDER1";
    public static final String PROVIDER2_COLUMN = "PROVIDER2";
    public static final String PROVIDER3_COLUMN = "PROVIDER3";
    public static final String MIN_STOCK_QUANTITY_COLUMN = "MIN_STOCK_QUANTITY";
    public static final String SKU_PREFIX = "_SKU_";

    public List<Product> readProducts(String fileName) throws IOException {
        List<Product> products = new ArrayList<>();
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet datatypeSheet = workbook.getSheetAt(0);

            Map<String, Integer> columnToIndexMap = mapHeaderColumns(datatypeSheet.getRow(0));

            Integer idColumnIndex = columnToIndexMap.get(ID_COLUMN);
            Integer skuColumnIndex = columnToIndexMap.get(SKU_COLUMN);
            Integer nameColumnIndex = columnToIndexMap.get(NAME_COLUMN);
            Integer priceColumnIndex = columnToIndexMap.get(PRICE_COLUMN);
            Integer costPriceColumnIndex = columnToIndexMap.get(COST_PRICE_COLUMN);
            Integer availableQuantityColumnIndex = columnToIndexMap.get(AVAILABLE_QUANTITY_COLUMN);
            Integer categoryIdColumnIndex = columnToIndexMap.get(CATEGORY_ID_COLUMN);
            Integer minAmountColumnIndex = columnToIndexMap.get(MIN_AMOUNT_ATTRIBUTE_COLUMN);
            Integer weightedColumnIndex = columnToIndexMap.get(WEIGHTED_ATTRIBUTE_COLUMN);
            Integer boycottColumnIndex = columnToIndexMap.get(BOYCOTT_ATTRIBUTE_COLUMN);
            Integer weightColumnIndex = columnToIndexMap.get(WEIGHT_COLUMN);
            Integer trackInventoryColumnIndex = columnToIndexMap.get(TRACK_INVENTORY_COLUMN);
            Integer publishedColumnIndex = columnToIndexMap.get(PUBLISHED_COLUMN);
            Integer channelIdColumnIndex = columnToIndexMap.get(CHANNEL_ID_COLUMN);
            Integer warehouseIdColumnIndex = columnToIndexMap.get(WAREHOUSE_ID_COLUMN);
            Integer ratingColumnIndex = columnToIndexMap.get(RATING_COLUMN);
            Integer boxItemBarcodeColumnIndex = columnToIndexMap.get(ITEM_BARCODE);
            Integer boxSizeColumnIndex = columnToIndexMap.get(BOX_SIZE_COLUMN);
            Integer minOrderQuantityColumnIndex = columnToIndexMap.get(MIN_ORDER_QUANTITY_COLUMN);
            Integer providerColumnIndex = columnToIndexMap.get(PROVIDER_COLUMN);
            Integer provider1Index = columnToIndexMap.get(PROVIDER1_COLUMN);
            Integer provider2Index = columnToIndexMap.get(PROVIDER2_COLUMN);
            Integer provider3Index = columnToIndexMap.get(PROVIDER3_COLUMN);
            Integer minStockQuantityColumnIndex = columnToIndexMap.get(MIN_STOCK_QUANTITY_COLUMN);

            for (int rowIndex = 1; rowIndex <= datatypeSheet.getLastRowNum(); rowIndex++) {
                Row row = datatypeSheet.getRow(rowIndex);
                if (row == null) {
                    break;
                }

                Integer keyColumnIndex = idColumnIndex != null ? idColumnIndex : skuColumnIndex;
                Cell keyCell = row.getCell(keyColumnIndex);
                String keyColumnValue = keyCell != null && !keyCell.getCellType().equals(CellType.BLANK)
                    ? keyCell.getStringCellValue() : null;
                if (keyColumnValue == null || keyColumnValue.isEmpty()) {
                    break;
                }

                Product product = new Product();
                if (idColumnIndex != null) {
                    product.setId(getStringCell(row.getCell(idColumnIndex)));
                }
                if (skuColumnIndex != null) {
                    product.setSku(parseBarcode(getStringCell(row.getCell(skuColumnIndex))));
                }
                if (nameColumnIndex != null) {
                    product.setName(getStringCell(row.getCell(nameColumnIndex)));
                }
                if (priceColumnIndex != null) {
                    product.setPrice(getNumericCell(row.getCell(priceColumnIndex)));
                }
                if (costPriceColumnIndex != null) {
                    product.setCostPrice(getNumericCell(row.getCell(costPriceColumnIndex)));
                }
                if (availableQuantityColumnIndex != null) {
                    product.setAvailableQuantity(getIntegerCell(row.getCell(availableQuantityColumnIndex)));
                }
                if (categoryIdColumnIndex != null) {
                    product.setCategoryId(getStringCell(row.getCell(categoryIdColumnIndex)));
                }
                if (trackInventoryColumnIndex != null) {
                    product.setTrackInventory(getBooleanCell(row.getCell(trackInventoryColumnIndex)));
                }
                if (publishedColumnIndex != null) {
                    product.setPublished(getBooleanCell(row.getCell(publishedColumnIndex)));
                }
                if (channelIdColumnIndex != null) {
                    product.setChannelId(getStringCell(row.getCell(channelIdColumnIndex)));
                }
                if (warehouseIdColumnIndex != null) {
                    product.setWarehouseId(getStringCell(row.getCell(warehouseIdColumnIndex)));
                }
                if (minAmountColumnIndex != null) {
                    product.setMinAmount(getStringCell(row.getCell(minAmountColumnIndex)));
                }
                if (weightedColumnIndex != null) {
                    product.setWeighted(getStringCell(row.getCell(weightedColumnIndex)));
                }
                if (boycottColumnIndex != null) {
                    product.setBoycott(getBooleanCell(row.getCell(boycottColumnIndex)));
                }
                if (weightColumnIndex != null) {
                    product.setWeight(getNumericCell(row.getCell(weightColumnIndex)));
                }
                if (ratingColumnIndex != null) {
                    product.setRating(getNumericCell(row.getCell(ratingColumnIndex)));
                }
                if (boxItemBarcodeColumnIndex != null) {
                    product.setBoxItemBarcode(parseBarcode(getStringCell(row.getCell(boxItemBarcodeColumnIndex))));
                }
                if (boxSizeColumnIndex != null) {
                    product.setBoxSize(getIntegerCell(row.getCell(boxSizeColumnIndex)));
                }
                if (minOrderQuantityColumnIndex != null) {
                    product.setMinOrderQuantity(getIntegerCell(row.getCell(minOrderQuantityColumnIndex)));
                }
                if (providerColumnIndex != null) {
                    product.setProvider(getStringCell(row.getCell(providerColumnIndex)));
                }
                if (provider1Index != null) {
                    product.addProvider(getStringCell(row.getCell(provider1Index)));
                }
                if (provider2Index != null) {
                    product.addProvider(getStringCell(row.getCell(provider2Index)));
                }
                if (provider3Index != null) {
                    product.addProvider(getStringCell(row.getCell(provider3Index)));
                }
                if (minStockQuantityColumnIndex != null) {
                    product.setMinStockQuantity(getIntegerCell(row.getCell(minStockQuantityColumnIndex)));
                }

                products.add(product);
            }
        }
        return products;
    }

    public List<ProductRefill> readProductsRefill(String fileName) throws IOException {
        List<ProductRefill> productsRefill = new ArrayList<>();
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet datatypeSheet = workbook.getSheetAt(0);

            Map<String, Integer> columnToIndexMap = mapHeaderColumns(datatypeSheet.getRow(0));

            Integer skuColumnIndex = columnToIndexMap.get(SKU_COLUMN);
            Integer nameColumnIndex = columnToIndexMap.get(NAME_COLUMN);
            Integer costPriceColumnIndex = columnToIndexMap.get(COST_PRICE_COLUMN);
            Integer availableQuantityColumnIndex = columnToIndexMap.get(AVAILABLE_QUANTITY_COLUMN);
            Integer neededQuantityColumnIndex = columnToIndexMap.get(NEEDED_QUANTITY_COLUMN);

            for (int rowIndex = 1; rowIndex <= datatypeSheet.getLastRowNum(); rowIndex++) {
                Row row = datatypeSheet.getRow(rowIndex);
                if (row == null) {
                    break;
                }

                Cell keyCell = row.getCell(skuColumnIndex);
                String keyColumnValue = keyCell != null && !keyCell.getCellType().equals(CellType.BLANK)
                    ? keyCell.getStringCellValue() : null;
                if (keyColumnValue == null || keyColumnValue.isEmpty()) {
                    break;
                }

                ProductRefill productRefill = new ProductRefill();
                productRefill.setSku(parseBarcode(getStringCell(row.getCell(skuColumnIndex))));
                if (nameColumnIndex != null) {
                    productRefill.setName(getStringCell(row.getCell(nameColumnIndex)));
                }
                if (costPriceColumnIndex != null) {
                    productRefill.setCostPrice(getNumericCell(row.getCell(costPriceColumnIndex)));
                }
                if (availableQuantityColumnIndex != null) {
                    productRefill.setAvailableQuantity(getIntegerCell(row.getCell(availableQuantityColumnIndex)));
                }
                if (neededQuantityColumnIndex != null) {
                    productRefill.setNeededQuantity(getIntegerCell(row.getCell(neededQuantityColumnIndex)));
                }

                productsRefill.add(productRefill);
            }
        }
        return productsRefill;
    }

    private Map<String, Integer> mapHeaderColumns(Row headersRow) {
        Map<String, Integer> columnToIndexMap = new HashMap<>();
        for (int headerCellIndex = 0; headerCellIndex <= headersRow.getLastCellNum(); headerCellIndex++) {
            Cell headerCell = headersRow.getCell(headerCellIndex);
            if (headerCell == null) {
                break;
            }
            String columnTitle = headerCell.getStringCellValue().toUpperCase(Locale.ROOT);
            columnToIndexMap.put(columnTitle, headerCellIndex);
        }
        return columnToIndexMap;
    }

    private String parseBarcode(String sku) {
        return sku == null ? null : sku.replaceFirst(SKU_PREFIX, "");
    }

    private String getStringCell(Cell cell) {
        if (cell == null || cell.getCellType().equals(CellType.BLANK)) {
            return null;
        }
        if (cell.getCellType().equals(CellType.NUMERIC)) {
            return Integer.toString((int) cell.getNumericCellValue());
        }
        return cell.getStringCellValue();
    }

    private Double getNumericCell(Cell cell) {
        if (cell == null || cell.getCellType().equals(CellType.BLANK)) {
            return null;
        }
        if (cell.getCellType().equals(CellType.STRING)) {
            String value = cell.getStringCellValue();
            if (value == null || value.isBlank()) {
                return null;
            }
            return Double.parseDouble(value);
        }
        return cell.getNumericCellValue();
    }

    private Integer getIntegerCell(Cell cell) {
        Double val = getNumericCell(cell);
        return val == null ? null : val.intValue();
    }

    private Boolean getBooleanCell(Cell cell) {
        if (cell == null || cell.getCellType().equals(CellType.BLANK)) {
            return null;
        }
        if (cell.getCellType().equals(CellType.BOOLEAN)) {
            return cell.getBooleanCellValue();
        }
        if (cell.getCellType().equals(CellType.STRING)) {
            String value = cell.getStringCellValue();
            if (value == null || value.isBlank()) {
                return null;
            }
            return Boolean.parseBoolean(value);
        }
        return null;
    }
}

