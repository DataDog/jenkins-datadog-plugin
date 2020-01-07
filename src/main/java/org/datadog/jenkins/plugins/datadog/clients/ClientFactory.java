/*
The MIT License

Copyright (c) 2015-Present Datadog, Inc <opensource@datadoghq.com>
All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

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
