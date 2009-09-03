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
            final String type,
            final T callback)
    {
        this(server, type, null, callback);
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
            final String type,
            final String name,
            final T callback)
    {
        mMBeanServer = server;
        mType = type;
        mName = name;
        mObjectName = null;
        mCallback = callback;
    }

    /**
        Listen for the registration of AMX DomainRoot 
        Listening starts automatically.
     */
    public static <T extends Callback> MBeanListener<T> listenForDomainRoot(
        final MBeanServerConnection server,
        final T callback)
    {
        final MBeanListener<T> listener = new MBeanListener<T>( server, AMXGlassfish.DEFAULT.domainRoot(), callback);
        listener.startListening();
        return listener;
    }

    private static final class WaitForDomainRootListenerCallback extends MBeanListener.CallbackImpl {
        private final MBeanServerConnection mConn;

        public WaitForDomainRootListenerCallback( final MBeanServerConnection conn ) {
            mConn = conn;
        }
        
        @Override
        public void mbeanRegistered(final ObjectName objectName, final MBeanListener listener) {
            super.mbeanRegistered(objectName,listener);
            AMXUtil.invokeWaitAMXReady(mConn);
            mLatch.countDown();
        }
    }

    /**
        Wait until AMX has loaded and is ready for use.
        <p>
        This will <em>not</em> cause AMX to load; it will block forever until AMX is ready. In other words,
        don't call this method unless it's a convenient thread that can wait forever.
     */
    public static ObjectName waitAMXReady( final MBeanServerConnection server)
    {
        final WaitForDomainRootListenerCallback callback = new WaitForDomainRootListenerCallback(server);
        listenForDomainRoot( server, callback );
        callback.await();
        return callback.getRegistered();
    }
    
    /**
        Listen for the registration of the {@link BootAMXMBean}.
        Listening starts automatically.  See {@link AMXBooter#BootAMXCallback}.
     */
    public static <T extends Callback> MBeanListener<T> listenForBootAMX(
        final MBeanServerConnection server,
        final T callback)
    {
        final MBeanListener<T> listener = new MBeanListener<T>( server, BootAMXMBean.OBJECT_NAME, callback);
        listener.startListening();
        return listener;
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

            final ObjectName pattern = AMXUtil.newObjectName(AMXGlassfish.DEFAULT.amxJMXDomain(), props);
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
            else if ( objectName.getDomain().equals( AMXGlassfish.DEFAULT.amxJMXDomain() ) )
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



