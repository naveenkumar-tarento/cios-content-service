package com.igot.cios.entity;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileInfoEntityTest {

    @Test
    void settersAndGetters_roundTrip() {
        Timestamp initiatedOn = new Timestamp(System.currentTimeMillis());
        Timestamp completedOn = new Timestamp(System.currentTimeMillis());

        FileInfoEntity entity = new FileInfoEntity();
        entity.setFileId("file-1");
        entity.setFileName("content.xlsx");
        entity.setInitiatedOn(initiatedOn);
        entity.setCompletedOn(completedOn);
        entity.setStatus("InProgress");
        entity.setPartnerId("partner-1");
        entity.setGcpFileName("gcp-file-1");
        entity.setContentUploadedGCPFileName("gcp-content-1");

        assertEquals("file-1", entity.getFileId());
        assertEquals("content.xlsx", entity.getFileName());
        assertEquals(initiatedOn, entity.getInitiatedOn());
        assertEquals(completedOn, entity.getCompletedOn());
        assertEquals("InProgress", entity.getStatus());
        assertEquals("partner-1", entity.getPartnerId());
        assertEquals("gcp-file-1", entity.getGcpFileName());
        assertEquals("gcp-content-1", entity.getContentUploadedGCPFileName());
    }

    @Test
    void allArgsConstructor_setsFields() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        FileInfoEntity entity = new FileInfoEntity("file-2", "name.csv", now, now, "Done", "partner-2", "gcp-2", "gcp-content-2");
        assertEquals("file-2", entity.getFileId());
        assertEquals("Done", entity.getStatus());
    }

    @Test
    void sameInstance_equalsItself() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        FileInfoEntity entity1 = new FileInfoEntity("file-1", "n", now, now, "s", "p", "g", "c");
        assertEquals(entity1, entity1);
    }
}
