package com.igot.cios.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cios.dto.SBApiResponse;
import com.igot.cios.entity.FileInfoEntity;
import com.igot.cios.plugins.DataTransformUtility;
import com.igot.cios.repository.FileInfoRepository;
import com.igot.cios.storage.StoreFileToGCP;
import com.igot.cios.util.CbServerProperties;
import com.igot.cios.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardContentConsumerTest {

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private FileInfoRepository fileInfoRepository;
    @Mock
    private StoreFileToGCP gcpBucket;
    @Mock
    private CbServerProperties cbServerProperties;
    @Mock
    private DataTransformUtility dataTransformUtility;

    private OnboardContentConsumer consumer;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        consumer = new OnboardContentConsumer(objectMapper, fileInfoRepository, gcpBucket, cbServerProperties, dataTransformUtility);
    }

    @Test
    void consumeMessage_blankMessage_logsAndReturns() {
        assertDoesNotThrow(() -> consumer.consumeMessage(""));
    }

    @Test
    void consumeMessage_executorRejects_catchesException() {
        java.util.concurrent.ExecutorService executorService =
                (java.util.concurrent.ExecutorService) org.springframework.test.util.ReflectionTestUtils
                        .getField(consumer, "executorService");
        executorService.shutdownNow();

        assertDoesNotThrow(() -> consumer.consumeMessage("{}"));
    }

    @Test
    void consumeMessage_validMessage_submitsForProcessing() {
        assertDoesNotThrow(() -> consumer.consumeMessage("{}"));
    }

    @Test
    void processOnboardingContent_noExistingFileInfo_handlesGracefullyViaErrorPath() throws Exception {
        Map<String, Object> receivedMessage = new HashMap<>();
        receivedMessage.put(Constants.PARTNER_CODE, "partner1");
        receivedMessage.put(Constants.FILE_NAME, "file.xlsx");
        receivedMessage.put(Constants.FILE_ID, "file-1");
        receivedMessage.put(Constants.PARTNER_ID, "p1");

        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(receivedMessage);
        when(objectMapper.convertValue(any(), eq(Timestamp.class))).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(fileInfoRepository.findById("file-1")).thenReturn(Optional.empty());

        Map<String, Object> errorResult = new HashMap<>();
        File logFile = Files.createTempFile("onboard-error-log", ".csv").toFile();
        errorResult.put(Constants.LOG_FILE, logFile);
        errorResult.put(Constants.HAS_FAILURES, true);
        when(dataTransformUtility.processRowsAndCreateLogs(isNull(), eq("file-1"), eq("file.xlsx"), eq("partner1"), anyString(), eq("p1")))
                .thenReturn(errorResult);

        SBApiResponse uploadResponse = SBApiResponse.createDefaultResponse("test");
        uploadResponse.getParams().setStatus(Constants.FAILED);
        when(gcpBucket.uploadCiosLogsFile(any(), any(), any())).thenReturn(uploadResponse);

        consumer.processOnboardingContent("{\"partnerCode\":\"partner1\"}");

        verify(dataTransformUtility).processRowsAndCreateLogs(isNull(), eq("file-1"), eq("file.xlsx"), eq("partner1"), anyString(), eq("p1"));
    }

    @Test
    void processOnboardingContent_downloadFails_returnsEarlyWithoutProcessing() throws Exception {
        Map<String, Object> receivedMessage = new HashMap<>();
        receivedMessage.put(Constants.PARTNER_CODE, "partner1");
        receivedMessage.put(Constants.FILE_NAME, "file.xlsx");
        receivedMessage.put(Constants.FILE_ID, "file-1");
        receivedMessage.put(Constants.PARTNER_ID, "p1");

        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(receivedMessage);
        when(objectMapper.convertValue(any(), eq(Timestamp.class))).thenReturn(new Timestamp(System.currentTimeMillis()));

        FileInfoEntity fileInfoEntity = new FileInfoEntity();
        fileInfoEntity.setContentUploadedGCPFileName("uploaded.xlsx");
        when(fileInfoRepository.findById("file-1")).thenReturn(Optional.of(fileInfoEntity));

        ResponseEntity<Object> downloadResponse = ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        when(gcpBucket.downloadCiosContentFile("uploaded.xlsx")).thenReturn(downloadResponse);

        consumer.processOnboardingContent("{\"partnerCode\":\"partner1\"}");

        verifyNoInteractions(dataTransformUtility);
    }

    @Test
    void processOnboardingContent_emptyProcessedData_writesErrorLog() throws Exception {
        Map<String, Object> receivedMessage = new HashMap<>();
        receivedMessage.put(Constants.PARTNER_CODE, "partner1");
        receivedMessage.put(Constants.FILE_NAME, "file.xlsx");
        receivedMessage.put(Constants.FILE_ID, "file-1");
        receivedMessage.put(Constants.PARTNER_ID, "p1");

        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(receivedMessage);
        when(objectMapper.convertValue(any(), eq(Timestamp.class))).thenReturn(new Timestamp(System.currentTimeMillis()));

        FileInfoEntity fileInfoEntity = new FileInfoEntity();
        fileInfoEntity.setContentUploadedGCPFileName("uploaded.xlsx");
        when(fileInfoRepository.findById("file-1")).thenReturn(Optional.of(fileInfoEntity));

        ByteArrayResource resource = new ByteArrayResource("excel-bytes".getBytes());
        when(gcpBucket.downloadCiosContentFile("uploaded.xlsx")).thenReturn(ResponseEntity.ok(resource));
        when(dataTransformUtility.processExcelFile(any())).thenReturn(List.of());

        Map<String, Object> errorResult = new HashMap<>();
        File logFile = Files.createTempFile("onboard-empty-log", ".csv").toFile();
        errorResult.put(Constants.LOG_FILE, logFile);
        errorResult.put(Constants.HAS_FAILURES, true);
        when(dataTransformUtility.processRowsAndCreateLogs(isNull(), eq("file-1"), eq("file.xlsx"), eq("partner1"), anyString(), eq("p1")))
                .thenReturn(errorResult);

        SBApiResponse uploadResponse = SBApiResponse.createDefaultResponse("test");
        uploadResponse.getParams().setStatus(Constants.FAILED);
        when(gcpBucket.uploadCiosLogsFile(any(), any(), any())).thenReturn(uploadResponse);

        consumer.processOnboardingContent("{\"partnerCode\":\"partner1\"}");

        verify(dataTransformUtility).processRowsAndCreateLogs(isNull(), eq("file-1"), eq("file.xlsx"), eq("partner1"), anyString(), eq("p1"));
    }

    @Test
    void processOnboardingContent_existingFileWithSuccessfulDownload_processesExcelData() throws Exception {
        Map<String, Object> receivedMessage = new HashMap<>();
        receivedMessage.put(Constants.PARTNER_CODE, "partner1");
        receivedMessage.put(Constants.FILE_NAME, "file.xlsx");
        receivedMessage.put(Constants.FILE_ID, "file-1");
        receivedMessage.put(Constants.PARTNER_ID, "p1");

        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(receivedMessage);
        when(objectMapper.convertValue(any(), eq(Timestamp.class))).thenReturn(new Timestamp(System.currentTimeMillis()));

        FileInfoEntity fileInfoEntity = new FileInfoEntity();
        fileInfoEntity.setContentUploadedGCPFileName("uploaded.xlsx");
        when(fileInfoRepository.findById("file-1")).thenReturn(Optional.of(fileInfoEntity));

        ByteArrayResource resource = new ByteArrayResource("excel-bytes".getBytes());
        ResponseEntity<Object> downloadResponse = ResponseEntity.ok(resource);
        when(gcpBucket.downloadCiosContentFile("uploaded.xlsx")).thenReturn(downloadResponse);

        when(dataTransformUtility.processExcelFile(any())).thenReturn(List.of(Map.of("col1", "val1")));

        Map<String, Object> result = new HashMap<>();
        File logFile = Files.createTempFile("onboard-success-log", ".csv").toFile();
        result.put(Constants.LOG_FILE, logFile);
        result.put(Constants.HAS_FAILURES, false);
        when(dataTransformUtility.processRowsAndCreateLogs(anyList(), eq("file-1"), eq("file.xlsx"), eq("partner1"), isNull(), eq("p1")))
                .thenReturn(result);

        SBApiResponse uploadResponse = SBApiResponse.createDefaultResponse("test");
        uploadResponse.getParams().setStatus(Constants.SUCCESS);
        uploadResponse.getResult().put(Constants.NAME, "uploaded-log.csv");
        when(gcpBucket.uploadCiosLogsFile(any(), any(), any())).thenReturn(uploadResponse);

        consumer.processOnboardingContent("{\"partnerCode\":\"partner1\"}");

        verify(dataTransformUtility).createFileInfo(eq("p1"), eq("file-1"), eq("file.xlsx"), any(), any(), eq(Constants.CONTENT_UPLOAD_SUCCESSFULLY), eq("uploaded-log.csv"), eq("uploaded.xlsx"));
    }

    @Test
    void uploadLogFileToGCP_failedUpload_logsErrorButStillCreatesFileInfo() throws Exception {
        File logFile = Files.createTempFile("onboard-log", ".csv").toFile();
        SBApiResponse uploadResponse = SBApiResponse.createDefaultResponse("test");
        uploadResponse.getParams().setStatus(Constants.FAILED);
        uploadResponse.getParams().setErrmsg("upload failed");
        when(gcpBucket.uploadCiosLogsFile(eq(logFile), any(), any())).thenReturn(uploadResponse);

        consumer.uploadLogFileToGCP(logFile, "p1", "file-1", "file.xlsx", new Timestamp(System.currentTimeMillis()), true, "uploaded.xlsx");

        verify(dataTransformUtility).createFileInfo(eq("p1"), eq("file-1"), eq("file.xlsx"), any(), any(), eq(Constants.CONTENT_UPLOAD_FAILED), eq(""), eq("uploaded.xlsx"));
    }
}
