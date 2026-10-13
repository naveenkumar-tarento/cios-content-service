package com.igot.cios.storage;

import com.igot.cios.dto.SBApiResponse;
import com.igot.cios.util.CbServerProperties;
import com.igot.cios.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.sunbird.cloud.storage.BaseStorageService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreFileToGCPTest {

    @Mock
    private CbServerProperties cbServerProperties;

    @Mock
    private BaseStorageService storageService;

    private StoreFileToGCP storeFileToGCP;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        storeFileToGCP = new StoreFileToGCP(cbServerProperties);
        ReflectionTestUtils.setField(storeFileToGCP, "storageService", storageService);
    }

    @Test
    void uploadFile_success_returnsUrlInResult() throws IOException {
        File sourceFile = Files.createTempFile("gcp-upload-test", ".txt").toFile();
        Files.writeString(sourceFile.toPath(), "hello world");
        when(storageService.upload(anyString(), anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn("https://cloud/bucket/object");

        SBApiResponse response = storeFileToGCP.uploadFile(sourceFile, "cloudFolder", "container1");

        assertEquals(sourceFile.getName(), response.getResult().get(Constants.NAME));
        assertEquals("https://cloud/bucket/object", response.getResult().get(Constants.URL));
        assertFalse(sourceFile.exists());
    }

    @Test
    void uploadFile_exceptionThrown_returnsFailedResponse() throws IOException {
        File sourceFile = Files.createTempFile("gcp-upload-test-fail", ".txt").toFile();
        when(storageService.upload(anyString(), anyString(), anyString(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("upload failed"));

        SBApiResponse response = storeFileToGCP.uploadFile(sourceFile, "cloudFolder", "container1");

        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

    @Test
    void uploadCiosLogsFile_success_copiesAndUploadsTempFile() throws IOException {
        File sourceFile = Files.createTempFile("log-source", ".csv").toFile();
        Files.writeString(sourceFile.toPath(), "log,data");
        when(storageService.upload(anyString(), anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn("https://cloud/bucket/log");

        SBApiResponse response = storeFileToGCP.uploadCiosLogsFile(sourceFile, "container1", "logsFolder");

        assertTrue(response.getResult().get(Constants.NAME).toString().endsWith(sourceFile.getName()));
        assertFalse(sourceFile.exists());
    }

    @Test
    void uploadCiosLogsFile_sourceMissing_returnsFailedResponse() {
        File missingFile = new File("non-existent-file-xyz.csv");

        SBApiResponse response = storeFileToGCP.uploadCiosLogsFile(missingFile, "container1", "logsFolder");

        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

    @Test
    void uploadCiosContentFile_success_transfersAndUploads() {
        MultipartFile multipartFile = new MockMultipartFile("file", "content.xlsx", "application/octet-stream", "data".getBytes());
        when(storageService.upload(anyString(), anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn("https://cloud/bucket/content");

        SBApiResponse response = storeFileToGCP.uploadCiosContentFile(multipartFile, "container1", "contentFolder");

        assertEquals("https://cloud/bucket/content", response.getResult().get(Constants.URL));
    }

    @Test
    void downloadCiosContentFile_success_returnsFileBody() throws IOException {
        when(cbServerProperties.getCiosContentFileCloudFolderName()).thenReturn("contentFolder");
        when(cbServerProperties.getCiosCloudContainerName()).thenReturn("container1");

        String fileName = "download-test-" + System.currentTimeMillis() + ".txt";
        Path tmpPath = Path.of(Constants.LOCAL_BASE_PATH + fileName);
        Files.createDirectories(tmpPath.getParent());
        Files.writeString(tmpPath, "downloaded content");

        ResponseEntity<Object> response = storeFileToGCP.downloadCiosContentFile(fileName);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ByteArrayResource);
        assertFalse(Files.exists(tmpPath));
    }

    @Test
    void downloadCiosContentFile_fileMissingAfterDownload_returnsInternalServerError() {
        when(cbServerProperties.getCiosContentFileCloudFolderName()).thenReturn("contentFolder");
        when(cbServerProperties.getCiosCloudContainerName()).thenReturn("container1");

        ResponseEntity<Object> response = storeFileToGCP.downloadCiosContentFile("never-created-" + System.currentTimeMillis() + ".txt");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
