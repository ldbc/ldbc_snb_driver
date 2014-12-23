package com.ldbc.driver.runtime.executor;

import com.ldbc.driver.*;
import com.ldbc.driver.runtime.ConcurrentErrorReporter;
import com.ldbc.driver.runtime.DefaultQueues;
import com.ldbc.driver.runtime.QueueEventSubmitter;
import com.ldbc.driver.runtime.coordination.GlobalCompletionTimeReader;
import com.ldbc.driver.runtime.coordination.LocalCompletionTimeWriter;
import com.ldbc.driver.runtime.metrics.ConcurrentMetricsService;
import com.ldbc.driver.runtime.scheduling.Spinner;
import com.ldbc.driver.temporal.TimeSource;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class SingleThreadOperationHandlerExecutor_NEW implements OperationExecutor_NEW {
    static final Operation TERMINATE_OPERATION = new Operation() {
        @Override
        public Object marshalResult(String serializedOperationResult) throws SerializingMarshallingException {
            return null;
        }

        @Override
        public String serializeResult(Object operationResultInstance) throws SerializingMarshallingException {
            return null;
        }
    };

    private final SingleThreadOperationHandlerExecutorThread_NEW executorThread;
    private final QueueEventSubmitter<Operation> operationQueueEventSubmitter;
    private final AtomicLong uncompletedHandlers = new AtomicLong(0);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public SingleThreadOperationHandlerExecutor_NEW(Db db,
                                                    WorkloadStreams.WorkloadStreamDefinition streamDefinition,
                                                    LocalCompletionTimeWriter localCompletionTimeWriter,
                                                    GlobalCompletionTimeReader globalCompletionTimeReader,
                                                    Spinner spinner,
                                                    TimeSource timeSource,
                                                    ConcurrentErrorReporter errorReporter,
                                                    ConcurrentMetricsService metricsService,
                                                    ChildOperationGenerator childOperationGenerator,
                                                    int boundedQueueSize) {
        Queue<Operation> operationQueue = DefaultQueues.newAlwaysBlockingBounded(boundedQueueSize);
        this.operationQueueEventSubmitter = QueueEventSubmitter.queueEventSubmitterFor(operationQueue);


        OperationHandlerRunnableContextRetriever operationHandlerRunnableContextInitializer = new OperationHandlerRunnableContextRetriever(
                streamDefinition,
                db,
                localCompletionTimeWriter,
                globalCompletionTimeReader,
                spinner,
                timeSource,
                errorReporter,
                metricsService
        );

        this.executorThread = new SingleThreadOperationHandlerExecutorThread_NEW(
                operationQueue,
                errorReporter,
                uncompletedHandlers,
                operationHandlerRunnableContextInitializer,
                childOperationGenerator
        );
        this.executorThread.start();
    }

    public final void execute(Operation operation) throws OperationHandlerExecutorException {
        uncompletedHandlers.incrementAndGet();
        try {
            operationQueueEventSubmitter.submitEventToQueue(operation);
        } catch (InterruptedException e) {
            throw new OperationHandlerExecutorException("Error encountered while submitting handler to queue", e);
        }
    }

    @Override
    synchronized public final void shutdown(long waitAsMilli) throws OperationHandlerExecutorException {
        if (shutdown.get())
            throw new OperationHandlerExecutorException("Executor has already been shutdown");
        try {
            operationQueueEventSubmitter.submitEventToQueue(TERMINATE_OPERATION);
            executorThread.join(waitAsMilli);
            if (uncompletedHandlers.get() > 0) {
                executorThread.forceShutdown();
                throw new OperationHandlerExecutorException(String.format("Executor shutdown before all handlers could complete - %s uncompleted handlers", uncompletedHandlers));
            }
        } catch (Exception e) {
            throw new OperationHandlerExecutorException("Error encountered while trying to shutdown", e);
        }
        shutdown.set(true);
    }

    @Override
    public long uncompletedOperationHandlerCount() {
        return uncompletedHandlers.get();
    }
}
