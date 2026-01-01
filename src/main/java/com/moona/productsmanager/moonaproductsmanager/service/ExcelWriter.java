package com.moona.productsmanager.moonaproductsmanager.service;

import com.moona.productsmanager.moonaproductsmanager.model.Product;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
public class ExcelWriter {

    public void exportProducts(List<Product> products) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Products");

        List<Object> headers = Arrays.asList(ExcelReader.ID_COLUMN, ExcelReader.SKU_COLUMN, ExcelReader.NAME_COLUMN,
            ExcelReader.COST_PRICE_COLUMN, ExcelReader.PRICE_COLUMN, ExcelReader.CATEGORY_ID_COLUMN,
            ExcelReader.CATEGORY_NAME_COLUMN, ExcelReader.PARENT_CATEGORY_NAME_COLUMN, ExcelReader.RATING_COLUMN,
            ExcelReader.AVAILABLE_QUANTITY_COLUMN, ExcelReader.MIN_AMOUNT_ATTRIBUTE_COLUMN,
            ExcelReader.WEIGHTED_ATTRIBUTE_COLUMN, ExcelReader.WEIGHT_COLUMN, ExcelReader.TRACK_INVENTORY_COLUMN,
            ExcelReader.PUBLISHED_COLUMN, ExcelReader.IMAGE_COLUMN);
        writeRow(headers, sheet.createRow(0));

        int rowIndex = 0;
        for (Product product : products) {
            Row row = sheet.createRow(++rowIndex);
            writeRow(getProductRecords(product), row);
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = format.format(new Date());
        String fileName = "MoonaProducts-" + dateString + ".xlsx";

        try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
            workbook.write(outputStream);
        }
    }

    public void exportRefillProducts(String fileName, List<Product> products) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("ProductsRefill");

        List<Object> headers = Arrays.asList(ExcelReader.SKU_COLUMN, ExcelReader.NAME_COLUMN,
            ExcelReader.CATEGORY_NAME_COLUMN, ExcelReader.AVAILABLE_QUANTITY_COLUMN,
            ExcelReader.NEEDED_QUANTITY_COLUMN, ExcelReader.COST_PRICE_COLUMN);
        writeRow(headers, sheet.createRow(0));

        int rowIndex = 0;
        for (Product product : products) {
            Row row = sheet.createRow(++rowIndex);
            writeRow(getRefillProductRecords(product), row);
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = format.format(new Date());
        if (fileName == null) {
            fileName = "MoonaProducts-" + dateString;
        }
        fileName += ".xlsx";

        try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
            workbook.write(outputStream);
        }
    }

    private List<Object> getProductRecords(Product product) {
        List<Object> records = new ArrayList<>();
        records.add(product.getId());
        records.add(product.getSku());
        records.add(product.getName());
        records.add(product.getCostPrice());
        records.add(product.getPrice());
        records.add(product.getCategoryId());
        records.add(product.getCategoryName());
        records.add(product.getParentCategoryName());
        records.add(product.getRating());
        records.add(product.getAvailableQuantity());
        records.add(product.getMinAmount());
        records.add(product.getWeighted());
        records.add(product.getWeight());
        records.add(product.getTrackInventory());
        records.add(product.getPublished());
        records.add(product.getImageUrl());
        return records;
    }

    private List<Object> getRefillProductRecords(Product product) {
        List<Object> records = new ArrayList<>();
        records.add(product.getSku());
        records.add(product.getName());
        records.add(product.getCategoryName());
        records.add(product.getAvailableQuantity());
        records.add(product.getNeededQuantity());
        records.add(product.getCostPrice());
        return records;
    }

    private void writeRow(List<Object> records, Row row) {
        for (int colIndex = 0; colIndex < records.size(); colIndex++) {
            Object record = records.get(colIndex);
            if (record == null) {
                continue;
            }
            Cell cell = row.createCell(colIndex);
            if (record instanceof String) {
                cell.setCellValue((String) record);
            } else if (record instanceof Integer) {
                cell.setCellValue((Integer) record);
            } else if (record instanceof Double) {
                cell.setCellValue((Double) record);
            } else if (record instanceof Boolean) {
                cell.setCellValue((Boolean) record);
            }
        }
    }
}

