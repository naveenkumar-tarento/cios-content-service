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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseraSchedulerServiceTest {

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
    private CourseraSchedulerService courseraSchedulerService;

    private ObjectMapper realObjectMapper;

    @BeforeEach
    void setUp() {
        realObjectMapper = new ObjectMapper();
    }

    @Test
    void loadCourseraEnrollment_jsonProcessingException_returnsFailedResponse() throws JsonProcessingException {
        when(cbServerProperties.getCourseraEnrollmentListLimit()).thenReturn(50);
        when(objectMapper.createArrayNode()).thenReturn(realObjectMapper.createArrayNode());
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {
        });

        SBApiResponse result = courseraSchedulerService.loadCourseraEnrollment();

        assertEquals(Constants.FAILED, result.getParams().getStatus());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getResponseCode());
    }

    @Test
    void loadCourseraEnrollment_success_singleIteration() throws Exception {
        when(cbServerProperties.getCourseraEnrollmentListLimit()).thenReturn(50);
        when(cbServerProperties.getServiceLocatorHost()).thenReturn("http://localhost");
        when(cbServerProperties.getServiceLocatorFixedUrl()).thenReturn("/svc");
        when(objectMapper.createArrayNode()).thenReturn(realObjectMapper.createArrayNode());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        ObjectNode apiResponseBody = realObjectMapper.createObjectNode();
        apiResponseBody.put("count", 0);
        apiResponseBody.putArray("data");
        when(objectMapper.valueToTree(any())).thenReturn(apiResponseBody);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(ResponseEntity.ok(new Object()));

        ObjectNode partnerInfo = realObjectMapper.createObjectNode();
        partnerInfo.put("id", "partner-1");
        when(dataTransformUtility.fetchPartnerInfoUsingApi(any())).thenReturn(partnerInfo);

        SBApiResponse result = courseraSchedulerService.loadCourseraEnrollment();

        assertEquals(HttpStatus.OK, result.getResponseCode());
    }

    @Test
    void formUrlMapForEnrollment_buildsExpectedKeys() throws Exception {
        when(cbServerProperties.getCourseraDateRange()).thenReturn(30L);
        when(cbServerProperties.getCourseraDateBefore()).thenReturn("before");
        when(cbServerProperties.getCourseraDateAfter()).thenReturn("after");

        Method method = CourseraSchedulerService.class.getDeclaredMethod("formUrlMapForEnrollment", int.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(courseraSchedulerService, 0, 50);

        assertEquals("50", result.get("limit"));
        assertEquals("0", result.get("start"));
        assertNotNull(result.get("before"));
        assertNotNull(result.get("after"));
    }

    @Test
    void performEnrollmentCall_failureStatus_throwsException() throws Exception {
        when(cbServerProperties.getServiceLocatorHost()).thenReturn("http://localhost");
        when(cbServerProperties.getServiceLocatorFixedUrl()).thenReturn("/svc");
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        Method method = CourseraSchedulerService.class.getDeclaredMethod("performEnrollmentCall", String.class, String.class);
        method.setAccessible(true);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () ->
                method.invoke(courseraSchedulerService, "coursera", "{}"));
        assertTrue(thrown.getCause() instanceof IllegalStateException);
    }

    @Test
    void performEnrollmentCall_missingResponseBody_logsAndReturnsNull() throws Exception {
        when(cbServerProperties.getServiceLocatorHost()).thenReturn("http://localhost");
        when(cbServerProperties.getServiceLocatorFixedUrl()).thenReturn("/svc");
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(ResponseEntity.ok(new Object()));
        when(objectMapper.valueToTree(any())).thenReturn(realObjectMapper.missingNode());

        Method method = CourseraSchedulerService.class.getDeclaredMethod("performEnrollmentCall", String.class, String.class);
        method.setAccessible(true);

        Object result = method.invoke(courseraSchedulerService, "coursera", "{}");

        assertNull(result);
    }

    @Test
    void callEnrollmentAPI_matchingAndNotCompleted_pushesToKafka() throws Exception {
        ObjectNode contentData = realObjectMapper.createObjectNode();
        contentData.put("contentType", "online");
        contentData.put("isCompleted", true);
        contentData.put("courseid", "ext-course-1");
        contentData.put(Constants.USER_ID, "user1@example.com");
        contentData.put(Constants.COMPLETED_ON, "1706359845000");

        when(cbServerProperties.getCourseraEnrollmentListCourseType()).thenReturn("online");
        when(cbServerProperties.getTopic()).thenReturn("topic1");

        ObjectNode ciosResponse = realObjectMapper.createObjectNode();
        ObjectNode content = realObjectMapper.createObjectNode();
        content.put("contentId", "internal-1");
        ciosResponse.set("content", content);
        when(dataTransformUtility.callCiosReadApi(anyString(), anyString())).thenReturn(ciosResponse);

        Map<String, Object> enrollmentRecord = new HashMap<>();
        enrollmentRecord.put(Constants.PROGRESS, 50);
        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(enrollmentRecord));
        when(dataTransformUtility.updateDateFormatFromTimestampForCoursera(anyLong())).thenReturn("2024-01-01T00:00:00.000Z");

        Method method = CourseraSchedulerService.class.getDeclaredMethod("callEnrollmentAPI", String.class, String.class, JsonNode.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(courseraSchedulerService, "coursera", "partner-1", contentData));

        verify(kafkaProducer, times(1)).push(anyString(), any());
        verify(payloadValidation, times(1)).validatePayload(anyString(), any());
    }

    @Test
    void callEnrollmentAPI_alreadyCompleted_skipsKafkaPush() throws Exception {
        ObjectNode contentData = realObjectMapper.createObjectNode();
        contentData.put("contentType", "online");
        contentData.put("isCompleted", true);
        contentData.put("courseid", "ext-course-1");
        contentData.put(Constants.USER_ID, "user1@example.com");

        when(cbServerProperties.getCourseraEnrollmentListCourseType()).thenReturn("online");

        ObjectNode ciosResponse = realObjectMapper.createObjectNode();
        ObjectNode content = realObjectMapper.createObjectNode();
        content.put("contentId", "internal-1");
        ciosResponse.set("content", content);
        when(dataTransformUtility.callCiosReadApi(anyString(), anyString())).thenReturn(ciosResponse);

        Map<String, Object> enrollmentRecord = new HashMap<>();
        enrollmentRecord.put(Constants.PROGRESS, 100);
        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(enrollmentRecord));

        Method method = CourseraSchedulerService.class.getDeclaredMethod("callEnrollmentAPI", String.class, String.class, JsonNode.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(courseraSchedulerService, "coursera", "partner-1", contentData));

        verify(kafkaProducer, never()).push(anyString(), any());
    }

    @Test
    void callEnrollmentAPI_noEnrollmentRecord_skipsKafkaPush() throws Exception {
        ObjectNode contentData = realObjectMapper.createObjectNode();
        contentData.put("contentType", "online");
        contentData.put("isCompleted", true);
        contentData.put("courseid", "ext-course-1");
        contentData.put(Constants.USER_ID, "user1@example.com");

        when(cbServerProperties.getCourseraEnrollmentListCourseType()).thenReturn("online");

        ObjectNode ciosResponse = realObjectMapper.createObjectNode();
        ObjectNode content = realObjectMapper.createObjectNode();
        content.put("contentId", "internal-1");
        ciosResponse.set("content", content);
        when(dataTransformUtility.callCiosReadApi(anyString(), anyString())).thenReturn(ciosResponse);

        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), any(), any()))
                .thenReturn(new ArrayList<>());

        Method method = CourseraSchedulerService.class.getDeclaredMethod("callEnrollmentAPI", String.class, String.class, JsonNode.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(courseraSchedulerService, "coursera", "partner-1", contentData));

        verify(kafkaProducer, never()).push(anyString(), any());
    }

    @Test
    void callEnrollmentAPI_contentTypeMismatch_skipsEntirely() throws Exception {
        ObjectNode contentData = realObjectMapper.createObjectNode();
        contentData.put("contentType", "offline");
        contentData.put("isCompleted", true);

        when(cbServerProperties.getCourseraEnrollmentListCourseType()).thenReturn("online");

        Method method = CourseraSchedulerService.class.getDeclaredMethod("callEnrollmentAPI", String.class, String.class, JsonNode.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(courseraSchedulerService, "coursera", "partner-1", contentData));

        verifyNoInteractions(dataTransformUtility);
        verify(kafkaProducer, never()).push(anyString(), any());
    }

    @Test
    void callEnrollmentAPI_exceptionThrown_wrapsAsCiosContentException() throws Exception {
        ObjectNode contentData = realObjectMapper.createObjectNode();
        contentData.put("contentType", "online");
        contentData.put("isCompleted", true);
        contentData.put("courseid", "ext-course-1");

        when(cbServerProperties.getCourseraEnrollmentListCourseType()).thenReturn("online");
        when(dataTransformUtility.callCiosReadApi(anyString(), anyString())).thenThrow(new RuntimeException("api failed"));

        Method method = CourseraSchedulerService.class.getDeclaredMethod("callEnrollmentAPI", String.class, String.class, JsonNode.class);
        method.setAccessible(true);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () ->
                method.invoke(courseraSchedulerService, "coursera", "partner-1", contentData));
        assertTrue(thrown.getCause() instanceof CiosContentException);
    }
}
