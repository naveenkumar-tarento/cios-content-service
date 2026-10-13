package com.igot.cios.util.transactional.cassandrautils;

public class CassandraConnectionMngrFactory {

    private static volatile CassandraConnectionManager instance;

    private CassandraConnectionMngrFactory() {
    }

    public static CassandraConnectionManager getInstance() {
        if (instance == null) {
            synchronized (CassandraConnectionMngrFactory.class) {
                if (instance == null) {
                    instance = new CassandraConnectionManagerImpl();
                }
            }
        }
        return instance;
    }
}