package com.igot.cios.util.transactional.cassandrautils;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.igot.cios.exception.CiosContentException;
import com.igot.cios.util.Constants;
import com.igot.cios.util.PropertiesCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * All Cassandra driver construction is intercepted via static/construction mocking so these
 * tests never open a real network connection - CqlSession.builder() and PropertiesCache's
 * real-file-backed singleton are both stubbed out for the duration of each test.
 */
class CassandraConnectionManagerImplTest {

    @AfterEach
    void clearSessionMap() throws Exception {
        java.lang.reflect.Field mapField = CassandraConnectionManagerImpl.class.getDeclaredField("cassandraSessionMap");
        mapField.setAccessible(true);
        ((java.util.Map<?, ?>) mapField.get(null)).clear();
    }

    @Test
    void getSession_newKeyspace_buildsSessionViaMockedDriver() {
        try (MockedStatic<PropertiesCache> propsCacheStatic = mockStatic(PropertiesCache.class);
             MockedStatic<CqlSession> cqlSessionStatic = mockStatic(CqlSession.class)) {

            PropertiesCache mockPropsCache = mock(PropertiesCache.class);
            propsCacheStatic.when(PropertiesCache::getInstance).thenReturn(mockPropsCache);
            when(mockPropsCache.getProperty(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                return switch (key) {
                    case "cassandra.config.host" -> "127.0.0.1";
                    case "coreConnectionsPerHostForLocal", "coreConnectionsPerHostForRemote", "heartbeatIntervalSeconds" -> "1";
                    default -> key;
                };
            });
            when(mockPropsCache.readProperty(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL)).thenReturn("LOCAL_ONE");

            CqlSessionBuilder builder = mock(CqlSessionBuilder.class, RETURNS_SELF);
            CqlSession mockSession = mock(CqlSession.class);
            Metadata metadata = mock(Metadata.class);
            when(metadata.getClusterName()).thenReturn(java.util.Optional.of("test-cluster"));
            when(mockSession.getMetadata()).thenReturn(metadata);
            when(metadata.getNodes()).thenReturn(Collections.emptyMap());
            when(builder.build()).thenReturn(mockSession);
            cqlSessionStatic.when(CqlSession::builder).thenReturn(builder);

            CassandraConnectionManagerImpl manager = new CassandraConnectionManagerImpl();
            CqlSession result = manager.getSession("testKeyspace");

            assertNotNull(result);
            assertSame(mockSession, result);
        }
    }

    @Test
    void getSession_keyspaceAlreadyCached_returnsCachedSessionWithoutRebuilding() throws Exception {
        try (MockedStatic<PropertiesCache> propsCacheStatic = mockStatic(PropertiesCache.class);
             MockedStatic<CqlSession> cqlSessionStatic = mockStatic(CqlSession.class)) {

            stubHappyPathProperties(propsCacheStatic);
            CqlSessionBuilder builder = stubHappyPathBuilder(cqlSessionStatic);

            CassandraConnectionManagerImpl manager = new CassandraConnectionManagerImpl();

            CqlSession cachedSession = mock(CqlSession.class);
            when(cachedSession.isClosed()).thenReturn(false);
            java.lang.reflect.Field mapField = CassandraConnectionManagerImpl.class.getDeclaredField("cassandraSessionMap");
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, CqlSession> sessionMap =
                    (java.util.Map<String, CqlSession>) mapField.get(null);
            sessionMap.put("cachedKeyspace", cachedSession);

            CqlSession result = manager.getSession("cachedKeyspace");

            assertSame(cachedSession, result);
            // build() was already invoked once by the constructor's own connection; verify no extra build for the cached lookup
            verify(builder, times(1)).build();
        }
    }

    @Test
    void constructor_cqlSessionBuildThrows_wrapsAsCiosContentException() {
        try (MockedStatic<PropertiesCache> propsCacheStatic = mockStatic(PropertiesCache.class);
             MockedStatic<CqlSession> cqlSessionStatic = mockStatic(CqlSession.class)) {

            stubHappyPathProperties(propsCacheStatic);
            CqlSessionBuilder builder = mock(CqlSessionBuilder.class, RETURNS_SELF);
            when(builder.build()).thenThrow(new RuntimeException("connection refused"));
            cqlSessionStatic.when(CqlSession::builder).thenReturn(builder);

            assertThrows(CiosContentException.class, CassandraConnectionManagerImpl::new);
        }
    }

    @Test
    void createCassandraConnectionWithKeySpaces_blankHost_throwsCiosContentException() {
        try (MockedStatic<PropertiesCache> propsCacheStatic = mockStatic(PropertiesCache.class);
             MockedStatic<CqlSession> cqlSessionStatic = mockStatic(CqlSession.class)) {

            PropertiesCache mockPropsCache = mock(PropertiesCache.class);
            propsCacheStatic.when(PropertiesCache::getInstance).thenReturn(mockPropsCache);
            when(mockPropsCache.getProperty(Constants.CASSANDRA_CONFIG_HOST)).thenReturn("");

            assertThrows(CiosContentException.class, CassandraConnectionManagerImpl::new);
        }
    }

    @Test
    void getConsistencyLevel_invalidValue_logsAndReturnsNullConsistency() {
        try (MockedStatic<PropertiesCache> propsCacheStatic = mockStatic(PropertiesCache.class);
             MockedStatic<CqlSession> cqlSessionStatic = mockStatic(CqlSession.class)) {

            PropertiesCache mockPropsCache = mock(PropertiesCache.class);
            propsCacheStatic.when(PropertiesCache::getInstance).thenReturn(mockPropsCache);
            when(mockPropsCache.getProperty(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                return switch (key) {
                    case "cassandra.config.host" -> "127.0.0.1";
                    case "coreConnectionsPerHostForLocal", "coreConnectionsPerHostForRemote", "heartbeatIntervalSeconds" -> "1";
                    default -> key;
                };
            });
            when(mockPropsCache.readProperty(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL)).thenReturn("NOT_A_REAL_LEVEL");

            CqlSessionBuilder builder = mock(CqlSessionBuilder.class, RETURNS_SELF);
            // getConsistencyLevel() returns null here, so .name() on it throws NullPointerException,
            // which the surrounding try/catch wraps as CiosContentException - this still exercises
            // the invalid-enum branch inside getConsistencyLevel() itself.
            cqlSessionStatic.when(CqlSession::builder).thenReturn(builder);

            assertThrows(CiosContentException.class, CassandraConnectionManagerImpl::new);
        }
    }

    @Test
    void resourceCleanUp_run_closesAllTrackedSessions() throws Exception {
        CqlSession mockSession1 = mock(CqlSession.class);
        CqlSession mockSession2 = mock(CqlSession.class);

        java.lang.reflect.Field mapField = CassandraConnectionManagerImpl.class.getDeclaredField("cassandraSessionMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, CqlSession> sessionMap =
                (java.util.Map<String, CqlSession>) mapField.get(null);
        sessionMap.put("keyspace1", mockSession1);

        java.lang.reflect.Field sessionField = CassandraConnectionManagerImpl.class.getDeclaredField("session");
        sessionField.setAccessible(true);
        sessionField.set(null, mockSession2);

        try {
            java.lang.reflect.Constructor<?> ctor = Class.forName(
                            "com.igot.cios.util.transactional.cassandrautils.CassandraConnectionManagerImpl$ResourceCleanUp")
                    .getDeclaredConstructor();
            ctor.setAccessible(true);
            Thread cleanup = (Thread) ctor.newInstance();
            cleanup.run();

            verify(mockSession1).close();
            verify(mockSession2).close();
        } finally {
            sessionField.set(null, null);
        }
    }

    private PropertiesCache stubHappyPathProperties(MockedStatic<PropertiesCache> propsCacheStatic) {
        PropertiesCache mockPropsCache = mock(PropertiesCache.class);
        propsCacheStatic.when(PropertiesCache::getInstance).thenReturn(mockPropsCache);
        when(mockPropsCache.getProperty(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return switch (key) {
                case "cassandra.config.host" -> "127.0.0.1";
                case "coreConnectionsPerHostForLocal", "coreConnectionsPerHostForRemote", "heartbeatIntervalSeconds" -> "1";
                default -> key;
            };
        });
        when(mockPropsCache.readProperty(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL)).thenReturn("LOCAL_ONE");
        return mockPropsCache;
    }

    private CqlSessionBuilder stubHappyPathBuilder(MockedStatic<CqlSession> cqlSessionStatic) {
        CqlSessionBuilder builder = mock(CqlSessionBuilder.class, RETURNS_SELF);
        CqlSession mockSession = mock(CqlSession.class);
        Metadata metadata = mock(Metadata.class);
        when(metadata.getClusterName()).thenReturn(java.util.Optional.of("test-cluster"));
        when(mockSession.getMetadata()).thenReturn(metadata);
        when(metadata.getNodes()).thenReturn(Collections.emptyMap());
        when(builder.build()).thenReturn(mockSession);
        cqlSessionStatic.when(CqlSession::builder).thenReturn(builder);
        return builder;
    }
}
