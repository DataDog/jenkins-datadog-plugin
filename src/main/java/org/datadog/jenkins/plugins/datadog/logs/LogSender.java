package org.datadog.jenkins.plugins.datadog.logs;

import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import org.datadog.jenkins.plugins.datadog.DatadogBuildListener;
import org.datadog.jenkins.plugins.datadog.clients.DatadogHttpClient;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.logging.Logger;


public class LogSender extends ConsoleLogFilter implements Serializable {
    public final static Logger LOG = Logger.getLogger(LogSender.class.getName());

    private transient Run<?, ?> run;
    public LogSender() {}

    @Override
    public OutputStream decorateLogger(AbstractBuild abstractBuild, OutputStream outputStream)
            throws IOException, InterruptedException {
        return null;
    }

    public LogSender(Run<?, ?> run)
    {
        this.run = run;
    }
    private static final long serialVersionUID = 1L;

    public OutputStream decorateLogger(Run build, OutputStream logger) throws IOException, InterruptedException
    {
        // Instantiate the Datadog Client
        DatadogHttpClient client = (DatadogHttpClient) getDescriptor().leaseDatadogClient();

        if (build != null && build instanceof AbstractBuild<?, ?>)
        {
            return logger;
        }
        if (run != null)
        {
            LogsWriter datadoglogs = getLogsWriter(run, logger);
            return new LogsOutputStream(logger, datadoglogs);
        }
        else
        {
            return logger;
        }
        // TODO - figure out how to send logs to DD
        client.sendLogs(datadoglogs, site);
    }

    LogsWriter getLogsWriter(Run<?, ?> build, OutputStream errorStream) throws IOException, InterruptedException {
        return new LogsWriter(build, errorStream, null, build.getCharset());
    }

    /**
     * Getter function for the {@link DatadogBuildListener.DescriptorImpl} class.
     *
     * @return a new {@link DatadogBuildListener.DescriptorImpl} class.
     */
    public DatadogBuildListener.DescriptorImpl getDescriptor() {
        return new DatadogBuildListener.DescriptorImpl();
    }
}
