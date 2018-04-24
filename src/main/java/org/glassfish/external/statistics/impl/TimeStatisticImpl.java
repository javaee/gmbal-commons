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

import org.glassfish.external.statistics.TimeStatistic;
import java.util.Map;
import java.lang.reflect.*;

/** 
 * @author Sreenivas Munnangi
 */
public final class TimeStatisticImpl extends StatisticImpl 
    implements TimeStatistic, InvocationHandler {
    
    private long count = 0L;
    private long maxTime = 0L;
    private long minTime = 0L;
    private long totTime = 0L;
    private final long initCount;
    private final long initMaxTime;
    private final long initMinTime;
    private final long initTotTime;

    private final TimeStatistic ts = 
            (TimeStatistic) Proxy.newProxyInstance(
            TimeStatistic.class.getClassLoader(),
            new Class[] { TimeStatistic.class },
            this);

    public synchronized final String toString() {
        return super.toString() + NEWLINE + 
            "Count: " + getCount() + NEWLINE +
            "MinTime: " + getMinTime() + NEWLINE +
            "MaxTime: " + getMaxTime() + NEWLINE +
            "TotalTime: " + getTotalTime();
    }

    public TimeStatisticImpl(long counter, long maximumTime, long minimumTime,
                             long totalTime, String name, String unit, 
                             String desc, long startTime, long sampleTime) {
        super(name, unit, desc, startTime, sampleTime);
        count = counter;
        initCount = counter;
        maxTime = maximumTime;
        initMaxTime = maximumTime;
        minTime = minimumTime;
        initMinTime = minimumTime;
        totTime = totalTime;
        initTotTime = totalTime;
    }

    public synchronized TimeStatistic getStatistic() {
        return ts;
    }
    
    public synchronized Map getStaticAsMap() {
        Map m = super.getStaticAsMap();
        m.put("count", getCount());
        m.put("maxtime", getMaxTime());
        m.put("mintime", getMinTime());
        m.put("totaltime", getTotalTime());
        return m;
    }

     public synchronized void incrementCount(long current) {
        if (count == 0) {
            totTime = current;
            maxTime = current;
            minTime = current;
        } else {
            totTime = totTime + current;
            maxTime = (current >= maxTime ? current : maxTime);
            minTime = (current >= minTime ? minTime : current);
        }
        count++;
        sampleTime = System.currentTimeMillis();
     }

    /**
     * Returns the number of times an operation was invoked 
     */
    public synchronized long getCount() {
        return count;
    }
    
    /**
     * Returns the maximum amount of time that it took for one invocation of an
     * operation, since measurement started.
     */
    public synchronized long getMaxTime() {
        return maxTime;
    }
    
    /**
     * Returns the minimum amount of time that it took for one invocation of an
     * operation, since measurement started.
     */
    public synchronized long getMinTime() {
        return minTime;
    }    

    /**
     * Returns the amount of time that it took for all invocations, 
     * since measurement started.
     */
    public synchronized long getTotalTime() {
        return totTime;
    }

    @Override
    public synchronized void reset() {
        super.reset();
        count = initCount;
        maxTime = initMaxTime;
        minTime = initMinTime;
        totTime = initTotTime;
        sampleTime = -1L;
    }

    // todo: equals implementation
    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        checkMethod(m);

        Object result;
        try {
            result = m.invoke(this, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (Exception e) {
            throw new RuntimeException("unexpected invocation exception: " +
                       e.getMessage());
        }
        return result;
    }
}
