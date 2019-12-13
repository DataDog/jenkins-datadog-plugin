package org.datadog.jenkins.plugins.datadog.clients;

import hudson.util.Secret;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogGlobalConfiguration;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

public class ClientFactory {

    public static DatadogClient getClient(DatadogClient.ClientType type, String apiUrl, Secret apiKey, String host, Integer port){
        switch(type){
            case HTTP:
                return DatadogHttpClient.getInstance(apiUrl, apiKey);
            case DSD:
                return DogStatsDClient.getInstance(host, port);
            default:
                return null;
        }
    }

    public static DatadogClient getClient() {
        DatadogGlobalConfiguration descriptor = DatadogUtilities.getDatadogGlobalDescriptor();
        return ClientFactory.getClient(DatadogClient.ClientType.valueOf(descriptor.getReportWith()),
                descriptor.getTargetApiURL(), descriptor.getTargetApiKey(),
                descriptor.getTargetHost(), descriptor.getTargetPort());
    }
}
