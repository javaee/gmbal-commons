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

package org.glassfish.external.probe.provider;

import java.util.ArrayList;

/**
 *
 * @author abbagani
 */
public class StatsProviderManager {

   private StatsProviderManager(){
   }

   
   public static boolean register(String configElement, PluginPoint pp,
                                    String subTreeRoot, Object statsProvider) {
        return (register(pp, configElement, subTreeRoot, statsProvider, null));
   }

   public static boolean register(PluginPoint pp, String configElement,
                                  String subTreeRoot, Object statsProvider,
                                  String invokerId) {
        StatsProviderInfo spInfo =
            new StatsProviderInfo(configElement, pp, subTreeRoot, statsProvider, invokerId);

        return registerStatsProvider(spInfo);
   }

   public static boolean register(String configElement, PluginPoint pp,
                                    String subTreeRoot, Object statsProvider,
                                    String configLevelStr) {
        return(register(configElement, pp, subTreeRoot, statsProvider, configLevelStr, null));
   }

   public static boolean register(String configElement, PluginPoint pp,
                                    String subTreeRoot, Object statsProvider,
                                    String configLevelStr, String invokerId) {
        StatsProviderInfo spInfo =
            new StatsProviderInfo(configElement, pp, subTreeRoot, statsProvider, invokerId);
        spInfo.setConfigLevel(configLevelStr);

        return registerStatsProvider(spInfo);
   }

   private synchronized static boolean registerStatsProvider(StatsProviderInfo spInfo) {
      //Ideally want to start this in a thread, so we can reduce the startup time
      if (spmd == null) {
          //Make an entry into the toBeRegistered map
          toBeRegistered.add(spInfo);
      } else {
          spmd.register(spInfo);
          return true;
      }
       return false;
   }

   public synchronized static boolean unregister(Object statsProvider) {
      //Unregister the statsProvider if the delegate is not null
      if (spmd == null) {
          for (StatsProviderInfo spInfo : toBeRegistered) {
              if (spInfo.getStatsProvider() == statsProvider) {
                  toBeRegistered.remove(spInfo);
                  break;
              }
          }

      } else {
          spmd.unregister(statsProvider);
          return true;
      }
       return false;
   }


   public static boolean hasListeners(String probeStr) {
      //See if the probe has any listeners registered
      if (spmd == null) {
          return false;
      } else {
          return spmd.hasListeners(probeStr);
      }
   }


   public synchronized static void setStatsProviderManagerDelegate(
                                    StatsProviderManagerDelegate lspmd) {
      if (lspmd == null) {
          //Should log and throw an exception
          return;
      }

      //Assign the Delegate
      spmd = lspmd;

      //First register the pending StatsProviderRegistryElements
      for (StatsProviderInfo spInfo : toBeRegistered) {
          spmd.register(spInfo);
      }

      //Now that you registered the pending calls, Clear the toBeRegistered store
      toBeRegistered.clear();
   }

   static StatsProviderManagerDelegate spmd; // populate this during our initilaization process
   private static ArrayList<StatsProviderInfo> toBeRegistered = new ArrayList();
}
