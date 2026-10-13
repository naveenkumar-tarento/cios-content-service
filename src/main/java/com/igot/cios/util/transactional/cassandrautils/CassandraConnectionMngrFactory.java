package com.igot.cios.util.transactional.cassandrautils;

public class CassandraConnectionMngrFactory {

    private CassandraConnectionMngrFactory() {
    }

    private static final class Holder {
        private static final CassandraConnectionManager INSTANCE = new CassandraConnectionManagerImpl();
    }

    public static CassandraConnectionManager getInstance() {
        return Holder.INSTANCE;
    }
}