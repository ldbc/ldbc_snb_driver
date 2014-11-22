package com.ldbc.driver;

public abstract class OperationHandler_NEW<OPERATION_TYPE extends Operation<?>, DB_CONNECTION_STATE_TYPE extends DbConnectionState> implements Runnable {
    public abstract OperationResultReport executeOperation(OPERATION_TYPE operation, DB_CONNECTION_STATE_TYPE dbConnectionState) throws DbException;

    @Override
    public String toString() {
        return String.format("OperationHandler [type=%s]", getClass().getName());
    }
}