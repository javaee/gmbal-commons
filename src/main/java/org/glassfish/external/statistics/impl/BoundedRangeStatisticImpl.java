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

import org.glassfish.external.statistics.BoundedRangeStatistic;
import java.util.Map;
import java.lang.reflect.*;


/** 
 * @author Sreenivas Munnangi
 */
public final class BoundedRangeStatisticImpl extends StatisticImpl 
    implements BoundedRangeStatistic, InvocationHandler {
    
    private long lowerBound = 0L;
    private long upperBound = 0L;
    private long currentVal = 0L;
    private long highWaterMark = Long.MIN_VALUE;
    private long lowWaterMark = Long.MAX_VALUE;
    
    private final long initLowerBound;
    private final long initUpperBound;
    private final long initCurrentVal;
    private final long initHighWaterMark;
    private final long initLowWaterMark;
    
    private final BoundedRangeStatistic bs = 
            (BoundedRangeStatistic) Proxy.newProxyInstance(
            BoundedRangeStatistic.class.getClassLoader(),
            new Class[] { BoundedRangeStatistic.class },
            this);

    public synchronized String toString() {
        return super.toString() + NEWLINE + 
            "Current: " + getCurrent() + NEWLINE +
            "LowWaterMark: " + getLowWaterMark() + NEWLINE +
            "HighWaterMark: " + getHighWaterMark() + NEWLINE +
            "LowerBound: " + getLowerBound() + NEWLINE +
            "UpperBound: " + getUpperBound();
    }


    public BoundedRangeStatisticImpl(long curVal, long highMark, long lowMark,
                                     long upper, long lower, String name,
                                     String unit, String desc, long startTime,
                                     long sampleTime) {
        super(name, unit, desc, startTime, sampleTime);
        currentVal = curVal;
        initCurrentVal = curVal;
        highWaterMark = highMark;
        initHighWaterMark = highMark;
        lowWaterMark = lowMark;
        initLowWaterMark = lowMark;
        upperBound = upper;
        initUpperBound = upper;
        lowerBound = lower;
        initLowerBound = lower;
    }
    
    public synchronized BoundedRangeStatistic getStatistic() {
        return bs;
    }

    public synchronized Map getStaticAsMap() {
        Map m = super.getStaticAsMap();
        m.put("current", getCurrent());
        m.put("lowerbound", getLowerBound());
        m.put("upperbound", getUpperBound());
        m.put("lowwatermark", getLowWaterMark());
        m.put("highwatermark", getHighWaterMark());
        return m;
    }

    public synchronized long getCurrent() {
        return currentVal;
    }
    
    public synchronized void setCurrent(long curVal) {
        currentVal = curVal;
        lowWaterMark = (curVal >= lowWaterMark ? lowWaterMark : curVal);
        highWaterMark = (curVal >= highWaterMark ? curVal : highWaterMark);
        sampleTime = System.currentTimeMillis();
    }

    public synchronized long getHighWaterMark() {
        return highWaterMark;
    }
    
    public synchronized void setHighWaterMark(long hwm) {
        highWaterMark = hwm;
    }
    
    public synchronized long getLowWaterMark() {
        return lowWaterMark;
    }
    
    public synchronized void setLowWaterMark(long lwm) {
        lowWaterMark = lwm;
    }
    
    public synchronized long getLowerBound() {
        return lowerBound;
    }
    
    public synchronized long getUpperBound() {
        return upperBound;
    }
	
    @Override
    public synchronized void reset() {
        super.reset();
        lowerBound = initLowerBound;
        upperBound = initUpperBound;
        currentVal = initCurrentVal;
        highWaterMark = initHighWaterMark;
        lowWaterMark = initLowWaterMark;
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
