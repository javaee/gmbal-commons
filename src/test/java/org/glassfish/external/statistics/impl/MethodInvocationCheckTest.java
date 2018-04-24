/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2018 Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;

/**
 * Since management-api code is being integrated into jdk it is important not to allow invoking any methods via
 * reflection. This tests additional checks added into method invoke
 */
public class MethodInvocationCheckTest {

    private static final Logger LOGGER = Logger.getLogger(MethodInvocationCheckTest.class.getName());

    public Throwable t = null;
    private int fails = 0;

    @Test
    public void test() throws Throwable {
        checkImpl(new AverageRangeStatisticImpl(0l, 0l, 0l, null, null, null, 0l, 0l));
        checkImpl(new BoundaryStatisticImpl(0l, 0l, null, null, null, 0l, 0l));
        checkImpl(new BoundedRangeStatisticImpl(0l, 0l, 0l, 0l, 0l, null, null, null, 0l, 0l));
        checkImpl(new CountStatisticImpl(0l, null, null, null, 0l, 0l));
        checkImpl(new RangeStatisticImpl(0l, 0l, 0l, null, null, null, 0l, 0l));
        checkImpl(new StringStatisticImpl(null, null, null));
        checkImpl(new TimeStatisticImpl(0l, 0l, 0l, 0l, null, null, null, 0l, 0l));

        // at least one check failed >> fail
        if (fails > 0) {
            LOGGER.log(INFO,  "failed checks total = " + fails);

            // can't be null
            throw t;
        }
    }

    private void checkImpl(StatisticImpl statistic) throws Throwable {
        Method unknownMethod = Unknown.class.getMethod("invokeme");
        LOGGER.log(INFO,  "unknownMethod = " + unknownMethod);
        checkExceptionThrown(new Unknown(), statistic, unknownMethod, null);

        Method staticMethod = Extended.class.getMethod("invokeme");
        LOGGER.log(INFO,  "staticMethod = " + staticMethod);
        checkExceptionThrown(null, statistic, staticMethod, new String[0]);

        checkExceptionThrown(null, statistic, null, null);
    }

    private void checkExceptionThrown(Object proxy, StatisticImpl statistic, Method method, Object[] args) throws Throwable {
        try {

            if (statistic instanceof AverageRangeStatisticImpl)
                ((AverageRangeStatisticImpl) statistic).invoke(proxy, method, args);
            else if (statistic instanceof BoundaryStatisticImpl)
                ((BoundaryStatisticImpl) statistic).invoke(proxy, method, args);
            else if (statistic instanceof BoundedRangeStatisticImpl)
                ((BoundedRangeStatisticImpl) statistic).invoke(proxy, method, args);
            else if (statistic instanceof CountStatisticImpl)
                ((CountStatisticImpl) statistic).invoke(proxy, method, args);
            else if (statistic instanceof RangeStatisticImpl)
                ((RangeStatisticImpl) statistic).invoke(proxy, method, args);
            else if (statistic instanceof StringStatisticImpl)
                ((StringStatisticImpl) statistic).invoke(proxy, method, args);
            else if (statistic instanceof TimeStatisticImpl)
                ((TimeStatisticImpl) statistic).invoke(proxy, method, args);
            else
                throw new IllegalStateException("Unknown tested object class: [" + statistic.getClass().getName() + "] - problem in test");

            LOGGER.log(INFO,  "TEST FAILED, expected exception not thrown.");
        } catch (Throwable throwable) {
            checkException(throwable);
        }
    }

    private void checkException(Throwable throwable) throws Throwable {

        if (!(throwable instanceof RuntimeException) || !"Invalid method on invoke".equals(throwable.getMessage())) {
            LOGGER.log(INFO,  "TEST FAILED, unexpected throwable cought.");
            throwable.printStackTrace();
            t = throwable;
            fails++;
        } else {
            LOGGER.log(INFO,  "TEST PASSED.");
        }

    }

    public static class Extended extends StatisticImpl {

        protected Extended(String name, String unit, String desc) {
            super(name, unit, desc);
        }

        @SuppressWarnings("unused")
        public static void invokeme() {
            throw new RuntimeException("This method shouldn't be invoked - it's static! If it is, it's failure!");
        }
    }

    public static class Unknown {

        @SuppressWarnings("unused")
        public void invokeme() {
            throw new RuntimeException("This method shouldn't be invoked - it's static! If it is, it's failure!");
        }
    }

    public static void main(String[] args) throws Throwable {
        new MethodInvocationCheckTest().test();
    }

}
