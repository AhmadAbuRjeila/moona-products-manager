package com.moona.productsmanager.moonaproductsmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moona.productsmanager.moonaproductsmanager.model.CategoryExportRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoriesExportService {

    private static final Logger log = LoggerFactory.getLogger(CategoriesExportService.class);

    private static final String CATEGORY_QUERY = "query categories($first: Int = 100, $after: String) {\n" +
            "  categories(first: $first, after: $after) {\n" +
            "    edges {\n" +
            "      node {\n" +
            "        id\n" +
            "        name\n" +
            "        parent {\n" +
            "          id\n" +
            "          name\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "    pageInfo {\n" +
            "      hasNextPage\n" +
            "      endCursor\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private final ApiClient apiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CategoriesExportService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public Mono<String> exportCategoriesToFile() {
        log.info("Starting categories export");
        return fetchAllCategories()
                .flatMap(rows -> {
                    try {
                        writeExcel(rows);
                        log.info("Exported {} category rows", rows.size());
                        return Mono.just("Exported " + rows.size() + " category rows");
                    } catch (IOException e) {
                        log.error("Failed to write categories export file", e);
                        return Mono.error(e);
                    }
                });
    }

    Mono<List<CategoryExportRow>> fetchAllCategories() {
        return fetchPage(new ArrayList<>(), null);
    }

    private Mono<List<CategoryExportRow>> fetchPage(List<CategoryExportRow> accumulator, String afterCursor) {
        return fetchCategories(afterCursor)
                .flatMap(body -> {
                    JsonNode root;
                    try {
                        root = objectMapper.readTree(body);
                    } catch (IOException e) {
                        return Mono.error(e);
                    }

                    JsonNode categoriesNode = root.path("data").path("categories");
                    JsonNode edges = categoriesNode.path("edges");
                    int pageCount = 0;
                    if (edges.isArray()) {
                        for (JsonNode edge : edges) {
                            JsonNode node = edge.path("node");
                            CategoryExportRow row = toRow(node);
                            if (row != null) {
                                accumulator.add(row);
                                pageCount++;
                            }
                        }
                    }
                    log.info("Fetched {} categories this page (total rows={})", pageCount, accumulator.size());

                    boolean hasNextPage = categoriesNode.path("pageInfo").path("hasNextPage").asBoolean(false);
                    String endCursor = categoriesNode.path("pageInfo").path("endCursor").asText(null);
                    if (hasNextPage && endCursor != null && !endCursor.isBlank()) {
                        return fetchPage(accumulator, endCursor);
                    }
                    log.info("No more category pages; total rows={}", accumulator.size());
                    return Mono.just(accumulator);
                });
    }

    private CategoryExportRow toRow(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        String id = node.path("id").asText(null);
        String name = node.path("name").asText(null);
        JsonNode parent = node.path("parent");
        if (parent.isMissingNode() || parent.isNull()) {
            return CategoryExportRow.parentOnly(id, name);
        }
        String parentId = parent.path("id").asText(null);
        String parentName = parent.path("name").asText(null);
        return CategoryExportRow.parentAndChild(parentId, parentName, id, name);
    }

    private Mono<String> fetchCategories(String after) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("first", 100);
        variables.put("after", after);
        return apiClient.mutation(CATEGORY_QUERY, variables);
    }

    private void writeExcel(List<CategoryExportRow> rows) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Categories");

        List<Object> headers = List.of(
                "Parent Category ID",
                "Parent Category Name",
                "Child Category ID",
                "Child Category Name"
        );
        writeRow(headers, sheet.createRow(0));

        int rowIndex = 0;
        for (CategoryExportRow rowData : rows) {
            Row row = sheet.createRow(++rowIndex);
            writeRow(Arrays.asList(
                    rowData.getParentId(),
                    rowData.getParentName(),
                    rowData.getChildId(),
                    rowData.getChildName()
            ), row);
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = format.format(new Date());
        String fileName = "MoonaCategories-" + dateString + ".xlsx";
        try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
            workbook.write(outputStream);
        }
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
            }
        }
    }
}
