package com.transferwise.idempotence4j.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * SHA-256 fingerprint of a request: HTTP method, path, request parameters and deserialized request bodies.
 * Bodies are serialized with sorted properties, so formatting and field order of the original payload do not matter.
 */
public class RequestFingerprint {
    private static final ObjectMapper CANONICAL_MAPPER = JsonMapper.builder()
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .findAndAddModules()
        .build();

    public static String of(HttpServletRequest request, List<Object> bodies) {
        Map<String, List<String>> parameters = new TreeMap<>();
        request.getParameterMap().forEach((name, values) -> parameters.put(name, Arrays.asList(values)));

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("method", request.getMethod());
        content.put("path", request.getRequestURI());
        content.put("parameters", parameters);
        content.put("bodies", bodies);

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(CANONICAL_MAPPER.writeValueAsBytes(content));
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize request body for fingerprinting", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
