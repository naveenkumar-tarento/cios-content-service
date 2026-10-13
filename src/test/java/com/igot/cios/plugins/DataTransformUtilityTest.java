package com.igot.cios.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cios.dto.LogStatus;
import com.igot.cios.entity.CornellContentEntity;
import com.igot.cios.entity.FileInfoEntity;
import com.igot.cios.exception.CiosContentException;
import com.igot.cios.repository.CornellContentRepository;
import com.igot.cios.repository.FileInfoRepository;
import com.igot.cios.util.CbServerProperties;
import com.igot.cios.util.Constants;
import com.igot.cios.util.elasticsearch.service.EsUtilService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Timestamp;
import java.util.*;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataTransformUtilityTest {

    @InjectMocks
    private DataTransformUtility dataTransformUtility;

    private ObjectMapper objectMapper;

    @Mock
    private CbServerProperties cbServerProperties;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FileInfoRepository fileInfoRepository;

    @Mock
    private EsUtilService esUtilService;

    @Mock
    private CornellContentRepository cornellContentRepository;

    @Mock
    private LogStatus logStatus;


    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(dataTransformUtility, "objectMapper", objectMapper);
    }

    // ---------------- getAdminAccessToken ----------------
    @Test
    void getAdminAccessToken_success() {
        cbServerProperties.keycloakUrl = "http://keycloak";
        cbServerProperties.ssoUsername = "admin";
        cbServerProperties.ssoPassword = "password";

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "test-token");

        ResponseEntity<Map<String, Object>> response =
                new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        doReturn(response)
                .when(restTemplate)
                .exchange(
                        anyString(),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
                );

        String token = dataTransformUtility.getAdminAccessToken();

        assertEquals("test-token", token);
    }


    @Test
    void getAdminAccessToken_failure() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenThrow(new RuntimeException("KC down"));

        assertThrows(
                CiosContentException.class,
                () -> dataTransformUtility.getAdminAccessToken()
        );
    }

    @Test
    void createFileInfo_success() {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        when(fileInfoRepository.save(any(FileInfoEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String fileId = dataTransformUtility.createFileInfo(
                "partner1",
                null,
                "file.csv",
                now,
                now,
                "SUCCESS",
                "gcp.csv",
                "content.csv"
        );

        assertNotNull(fileId);
        verify(fileInfoRepository).save(any(FileInfoEntity.class));
    }

    @Test
    void updateDateFormatFromTimestampForCoursera_success() {
        long timestamp = 1700000000000L;

        String result =
                dataTransformUtility.updateDateFormatFromTimestampForCoursera(timestamp);

        assertNotNull(result);
        assertTrue(result.contains("-"));
    }

    @Test
    void getExistingMappers_success() throws Exception {
        cbServerProperties.keycloakUrl = "http://keycloak";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseNode = mapper.readTree("""
            [
              {"name": "username", "id": "id-1"},
              {"name": "email", "id": "id-2"}
            ]
            """);

        ResponseEntity<JsonNode> response =
                new ResponseEntity<>(responseNode, HttpStatus.OK);
        doReturn(response)
                .when(restTemplate)
                .exchange(
                        anyString(),
                        eq(HttpMethod.GET),
                        any(HttpEntity.class),
                        eq(JsonNode.class)
                );

        Map<String, String> result =
                dataTransformUtility.getExistingMappers("token", "client-id");

        assertEquals(2, result.size());
        assertEquals("id-1", result.get("username"));
        assertEquals("id-2", result.get("email"));
    }

    @Test
    void updateProtocolMapper_success() {
        cbServerProperties.keycloakUrl = "http://keycloak";

        Map<String, Object> mapperPayload = new HashMap<>();
        mapperPayload.put("id", "mapper-id");
        mapperPayload.put("name", "firstname");

        doReturn(ResponseEntity.noContent().build())
                .when(restTemplate)
                .exchange(
                        anyString(),
                        eq(HttpMethod.PUT),
                        any(HttpEntity.class),
                        eq(Void.class)
                );
        assertDoesNotThrow(() ->
                dataTransformUtility.updateProtocolMapper(
                        "token",
                        "client-id",
                        mapperPayload
                )
        );
    }

    @Test
    void createProtocolMapper_success() {
        cbServerProperties.keycloakUrl = "http://keycloak";

        Map<String, Object> mapperPayload = new HashMap<>();
        mapperPayload.put("name", "email-mapper");
        mapperPayload.put("protocol", "saml");
        mapperPayload.put("protocolMapper", "saml-user-property-mapper");

        doReturn(new ResponseEntity<>("mapper-id", HttpStatus.CREATED))
                .when(restTemplate)
                .postForEntity(
                        anyString(),
                        any(HttpEntity.class),
                        eq(String.class)
                );

        assertDoesNotThrow(() ->
                dataTransformUtility.createProtocolMapper(
                        "token",
                        "client-id",
                        mapperPayload
                )
        );
        verify(restTemplate, times(1)).postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void createProtocolMapper_failure_httpError() {
        cbServerProperties.keycloakUrl = "http://keycloak";

        Map<String, Object> mapperPayload = new HashMap<>();
        mapperPayload.put("name", "email-mapper");

        doThrow(new org.springframework.web.client.HttpClientErrorException(
                HttpStatus.NOT_FOUND,
                "404 Not Found: {\"error\":\"Could not find client\"}"
        )).when(restTemplate)
                .postForEntity(
                        anyString(),
                        any(HttpEntity.class),
                        eq(String.class)
                );

        assertThrows(
                CiosContentException.class,
                () -> dataTransformUtility.createProtocolMapper(
                        "token",
                        "invalid-client-id",
                        mapperPayload
                )
        );
    }

    @Test
    void createProtocolMapper_failure_generalException() {
        cbServerProperties.keycloakUrl = "http://keycloak";

        Map<String, Object> mapperPayload = new HashMap<>();
        mapperPayload.put("name", "email-mapper");

        doThrow(new RuntimeException("Connection timeout"))
                .when(restTemplate)
                .postForEntity(
                        anyString(),
                        any(HttpEntity.class),
                        eq(String.class)
                );

        assertThrows(
                CiosContentException.class,
                () -> dataTransformUtility.createProtocolMapper(
                        "token",
                        "client-id",
                        mapperPayload
                )
        );
    }

    @Test
    void dataBulkSave_success_withMultipleEntities() {
        String partnerCode = "TEST_PARTNER";
        String partnerId = "partner-123";
        List<CornellContentEntity> entityList = new ArrayList<>();

        CornellContentEntity entity1 = createMockContentEntity("ext-001", partnerId, partnerCode);
        CornellContentEntity entity2 = createMockContentEntity("ext-002", partnerId, partnerCode);
        entityList.add(entity1);
        entityList.add(entity2);

        when(cornellContentRepository.saveAll(anyList())).thenReturn(entityList);
        when(cornellContentRepository.countByPartnerCode(partnerCode)).thenReturn(2L);
        when(esUtilService.addDocument(anyString(), anyString(), anyMap(), anyString())).thenReturn("doc-id");
        when(cbServerProperties.getElasticCiosContentJsonPath()).thenReturn("/path/to/mapping.json");

        JsonNode partnerResponse = createMockPartnerInfoResponse(partnerId);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(ResponseEntity.ok(createPartnerApiResponse(partnerResponse)));

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("success"));

        
        ReflectionTestUtils.invokeMethod(dataTransformUtility, "dataBulkSave", entityList, partnerCode);

       
        verify(cornellContentRepository, times(1)).saveAll(entityList);
        verify(esUtilService, times(2)).addDocument(
            eq(Constants.CIOS_CONTENT_INDEX_NAME),
            anyString(),
            anyMap(),
            anyString()
        );
        verify(cornellContentRepository, times(1)).countByPartnerCode(partnerCode);
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void dataBulkSave_success_withSingleEntity() {
        String partnerCode = "TEST_PARTNER";
        String partnerId = "partner-123";
        List<CornellContentEntity> entityList = new ArrayList<>();

        CornellContentEntity entity = createMockContentEntity("ext-001", partnerId, partnerCode);
        entityList.add(entity);

        when(cornellContentRepository.saveAll(anyList())).thenReturn(entityList);
        when(cornellContentRepository.countByPartnerCode(partnerCode)).thenReturn(1L);
        when(esUtilService.addDocument(anyString(), anyString(), anyMap(), anyString())).thenReturn("doc-id");
        when(cbServerProperties.getElasticCiosContentJsonPath()).thenReturn("/path/to/mapping.json");

        JsonNode partnerResponse = createMockPartnerInfoResponse(partnerId);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(ResponseEntity.ok(createPartnerApiResponse(partnerResponse)));

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("success"));

        
        ReflectionTestUtils.invokeMethod(dataTransformUtility, "dataBulkSave", entityList, partnerCode);

       
        verify(cornellContentRepository, times(1)).saveAll(entityList);
        verify(esUtilService, times(1)).addDocument(
            eq(Constants.CIOS_CONTENT_INDEX_NAME),
            eq(partnerCode + "_ext-001"),
            anyMap(),
            anyString()
        );
        verify(cornellContentRepository, times(1)).countByPartnerCode(partnerCode);
    }

    @Test
    void dataBulkSave_success_withEmptyList() {
        String partnerCode = "TEST_PARTNER";
        List<CornellContentEntity> entityList = new ArrayList<>();

        when(cornellContentRepository.saveAll(anyList())).thenReturn(entityList);
        when(cornellContentRepository.countByPartnerCode(partnerCode)).thenReturn(0L);

        JsonNode partnerResponse = createMockPartnerInfoResponse("partner-123");
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(ResponseEntity.ok(createPartnerApiResponse(partnerResponse)));

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("success"));

        
        ReflectionTestUtils.invokeMethod(dataTransformUtility, "dataBulkSave", entityList, partnerCode);

       
        verify(cornellContentRepository, times(1)).saveAll(entityList);
        verify(esUtilService, never()).addDocument(anyString(), anyString(), anyMap(), anyString());
        verify(cornellContentRepository, times(1)).countByPartnerCode(partnerCode);
    }

    @Test
    void dataBulkSave_handlesElasticsearchException() {
        
        String partnerCode = "TEST_PARTNER";
        String partnerId = "partner-123";
        List<CornellContentEntity> entityList = new ArrayList<>();

        CornellContentEntity entity1 = createMockContentEntity("ext-001", partnerId, partnerCode);
        CornellContentEntity entity2 = createMockContentEntity("ext-002", partnerId, partnerCode);
        entityList.add(entity1);
        entityList.add(entity2);

        when(cornellContentRepository.saveAll(anyList())).thenReturn(entityList);
        when(cornellContentRepository.countByPartnerCode(partnerCode)).thenReturn(2L);
        when(cbServerProperties.getElasticCiosContentJsonPath()).thenReturn("/path/to/mapping.json");

        // First call succeeds, second call fails
        when(esUtilService.addDocument(anyString(), anyString(), anyMap(), anyString()))
            .thenReturn("doc-id")
            .thenThrow(new RuntimeException("ES connection error"));

        JsonNode partnerResponse = createMockPartnerInfoResponse(partnerId);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(ResponseEntity.ok(createPartnerApiResponse(partnerResponse)));

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("success"));

        assertDoesNotThrow(() ->
            ReflectionTestUtils.invokeMethod(dataTransformUtility, "dataBulkSave", entityList, partnerCode)
        );

       
        verify(cornellContentRepository, times(1)).saveAll(entityList);
        verify(esUtilService, times(2)).addDocument(anyString(), anyString(), anyMap(), anyString());
        verify(cornellContentRepository, times(1)).countByPartnerCode(partnerCode);
    }

    @Test
    void dataBulkSave_verifyUniqueIdFormat() {
        
        String partnerCode = "TEST_PARTNER";
        String partnerId = "partner-123";
        String externalId = "ext-001";
        List<CornellContentEntity> entityList = new ArrayList<>();

        CornellContentEntity entity = createMockContentEntity(externalId, partnerId, partnerCode);
        entityList.add(entity);

        when(cornellContentRepository.saveAll(anyList())).thenReturn(entityList);
        when(cornellContentRepository.countByPartnerCode(partnerCode)).thenReturn(1L);
        when(esUtilService.addDocument(anyString(), anyString(), anyMap(), anyString())).thenReturn("doc-id");
        when(cbServerProperties.getElasticCiosContentJsonPath()).thenReturn("/path/to/mapping.json");

        JsonNode partnerResponse = createMockPartnerInfoResponse(partnerId);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(ResponseEntity.ok(createPartnerApiResponse(partnerResponse)));

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("success"));

        
        ReflectionTestUtils.invokeMethod(dataTransformUtility, "dataBulkSave", entityList, partnerCode);


        String expectedUniqueId = partnerCode + "_" + externalId;
        verify(esUtilService, times(1)).addDocument(
            eq(Constants.CIOS_CONTENT_INDEX_NAME),
            eq(expectedUniqueId),
            anyMap(),
            anyString()
        );
    }

    @Test
    void dataBulkSave_updatesTotalCourseCount() {
        
        String partnerCode = "TEST_PARTNER";
        String partnerId = "partner-123";
        Long expectedCourseCount = 5L;
        List<CornellContentEntity> entityList = new ArrayList<>();

        CornellContentEntity entity = createMockContentEntity("ext-001", partnerId, partnerCode);
        entityList.add(entity);

        when(cornellContentRepository.saveAll(anyList())).thenReturn(entityList);
        when(cornellContentRepository.countByPartnerCode(partnerCode)).thenReturn(expectedCourseCount);
        when(esUtilService.addDocument(anyString(), anyString(), anyMap(), anyString())).thenReturn("doc-id");
        when(cbServerProperties.getElasticCiosContentJsonPath()).thenReturn("/path/to/mapping.json");

        JsonNode partnerResponse = createMockPartnerInfoResponse(partnerId);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(ResponseEntity.ok(createPartnerApiResponse(partnerResponse)));

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("success"));

        
        ReflectionTestUtils.invokeMethod(dataTransformUtility, "dataBulkSave", entityList, partnerCode);

       
        verify(cornellContentRepository, times(1)).countByPartnerCode(partnerCode);

        // Verify postForEntity was called with updated partner data
        verify(restTemplate, times(1)).postForEntity(
            anyString(),
            argThat(httpEntity -> {
                try {
                    JsonNode body = objectMapper.readTree(objectMapper.writeValueAsString(((HttpEntity<?>) httpEntity).getBody()));
                    return body.path(Constants.DATA).get(Constants.TOTAL_COURSES_COUNT).asLong() == expectedCourseCount;
                } catch (Exception e) {
                    return false;
                }
            }),
            eq(String.class)
        );
    }

    @Test
    void dataBulkSave_callsFlattenContentData() {
        
        String partnerCode = "TEST_PARTNER";
        String partnerId = "partner-123";
        List<CornellContentEntity> entityList = new ArrayList<>();

        CornellContentEntity entity = createMockContentEntity("ext-001", partnerId, partnerCode);
        entityList.add(entity);

        when(cornellContentRepository.saveAll(anyList())).thenReturn(entityList);
        when(cornellContentRepository.countByPartnerCode(partnerCode)).thenReturn(1L);
        when(esUtilService.addDocument(anyString(), anyString(), anyMap(), anyString())).thenReturn("doc-id");
        when(cbServerProperties.getElasticCiosContentJsonPath()).thenReturn("/path/to/mapping.json");

        JsonNode partnerResponse = createMockPartnerInfoResponse(partnerId);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(ResponseEntity.ok(createPartnerApiResponse(partnerResponse)));

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("success"));

        
        ReflectionTestUtils.invokeMethod(dataTransformUtility, "dataBulkSave", entityList, partnerCode);

        verify(esUtilService, times(1)).addDocument(
            anyString(),
            anyString(),
            argThat(map -> !map.containsKey(Constants.CIOS_DATA)), // After flattening, CIOS_DATA should be removed
            anyString()
        );
    }

    @Test
    void dataBulkSave_updatesPartnerInfo() {
        
        String partnerCode = "TEST_PARTNER";
        String partnerId = "partner-123";
        List<CornellContentEntity> entityList = new ArrayList<>();

        CornellContentEntity entity = createMockContentEntity("ext-001", partnerId, partnerCode);
        entityList.add(entity);

        when(cornellContentRepository.saveAll(anyList())).thenReturn(entityList);
        when(cornellContentRepository.countByPartnerCode(partnerCode)).thenReturn(1L);
        when(esUtilService.addDocument(anyString(), anyString(), anyMap(), anyString())).thenReturn("doc-id");
        when(cbServerProperties.getElasticCiosContentJsonPath()).thenReturn("/path/to/mapping.json");

        JsonNode partnerResponse = createMockPartnerInfoResponse(partnerId);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(ResponseEntity.ok(createPartnerApiResponse(partnerResponse)));

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("success"));

        
        ReflectionTestUtils.invokeMethod(dataTransformUtility, "dataBulkSave", entityList, partnerCode);

       
        verify(restTemplate, times(1)).postForEntity(
            anyString(),
            any(HttpEntity.class),
            eq(String.class)
        );
    }

    @Test
    void transformData_validSpec_transformsData() {
        Map<String, Object> source = new HashMap<>();
        source.put("name", "test");
        List<Object> spec = List.of(Map.of("operation", "shift", "spec", Map.of("*", "&")));

        JsonNode result = dataTransformUtility.transformData(source, spec);

        assertNotNull(result);
        assertEquals("test", result.get("name").asText());
    }

    @Test
    void processExcelFile_csvFile_parsesRows() {
        String csvContent = "userid,courseid\nuser1,course1\nuser2,course2\n";
        MultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", csvContent.getBytes());

        List<Map<String, String>> rows = dataTransformUtility.processExcelFile(file);

        assertEquals(2, rows.size());
        assertEquals("user1", rows.get(0).get("userid"));
    }

    @Test
    void processExcelFile_csvFile_stopsAtTrailingBlankRow() {
        String csvContent = "userid,courseid\nuser1,course1\n,\n";
        MultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", csvContent.getBytes());

        List<Map<String, String>> rows = dataTransformUtility.processExcelFile(file);

        assertEquals(1, rows.size());
    }

    @Test
    void processExcelFile_xlsxFile_parsesRows() throws Exception {
        byte[] xlsxBytes;
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("userid");
            header.createCell(1).setCellValue("courseid");
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("user1");
            dataRow.createCell(1).setCellValue("course1");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            xlsxBytes = bos.toByteArray();
        }
        MultipartFile file = new MockMultipartFile("file", "data.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBytes);

        List<Map<String, String>> rows = dataTransformUtility.processExcelFile(file);

        assertEquals(1, rows.size());
        assertEquals("user1", rows.get(0).get("userid"));
    }

    @Test
    void processExcelFile_xlsxFile_handlesBlankHeaderDateCellAndStopsAtNullRow() throws Exception {
        byte[] xlsxBytes;
        try (Workbook workbook = new XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.CellStyle dateStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.CreationHelper creationHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("m/d/yy"));

            Sheet sheet = workbook.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("userid");
            // column 1 header intentionally left uncreated (blank) to exercise the skip branch
            header.createCell(2).setCellValue("courseid");
            header.createCell(3).setCellValue("completedon");

            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("user1");
            // column 2 value intentionally left uncreated (blank) to exercise the empty-cell branch
            org.apache.poi.ss.usermodel.Cell dateCell = dataRow.createCell(3);
            dateCell.setCellValue(new java.util.Date(1700000000000L));
            dateCell.setCellStyle(dateStyle);

            // row 2 intentionally left uncreated (null) to exercise the stop-at-null-row branch
            sheet.createRow(3);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            xlsxBytes = bos.toByteArray();
        }
        MultipartFile file = new MockMultipartFile("file", "data.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBytes);

        List<Map<String, String>> rows = dataTransformUtility.processExcelFile(file);

        assertEquals(1, rows.size());
        assertEquals("user1", rows.get(0).get("userid"));
        assertTrue(rows.get(0).get("completedon").endsWith("Z"));
    }

    @Test
    void processExcelFile_unsupportedFileType_throwsIllegalStateException() {
        MultipartFile file = new MockMultipartFile("file", "data.txt", "text/plain", "data".getBytes());

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> dataTransformUtility.processExcelFile(file));
        assertTrue(thrown.getMessage().contains("Unsupported file type"));
    }

    @Test
    void processExcelFile_nullFileName_throwsIllegalStateException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> dataTransformUtility.processExcelFile(file));
    }

    @Test
    void updatingPartnerInfo_success_returnsBody() {
        when(cbServerProperties.getCbPoresbaseUrl()).thenReturn("http://cb");
        when(cbServerProperties.getPartnerCreateEndPoint()).thenReturn("/partner/create");
        JsonNode payload = objectMapper.createObjectNode().put("name", "partner1");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("updated"));

        String result = dataTransformUtility.updatingPartnerInfo(payload);

        assertEquals("updated", result);
    }

    @Test
    void updatingPartnerInfo_failureStatus_throwsCiosContentException() {
        when(cbServerProperties.getCbPoresbaseUrl()).thenReturn("http://cb");
        when(cbServerProperties.getPartnerCreateEndPoint()).thenReturn("/partner/create");
        JsonNode payload = objectMapper.createObjectNode();
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        assertThrows(CiosContentException.class, () -> dataTransformUtility.updatingPartnerInfo(payload));
    }

    @Test
    void fetchResultUsingGet_success_returnsBody() {
        Map<String, Object> body = Map.of("result", Map.of("id", "p1"));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body));

        Object result = dataTransformUtility.fetchResultUsingGet("http://api", new HashMap<>());

        assertEquals(body, result);
    }

    @Test
    void fetchResultUsingGet_httpClientErrorException_parsesErrorBody() {
        HttpClientErrorException ex = HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found",
                HttpHeaders.EMPTY, "{\"error\":\"missing\"}".getBytes(), null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(ex);

        Object result = dataTransformUtility.fetchResultUsingGet("http://api", Map.of("Authorization", "token"));

        assertNotNull(result);
    }

    @Test
    void fetchResultUsingGet_genericException_returnsNull() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("connection refused"));

        Object result = dataTransformUtility.fetchResultUsingGet("http://api", null);

        assertNull(result);
    }

    @Test
    void fetchPartnerInfoUsingApi_success_returnsResultNode() {
        when(cbServerProperties.getCbPoresbaseUrl()).thenReturn("http://cb");
        when(cbServerProperties.getPartnerReadEndPoint()).thenReturn("/partner/read/");
        Map<String, Object> resultMap = Map.of("id", "partner1");
        Map<String, Object> body = new HashMap<>();
        body.put("result", resultMap);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body));

        JsonNode result = dataTransformUtility.fetchPartnerInfoUsingApi("partner1");

        assertEquals("partner1", result.get("id").asText());
    }

    @Test
    void fetchPartnerInfoUsingApi_nullResponse_throwsIllegalStateException() {
        when(cbServerProperties.getCbPoresbaseUrl()).thenReturn("http://cb");
        when(cbServerProperties.getPartnerReadEndPoint()).thenReturn("/partner/read/");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("down"));

        assertThrows(IllegalStateException.class, () -> dataTransformUtility.fetchPartnerInfoUsingApi("partner1"));
    }

    @Test
    void validateRowData_validArray_returnsEmptyErrorList() {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("userid", "user1");
        row.put("courseid", "course1");
        com.fasterxml.jackson.databind.node.ArrayNode array = objectMapper.createArrayNode();
        array.add(row);

        List<String> errors = dataTransformUtility.validateRowData(Constants.PROGRESS_DATA_VALIDATION_FILE, array);

        assertTrue(errors.isEmpty());
    }

    @Test
    void validateRowData_invalidObject_returnsErrors() {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("userid", 123);

        List<String> errors = dataTransformUtility.validateRowData(Constants.PROGRESS_DATA_VALIDATION_FILE, row);

        assertFalse(errors.isEmpty());
    }

    @Test
    void saveOrUpdateCornellContent_newContent_createsEntity() {
        when(cornellContentRepository.findByExternalIdAndPartnerId("ext1", "partner1")).thenReturn(Optional.empty());
        JsonNode transformData = objectMapper.createObjectNode();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        CornellContentEntity result = dataTransformUtility.saveOrUpdateCornellContent(
                "ext1", transformData, transformData, now, "file1", "partner1", "partnerCode1");

        assertEquals("ext1", result.getExternalId());
        assertFalse(result.getIsActive());
    }

    @Test
    void saveOrUpdateCornellContent_existingNotInitiated_updatesEntity() {
        CornellContentEntity existing = createMockContentEntity("ext1", "partner1", "partnerCode1");
        when(cornellContentRepository.findByExternalIdAndPartnerId("ext1", "partner1")).thenReturn(Optional.of(existing));
        ObjectNode transformData = objectMapper.createObjectNode();
        ObjectNode content = objectMapper.createObjectNode();
        content.put("name", "Updated");
        transformData.set(Constants.CONTENT, content);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        CornellContentEntity result = dataTransformUtility.saveOrUpdateCornellContent(
                "ext1", transformData, transformData, now, "file1", "partner1", "partnerCode1");

        assertEquals(transformData, result.getCiosData());
    }

    @Test
    void saveOrUpdateCornellContent_existingWithLiveStatus_stillUpdatesDueToJsonNodeEqualsStringComparison() {
        // getCiosData().get(CONTENT).get("status").equals("live") compares a JsonNode to a String
        // and is always false, so the "skip update for live/draft" branch is unreachable today -
        // this documents existing behavior as-is rather than the apparently intended design.
        CornellContentEntity existing = new CornellContentEntity();
        existing.setExternalId("ext1");
        ObjectNode ciosData = objectMapper.createObjectNode();
        ObjectNode content = objectMapper.createObjectNode();
        content.put("status", "live");
        ciosData.set(Constants.CONTENT, content);
        existing.setCiosData(ciosData);
        when(cornellContentRepository.findByExternalIdAndPartnerId("ext1", "partner1")).thenReturn(Optional.of(existing));

        ObjectNode newTransformData = objectMapper.createObjectNode();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        CornellContentEntity result = dataTransformUtility.saveOrUpdateCornellContent(
                "ext1", newTransformData, newTransformData, now, "file1", "partner1", "partnerCode1");

        assertEquals(newTransformData, result.getCiosData());
    }

    @Test
    void callCiosReadApi_success_returnsBody() {
        when(cbServerProperties.getCbPoresbaseUrl()).thenReturn("http://cb");
        when(cbServerProperties.getFixedUrl()).thenReturn("/fixed/");
        Map<String, Object> body = Map.of("content", "data");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(ResponseEntity.ok(body));

        JsonNode result = dataTransformUtility.callCiosReadApi("course1", "partner1");

        assertNotNull(result);
    }

    @Test
    void callCiosReadApi_failureStatus_throwsCiosContentException() {
        when(cbServerProperties.getCbPoresbaseUrl()).thenReturn("http://cb");
        when(cbServerProperties.getFixedUrl()).thenReturn("/fixed/");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

        assertThrows(CiosContentException.class, () -> dataTransformUtility.callCiosReadApi("course1", "partner1"));
    }

    @Test
    void callCiosReadApi_exceptionThrown_wrapsAsCiosContentException() {
        when(cbServerProperties.getCbPoresbaseUrl()).thenReturn("http://cb");
        when(cbServerProperties.getFixedUrl()).thenReturn("/fixed/");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenThrow(new RuntimeException("down"));

        assertThrows(CiosContentException.class, () -> dataTransformUtility.callCiosReadApi("course1", "partner1"));
    }

    @Test
    void processRowsAndCreateLogs_withErrorMessage_writesErrorLog() throws Exception {
        JsonNode partnerResponse = createMockPartnerInfoResponse("partner1");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(createPartnerApiResponse(partnerResponse)));
        when(logStatus.isHasFailures()).thenReturn(true);
        when(logStatus.getErrorLogs()).thenReturn(new ArrayList<>());

        Map<String, Object> result = dataTransformUtility.processRowsAndCreateLogs(
                List.of(Map.of("col1", "val1")), "file1", "file.xlsx", "partner1", "some error", "partner1");

        assertTrue((boolean) result.get(Constants.HAS_FAILURES));
        assertNotNull(result.get(Constants.LOG_FILE));
        verify(logStatus, atLeastOnce()).clearLogs();
        verify(logStatus).setHasFailures(true);
    }

    @Test
    void processRowsAndCreateLogs_missingTransformSpec_writesErrorLog() throws Exception {
        JsonNode partnerResponse = createMockPartnerInfoResponse("partner1");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(createPartnerApiResponse(partnerResponse)));
        when(logStatus.isHasFailures()).thenReturn(true);
        when(logStatus.getErrorLogs()).thenReturn(new ArrayList<>());

        Map<String, Object> result = dataTransformUtility.processRowsAndCreateLogs(
                List.of(Map.of("col1", "val1")), "file1", "file.xlsx", "partner1", null, "partner1");

        assertTrue((boolean) result.get(Constants.HAS_FAILURES));
    }

    @Test
    void updateProcessedDataInDb_validRow_successfullyProcessesAndSavesContent() throws Exception {
        List<Map<String, String>> transformData = new ArrayList<>();
        Map<String, String> row = new HashMap<>();
        row.put("name", "Course One");
        row.put("description", "A great course");
        row.put("duration", "60 mins");
        row.put("redirectUrl", "http://redirect");
        row.put("appIcon", "http://icon");
        row.put("externalId", "ext-course-1");
        row.put("objectives", "learn stuff");
        transformData.add(row);

        Map<String, Object> shiftSpec = new HashMap<>();
        shiftSpec.put("name", "content.name");
        shiftSpec.put("description", "content.description");
        shiftSpec.put("duration", "content.duration");
        shiftSpec.put("redirectUrl", "content.redirectUrl");
        shiftSpec.put("appIcon", "content.appIcon");
        shiftSpec.put("externalId", "content.externalId");
        shiftSpec.put("objectives", "content.objectives");
        List<Object> spec = List.of(Map.of("operation", "shift", "spec", shiftSpec));

        when(cornellContentRepository.findByExternalIdAndPartnerId("ext-course-1", "partner1")).thenReturn(Optional.empty());
        when(cornellContentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(esUtilService.addDocument(anyString(), anyString(), anyMap(), anyString())).thenReturn("doc-id");
        when(cbServerProperties.getElasticCiosContentJsonPath()).thenReturn("/path/to/mapping.json");
        when(cornellContentRepository.countByPartnerCode("partner1")).thenReturn(1L);

        JsonNode partnerResponse = createMockPartnerInfoResponse("partner1");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(createPartnerApiResponse(partnerResponse)));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("updated"));

        Map<String, Object> result = dataTransformUtility.updateProcessedDataInDb(
                transformData, "partner1", "file.xlsx", "file-1", "partner1", spec);

        assertFalse((boolean) result.get(Constants.HAS_FAILURES));
        File logFile = (File) result.get(Constants.LOG_FILE);
        assertNotNull(logFile);
        assertTrue(logFile.exists());
        logFile.delete();
    }

    @Test
    void updateProcessedDataInDb_invalidRow_marksHasFailuresTrue() throws Exception {
        List<Map<String, String>> transformData = new ArrayList<>();
        Map<String, String> row = new HashMap<>();
        row.put("name", "incomplete");
        transformData.add(row);

        List<Object> spec = List.of(Map.of("operation", "shift", "spec", Map.of("name", "content.name")));

        JsonNode partnerResponse = createMockPartnerInfoResponse("partner1");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(createPartnerApiResponse(partnerResponse)));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("updated"));
        when(logStatus.isHasFailures()).thenReturn(true);
        when(logStatus.getErrorLogs()).thenReturn(new ArrayList<>());
        when(logStatus.getSuccessLogs()).thenReturn(new ArrayList<>());

        Map<String, Object> result = dataTransformUtility.updateProcessedDataInDb(
                transformData, "partner1", "file.xlsx", "file-1", "partner1", spec);

        assertTrue((boolean) result.get(Constants.HAS_FAILURES));
        verify(logStatus).setHasFailures(true);
    }

    @Test
    void updateProcessedDataInDb_invalidTransformSpec_wrapsExceptionAsCiosContentException() {
        List<Map<String, String>> transformData = List.of(Map.of("name", "test"));
        List<Object> invalidSpec = List.of("not-a-valid-spec-entry");

        assertThrows(CiosContentException.class, () -> dataTransformUtility.updateProcessedDataInDb(
                transformData, "partner1", "file.xlsx", "file1", "partner1", invalidSpec));
    }

    @Test
    void writeLogsToFile_withLogs_writesCorrectCsv() throws Exception {
        LinkedHashMap<String, String> log1 = new LinkedHashMap<>();
        log1.put("name", "test, with comma");
        log1.put("status", "success");

        File logFile = dataTransformUtility.writeLogsToFile(List.of(log1), "test-log-" + System.currentTimeMillis());

        assertTrue(logFile.exists());
        String content = java.nio.file.Files.readString(logFile.toPath());
        assertTrue(content.contains("\"test, with comma\""));
        logFile.delete();
    }

    @Test
    void writeLogsToFile_emptyLogs_createsFileWithNoRows() throws Exception {
        File logFile = dataTransformUtility.writeLogsToFile(Collections.emptyList(), "empty-log-" + System.currentTimeMillis());

        assertTrue(logFile.exists());
        logFile.delete();
    }

    @Test
    void getSsoConfigurationFromKeycloak_success_returnsBody() {
        cbServerProperties.keycloakUrl = "http://keycloak";
        when(cbServerProperties.getSsoConfigReadApi()).thenReturn("/clients/");
        ObjectNode responseBody = objectMapper.createObjectNode().put("clientId", "client1");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        JsonNode result = dataTransformUtility.getSsoConfigurationFromKeycloak("token1", "client1");

        assertEquals("client1", result.get("clientId").asText());
    }

    @Test
    void getSsoConfigurationFromKeycloak_failureStatus_throwsCiosContentException() {
        cbServerProperties.keycloakUrl = "http://keycloak";
        when(cbServerProperties.getSsoConfigReadApi()).thenReturn("/clients/");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

        assertThrows(CiosContentException.class,
                () -> dataTransformUtility.getSsoConfigurationFromKeycloak("token1", "client1"));
    }

    @Test
    void inflateAndDecode_validDeflatedBase64_returnsOriginalString() throws Exception {
        String original = "<samlp:Request>test</samlp:Request>";
        Deflater deflater = new Deflater(Deflater.DEFLATED, true);
        deflater.setInput(original.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        deflater.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            bos.write(buffer, 0, count);
        }
        deflater.end();
        String encoded = Base64.getEncoder().encodeToString(bos.toByteArray());

        String result = dataTransformUtility.inflateAndDecode(encoded);

        assertEquals(original, result);
    }

    @Test
    void inflateAndDecode_invalidBase64_throwsCiosContentException() {
        assertThrows(CiosContentException.class, () -> dataTransformUtility.inflateAndDecode("not-valid-base64!!"));
    }

    @Test
    void createSsoConfiguration_success_returnsIdFromLocationHeader() {
        cbServerProperties.keycloakUrl = "http://keycloak";
        cbServerProperties.ssoConfigCreateApi = "/clients";
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, "http://keycloak/clients/abc-123");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(headers, HttpStatus.CREATED));

        String result = dataTransformUtility.createSsoConfiguration("token1", Map.of("clientId", "client1"));

        assertEquals("abc-123", result);
    }

    @Test
    void createSsoConfiguration_noLocationHeader_returnsNull() {
        cbServerProperties.keycloakUrl = "http://keycloak";
        cbServerProperties.ssoConfigCreateApi = "/clients";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        String result = dataTransformUtility.createSsoConfiguration("token1", Map.of("clientId", "client1"));

        assertNull(result);
    }

    @Test
    void createSsoConfiguration_exceptionThrown_wrapsAsCiosContentException() {
        cbServerProperties.keycloakUrl = "http://keycloak";
        cbServerProperties.ssoConfigCreateApi = "/clients";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RuntimeException("down"));

        assertThrows(CiosContentException.class,
                () -> dataTransformUtility.createSsoConfiguration("token1", Map.of()));
    }

    @Test
    void updateSsoConfiguration_success_doesNotThrow() {
        cbServerProperties.keycloakUrl = "http://keycloak";
        cbServerProperties.ssoConfigCreateApi = "/clients";
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "client1");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        assertDoesNotThrow(() -> dataTransformUtility.updateSsoConfiguration("token1", "client-id-1", payload));
    }

    @Test
    void updateSsoConfiguration_exceptionThrown_wrapsAsCiosContentException() {
        cbServerProperties.keycloakUrl = "http://keycloak";
        cbServerProperties.ssoConfigCreateApi = "/clients";
        Map<String, Object> payload = new HashMap<>();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RuntimeException("down"));

        assertThrows(CiosContentException.class,
                () -> dataTransformUtility.updateSsoConfiguration("token1", "client-id-1", payload));
    }

    private CornellContentEntity createMockContentEntity(String externalId, String partnerId, String partnerCode) {
        CornellContentEntity entity = new CornellContentEntity();
        entity.setExternalId(externalId);
        entity.setPartnerId(partnerId);
        entity.setPartnerCode(partnerCode);
        entity.setIsActive(false);
        entity.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        entity.setUpdatedDate(new Timestamp(System.currentTimeMillis()));

        ObjectNode ciosData = objectMapper.createObjectNode();
        ObjectNode content = objectMapper.createObjectNode();
        content.put("name", "Test Content " + externalId);
        content.put("status", Constants.NOT_INITIATED);
        ciosData.set(Constants.CONTENT, content);
        entity.setCiosData(ciosData);

        return entity;
    }

    private JsonNode createMockPartnerInfoResponse(String partnerId) {
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode data = objectMapper.createObjectNode();
        data.put("id", partnerId);
        data.put("partnerName", "Test Partner");
        data.put(Constants.TOTAL_COURSES_COUNT, 0);
        response.set(Constants.DATA, data);
        return response;
    }

    private Map<String, Object> createPartnerApiResponse(JsonNode partnerData) {
        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put(Constants.RESULT, objectMapper.convertValue(partnerData, Map.class));
        return apiResponse;
    }
}
