package com.ldbc.driver.util;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import com.ldbc.driver.util.temporal.Time;

public class RandomDataGeneratorFactory
{

    private final RandomGenerator random;
    private final Long seed;

    public RandomDataGeneratorFactory()
    {
        this.random = getRandomGenerator( Time.now().asNano() );
        this.seed = null;
    }

    public RandomDataGeneratorFactory( RandomGenerator random )
    {
        this.random = random;
        this.seed = null;
    }

    public RandomDataGeneratorFactory( Long seed )
    {
        this.random = null;
        this.seed = seed;
    }

    public RandomDataGenerator newRandom( long seed )
    {
        return new RandomDataGenerator( getRandomGenerator( seed ) );
    }

    public RandomDataGenerator newRandom()
    {
        return new RandomDataGenerator( getRandomGenerator( getSeed() ) );
    }

    private RandomGenerator getRandomGenerator( long seed )
    {
        /* 
         * From Docs: http://commons.apache.org/proper/commons-math/javadocs/api-3.2/index.html
         * If no RandomGenerator is provided in the constructor, the default is to use a Well19937c generator
         */
        return new Well19937c( seed );
    }

    private Long getSeed()
    {
        return ( null == random ) ? seed : random.nextLong();
    }

}
