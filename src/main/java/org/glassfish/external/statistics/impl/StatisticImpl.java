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
import org.glassfish.external.statistics.Statistic;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 
 * @author Sreenivas Munnangi
 */
public abstract class StatisticImpl implements Statistic {
    
    private final String statisticName;
    private final String statisticUnit;
    private final String statisticDesc;
    protected long sampleTime = -1L;
    private long startTime;
    public static final String UNIT_COUNT = "count";
    public static final String UNIT_SECOND = "second";
    public static final String UNIT_MILLISECOND = "millisecond";
    public static final String UNIT_MICROSECOND = "microsecond";
    public static final String UNIT_NANOSECOND = "nanosecond";
    public static final String START_TIME = "starttime";
    public static final String LAST_SAMPLE_TIME = "lastsampletime";

    protected final Map<String, Object> statMap = new ConcurrentHashMap<String, Object> ();
    
    protected static final String NEWLINE = System.getProperty( "line.separator" );

    protected StatisticImpl(String name, String unit, String desc, 
                          long start_time, long sample_time) {

        if (isValidString(name)) {
            statisticName = name;
        } else {
            statisticName = "name";
        }

        if (isValidString(unit)) {
            statisticUnit = unit;
        } else {
            statisticUnit = "unit";
        }

        if (isValidString(desc)) {
            statisticDesc = desc;
        } else {
            statisticDesc = "description";
        }

        startTime = start_time;
        sampleTime = sample_time;
    }

    protected StatisticImpl(String name, String unit, String desc) {
        this(name, unit, desc, System.currentTimeMillis(), System.currentTimeMillis());
    }

    public synchronized Map getStaticAsMap() {
        if (isValidString(statisticName)) {
            statMap.put("name", statisticName);
        }
        if (isValidString(statisticUnit)) {
            statMap.put("unit", statisticUnit);
        }
        if (isValidString(statisticDesc)) {
            statMap.put("description", statisticDesc);
        }
        statMap.put(StatisticImpl.START_TIME, startTime);
        statMap.put(StatisticImpl.LAST_SAMPLE_TIME, sampleTime);
        return statMap;
    }
    
    public String getName() {
        return this.statisticName;
    }
    
    public String getDescription() {
        return this.statisticDesc;
    }
    
    public String getUnit() {
        return this.statisticUnit;
    }
    
    public synchronized long getLastSampleTime() {
        return sampleTime;
    }

    public synchronized long getStartTime() {
        return startTime;
    }

    public synchronized void reset() {
        startTime = System.currentTimeMillis();
    }

    public synchronized String toString() {
        return "Statistic " + getClass().getName() + NEWLINE +
            "Name: " + getName() + NEWLINE +
            "Description: " + getDescription() + NEWLINE +
            "Unit: " + getUnit() + NEWLINE +
            "LastSampleTime: " + getLastSampleTime() + NEWLINE +
            "StartTime: " + getStartTime();
    }

    protected static boolean isValidString(String str) {
        return (str!=null && str.length()>0);
    }

    protected void checkMethod(Method method) {
        if (method == null || method.getDeclaringClass() == null
                || !Statistic.class.isAssignableFrom(method.getDeclaringClass())
                || Modifier.isStatic(method.getModifiers())) {
            throw new RuntimeException("Invalid method on invoke");
        }
    }

}
