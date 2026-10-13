package com.igot.cios.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cios.dto.DeleteContentRequestDto;
import com.igot.cios.dto.RequestDto;
import com.igot.cios.dto.SBApiResponse;
import com.igot.cios.entity.FileInfoEntity;
import com.igot.cios.exception.CiosContentException;
import com.igot.cios.service.CiosContentService;
import com.igot.cios.util.elasticsearch.dto.SearchCriteria;
import com.igot.cios.util.elasticsearch.dto.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CiosContentControllerTest {

    @Mock
    private CiosContentService ciosContentService;

    private CiosContentController controller;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        controller = new CiosContentController(ciosContentService);
    }

    @Test
    void loadContentFromExcel_delegatesToService() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", "data".getBytes());
        SBApiResponse apiResponse = new SBApiResponse();
        apiResponse.setResponseCode(HttpStatus.OK);
        when(ciosContentService.loadContentFromExcel(file, "partner1", "p1")).thenReturn(apiResponse);

        ResponseEntity<SBApiResponse> response = controller.loadContentFromExcel(file, "partner1", "p1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void fetchContentFromDb_success_returnsOk() {
        RequestDto dto = new RequestDto();
        when(ciosContentService.fetchAllContentFromSecondaryDb(dto)).thenReturn(null);

        ResponseEntity<Object> response = controller.fetchContentFromDb(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void fetchContentFromDb_exceptionThrown_returnsInternalServerError() {
        RequestDto dto = new RequestDto();
        when(ciosContentService.fetchAllContentFromSecondaryDb(dto)).thenThrow(new RuntimeException("db error"));

        ResponseEntity<Object> response = controller.fetchContentFromDb(dto);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void loadContentProgressFromExcel_success_returnsOk() {
        MultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", "data".getBytes());

        ResponseEntity<Object> response = controller.loadContentProgressFromExcel(file, "partner1");

        verify(ciosContentService).loadContentProgressFromExcel(file, "partner1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void loadContentProgressFromExcel_exceptionThrown_returnsInternalServerError() {
        MultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", "data".getBytes());
        doThrow(new RuntimeException("processing failed")).when(ciosContentService).loadContentProgressFromExcel(file, "partner1");

        ResponseEntity<Object> response = controller.loadContentProgressFromExcel(file, "partner1");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void getAllFileInfosByPartnerId_withResults_returnsOk() {
        when(ciosContentService.getAllFileInfos("partner1")).thenReturn(List.of(new FileInfoEntity()));

        ResponseEntity<List<FileInfoEntity>> response = controller.getAllFileInfosByPartnerId("partner1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAllFileInfosByPartnerId_empty_returnsNoContent() {
        when(ciosContentService.getAllFileInfos("partner1")).thenReturn(Collections.emptyList());

        ResponseEntity<List<FileInfoEntity>> response = controller.getAllFileInfosByPartnerId("partner1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void deleteNotPublishContent_success() {
        DeleteContentRequestDto dto = new DeleteContentRequestDto();
        ResponseEntity<Object> serviceResponse = ResponseEntity.ok().build();
        when(ciosContentService.deleteNotPublishContent(dto)).thenReturn(serviceResponse);

        ResponseEntity<Object> response = controller.deleteNotPublishContent(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteNotPublishContent_ciosContentException_returnsBadRequest() {
        DeleteContentRequestDto dto = new DeleteContentRequestDto();
        when(ciosContentService.deleteNotPublishContent(dto)).thenThrow(new CiosContentException("ERR", "bad request"));

        ResponseEntity<Object> response = controller.deleteNotPublishContent(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deleteNotPublishContent_dataAccessException_returnsInternalServerError() {
        DeleteContentRequestDto dto = new DeleteContentRequestDto();
        when(ciosContentService.deleteNotPublishContent(dto)).thenThrow(new DataRetrievalFailureException("db down"));

        ResponseEntity<Object> response = controller.deleteNotPublishContent(dto);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void readContentByExternalId_delegatesToService() {
        when(ciosContentService.readContentByExternalId("partner1", "ext1")).thenReturn("content");

        ResponseEntity<Object> response = controller.readContentByExternalId("partner1", "ext1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("content", response.getBody());
    }

    @Test
    void searchContent_delegatesToService() {
        SearchCriteria criteria = new SearchCriteria();
        SearchResult result = new SearchResult();
        when(ciosContentService.searchContent(criteria)).thenReturn(result);

        ResponseEntity<Object> response = controller.searchContent(criteria);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(result, response.getBody());
    }

    @Test
    void updateContent_delegatesToService() {
        JsonNode jsonNode = new ObjectMapper().createObjectNode();
        when(ciosContentService.updateContent(jsonNode)).thenReturn("updated");

        ResponseEntity<Object> response = controller.updateContent(jsonNode);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("updated", response.getBody());
    }
}
