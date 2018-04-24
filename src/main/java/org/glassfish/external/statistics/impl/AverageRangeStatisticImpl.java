/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

    private long currentVal = 0L;
    private long highWaterMark = Long.MIN_VALUE;
    private long lowWaterMark = Long.MAX_VALUE;
    private long numberOfSamples = 0L;
    private long runningTotal = 0L;

    private final long initCurrentVal;
    private final long initHighWaterMark;
    private final long initLowWaterMark;
    private final long initNumberOfSamples;
    private final long initRunningTotal;

    private final AverageRangeStatistic as = 
            (AverageRangeStatistic) Proxy.newProxyInstance(
            AverageRangeStatistic.class.getClassLoader(),
            new Class[] { AverageRangeStatistic.class },
            this);

    public AverageRangeStatisticImpl(long curVal, long highMark, long lowMark,
                                     String name, String unit, String desc,
                                     long startTime, long sampleTime) {
        super(name, unit, desc, startTime, sampleTime);
        currentVal = curVal;
        initCurrentVal = curVal;
        highWaterMark = highMark;
        initHighWaterMark = highMark;
        lowWaterMark = lowMark;
        initLowWaterMark = lowMark;
        numberOfSamples = 0L;
        initNumberOfSamples = numberOfSamples;
        runningTotal = 0L;
        initRunningTotal = runningTotal;
    }

    public synchronized AverageRangeStatistic getStatistic() {
        return as;
    }

    public synchronized String toString() {
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
    
    public synchronized void reset() {
        super.reset();
        currentVal = initCurrentVal;
        highWaterMark = initHighWaterMark;
        lowWaterMark = initLowWaterMark;
        numberOfSamples = initNumberOfSamples;
        runningTotal = initRunningTotal;
        sampleTime = -1L;
    }    

    public synchronized long getAverage() {
        if(numberOfSamples == 0) {
            return -1;
        } else {
            return runningTotal / numberOfSamples;
        }
    }
    
    public synchronized long getCurrent() {
        return currentVal;
    }

    public synchronized void setCurrent(long curVal) {
        currentVal = curVal;
        lowWaterMark = (curVal >= lowWaterMark ? lowWaterMark : curVal);
        highWaterMark = (curVal >= highWaterMark ? curVal : highWaterMark);
        numberOfSamples++;
        runningTotal += curVal;
        sampleTime = System.currentTimeMillis();
    }
    
    public synchronized long getHighWaterMark() {
        return highWaterMark;
    }
    
    public synchronized long getLowWaterMark() {
        return lowWaterMark;
    }

    // todo: equals implementation
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        checkMethod(method);

        Object result;
        try {
            result = method.invoke(this, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (Exception e) {
            throw new RuntimeException("unexpected invocation exception: " +
                       e.getMessage());
        }
        return result;
    }
    
}
