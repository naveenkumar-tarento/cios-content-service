package com.igot.cios.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cios.exception.CiosContentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayloadValidationTest {

    private final PayloadValidation payloadValidation = new PayloadValidation();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validatePayload_validObject_doesNotThrow() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("userid", "user1");
        payload.put("courseid", "course1");
        payload.put("completedon", "2024-01-01T00:00:00.000Z");
        payload.put("status", "success");

        assertDoesNotThrow(() -> payloadValidation.validatePayload(Constants.PROGRESS_DATA_VALIDATION_FILE, payload));
    }

    @Test
    void validatePayload_invalidObject_throwsCiosContentException() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("userid", 12345);

        CiosContentException thrown = assertThrows(CiosContentException.class, () ->
                payloadValidation.validatePayload(Constants.PROGRESS_DATA_VALIDATION_FILE, payload));
        assertDoesNotThrow(thrown::getMessage);
    }

    @Test
    void validatePayload_validArray_doesNotThrow() {
        ArrayNode arrayPayload = objectMapper.createArrayNode();
        ObjectNode item = objectMapper.createObjectNode();
        item.put("userid", "user1");
        item.put("courseid", "course1");
        arrayPayload.add(item);

        assertDoesNotThrow(() -> payloadValidation.validatePayload(Constants.PROGRESS_DATA_VALIDATION_FILE, arrayPayload));
    }

    @Test
    void validatePayload_invalidArray_throwsCiosContentException() {
        ArrayNode arrayPayload = objectMapper.createArrayNode();
        ObjectNode item = objectMapper.createObjectNode();
        item.put("userid", 999);
        arrayPayload.add(item);

        assertThrows(CiosContentException.class, () ->
                payloadValidation.validatePayload(Constants.PROGRESS_DATA_VALIDATION_FILE, arrayPayload));
    }

    @Test
    void validatePayload_unresolvableSchemaPath_wrapsAsCiosContentException() {
        JsonNode payload = objectMapper.createObjectNode();
        assertThrows(CiosContentException.class, () ->
                payloadValidation.validatePayload("/non-existent-schema.json", payload));
    }
}
