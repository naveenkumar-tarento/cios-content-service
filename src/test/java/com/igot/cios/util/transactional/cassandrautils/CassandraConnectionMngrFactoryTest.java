package com.igot.cios.util.transactional.cassandrautils;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.igot.cios.util.Constants;
import com.igot.cios.util.PropertiesCache;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * getInstance() lazily triggers the Holder class's static initializer, which builds a real
 * CassandraConnectionManagerImpl - so, just like CassandraConnectionManagerImplTest, the
 * Cassandra driver's static entry point is intercepted here to guarantee no real network
 * connection is ever attempted. This must be the first call to getInstance() in the whole
 * test suite, since the Holder class (and therefore the singleton instance) is only
 * initialized once per JVM.
 */
class CassandraConnectionMngrFactoryTest {

    @Test
    void getInstance_returnsSingletonBackedByMockedDriver() {
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

            CassandraConnectionManager first = CassandraConnectionMngrFactory.getInstance();
            CassandraConnectionManager second = CassandraConnectionMngrFactory.getInstance();

            assertNotNull(first);
            assertSame(first, second);
        }
    }
}
