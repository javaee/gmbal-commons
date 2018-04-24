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

package org.glassfish.external.amx;

import java.util.Set;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import java.util.concurrent.CountDownLatch;

import static org.glassfish.external.amx.AMX.*;

/**
 * Listens for registration of MBeans of various types.
 * Intended usage is for subsystems to lazy-load only when the Parent
 * MBean is registered.
 */
@org.glassfish.external.arc.Taxonomy(stability = org.glassfish.external.arc.Stability.UNCOMMITTED)
public class MBeanListener<T extends MBeanListener.Callback> implements NotificationListener
{
    private static void debug(final Object o) { System.out.println( "" + o ); }
    
    /** listen for MBeans in a given domain of a given type[name] 
        OR an ObjectName (below) */
    private final String mJMXDomain;
    private final String mType;
    private final String mName;
    
    /** mType and mName should be null if mObjectName is non-null, and vice versa */
    private final ObjectName mObjectName;

    private final MBeanServerConnection mMBeanServer;

    private final T mCallback;

    public String toString()
    {
        return "MBeanListener: ObjectName=" + mObjectName + ", type=" + mType + ", name=" + mName;
    }
    
    public String getType()
    {
        return mType;
    }

    public String getName()
    {
        return mName;
    }

    public MBeanServerConnection getMBeanServer()
    {
        return mMBeanServer;
    }
    
    /** Callback interface.  */
    public interface Callback
    {
        public void mbeanRegistered(final ObjectName objectName, final MBeanListener listener);
        public void mbeanUnregistered(final ObjectName objectName, final MBeanListener listener);
    }
    
    /**
        Default callback implementation, can be subclassed if needed
        Remembers only the last MBean that was seen.
     */
    public static class CallbackImpl implements MBeanListener.Callback
    {
        private volatile ObjectName mRegistered = null;
        private volatile ObjectName mUnregistered = null;
        private final boolean mStopAtFirst;
        
        public CallbackImpl() {
            this(true);
        }
        
        public CallbackImpl(final boolean stopAtFirst)
        {
            mStopAtFirst = stopAtFirst;
        }
        
        public ObjectName getRegistered()   { return mRegistered; }
        public ObjectName getUnregistered() { return mUnregistered; }
        
        protected final CountDownLatch mLatch = new CountDownLatch(1);
        
        /** Optional: wait for the CountDownLatch to fire
            If used, the subclass should countDown() the latch when the 
            appropriate event happens
        */
        public void await()
        {
            try
            {
                mLatch.await(); // wait until BootAMXMBean is ready
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }

        public void mbeanRegistered(final ObjectName objectName, final MBeanListener listener)
        {
            mRegistered = objectName;
            if ( mStopAtFirst )
            {
                listener.stopListening();
            }
        }
        public void mbeanUnregistered(final ObjectName objectName, final MBeanListener listener)
        {
            mUnregistered = objectName;
            if ( mStopAtFirst )
            {
                listener.stopListening();
            }
        }
    }
    
    public T getCallback()
    {
        return mCallback;
    }
 
    /**
     * Listener for a specific MBean.
     * Caller must call {@link #start} to start listening.
     * @param server
     * @param objectName
     * @param callback
     */
    public MBeanListener(
            final MBeanServerConnection server,
            final ObjectName objectName,
            final T callback)
    {
        mMBeanServer = server;
        mObjectName = objectName;
        mJMXDomain = null;
        mType = null;
        mName = null;
        mCallback = callback;
    }
    
    /**
     * Listener for all MBeans of specified type, with or without a name.
     * Caller must call {@link #start} to start listening.
     * @param server
     * @param type type of the MBean (as found in the ObjectName)
     * @param callback
     */
    public MBeanListener(
            final MBeanServerConnection server,
            final String domain,
            final String type,
            final T callback)
    {
        this(server, domain, type, null, callback);
    }

    /**
     * Listener for MBeans of specified type, with specified name (or any name
     * if null is passed for the name).
     * Caller must call {@link #start} to start listening.
     * @param server
     * @param type type of the MBean (as found in the ObjectName)
     * @param name name of the MBean, or null if none
     * @param callback
     */
    public MBeanListener(
            final MBeanServerConnection server,
            final String domain,
            final String type,
            final String name,
            final T callback)
    {
        mMBeanServer = server;
        mJMXDomain = domain;
        mType = type;
        mName = name;
        mObjectName = null;
        mCallback = callback;
    }

  
    private boolean isRegistered( final MBeanServerConnection conn, final ObjectName objectName )
    {
        try
        {
            return conn.isRegistered(objectName);
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
    Start listening.  If the required MBean(s) are already present, the callback
    will be synchronously made before returning.  It is also possible that the
    callback could happen twice for the same MBean.
     */
    public void startListening()
    {
        // race condition: must listen *before* looking for existing MBeans
        try
        {
            mMBeanServer.addNotificationListener( AMXUtil.getMBeanServerDelegateObjectName(), this, null, this);
        }
        catch (final Exception e)
        {
            throw new RuntimeException("Can't add NotificationListener", e);
        }

        if ( mObjectName != null )
        {
            if ( isRegistered(mMBeanServer, mObjectName) )
            {
                mCallback.mbeanRegistered(mObjectName, this);
            }
        }
        else
        {
            // query for AMX MBeans of the requisite type
            String props = TYPE_KEY + "=" + mType;
            if (mName != null)
            {
                props = props + "," + NAME_KEY + mName;
            }

            final ObjectName pattern = AMXUtil.newObjectName(mJMXDomain + ":" +props);
            try
            {
                final Set<ObjectName> matched = mMBeanServer.queryNames(pattern, null);
                for (final ObjectName objectName : matched)
                {
                    mCallback.mbeanRegistered(objectName, this);
                }
            }
            catch( final Exception e )
            {
                throw new RuntimeException(e);
            }
        }
    }
    
    
    /** unregister the listener */
    public void stopListening()
    {
        try
        {
            mMBeanServer.removeNotificationListener( AMXUtil.getMBeanServerDelegateObjectName(), this);
        }
        catch (final Exception e)
        {
            throw new RuntimeException("Can't remove NotificationListener " + this, e);
        }
    }

    public void handleNotification(
            final Notification notifIn,
            final Object handback)
    {
        if (notifIn instanceof MBeanServerNotification)
        {
            final MBeanServerNotification notif = (MBeanServerNotification) notifIn;
            final ObjectName objectName = notif.getMBeanName();

            boolean match = false;
            if ( mObjectName != null && mObjectName.equals(objectName) )
            {
                match = true;
            }
            else if ( objectName.getDomain().equals( mJMXDomain ) )
            {
                if ( mType != null && mType.equals(objectName.getKeyProperty(TYPE_KEY)) )
                {
                    final String mbeanName = objectName.getKeyProperty(NAME_KEY);
                    if (mName != null && mName.equals(mbeanName))
                    {
                        match = true;
                    }
                }
            }
            
            if ( match )
            {
                final String notifType = notif.getType();
                if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(notifType))
                {
                    mCallback.mbeanRegistered(objectName, this);
                }
                else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(notifType))
                {
                    mCallback.mbeanUnregistered(objectName, this);
                }
            }
        }
    }

}



