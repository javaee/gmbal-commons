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
package org.glassfish.external.amx;

import javax.management.ObjectName;

/**
 * AMX behavior specific to Glassfish V3.
 */
public final class AMXGlassfish
{
    public static final String DEFAULT_JMX_DOMAIN = "amx";
    
    /** Default domain support */
    public static final AMXGlassfish DEFAULT = new AMXGlassfish(DEFAULT_JMX_DOMAIN);
    
    private final String     mJMXDomain;
    private final ObjectName mDomainRoot;
    
    /** Anything other than {@link #DEFAULT} is not supported in Glassfish V3 */
    public AMXGlassfish(final String jmxDomain)
    {
        mJMXDomain = jmxDomain;
        mDomainRoot = newObjectName("", "domain-root", null);
    }

    /** JMX domain used by AMX MBeans.
     * <p>
     * All MBeans in this domain must be AMX-compliant, see http://tinyurl.com/nryoqp =
    https://glassfish.dev.java.net/nonav/v3/admin/planning/V3Changes/V3_AMX_SPI.html
    */
    public String amxJMXDomain()
    {
        return mJMXDomain;
    }
    
    /** JMX domain used by AMX support MBeans.  Private use only */
    public String amxSupportDomain()
    {
        return amxJMXDomain() + "-support";
    }

    /** name of the Domain Admin Server (DAS) as found in an ObjectName */
    public String dasName()
    {
        return "server";
    }
    
    /** name of the Domain Admin Server (DAS) &lt;config> */
    public String dasConfig()
    {
        return dasName() + "-config";
    }

    /** return the ObjectName of the AMX DomainRoot MBean */
    public ObjectName domainRoot()
    {
        return mDomainRoot;
    }

    /** ObjectName for top-level monitoring MBean (parent of those for each server) */
    public ObjectName monitoringRoot()
    {
        return newObjectName("/", "mon", null);
    }

    /** ObjectName for top-level monitoring MBean for specified server */
    public ObjectName serverMon(final String serverName)
    {
        return newObjectName("/mon", "server-mon", serverName);
    }
    

    /** Make a new AMX ObjectName with unchecked exception.
     *  name must be null to create a singleton ObjectName.
     *  Note that the arguments must not contain the characters
     * @param pp The parent part
     * @param type The ObjectName type
     * @param name The ObjectName name
     * @return The objectname with pp, type, and (optionally) name.
     */
    public ObjectName newObjectName(
            final String pp,
            final String type,
            final String name)
    {
        String props = prop(AMX.PARENT_PATH_KEY, pp) + "," + prop(AMX.TYPE_KEY, type);
        if (name != null) {
            props = props + "," + prop(AMX.NAME_KEY, name);
        }

        return newObjectName( props);
    }

    /** Make a new ObjectName with unchecked exception */
    public ObjectName newObjectName(final String s)
    {
        String name = s;
        if ( ! name.startsWith( amxJMXDomain() ) ) {
            name = amxJMXDomain() + ":" + name;
        }
        
        try {
            return new ObjectName( name );
        } catch( final Exception e ) {
            throw new RuntimeException("bad ObjectName: " + name, e);
        }
    }

    private static String prop(final String key, final String value)
    {
        return key + "=" + value;
    }
}





