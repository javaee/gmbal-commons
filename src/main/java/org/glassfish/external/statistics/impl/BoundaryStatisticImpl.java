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
import org.glassfish.external.statistics.BoundaryStatistic;
import java.util.Map;
import java.lang.reflect.*;

/** 
 * @author Sreenivas Munnangi
 */
public final class BoundaryStatisticImpl extends StatisticImpl 
    implements BoundaryStatistic, InvocationHandler {
    
    private final long lowerBound;
    private final long upperBound;
	
    private final BoundaryStatistic bs = 
            (BoundaryStatistic) Proxy.newProxyInstance(
            BoundaryStatistic.class.getClassLoader(),
            new Class[] { BoundaryStatistic.class },
            this);

    public BoundaryStatisticImpl(long lower, long upper, String name,
                                 String unit, String desc, long startTime,
                                 long sampleTime) {
        super(name, unit, desc, startTime, sampleTime);
        upperBound = upper;
        lowerBound = lower;
    }

    public synchronized BoundaryStatistic getStatistic() {
        return bs;
    }
    
    public synchronized Map getStaticAsMap() {
        Map m = super.getStaticAsMap();
        m.put("lowerbound", getLowerBound());
        m.put("upperbound", getUpperBound());
        return m;
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
