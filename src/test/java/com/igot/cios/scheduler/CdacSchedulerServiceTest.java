package com.igot.cios.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cios.dto.SBApiResponse;
import com.igot.cios.exception.CiosContentException;
import com.igot.cios.kafka.KafkaProducer;
import com.igot.cios.plugins.DataTransformUtility;
import com.igot.cios.util.CbServerProperties;
import com.igot.cios.util.Constants;
import com.igot.cios.util.PayloadValidation;
import com.igot.cios.util.transactional.cassandrautils.CassandraOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CdacSchedulerServiceTest {

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private KafkaProducer kafkaProducer;
    @Mock
    private PayloadValidation payloadValidation;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private CbServerProperties cbServerProperties;
    @Mock
    private CassandraOperation cassandraOperation;
    @Mock
    private DataTransformUtility dataTransformUtility;

    @InjectMocks
    private CdacSchedulerService cdacSchedulerService;

    private ObjectMapper realObjectMapper;

    @BeforeEach
    void setUp() {
        realObjectMapper = new ObjectMapper();
    }

    @Test
    void loadCdacEnrollment_jsonProcessingException_returnsFailedResponse() throws JsonProcessingException {
        when(cbServerProperties.getCdacEnrollmentServiceCode()).thenReturn("CDAC_SVC");
        when(cbServerProperties.getCdacApiKey()).thenReturn("key");
        when(cbServerProperties.getCornellDateRange()).thenReturn(7);
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {
        });

        SBApiResponse result = cdacSchedulerService.loadCdacEnrollment();

        assertEquals(Constants.FAILED, result.getParams().getStatus());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getResponseCode());
    }

    @Test
    void loadCdacEnrollment_success_returnsOk() throws Exception {
        when(cbServerProperties.getCdacEnrollmentServiceCode()).thenReturn("CDAC_SVC");
        when(cbServerProperties.getCdacApiKey()).thenReturn("key");
        when(cbServerProperties.getCornellDateRange()).thenReturn(7);
        cbServerProperties.cdacPartnerCode = "cdac";
        when(cbServerProperties.getServiceLocatorHost()).thenReturn("http://localhost");
        when(cbServerProperties.getServiceLocatorFixedUrl()).thenReturn("/svc");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        when(objectMapper.valueToTree(any())).thenReturn(realObjectMapper.createArrayNode());
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(ResponseEntity.ok(new Object()));

        ObjectNode partnerInfo = realObjectMapper.createObjectNode();
        partnerInfo.put("id", "partner-1");
        when(dataTransformUtility.fetchPartnerInfoUsingApi(any())).thenReturn(partnerInfo);

        SBApiResponse result = cdacSchedulerService.loadCdacEnrollment();

        assertEquals(HttpStatus.OK, result.getResponseCode());
    }

    @Test
    void formUrlMapForEnrollment_buildsExpectedKeys() throws Exception {
        when(cbServerProperties.getCornellDateRange()).thenReturn(7);

        Method method = CdacSchedulerService.class.getDeclaredMethod("formUrlMapForEnrollment");
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(cdacSchedulerService);

        assertNotNull(result.get(Constants.FROM_DATE));
        assertNotNull(result.get(Constants.TO_DATE));
    }

    @Test
    void formHeaderMap_includesApiKey() throws Exception {
        when(cbServerProperties.getCdacApiKey()).thenReturn("my-api-key");

        Method method = CdacSchedulerService.class.getDeclaredMethod("formHeaderMap");
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(cdacSchedulerService);

        assertEquals("my-api-key", result.get("API-Key"));
    }

    @Test
    void performEnrollmentCall_failureStatus_throwsException() throws Exception {
        when(cbServerProperties.getServiceLocatorHost()).thenReturn("http://localhost");
        when(cbServerProperties.getServiceLocatorFixedUrl()).thenReturn("/svc");
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        Method method = CdacSchedulerService.class.getDeclaredMethod("performEnrollmentCall", String.class, String.class);
        method.setAccessible(true);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () ->
                method.invoke(cdacSchedulerService, "cdac", "{}"));
        assertTrue(thrown.getCause() instanceof IllegalStateException);
    }

    @Test
    void performEnrollmentCall_emptyResponseBody_logsAndReturnsNoop() throws Exception {
        when(cbServerProperties.getServiceLocatorHost()).thenReturn("http://localhost");
        when(cbServerProperties.getServiceLocatorFixedUrl()).thenReturn("/svc");
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(ResponseEntity.ok(new Object()));
        when(objectMapper.valueToTree(any())).thenReturn(realObjectMapper.missingNode());

        Method method = CdacSchedulerService.class.getDeclaredMethod("performEnrollmentCall", String.class, String.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(cdacSchedulerService, "cdac", "{}"));
        verifyNoInteractions(dataTransformUtility);
    }

    @Test
    void updateDateFormatFromInputString_validInput_formatsAsIso() throws Exception {
        Method method = CdacSchedulerService.class.getDeclaredMethod("updateDateFormatFromInputString", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(cdacSchedulerService, "2024-01-15 10:30:00");

        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
    }

    @Test
    void updateDateFormatFromInputString_invalidInput_throwsIllegalState() throws Exception {
        Method method = CdacSchedulerService.class.getDeclaredMethod("updateDateFormatFromInputString", String.class);
        method.setAccessible(true);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () ->
                method.invoke(cdacSchedulerService, "not-a-date"));
        assertTrue(thrown.getCause() instanceof IllegalStateException);
    }

    @Test
    void callEnrollmentAPI_noExistingEnrollment_pushesToKafka() throws Exception {
        ObjectNode contentData = realObjectMapper.createObjectNode();
        contentData.put(Constants.COURSEID, 123);
        contentData.put(Constants.USER_ID, "user1@example.com");
        contentData.put("completedon", "2024-01-15 10:30:00");

        when(cbServerProperties.getTopic()).thenReturn("topic1");

        ObjectNode ciosResponse = realObjectMapper.createObjectNode();
        ObjectNode content = realObjectMapper.createObjectNode();
        content.put(Constants.CONTENTID, "internal-1");
        ciosResponse.set(Constants.CONTENT, content);
        when(dataTransformUtility.callCiosReadApi(anyString(), anyString())).thenReturn(ciosResponse);

        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), any(), any()))
                .thenReturn(List.of());

        Method method = CdacSchedulerService.class.getDeclaredMethod("callEnrollmentAPI", String.class, String.class, JsonNode.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(cdacSchedulerService, "cdac", "partner-1", contentData));

        verify(kafkaProducer, times(1)).push(anyString(), any());
        verify(payloadValidation, times(1)).validatePayload(anyString(), any());
    }

    @Test
    void callEnrollmentAPI_existingEnrollment_skipsKafkaPush() throws Exception {
        ObjectNode contentData = realObjectMapper.createObjectNode();
        contentData.put(Constants.COURSEID, "ext-course-1");
        contentData.put(Constants.USER_ID, "user1@example.com");

        ObjectNode ciosResponse = realObjectMapper.createObjectNode();
        ObjectNode content = realObjectMapper.createObjectNode();
        content.put(Constants.CONTENTID, "internal-1");
        ciosResponse.set(Constants.CONTENT, content);
        when(dataTransformUtility.callCiosReadApi(anyString(), anyString())).thenReturn(ciosResponse);

        Map<String, Object> enrollmentRecord = new HashMap<>();
        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(enrollmentRecord));

        Method method = CdacSchedulerService.class.getDeclaredMethod("callEnrollmentAPI", String.class, String.class, JsonNode.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(cdacSchedulerService, "cdac", "partner-1", contentData));

        verify(kafkaProducer, never()).push(anyString(), any());
    }

    @Test
    void callEnrollmentAPI_exceptionThrown_wrapsAsCiosContentException() throws Exception {
        ObjectNode contentData = realObjectMapper.createObjectNode();
        contentData.put(Constants.COURSEID, "ext-course-1");

        when(dataTransformUtility.callCiosReadApi(anyString(), anyString())).thenThrow(new RuntimeException("api failed"));

        Method method = CdacSchedulerService.class.getDeclaredMethod("callEnrollmentAPI", String.class, String.class, JsonNode.class);
        method.setAccessible(true);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () ->
                method.invoke(cdacSchedulerService, "cdac", "partner-1", contentData));
        assertTrue(thrown.getCause() instanceof CiosContentException);
    }
}
