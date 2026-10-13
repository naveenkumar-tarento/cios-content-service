package com.igot.cios.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CbServerPropertiesTest {

    @Test
    void settersAndGetters_roundTrip() {
        CbServerProperties props = new CbServerProperties();

        props.setFixedUrl("/fixed");
        props.setServiceLocatorHost("http://locator");
        props.setServiceLocatorFixedUrl("/locator/fixed");
        props.setTopic("cornell-topic");
        props.setProgressPathOfTragetFile("/spec/path.json");
        props.setCornellEnrollmentServiceCode("CORNELL_SVC");
        props.setCornellEnrollmentListLimit(50);
        props.setCornellEnrollmentListCourseType("course");
        props.setCornellDateRange(7);
        props.setCbPoresbaseUrl("http://cb-pores");
        props.setPartnerReadEndPoint("/partner/read");
        props.setPartnerCreateEndPoint("/partner/create");
        props.setElasticCiosContentJsonPath("/elastic/path.json");
        props.setCloudStorageTypeName("gcp");
        props.setCloudStorageSecret("secret");
        props.setCloudStorageKey("key");
        props.setCloudStorageEndpoint("http://storage");
        props.setCiosCloudContainerName("container");
        props.setCiosFileLogsCloudFolderName("logs-folder");
        props.setCiosContentOnboardTopic("onboard-topic");
        props.setCiosContentFileCloudFolderName("content-folder");
        props.cornellPartnerCode = "CORNELL";
        props.courseraPartnerCode = "COURSERA";
        props.setCourseraEnrollmentServiceCode("COURSERA_SVC");
        props.setCourseraEnrollmentListLimit(25);
        props.setCourseraEnrollmentListCourseType("course-type");
        props.setCourseraDateRange(30L);
        props.setCourseraDateBefore("before");
        props.setCourseraDateAfter("after");
        props.setCdacEnrollmentServiceCode("CDAC_SVC");
        props.cdacApiKey = "cdac-key";
        props.cdacPartnerCode = "CDAC";
        props.keycloakUrl = "http://keycloak";
        props.ssoRealm = "realm";
        props.ssoUsername = "admin";
        props.ssoPassword = "password";
        props.ssoAdminTokenEndpoint = "/token";
        props.ssoConfigCreateApi = "/sso/create";
        props.ssoConfigMapperReadApi = "/sso/mapper/read";
        props.ssoConfigMapperUpdateApi = "/sso/mapper/update";
        props.setHarvardSftpHost("sftp-host");
        props.setHarvardSftpPort(22);
        props.setHarvardSftpUsername("sftp-user");
        props.setHarvardSftpPassword("sftp-pass");
        props.setHarvardSftpRemoteDirectory("/remote");
        props.setHarvardSftpCompletedDirectory("/completed");
        props.harvardPartnerCode = "HARVARD";
        props.setHarvardAllowedFileExtensions(".xlsx,.csv");
        props.setSsoConfigReadApi("/sso/read");

        assertEquals("/fixed", props.getFixedUrl());
        assertEquals("http://locator", props.getServiceLocatorHost());
        assertEquals("/locator/fixed", props.getServiceLocatorFixedUrl());
        assertEquals("cornell-topic", props.getTopic());
        assertEquals("/spec/path.json", props.getProgressPathOfTragetFile());
        assertEquals("CORNELL_SVC", props.getCornellEnrollmentServiceCode());
        assertEquals(50, props.getCornellEnrollmentListLimit());
        assertEquals("course", props.getCornellEnrollmentListCourseType());
        assertEquals(7, props.getCornellDateRange());
        assertEquals("http://cb-pores", props.getCbPoresbaseUrl());
        assertEquals("/partner/read", props.getPartnerReadEndPoint());
        assertEquals("/partner/create", props.getPartnerCreateEndPoint());
        assertEquals("/elastic/path.json", props.getElasticCiosContentJsonPath());
        assertEquals("gcp", props.getCloudStorageTypeName());
        assertEquals("secret", props.getCloudStorageSecret());
        assertEquals("key", props.getCloudStorageKey());
        assertEquals("http://storage", props.getCloudStorageEndpoint());
        assertEquals("container", props.getCiosCloudContainerName());
        assertEquals("logs-folder", props.getCiosFileLogsCloudFolderName());
        assertEquals("onboard-topic", props.getCiosContentOnboardTopic());
        assertEquals("content-folder", props.getCiosContentFileCloudFolderName());
        assertEquals("CORNELL", props.cornellPartnerCode);
        assertEquals("COURSERA", props.courseraPartnerCode);
        assertEquals("COURSERA_SVC", props.getCourseraEnrollmentServiceCode());
        assertEquals(25, props.getCourseraEnrollmentListLimit());
        assertEquals("course-type", props.getCourseraEnrollmentListCourseType());
        assertEquals(30L, props.getCourseraDateRange());
        assertEquals("before", props.getCourseraDateBefore());
        assertEquals("after", props.getCourseraDateAfter());
        assertEquals("CDAC_SVC", props.getCdacEnrollmentServiceCode());
        assertEquals("cdac-key", props.cdacApiKey);
        assertEquals("CDAC", props.cdacPartnerCode);
        assertEquals("http://keycloak", props.keycloakUrl);
        assertEquals("realm", props.ssoRealm);
        assertEquals("admin", props.ssoUsername);
        assertEquals("password", props.ssoPassword);
        assertEquals("/token", props.ssoAdminTokenEndpoint);
        assertEquals("/sso/create", props.ssoConfigCreateApi);
        assertEquals("/sso/mapper/read", props.ssoConfigMapperReadApi);
        assertEquals("/sso/mapper/update", props.ssoConfigMapperUpdateApi);
        assertEquals("sftp-host", props.getHarvardSftpHost());
        assertEquals(22, props.getHarvardSftpPort());
        assertEquals("sftp-user", props.getHarvardSftpUsername());
        assertEquals("sftp-pass", props.getHarvardSftpPassword());
        assertEquals("/remote", props.getHarvardSftpRemoteDirectory());
        assertEquals("/completed", props.getHarvardSftpCompletedDirectory());
        assertEquals("HARVARD", props.harvardPartnerCode);
        assertEquals(".xlsx,.csv", props.getHarvardAllowedFileExtensions());
        assertEquals("/sso/read", props.getSsoConfigReadApi());
    }

    @Test
    void lombokGeneratedAccessors_forPublicFields_roundTrip() {
        CbServerProperties props = new CbServerProperties();

        props.setCornellPartnerCode("CORNELL");
        props.setCourseraPartnerCode("COURSERA");
        props.setCdacApiKey("cdac-key");
        props.setCdacPartnerCode("CDAC");
        props.setKeycloakUrl("http://keycloak");
        props.setSsoRealm("realm");
        props.setSsoUsername("admin");
        props.setSsoPassword("password");
        props.setSsoAdminTokenEndpoint("/token");
        props.setSsoConfigCreateApi("/sso/create");
        props.setSsoConfigMapperReadApi("/sso/mapper/read");
        props.setSsoConfigMapperUpdateApi("/sso/mapper/update");
        props.setHarvardPartnerCode("HARVARD");

        assertEquals("CORNELL", props.getCornellPartnerCode());
        assertEquals("COURSERA", props.getCourseraPartnerCode());
        assertEquals("cdac-key", props.getCdacApiKey());
        assertEquals("CDAC", props.getCdacPartnerCode());
        assertEquals("http://keycloak", props.getKeycloakUrl());
        assertEquals("realm", props.getSsoRealm());
        assertEquals("admin", props.getSsoUsername());
        assertEquals("password", props.getSsoPassword());
        assertEquals("/token", props.getSsoAdminTokenEndpoint());
        assertEquals("/sso/create", props.getSsoConfigCreateApi());
        assertEquals("/sso/mapper/read", props.getSsoConfigMapperReadApi());
        assertEquals("/sso/mapper/update", props.getSsoConfigMapperUpdateApi());
        assertEquals("HARVARD", props.getHarvardPartnerCode());
    }
}
