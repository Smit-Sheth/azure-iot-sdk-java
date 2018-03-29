// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package tests.unit.com.microsoft.azure.sdk.iot.device.transport;

import com.microsoft.azure.sdk.iot.device.transport.RetryDecision;
import com.microsoft.azure.sdk.iot.device.transport.ExponentialBackoffWithJitter;
import com.microsoft.azure.sdk.iot.device.transport.RetryPolicy;
import javafx.util.Duration;
import java.util.Random;
import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExponentialBackoffWithJitterTest
{
    @Mocked
    Random mockedRandom;

    // Tests_SRS_EXPONENTIALBACKOFF_28_001: [If the retryCount is less than or equal to 0, the function shall throw an IllegalArgumentException.]
    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsWithZeroRetryCount()
    {
        //act
        final RetryPolicy exp = new ExponentialBackoffWithJitter(
                0, Duration.millis(100), Duration.seconds(10), Duration.millis(100), true);
    }

    // Tests_SRS_EXPONENTIALBACKOFF_28_002: [Constructor should save retryCount, minBackoff, maxBackoff, deltaBackoff and firstFastRetry]
    @Test
    public void constructorSavesParameterToLocal()
    {
        //act
        final RetryPolicy exp = new ExponentialBackoffWithJitter(
                10, Duration.seconds(2), Duration.seconds(2), Duration.seconds(2), false);

        // assert
        assertEquals(10, Deencapsulation.getField(exp, "retryCount"));
        assertEquals(Duration.seconds(2), Deencapsulation.getField(exp, "minBackoff"));
        assertEquals(Duration.seconds(2), Deencapsulation.getField(exp, "maxBackoff"));
        assertEquals(Duration.seconds(2), Deencapsulation.getField(exp, "deltaBackoff"));
        assertFalse((boolean)Deencapsulation.getField(exp, "firstFastRetry"));
    }

    // Tests_SRS_EXPONENTIALBACKOFF_28_006: [Constructor should have default values retryCount, minBackoff, maxBackoff, deltaBackoff and firstFastRetry]
    @Test
    public void constructorHaveDefaultValues()
    {
        //act
        final RetryPolicy exp = new ExponentialBackoffWithJitter();

        // assert
        assertEquals(Integer.MAX_VALUE, Deencapsulation.getField(exp, "retryCount"));
        assertEquals(Duration.millis(100), Deencapsulation.getField(exp, "minBackoff"));
        assertEquals(Duration.seconds(10), Deencapsulation.getField(exp, "maxBackoff"));
        assertEquals(Duration.millis(100), Deencapsulation.getField(exp, "deltaBackoff"));
        assertTrue((boolean)Deencapsulation.getField(exp, "firstFastRetry"));
    }

    // Tests_SRS_EXPONENTIALBACKOFF_28_003: [The function shall indicate immediate retry on first retry if firstFastRetry is true]
    @Test
    public void shouldRetryResultWithFirstFastRetry()
    {
        // arrange
        final RetryPolicy exp = new ExponentialBackoffWithJitter(
            10, Duration.millis(100), Duration.seconds(10), Duration.millis(100), true);
        RetryDecision expected = new RetryDecision(true, Duration.ZERO);

        // act
        RetryDecision actual = exp.getRetryDecision(0, null);

        //assert
        assertEquals(true, actual.shouldRetry());
        assertEquals(Duration.ZERO, actual.getDuration());
    }

    // Tests_SRS_EXPONENTIALBACKOFF_28_004: [The function shall return non-zero wait time on first retry if firstFastRetry is false]
    @Test
    public void shouldRetryResultWithNoFirstFastRetry()
    {
        // arrange
        final RetryPolicy exp = new ExponentialBackoffWithJitter(
                10, Duration.millis(100), Duration.seconds(10), Duration.millis(100), false);

        // act
        RetryDecision actual = exp.getRetryDecision(0, null);

        //assert
        assertTrue(actual.shouldRetry());
        assertTrue(actual.getDuration().greaterThan(Duration.ZERO));
    }

    // Tests_SRS_EXPONENTIALBACKOFF_28_005: [The function shall return waitTime according to
    //      F(x) = min(Cmin+ (2^(x-1)-1) * rand(C * (1 – Jd), C*(1-Ju)), Cmax) where  x is the xth retry.]
    @Test
    public void shouldRetryResultWithOnlyCurrentRetryCount()
    {
        // arrange
        final RetryPolicy exp = new ExponentialBackoffWithJitter(
                10, Duration.ZERO, Duration.ZERO, Duration.ZERO, true);
        Deencapsulation.setField(exp, "random", mockedRandom);

        new NonStrictExpectations()
        {
            {
                mockedRandom.nextInt(anyInt);
                result = 0;
            }
        };

        // act
        RetryDecision actual = exp.getRetryDecision(1, null);

        //assert
        assertTrue(actual.shouldRetry());
        assertEquals(Duration.ZERO, actual.getDuration());
    }

    // Tests_SRS_EXPONENTIALBACKOFF_28_005: [The function shall return waitTime according to
    //      F(x) = min(Cmin+ (2^(x-1)-1) * rand(C * (1 – Jd), C*(1-Ju)), Cmax) where  x is the xth retry.]
    @Test
    public void shouldRetryResultWithMinMaxBackOffReturnsMinBackOff()
    {
        // arrange
        final RetryPolicy exp = new ExponentialBackoffWithJitter(
                10, Duration.millis(10), Duration.millis(100), Duration.ZERO, true);
        Deencapsulation.setField(exp, "random", mockedRandom);

        new NonStrictExpectations()
        {
            {
                mockedRandom.nextInt(anyInt);
                result = 0;
            }
        };

        // act
        RetryDecision actual = exp.getRetryDecision(1, null);

        //assert
        assertTrue(actual.shouldRetry());
        assertEquals(Duration.millis(10), actual.getDuration());
    }

    // Tests_SRS_EXPONENTIALBACKOFF_28_005: [The function shall return waitTime according to
    //      F(x) = min(Cmin+ (2^(x-1)-1) * rand(C * (1 – Jd), C*(1-Ju)), Cmax) where  x is the xth retry.]
    @Test
    public void shouldRetryResultWithLargeCurrentRetryCountReturnsMaxBackOff()
    {
        // arrange
        final RetryPolicy exp = new ExponentialBackoffWithJitter(
                10000, Duration.millis(10), Duration.millis(100), Duration.millis(10), true);
        Deencapsulation.setField(exp, "random", mockedRandom);

        new NonStrictExpectations()
        {
            {
                mockedRandom.nextInt(anyInt);
                result = 0;
            }
        };

        // act
        RetryDecision actual = exp.getRetryDecision(999, null);

        //assert
        assertTrue(actual.shouldRetry());
        assertEquals(Duration.millis(100), actual.getDuration());
    }

    // Tests_SRS_EXPONENTIALBACKOFF_28_005: [The function shall return waitTime according to
    //      F(x) = min(Cmin+ (2^(x-1)-1) * rand(C * (1 – Jd), C*(1-Ju)), Cmax) where  x is the xth retry.]
    @Test
    public void shouldRetryResultRetry1stTime()
    {
        // arrange
        final RetryPolicy exp = new ExponentialBackoffWithJitter(
                10000, Duration.millis(10), Duration.millis(100), Duration.millis(10), true);
        final double deltaBackoffLowBound = 10 * 0.8;
        final int count = 2;
        Deencapsulation.setField(exp, "random", mockedRandom);

        new NonStrictExpectations()
        {
            {
                mockedRandom.nextInt(anyInt);
                result = 0;
            }
        };

        // act
        RetryDecision actual = exp.getRetryDecision(count, null);

        //assert
        int expected = (int)((Math.pow(2.0, (double)count) - 1.0) * deltaBackoffLowBound) + 10;
        assertTrue(actual.shouldRetry());
        assertEquals(Duration.millis(expected), actual.getDuration());
    }
}


