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
import org.glassfish.external.statistics.StringStatistic;
import java.util.Map;
import java.lang.reflect.*;

/** 
 * @author Sreenivas Munnangi
 */
public final class StringStatisticImpl extends StatisticImpl
    implements StringStatistic, InvocationHandler {
    
    private volatile String str = null;
    private final String initStr;

    private final StringStatistic ss = 
            (StringStatistic) Proxy.newProxyInstance(
            StringStatistic.class.getClassLoader(),
            new Class[] { StringStatistic.class },
            this);

    public StringStatisticImpl(String str, String name, String unit, 
                              String desc, long sampleTime, long startTime) {
        super(name, unit, desc, startTime, sampleTime);
        this.str = str;
        initStr = str;
    }
    
    public StringStatisticImpl(String name, String unit, String desc) {
        this("", name, unit, desc, System.currentTimeMillis(), System.currentTimeMillis());
    }
    
    public synchronized StringStatistic getStatistic() {
        return ss;
    }

    public synchronized Map getStaticAsMap() {
        Map m = super.getStaticAsMap();
        if (getCurrent() != null) {
            m.put("current", getCurrent());
        }
        return m;
    }

    public synchronized String toString() {
        return super.toString() + NEWLINE + "Current-value: " + getCurrent();
    }

    public String getCurrent() {
        return str;
    }

    public void setCurrent(String str) {
        this.str = str;
        sampleTime = System.currentTimeMillis();
    }

    @Override
    public synchronized void reset() {
        super.reset();
        this.str = initStr;
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
