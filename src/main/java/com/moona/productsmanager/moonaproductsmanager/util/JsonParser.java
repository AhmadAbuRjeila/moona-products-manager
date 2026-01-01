package com.moona.productsmanager.moonaproductsmanager.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;

public class JsonParser {
    private JsonParser() {
    }

    public static StringBuilder getErrorsAsString(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        ArrayNode errors = (ArrayNode) root.path("errors");
        StringBuilder errorsStr = new StringBuilder();
        errors.forEach(error -> errorsStr
            .append(error.path("field").asText())
            .append(",")
            .append(error.path("code").asText())
            .append(",")
            .append(error.path("message").asText())
            .append("\n"));
        return errorsStr;
    }
}

