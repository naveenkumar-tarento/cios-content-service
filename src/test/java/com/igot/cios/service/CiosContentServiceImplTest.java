package com.igot.cios.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cios.dto.DeleteContentRequestDto;
import com.igot.cios.dto.RequestDto;
import com.igot.cios.dto.SBApiResponse;
import com.igot.cios.entity.CornellContentEntity;
import com.igot.cios.entity.FileInfoEntity;
import com.igot.cios.exception.CiosContentException;
import com.igot.cios.kafka.KafkaProducer;
import com.igot.cios.plugins.DataTransformUtility;
import com.igot.cios.repository.CornellContentRepository;
import com.igot.cios.repository.FileInfoRepository;
import com.igot.cios.service.impl.CiosContentServiceImpl;
import com.igot.cios.storage.StoreFileToGCP;
import com.igot.cios.util.CbServerProperties;
import com.igot.cios.util.Constants;
import com.igot.cios.util.PayloadValidation;
import com.igot.cios.util.elasticsearch.dto.SearchCriteria;
import com.igot.cios.util.elasticsearch.dto.SearchResult;
import com.igot.cios.util.elasticsearch.service.EsUtilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CiosContentServiceImplTest {

    @Mock
    private CornellContentRepository repository;

    @Mock
    private EsUtilService esUtilService;

    @Mock
    private DataTransformUtility dataTransformUtility;

    @Mock
    private CbServerProperties cbServerProperties;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private PayloadValidation payloadValidation;

    @Mock
    private FileInfoRepository fileInfoRepository;

    @Mock
    private StoreFileToGCP storeFileToGCP;

    @InjectMocks
    private CiosContentServiceImpl ciosContentService;

    private ObjectMapper realObjectMapper;
    private DeleteContentRequestDto deleteContentRequestDto;
    private String partnerCode;
    private String partnerId;
    private List<String> externalIds;

    @BeforeEach
    void setUp() {
        realObjectMapper = new ObjectMapper();
        partnerCode = "TEST_PARTNER";
        externalIds = Arrays.asList("ext-001", "ext-002");

        deleteContentRequestDto = new DeleteContentRequestDto();
        deleteContentRequestDto.setPartnerCode(partnerCode);
        deleteContentRequestDto.setExternalId(externalIds);

        // Set the real ObjectMapper for the service
        ReflectionTestUtils.setField(ciosContentService, "objectMapper", realObjectMapper);
    }

    // -------------------- DELETE CONTENT TEST CASES --------------------

    @Test
    void deleteNotPublishContent_success_multipleValidEntities() {
        // Arrange
        List<CornellContentEntity> entities = new ArrayList<>();
        CornellContentEntity entity1 = createMockEntity("ext-001", false, Constants.NOT_INITIATED);
        CornellContentEntity entity2 = createMockEntity("ext-002", false, Constants.DRAFT);
        entities.add(entity1);
        entities.add(entity2);

        when(repository.findByExternalIdInAndPartnerCode(externalIds, partnerCode)).thenReturn(entities);
        doNothing().when(repository).delete(any(CornellContentEntity.class));
        doNothing().when(esUtilService).deleteDocument(anyString(), anyString());
        when(repository.countByPartnerCode(partnerCode)).thenReturn(20L);

        JsonNode partnerResponse = createMockPartnerResponse();
        when(dataTransformUtility.fetchPartnerInfoUsingApi(partnerCode)).thenReturn(partnerResponse);
        when(dataTransformUtility.updatingPartnerInfo(any(JsonNode.class))).thenReturn("success");

        // Act
        ResponseEntity<?> response = ciosContentService.deleteNotPublishContent(deleteContentRequestDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        SBApiResponse apiResponse = (SBApiResponse) response.getBody();
        assertEquals(Constants.SUCCESS, apiResponse.getResult().get(Constants.STATUS));

        verify(repository, times(2)).delete(any(CornellContentEntity.class));
        verify(esUtilService, times(2)).deleteDocument(anyString(), eq(Constants.CIOS_CONTENT_INDEX_NAME));
    }

    @Test
    void deleteNotPublishContent_error_entityIsActive() {
        // Arrange
        List<CornellContentEntity> entities = new ArrayList<>();
        CornellContentEntity entity1 = createMockEntity("ext-001", true, Constants.NOT_INITIATED);
        entities.add(entity1);

        when(repository.findByExternalIdInAndPartnerCode(externalIds, partnerCode)).thenReturn(entities);
        when(repository.countByPartnerCode(partnerCode)).thenReturn(10L);

        JsonNode partnerResponse = createMockPartnerResponse();
        when(dataTransformUtility.fetchPartnerInfoUsingApi(partnerCode)).thenReturn(partnerResponse);
        when(dataTransformUtility.updatingPartnerInfo(any(JsonNode.class))).thenReturn("success");

        // Act
        ResponseEntity<?> response = ciosContentService.deleteNotPublishContent(deleteContentRequestDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        SBApiResponse apiResponse = (SBApiResponse) response.getBody();
        assertNotNull(apiResponse);
        assertEquals(Constants.FAILED, apiResponse.getParams().getStatus());
        assertTrue(apiResponse.getParams().getErr().contains("External ID: ext-001 is live, cannot delete."));

        verify(repository, never()).delete(any(CornellContentEntity.class));
        verify(esUtilService, never()).deleteDocument(anyString(), anyString());
    }

    @Test
    void deleteNotPublishContent_error_statusNotDraftOrNotInitiated() {
        // Arrange
        List<CornellContentEntity> entities = new ArrayList<>();
        CornellContentEntity entity1 = createMockEntity("ext-001", false, "Published");
        entities.add(entity1);

        when(repository.findByExternalIdInAndPartnerCode(externalIds, partnerCode)).thenReturn(entities);
        when(repository.countByPartnerCode(partnerCode)).thenReturn(10L);

        JsonNode partnerResponse = createMockPartnerResponse();
        when(dataTransformUtility.fetchPartnerInfoUsingApi(partnerCode)).thenReturn(partnerResponse);
        when(dataTransformUtility.updatingPartnerInfo(any(JsonNode.class))).thenReturn("success");

        // Act
        ResponseEntity<?> response = ciosContentService.deleteNotPublishContent(deleteContentRequestDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        SBApiResponse apiResponse = (SBApiResponse) response.getBody();
        assertEquals(Constants.FAILED, apiResponse.getParams().getStatus());
        assertTrue(apiResponse.getParams().getErr().contains("External ID: ext-001 cannot be deleted because its status is not 'notInitiated'."));

        verify(repository, never()).delete(any(CornellContentEntity.class));
        verify(esUtilService, never()).deleteDocument(anyString(), anyString());
    }

    @Test
    void deleteNotPublishContent_error_externalIdDoesNotExist() {
        // Arrange
        List<CornellContentEntity> entities = new ArrayList<>();
        CornellContentEntity entity1 = createMockEntity("ext-001", false, Constants.NOT_INITIATED);
        entities.add(entity1);
        // ext-002 is missing from the returned entities

        when(repository.findByExternalIdInAndPartnerCode(externalIds, partnerCode)).thenReturn(entities);
        when(repository.countByPartnerCode(partnerCode)).thenReturn(10L);

        JsonNode partnerResponse = createMockPartnerResponse();
        when(dataTransformUtility.fetchPartnerInfoUsingApi(partnerCode)).thenReturn(partnerResponse);
        when(dataTransformUtility.updatingPartnerInfo(any(JsonNode.class))).thenReturn("success");

        // Act
        ResponseEntity<?> response = ciosContentService.deleteNotPublishContent(deleteContentRequestDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        SBApiResponse apiResponse = (SBApiResponse) response.getBody();
        assertEquals(Constants.FAILED, apiResponse.getParams().getStatus());
        assertTrue(apiResponse.getParams().getErr().contains("External ID: ext-002 does not exist."));

        verify(repository, times(1)).delete(any(CornellContentEntity.class)); // Only deletes ext-001
    }

    @Test
    void deleteNotPublishContent_error_ciosDataNull() {
        // Arrange
        List<CornellContentEntity> entities = new ArrayList<>();
        CornellContentEntity entity1 = new CornellContentEntity();
        entity1.setExternalId("ext-001");
        entity1.setPartnerId(partnerId);
        entity1.setPartnerCode(partnerCode);
        entity1.setIsActive(false);
        entity1.setCiosData(null); // Null ciosData

        entities.add(entity1);

        when(repository.findByExternalIdInAndPartnerCode(externalIds, partnerCode)).thenReturn(entities);
        when(repository.countByPartnerCode(partnerCode)).thenReturn(10L);

        JsonNode partnerResponse = createMockPartnerResponse();
        when(dataTransformUtility.fetchPartnerInfoUsingApi(partnerCode)).thenReturn(partnerResponse);
        when(dataTransformUtility.updatingPartnerInfo(any(JsonNode.class))).thenReturn("success");

        // Act
        ResponseEntity<?> response = ciosContentService.deleteNotPublishContent(deleteContentRequestDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        SBApiResponse apiResponse = (SBApiResponse) response.getBody();
        assertEquals(Constants.FAILED, apiResponse.getParams().getStatus());
        assertTrue(apiResponse.getParams().getErr().contains("External ID: ext-001 does not have a valid status in ciosData."));

        verify(repository, never()).delete(any(CornellContentEntity.class));
    }

    @Test
    void deleteNotPublishContent_error_missingStatusInCiosData() {
        // Arrange
        List<CornellContentEntity> entities = new ArrayList<>();
        CornellContentEntity entity1 = new CornellContentEntity();
        entity1.setExternalId("ext-001");
        entity1.setPartnerId(partnerId);
        entity1.setPartnerCode(partnerCode);
        entity1.setIsActive(false);

        // CiosData without status
        ObjectNode ciosData = realObjectMapper.createObjectNode();
        ObjectNode content = realObjectMapper.createObjectNode();
        content.put("name", "Test Content");
        ciosData.set("content", content);
        entity1.setCiosData(ciosData);

        entities.add(entity1);

        when(repository.findByExternalIdInAndPartnerCode(externalIds, partnerCode)).thenReturn(entities);
        when(repository.countByPartnerCode(partnerCode)).thenReturn(10L);

        JsonNode partnerResponse = createMockPartnerResponse();
        when(dataTransformUtility.fetchPartnerInfoUsingApi(partnerCode)).thenReturn(partnerResponse);
        when(dataTransformUtility.updatingPartnerInfo(any(JsonNode.class))).thenReturn("success");

        // Act
        ResponseEntity<?> response = ciosContentService.deleteNotPublishContent(deleteContentRequestDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        SBApiResponse apiResponse = (SBApiResponse) response.getBody();
        assertEquals(Constants.FAILED, apiResponse.getParams().getStatus());
        assertTrue(apiResponse.getParams().getErr().contains("External ID: ext-001 does not have a valid status in ciosData."));

        verify(repository, never()).delete(any(CornellContentEntity.class));
    }

    @Test
    void deleteNotPublishContent_mixedScenario_someSuccessSomeErrors() {
        // Arrange
        List<CornellContentEntity> entities = new ArrayList<>();
        CornellContentEntity entity1 = createMockEntity("ext-001", false, Constants.NOT_INITIATED); // Valid - should delete
        CornellContentEntity entity2 = createMockEntity("ext-002", true, Constants.DRAFT); // Invalid - is active

        List<String> mixedIds = Arrays.asList("ext-001", "ext-002", "ext-003"); // ext-003 doesn't exist
        deleteContentRequestDto.setExternalId(mixedIds);

        entities.add(entity1);
        entities.add(entity2);

        when(repository.findByExternalIdInAndPartnerCode(mixedIds, partnerCode)).thenReturn(entities);
        doNothing().when(repository).delete(any(CornellContentEntity.class));
        doNothing().when(esUtilService).deleteDocument(anyString(), anyString());
        when(repository.countByPartnerCode(partnerCode)).thenReturn(10L);

        JsonNode partnerResponse = createMockPartnerResponse();
        when(dataTransformUtility.fetchPartnerInfoUsingApi(partnerCode)).thenReturn(partnerResponse);
        when(dataTransformUtility.updatingPartnerInfo(any(JsonNode.class))).thenReturn("success");

        // Act
        ResponseEntity<?> response = ciosContentService.deleteNotPublishContent(deleteContentRequestDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        SBApiResponse apiResponse = (SBApiResponse) response.getBody();
        assertEquals(Constants.FAILED, apiResponse.getParams().getStatus());

        String errorMsg = apiResponse.getParams().getErr();
        assertTrue(errorMsg.contains("External ID: ext-002 is live, cannot delete."));
        assertTrue(errorMsg.contains("External ID: ext-003 does not exist."));

        // Only entity1 should be deleted
        verify(repository, times(1)).delete(entity1);
        verify(esUtilService, times(1)).deleteDocument(partnerCode + "_ext-001", Constants.CIOS_CONTENT_INDEX_NAME);
    }

    @Test
    void deleteNotPublishContent_emptyExternalIdList() {
        // Arrange
        deleteContentRequestDto.setExternalId(Collections.emptyList());
        when(repository.findByExternalIdInAndPartnerCode(anyList(), anyString())).thenReturn(Collections.emptyList());
        when(repository.countByPartnerCode(partnerCode)).thenReturn(10L);

        JsonNode partnerResponse = createMockPartnerResponse();
        when(dataTransformUtility.fetchPartnerInfoUsingApi(partnerCode)).thenReturn(partnerResponse);
        when(dataTransformUtility.updatingPartnerInfo(any(JsonNode.class))).thenReturn("success");

        // Act
        ResponseEntity<?> response = ciosContentService.deleteNotPublishContent(deleteContentRequestDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        SBApiResponse apiResponse = (SBApiResponse) response.getBody();
        assertEquals(Constants.SUCCESS, apiResponse.getResult().get(Constants.STATUS));

        verify(repository, never()).delete(any(CornellContentEntity.class));
        verify(esUtilService, never()).deleteDocument(anyString(), anyString());
    }

    @Test
    void saveOrUpdateContent_createNewContent_success() {
        // Arrange
        String externalId = "new-ext-001";
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        JsonNode transformData = createMockTransformData(externalId, "Test Content");

        when(repository.findByExternalIdAndPartnerId(externalId, partnerId)).thenReturn(Optional.empty());
        when(repository.save(any(CornellContentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cbServerProperties.getElasticCiosContentJsonPath()).thenReturn("/path/to/mapping.json");
        doNothing().when(dataTransformUtility).flattenContentData(anyMap());
        when(esUtilService.updateDocument(anyString(), anyString(), anyMap(), anyString())).thenReturn("document-id");

        // Act
        CornellContentEntity result = ReflectionTestUtils.invokeMethod(
            ciosContentService,
            "saveOrUpdateContent",
            externalId,
            transformData,
            currentTime,
            false,
            partnerCode,
            partnerId
        );

        // Assert
        assertNotNull(result);
        assertEquals(externalId, result.getExternalId());
        assertEquals(partnerId, result.getPartnerId());
        assertEquals(partnerCode, result.getPartnerCode());
        assertFalse(result.getIsActive());
        assertEquals(currentTime, result.getCreatedDate());
        assertEquals(currentTime, result.getUpdatedDate());
        assertNotNull(result.getCiosData());

        verify(repository, times(1)).findByExternalIdAndPartnerId(externalId, partnerId);
        verify(repository, times(1)).save(any(CornellContentEntity.class));
        verify(dataTransformUtility, times(1)).flattenContentData(anyMap());
        verify(esUtilService, times(1)).updateDocument(
            eq(Constants.CIOS_CONTENT_INDEX_NAME),
            eq(partnerCode + "_" + externalId),
            anyMap(),
            anyString()
        );
    }

    @Test
    void saveOrUpdateContent_updateExistingContent_success() {
        // Arrange
        String externalId = "existing-ext-001";
        Timestamp createdTime = new Timestamp(System.currentTimeMillis() - 100000);
        Timestamp updateTime = new Timestamp(System.currentTimeMillis());
        JsonNode transformData = createMockTransformData(externalId, "Updated Content");

        CornellContentEntity existingEntity = new CornellContentEntity();
        existingEntity.setExternalId(externalId);
        existingEntity.setPartnerId(partnerId);
        existingEntity.setPartnerCode(partnerCode);
        existingEntity.setIsActive(false);
        existingEntity.setCreatedDate(createdTime);
        existingEntity.setUpdatedDate(createdTime);
        existingEntity.setCiosData(createMockTransformData(externalId, "Old Content"));

        when(repository.findByExternalIdAndPartnerId(externalId, partnerId)).thenReturn(Optional.of(existingEntity));
        when(repository.save(any(CornellContentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cbServerProperties.getElasticCiosContentJsonPath()).thenReturn("/path/to/mapping.json");
        doNothing().when(dataTransformUtility).flattenContentData(anyMap());
        when(esUtilService.updateDocument(anyString(), anyString(), anyMap(), anyString())).thenReturn("document-id");

        // Act
        CornellContentEntity result = ReflectionTestUtils.invokeMethod(
            ciosContentService,
            "saveOrUpdateContent",
            externalId,
            transformData,
            updateTime,
            true,
            partnerCode,
            partnerId
        );

        // Assert
        assertNotNull(result);
        assertEquals(externalId, result.getExternalId());
        assertEquals(partnerId, result.getPartnerId());
        assertEquals(partnerCode, result.getPartnerCode());
        assertTrue(result.getIsActive());
        assertEquals(createdTime, result.getCreatedDate()); // Created date should remain unchanged
        assertEquals(updateTime, result.getUpdatedDate()); // Updated date should be new
        assertNotNull(result.getCiosData());

        verify(repository, times(1)).findByExternalIdAndPartnerId(externalId, partnerId);
        verify(repository, times(1)).save(any(CornellContentEntity.class));
        verify(dataTransformUtility, times(1)).flattenContentData(anyMap());
        verify(esUtilService, times(1)).updateDocument(
            eq(Constants.CIOS_CONTENT_INDEX_NAME),
            eq(partnerCode + "_" + externalId),
            anyMap(),
            anyString()
        );
    }

    @Test
    void saveOrUpdateContent_verifyElasticsearchDocumentUpdate() {
        // Arrange
        String externalId = "es-test-001";
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        JsonNode transformData = createMockTransformData(externalId, "ES Test Content");
        String elasticPath = "/elastic/mapping.json";
        String expectedUniqueId = partnerCode + "_" + externalId;

        when(repository.findByExternalIdAndPartnerId(externalId, partnerId)).thenReturn(Optional.empty());
        when(repository.save(any(CornellContentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cbServerProperties.getElasticCiosContentJsonPath()).thenReturn(elasticPath);
        doNothing().when(dataTransformUtility).flattenContentData(anyMap());
        when(esUtilService.updateDocument(anyString(), anyString(), anyMap(), anyString())).thenReturn("document-id");

        // Act
        CornellContentEntity result = ReflectionTestUtils.invokeMethod(
            ciosContentService,
            "saveOrUpdateContent",
            externalId,
            transformData,
            currentTime,
            false,
            partnerCode,
            partnerId
        );

        // Assert
        assertNotNull(result);
        verify(esUtilService, times(1)).updateDocument(
            eq(Constants.CIOS_CONTENT_INDEX_NAME),
            eq(expectedUniqueId),
            anyMap(),
            eq(elasticPath)
        );
    }

    @Test
    void saveOrUpdateContent_verifyDataFlattening() {
        // Arrange
        String externalId = "flatten-test-001";
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        JsonNode transformData = createMockTransformData(externalId, "Flatten Test");

        when(repository.findByExternalIdAndPartnerId(externalId, partnerId)).thenReturn(Optional.empty());
        when(repository.save(any(CornellContentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cbServerProperties.getElasticCiosContentJsonPath()).thenReturn("/path/to/mapping.json");
        doNothing().when(dataTransformUtility).flattenContentData(anyMap());
        when(esUtilService.updateDocument(anyString(), anyString(), anyMap(), anyString())).thenReturn("document-id");

        // Act
        CornellContentEntity result = ReflectionTestUtils.invokeMethod(
            ciosContentService,
            "saveOrUpdateContent",
            externalId,
            transformData,
            currentTime,
            false,
            partnerCode,
            partnerId
        );

        // Assert
        assertNotNull(result);
        verify(dataTransformUtility, times(1)).flattenContentData(argThat(map ->
            map.containsKey("externalId") &&
            map.get("externalId").equals(externalId)
        ));
    }

    @Test
    void saveOrUpdateContent_updateExistingContent_preservesCreatedDate() {
        // Arrange
        String externalId = "preserve-date-001";
        Timestamp originalCreatedTime = new Timestamp(System.currentTimeMillis() - 1000000);
        Timestamp updateTime = new Timestamp(System.currentTimeMillis());
        JsonNode transformData = createMockTransformData(externalId, "Date Test");

        CornellContentEntity existingEntity = new CornellContentEntity();
        existingEntity.setExternalId(externalId);
        existingEntity.setPartnerId(partnerId);
        existingEntity.setPartnerCode(partnerCode);
        existingEntity.setIsActive(false);
        existingEntity.setCreatedDate(originalCreatedTime);
        existingEntity.setUpdatedDate(originalCreatedTime);

        when(repository.findByExternalIdAndPartnerId(externalId, partnerId)).thenReturn(Optional.of(existingEntity));
        when(repository.save(any(CornellContentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cbServerProperties.getElasticCiosContentJsonPath()).thenReturn("/path/to/mapping.json");
        doNothing().when(dataTransformUtility).flattenContentData(anyMap());
        when(esUtilService.updateDocument(anyString(), anyString(), anyMap(), anyString())).thenReturn("document-id");

        // Act
        CornellContentEntity result = ReflectionTestUtils.invokeMethod(
            ciosContentService,
            "saveOrUpdateContent",
            externalId,
            transformData,
            updateTime,
            true,
            partnerCode,
            partnerId
        );

        // Assert
        assertNotNull(result);
        assertEquals(originalCreatedTime, result.getCreatedDate());
        assertEquals(updateTime, result.getUpdatedDate());
        assertNotEquals(result.getCreatedDate(), result.getUpdatedDate());
    }

    @Test
    void saveOrUpdateContent_togglesIsActiveFlag() {
        // Arrange
        String externalId = "active-toggle-001";
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        JsonNode transformData = createMockTransformData(externalId, "Active Toggle Test");

        CornellContentEntity existingEntity = new CornellContentEntity();
        existingEntity.setExternalId(externalId);
        existingEntity.setPartnerId(partnerId);
        existingEntity.setPartnerCode(partnerCode);
        existingEntity.setIsActive(false);
        existingEntity.setCreatedDate(currentTime);
        existingEntity.setUpdatedDate(currentTime);

        when(repository.findByExternalIdAndPartnerId(externalId, partnerId)).thenReturn(Optional.of(existingEntity));
        when(repository.save(any(CornellContentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cbServerProperties.getElasticCiosContentJsonPath()).thenReturn("/path/to/mapping.json");
        doNothing().when(dataTransformUtility).flattenContentData(anyMap());
        when(esUtilService.updateDocument(anyString(), anyString(), anyMap(), anyString())).thenReturn("document-id");

        // Act - Update to active
        CornellContentEntity result = ReflectionTestUtils.invokeMethod(
            ciosContentService,
            "saveOrUpdateContent",
            externalId,
            transformData,
            currentTime,
            true, // Toggle to active
            partnerCode,
            partnerId
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsActive());

        // Verify the entity was saved with the correct active status
        verify(repository, times(1)).save(argThat(entity ->
            entity.getIsActive() == true
        ));
    }

    @Test
    void saveOrUpdateContent_updatesPartnerCodeInTransformData() {
        // Arrange
        String externalId = "partner-code-001";
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        JsonNode transformData = createMockTransformData(externalId, "Partner Code Test");

        when(repository.findByExternalIdAndPartnerId(externalId, partnerId)).thenReturn(Optional.empty());
        when(repository.save(any(CornellContentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cbServerProperties.getElasticCiosContentJsonPath()).thenReturn("/path/to/mapping.json");
        doNothing().when(dataTransformUtility).flattenContentData(anyMap());
        when(esUtilService.updateDocument(anyString(), anyString(), anyMap(), anyString())).thenReturn("document-id");

        // Act
        CornellContentEntity result = ReflectionTestUtils.invokeMethod(
            ciosContentService,
            "saveOrUpdateContent",
            externalId,
            transformData,
            currentTime,
            false,
            partnerCode,
            partnerId
        );

        // Assert
        assertNotNull(result);
        JsonNode contentNode = result.getCiosData().path(Constants.CONTENT);
        assertTrue(contentNode.has(Constants.PARTNER_CODE));
        assertEquals(partnerCode, contentNode.get(Constants.PARTNER_CODE).asText());
    }

    // -------------------- HELPER METHODS --------------------

    // -------------------- loadContentFromExcel --------------------

    @Test
    void loadContentFromExcel_invalidFileFormat_returnsBadRequest() {
        MultipartFile file = new MockMultipartFile("file", "data.txt", "text/plain", "data".getBytes());

        SBApiResponse response = ciosContentService.loadContentFromExcel(file, "partner1", "p1");

        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void loadContentFromExcel_uploadFails_returnsInternalServerError() {
        MultipartFile file = new MockMultipartFile("file", "data.xlsx", "application/octet-stream", "data".getBytes());
        when(cbServerProperties.getCiosCloudContainerName()).thenReturn("container1");
        when(cbServerProperties.getCiosContentFileCloudFolderName()).thenReturn("folder1");
        SBApiResponse uploadResponse = SBApiResponse.createDefaultResponse("upload");
        uploadResponse.getParams().setStatus(Constants.FAILED);
        uploadResponse.getParams().setErrmsg("upload failed");
        uploadResponse.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
        when(storeFileToGCP.uploadCiosContentFile(any(), anyString(), anyString())).thenReturn(uploadResponse);

        SBApiResponse response = ciosContentService.loadContentFromExcel(file, "partner1", "p1");

        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

    @Test
    void loadContentFromExcel_success_pushesToKafka() {
        MultipartFile file = new MockMultipartFile("file", "data.xlsx", "application/octet-stream", "data".getBytes());
        when(cbServerProperties.getCiosCloudContainerName()).thenReturn("container1");
        when(cbServerProperties.getCiosContentFileCloudFolderName()).thenReturn("folder1");
        SBApiResponse uploadResponse = SBApiResponse.createDefaultResponse("upload");
        uploadResponse.setResponseCode(HttpStatus.OK);
        uploadResponse.getResult().put(Constants.NAME, "uploaded.xlsx");
        when(storeFileToGCP.uploadCiosContentFile(any(), anyString(), anyString())).thenReturn(uploadResponse);
        when(dataTransformUtility.createFileInfo(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn("file-1");
        when(cbServerProperties.getCiosContentOnboardTopic()).thenReturn("onboard-topic");

        SBApiResponse response = ciosContentService.loadContentFromExcel(file, "partner1", "p1");

        assertEquals(HttpStatus.OK, response.getResponseCode());
        verify(kafkaProducer).push(eq("onboard-topic"), any());
    }

    @Test
    void loadContentFromExcel_exceptionThrown_returnsBadRequest() {
        MultipartFile file = new MockMultipartFile("file", "data.xlsx", "application/octet-stream", "data".getBytes());
        when(cbServerProperties.getCiosCloudContainerName()).thenThrow(new RuntimeException("config missing"));

        SBApiResponse response = ciosContentService.loadContentFromExcel(file, "partner1", "p1");

        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    // -------------------- fetchAllContentFromSecondaryDb --------------------

    @Test
    void fetchAllContentFromSecondaryDb_withResults_returnsPaginatedResponse() {
        RequestDto dto = new RequestDto();
        dto.setPage(0);
        dto.setSize(10);
        Page<CornellContentEntity> page = mock(Page.class);
        when(page.getContent()).thenReturn(List.of(new CornellContentEntity()));
        when(page.getTotalPages()).thenReturn(1);
        when(page.getTotalElements()).thenReturn(1L);
        when(page.getNumberOfElements()).thenReturn(1);
        when(page.getSize()).thenReturn(10);
        when(page.getNumber()).thenReturn(0);
        when(repository.findAllCiosDataAndIsActive(any(), any(Pageable.class), any())).thenReturn(page);

        var result = ciosContentService.fetchAllContentFromSecondaryDb(dto);

        assertEquals(1, result.getResult().size());
    }

    @Test
    void fetchAllContentFromSecondaryDb_nullPageData_returnsEmptyResponse() {
        RequestDto dto = new RequestDto();
        dto.setPage(0);
        dto.setSize(10);
        when(repository.findAllCiosDataAndIsActive(any(), any(Pageable.class), any())).thenReturn(null);

        var result = ciosContentService.fetchAllContentFromSecondaryDb(dto);

        assertTrue(result.getResult().isEmpty());
    }

    @Test
    void fetchAllContentFromSecondaryDb_dataAccessException_wrapsAsCiosContentException() {
        RequestDto dto = new RequestDto();
        dto.setPage(0);
        dto.setSize(10);
        when(repository.findAllCiosDataAndIsActive(any(), any(Pageable.class), any()))
                .thenThrow(mock(DataAccessException.class));

        assertThrows(CiosContentException.class, () -> ciosContentService.fetchAllContentFromSecondaryDb(dto));
    }

    @Test
    void fetchAllContentFromSecondaryDb_genericException_wrapsAsCiosContentException() {
        RequestDto dto = new RequestDto();
        dto.setPage(0);
        dto.setSize(10);
        when(repository.findAllCiosDataAndIsActive(any(), any(Pageable.class), any()))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(CiosContentException.class, () -> ciosContentService.fetchAllContentFromSecondaryDb(dto));
    }

    // -------------------- loadContentProgressFromExcel --------------------

    @Test
    void loadContentProgressFromExcel_success_pushesEnrollment() {
        MultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", "userid,courseid\nuser1,course1".getBytes());
        when(dataTransformUtility.processExcelFile(file)).thenReturn(List.of(Map.of("userid", "user1", "courseid", "course1")));

        ObjectNode partnerInfo = realObjectMapper.createObjectNode();
        partnerInfo.put("id", "partner-1");
        partnerInfo.putArray("transformProgressJson");
        when(dataTransformUtility.fetchPartnerInfoUsingApi("partner1")).thenReturn(partnerInfo);

        ObjectNode transformed = realObjectMapper.createObjectNode();
        transformed.put("userid", "user1");
        when(dataTransformUtility.transformData(any(), any())).thenReturn(transformed);

        assertDoesNotThrow(() -> ciosContentService.loadContentProgressFromExcel(file, "partner1"));

        verify(kafkaProducer, times(1)).push(any(), any());
    }

    @Test
    void loadContentProgressFromExcel_exceptionThrown_wrapsAsCiosContentException() {
        MultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", "data".getBytes());
        when(dataTransformUtility.processExcelFile(file)).thenThrow(new RuntimeException("parse failed"));

        assertThrows(CiosContentException.class, () -> ciosContentService.loadContentProgressFromExcel(file, "partner1"));
    }

    // -------------------- getAllFileInfos --------------------

    @Test
    void getAllFileInfos_withResults_returnsList() {
        when(fileInfoRepository.findByPartnerId("partner1")).thenReturn(List.of(new FileInfoEntity()));

        List<FileInfoEntity> result = ciosContentService.getAllFileInfos("partner1");

        assertEquals(1, result.size());
    }

    @Test
    void getAllFileInfos_empty_returnsEmptyList() {
        when(fileInfoRepository.findByPartnerId("partner1")).thenReturn(Collections.emptyList());

        List<FileInfoEntity> result = ciosContentService.getAllFileInfos("partner1");

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllFileInfos_dataAccessException_wrapsAsCiosContentException() {
        when(fileInfoRepository.findByPartnerId("partner1")).thenThrow(mock(DataAccessException.class));

        assertThrows(CiosContentException.class, () -> ciosContentService.getAllFileInfos("partner1"));
    }

    @Test
    void getAllFileInfos_genericException_wrapsAsCiosContentException() {
        when(fileInfoRepository.findByPartnerId("partner1")).thenThrow(new RuntimeException("down"));

        assertThrows(CiosContentException.class, () -> ciosContentService.getAllFileInfos("partner1"));
    }

    // -------------------- readContentByExternalId --------------------

    @Test
    void readContentByExternalId_found_returnsCiosData() {
        CornellContentEntity entity = createMockEntity("ext1", false, Constants.NOT_INITIATED);
        when(repository.findByExternalIdAndPartnerCode("ext1", "partner1")).thenReturn(Optional.of(entity));

        Object result = ciosContentService.readContentByExternalId("partner1", "ext1");

        assertEquals(entity.getCiosData(), result);
    }

    @Test
    void readContentByExternalId_notFound_throwsCiosContentException() {
        when(repository.findByExternalIdAndPartnerCode("ext1", "partner1")).thenReturn(Optional.empty());

        assertThrows(CiosContentException.class, () -> ciosContentService.readContentByExternalId("partner1", "ext1"));
    }

    // -------------------- searchContent --------------------

    @Test
    void searchContent_success_returnsSearchResult() {
        SearchCriteria criteria = new SearchCriteria();
        SearchResult expected = new SearchResult();
        when(esUtilService.searchDocuments(Constants.CIOS_CONTENT_INDEX_NAME, criteria)).thenReturn(expected);

        SearchResult result = ciosContentService.searchContent(criteria);

        assertSame(expected, result);
    }

    @Test
    void searchContent_exceptionThrown_wrapsAsCiosContentException() {
        SearchCriteria criteria = new SearchCriteria();
        when(esUtilService.searchDocuments(anyString(), any())).thenThrow(new RuntimeException("es down"));

        assertThrows(CiosContentException.class, () -> ciosContentService.searchContent(criteria));
    }

    @Test
    void updateContent_delegatesToSaveOrUpdateContent() {
        ObjectNode transformData = realObjectMapper.createObjectNode();
        ObjectNode content = realObjectMapper.createObjectNode();
        content.put("name", "Course One");
        content.put("externalId", "ext1");
        content.put("isActive", true);
        ObjectNode contentPartner = realObjectMapper.createObjectNode();
        contentPartner.put("partnerCode", "partner1");
        contentPartner.put("id", "p1");
        content.set("contentPartner", contentPartner);
        transformData.set(Constants.CONTENT, content);

        when(repository.findByExternalIdAndPartnerId(anyString(), anyString())).thenReturn(Optional.empty());
        when(cbServerProperties.getElasticCiosContentJsonPath()).thenReturn("/path/mapping.json");

        Object result = ciosContentService.updateContent(transformData);

        assertNotNull(result);
        assertTrue(result instanceof CornellContentEntity);
        assertEquals("ext1", ((CornellContentEntity) result).getExternalId());
    }

    @Test
    void loadContentFromExcel_nullFilename_returnsBadRequest() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(null);

        SBApiResponse response = ciosContentService.loadContentFromExcel(file, "partner1", "p1");

        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void loadContentProgressFromExcel_callEnrollmentApiThrows_wrapsAsCiosContentException() {
        MultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", "userid,courseid\nuser1,course1".getBytes());
        when(dataTransformUtility.processExcelFile(file)).thenReturn(List.of(Map.of("userid", "user1", "courseid", "course1")));
        when(dataTransformUtility.fetchPartnerInfoUsingApi("partner1")).thenThrow(new RuntimeException("partner lookup failed"));

        assertThrows(CiosContentException.class, () -> ciosContentService.loadContentProgressFromExcel(file, "partner1"));
    }

    private CornellContentEntity createMockEntity(String externalId, boolean isActive, String status) {
        CornellContentEntity entity = new CornellContentEntity();
        entity.setExternalId(externalId);
        entity.setPartnerId(partnerId);
        entity.setPartnerCode(partnerCode);
        entity.setIsActive(isActive);
        entity.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        entity.setUpdatedDate(new Timestamp(System.currentTimeMillis()));

        // Create ciosData with status
        ObjectNode ciosData = realObjectMapper.createObjectNode();
        ObjectNode content = realObjectMapper.createObjectNode();
        content.put("status", status);
        content.put("name", "Test Content " + externalId);
        ciosData.set("content", content);
        entity.setCiosData(ciosData);

        return entity;
    }

    private JsonNode createMockPartnerResponse() {
        ObjectNode response = realObjectMapper.createObjectNode();
        ObjectNode data = realObjectMapper.createObjectNode();
        data.put("id", partnerId);
        data.put("partnerName", "Test Partner");
        data.put(Constants.TOTAL_COURSES_COUNT, 0);
        response.set(Constants.DATA, data);
        return response;
    }

    private JsonNode createMockTransformData(String externalId, String contentName) {
        ObjectNode transformData = realObjectMapper.createObjectNode();
        ObjectNode content = realObjectMapper.createObjectNode();
        content.put("externalId", externalId);
        content.put("name", contentName);
        content.put("status", Constants.NOT_INITIATED);
        transformData.set(Constants.CONTENT, content);
        return transformData;
    }
}
