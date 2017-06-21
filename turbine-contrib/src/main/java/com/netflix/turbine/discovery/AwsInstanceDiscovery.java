/*
 * Copyright 2013 Chris Fregly (chris@fregly.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.netflix.turbine.discovery;

import java.util.Collection;

/**
 * Class that encapsulates an {@link InstanceDiscovery} implementation that uses AWS directly to query the instances from a given ASG.
 * 
 * This plugin requires a list of ASG names specified using the {@link InstanceDiscovery#TURBINE_AGGREGATOR_CLUSTER_CONFIG} property.  
 * It then queries the set of instances for each ASG provided. 
 * 
 * Instance information retrieved from AWS must be translated to something that Turbine can understand i.e the {@link Instance} class.
 * This translation can be found in the {@link AwsUtil} class.
 * 
 * @author cfregly
 */
public class AwsInstanceDiscovery implements InstanceDiscovery {    
    
    private final AwsUtil awsUtil;
    
    public AwsInstanceDiscovery() {
        awsUtil = new AwsUtil();
    }
    
    /**
     * Method that queries AWS for a list of instances for the configured app and env names
     * 
     * @return collection of Turbine instances
     */
    @Override
    public Collection<Instance> getInstanceList() throws Exception {
        return awsUtil.getTurbineInstances();
    }
    
}