package com.ldbc.driver.generator.wrapper;

import com.ldbc.driver.Operation;
import com.ldbc.driver.generator.Generator;
import com.ldbc.driver.generator.GeneratorBuilder;
import com.ldbc.driver.generator.GeneratorException;
import com.ldbc.driver.util.Function;
import com.ldbc.driver.util.RandomDataGeneratorFactory;
import com.ldbc.driver.util.temporal.Duration;
import com.ldbc.driver.util.temporal.Time;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.matchers.JUnitMatchers.*;

public class StartTimeOperationGeneratorWrapperTest
{
    @Test
    public void shouldAssignStartTimesToOperations()
    {
        // Given
        long firstNanoTime = 1000;
        long incrementNanoTimeBy = 100;
        int testIterations = 10;

        // When
        Generator<Operation<?>> operationGenerator = new CappedGeneratorWrapper<Operation<?>>(
                new OperationGenerator(), testIterations );

        Function<Long, Time> timeFromLongFun = new Function<Long, Time>()
        {
            @Override
            public Time apply( Long from )
            {
                return Time.fromNano( from );
            }
        };

        Generator<Long> countGenerator = new GeneratorBuilder( new RandomDataGeneratorFactory() ).counterGenerator(
                firstNanoTime, incrementNanoTimeBy ).build();
        Generator<Time> counterStartTimeGenerator = new MapGeneratorWrapper<Long, Time>( countGenerator,
                timeFromLongFun );
        Generator<Operation<?>> startTimeOperationGenerator = new StartTimeOperationGeneratorWrapper(
                counterStartTimeGenerator, operationGenerator );

        // Then
        int count = 0;
        Time lastTime = Time.fromNano( firstNanoTime ).minus( Duration.fromNano( incrementNanoTimeBy ) );
        while ( startTimeOperationGenerator.hasNext() )
        {
            Operation<?> operation = startTimeOperationGenerator.next();
            assertThat( operation.getScheduledStartTime(),
                    is( lastTime.plus( Duration.fromNano( incrementNanoTimeBy ) ) ) );
            lastTime = operation.getScheduledStartTime();
            count++;
        }
        assertThat( count, is( testIterations ) );
    }

    static class OperationGenerator extends Generator<Operation<?>>
    {
        protected OperationGenerator()
        {
            super( null );
        }

        @Override
        protected Operation<?> doNext() throws GeneratorException
        {
            return new Operation<Object>()
            {
            };
        }
    }
}
