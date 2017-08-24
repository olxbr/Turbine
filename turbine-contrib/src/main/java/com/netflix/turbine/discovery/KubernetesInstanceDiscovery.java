package com.netflix.turbine.discovery;

import static java.lang.System.getenv;

import java.util.ArrayList;
import java.util.Collection;

import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public class KubernetesInstanceDiscovery implements InstanceDiscovery {

  private static final String HYSTRIXID_LABEL = "hystrixid";
  private static final String K8S_TOKEN_ENV = "K8S_TOKEN";

  private final DynamicStringProperty NAMESPACE = DynamicPropertyFactory.getInstance()
      .getStringProperty("turbine.k8s.namespace", "website");
  private final DynamicStringProperty APP = DynamicPropertyFactory.getInstance().getStringProperty("turbine.k8s.app",
      "glue");
  private final DynamicStringProperty CLUSTER = DynamicPropertyFactory.getInstance()
      .getStringProperty(TURBINE_AGGREGATOR_CLUSTER_CONFIG, "glue");

  @Override
  public Collection<Instance> getInstanceList() throws Exception {
    final Collection<Instance> instances = new ArrayList<Instance>();
    DefaultKubernetesClient defaultKubernetesClient = null;
    try {
      final Config config = new Config();
      config.setOauthToken(getenv(K8S_TOKEN_ENV));
      defaultKubernetesClient = new DefaultKubernetesClient(config);
      final PodList pods = defaultKubernetesClient.pods().inNamespace(NAMESPACE.get()).withLabel(HYSTRIXID_LABEL, APP.get())
          .list();
      for (Pod pod : pods.getItems()) {
        final String ip = pod.getStatus().getPodIP();
        if (ip == null || "".equals(ip)) {
          continue;
        }
        final boolean status = "Running".equalsIgnoreCase(pod.getStatus().getPhase());
        instances.add(new Instance(ip, CLUSTER.get(), status));
      }
    } finally {
      if (defaultKubernetesClient != null) {
        defaultKubernetesClient.close();
      }
    }
    return instances;
  }
}
