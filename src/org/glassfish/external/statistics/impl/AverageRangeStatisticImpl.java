/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.external.statistics.impl;

import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.lang.reflect.*;
import org.glassfish.external.statistics.AverageRangeStatistic;

/**
 * An implementation of AverageRangeStatistic that provides ways to change the
 * state externally through mutators.  Convenience class that is useful for
 * components that gather the statistical data.
 * By merely changing the count (which is a mandatory measurement), rest of the statistical
 * information could be deduced.
 */

public final class AverageRangeStatisticImpl extends StatisticImpl implements
        AverageRangeStatistic, InvocationHandler {

    /** DEFAULT_UPPER_BOUND is maximum value Long can attain */
    public static final long DEFAULT_MAX_BOUND = java.lang.Long.MAX_VALUE;

    private AtomicLong currentVal = new AtomicLong(Long.MIN_VALUE);
    private AtomicLong highWaterMark = new AtomicLong(Long.MIN_VALUE);
    private AtomicLong lowWaterMark = new AtomicLong(Long.MAX_VALUE);
    private long                         numberOfSamples;
    private long                         runningTotal;

    private AverageRangeStatistic as = (AverageRangeStatistic) Proxy.newProxyInstance(
            AverageRangeStatistic.class.getClassLoader(),
            new Class[] { AverageRangeStatistic.class },
            this);

    /**
     * Constructs an mutable instance of AverageRangeStatisticImpl.
     * @param curVal    The current value of this statistic
     * @param highMark  The highest value of this statistic, since measurement
     *                  started
     * @param lowMark   The lowest value of this statistic, since measurement
     *                  started
     * @param name      The name of the statistic
     * @param unit      The unit of measurement for this statistic
     * @param desc      A brief description of the statistic
     * @param startTime Time in milliseconds at which the measurement was started
     * @param sampleTime Time at which the last measurement was done.
     * @param numberOfSamples number of samples at present
     * @param runningTotal running total of sampled data at present
     **/
    public AverageRangeStatisticImpl(long curVal, long highMark, long lowMark,
                                     String name, String unit, String desc,
                                     long startTime, long sampleTime) {
        super(name, unit, desc, startTime, sampleTime);
        currentVal.set(curVal);
        highWaterMark.set(highMark);
        lowWaterMark.set(lowMark);
        numberOfSamples = 0L;
        runningTotal = 0L;
    }

    public synchronized AverageRangeStatistic getStatistic() {
        return as;
    }

    public String toString() {
        return super.toString() + NEWLINE +
            "Current: " + getCurrent() + NEWLINE +
            "LowWaterMark: " + getLowWaterMark() + NEWLINE +
            "HighWaterMark: " + getHighWaterMark() + NEWLINE +
            "Average:" + getAverage();
    }

    public synchronized Map getStaticAsMap() {
        Map m = super.getStaticAsMap();
        m.put("current", getCurrent());
        m.put("lowwatermark", getLowWaterMark());
        m.put("highwatermark", getHighWaterMark());
        m.put("average", getAverage());
        return m;
    }
    
    public void reset() {
        super.reset();
        currentVal.set(Long.MIN_VALUE);
        highWaterMark.set(Long.MIN_VALUE);
        lowWaterMark.set(Long.MAX_VALUE);
        this.resetAverageStats();
    }
    
    private void resetAverageStats() {
        numberOfSamples = 0L;
        runningTotal = 0L;
    }    

    public void setCount(long current) {
        this.currentVal.set(current);
        super.setLastSampleTime(System.currentTimeMillis());
        this.lowWaterMark.set((current < this.lowWaterMark.get()) ? (current) : (this.lowWaterMark.get()));
        this.highWaterMark.set((current > this.highWaterMark.get()) ? (current) : (this.highWaterMark.get()));
        if(DEFAULT_MAX_BOUND - runningTotal < current) {
            this.resetAverageStats();
        }
        numberOfSamples++;
        runningTotal += current;
    }
    
    public long getAverage() {
        if(numberOfSamples == 0) {
            return -1;
        } else {
            return runningTotal / numberOfSamples;
        }
    }
    
    public long getCurrent() {
        return currentVal.get();
    }
    public void setCurrent(long curVal) {
        currentVal.set(curVal);
    }
    
    public long getHighWaterMark() {
        return highWaterMark.get();
    }
    public void setHighWaterMark(long highMark) {
        highWaterMark.set(highMark);
    }
    
    public long getLowWaterMark() {
        long result = lowWaterMark.get();
        if(result == DEFAULT_MAX_BOUND) {
            result = 0L;
        }
        return result;
    }
    public void setLowWaterMark(long lowMark) {
        lowWaterMark.set(lowMark);
    }

    // todo: equals implementation
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result;
        try {
            result = method.invoke(this, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (Exception e) {
            throw new RuntimeException("unexpected invocation exception: " +
                       e.getMessage());
        } finally {
        }
        return result;
    }
    
}
