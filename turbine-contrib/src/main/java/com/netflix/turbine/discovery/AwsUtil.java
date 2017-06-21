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

import static com.netflix.turbine.discovery.InstanceDiscovery.TURBINE_AGGREGATOR_CLUSTER_CONFIG;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Reservation;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

/**
 * A utility class for querying information about AWS EC2 instances using the AWS APIs.
 * 
 * Converts EC2 Instance types into Turbine Instance types for use by the Turbine engine.
 * 
 * @author Chris Fregly (chris@fregly.com)
 */
public class AwsUtil {
    private final Logger logger = LoggerFactory.getLogger(AwsUtil.class);

    private final AmazonEC2Client ec2Client;
    
    private DynamicStringProperty app = DynamicPropertyFactory.getInstance().getStringProperty("turbine.ec2.tag.app", null);
    private DynamicStringProperty env = DynamicPropertyFactory.getInstance().getStringProperty("turbine.ec2.tag.env", null);
    private DynamicStringProperty cluster = DynamicPropertyFactory.getInstance().getStringProperty(TURBINE_AGGREGATOR_CLUSTER_CONFIG, null);
    
    public AwsUtil() {
    	ec2Client = new AmazonEC2Client();
    	String ec2Endpoint = "ec2." + DynamicPropertyFactory.getInstance().getStringProperty("turbine.region", "us-east-1").get() + ".amazonaws.com";
    	ec2Client.setEndpoint(ec2Endpoint);
    	logger.debug("Set the ec2Client endpoint to [{}]", ec2Endpoint);
    }

  /**
   * Convert from AWS ASG Instances to Turbine Instances
   * 
   * @return list of Turbine Instances (not AWS Instances)
   */
	public List<Instance> getTurbineInstances() {
    	
	    final String app = this.app.get();
	    final String env = this.env.get();
      
	    logger.info("finding ec2 instances by tag App={}* and Env={}", app, env);
	    
      final DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest().withFilters(
              new Filter().withName("tag-key").withValues("App"),
              new Filter().withName("tag-value").withValues(format("%s*", app)), 
              new Filter().withName("tag-key").withValues("Env"), 
              new Filter().withName("tag-value").withValues(format("%s", env)));
    	
      DescribeInstancesResult describeInstancesResult = ec2Client.describeInstances(describeInstancesRequest);
    	List<Reservation> reservations = describeInstancesResult.getReservations();
    	List<Instance> turbineInstances = new ArrayList<Instance>();
    	
    	// add all instances from each of the reservations - after converting to Turbine instance
    	for (Reservation reservation : reservations) {
    		List<com.amazonaws.services.ec2.model.Instance> ec2Instances = reservation.getInstances();
    		for (com.amazonaws.services.ec2.model.Instance ec2Instance : ec2Instances) {
    			String hostname = defaultIfBlank(ec2Instance.getPublicDnsName(), ec2Instance.getPrivateIpAddress());
    			String statusName = ec2Instance.getState().getName();
    			boolean status = statusName.equals("running"); // see com.amazonaws.services.ec2.model.InstanceState for values
    			
    			Instance turbineInstance = new Instance(hostname, cluster.get(), status);
    			
    			turbineInstances.add(turbineInstance);
    		}    		
    	}

    	return turbineInstances;
	}
	
}