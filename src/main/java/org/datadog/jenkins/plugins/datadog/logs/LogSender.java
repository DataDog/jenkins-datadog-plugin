package org.datadog.jenkins.plugins.datadog.logs;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.logging.Logger;


@Extension
public abstract class LogSender extends ConsoleLogFilter implements Serializable {
    public final static Logger LOG = Logger.getLogger(LogSender.class.getName());

    private static final long serialVersionUID = 1L;

    private transient Run<?, ?> run;
    public LogSender() {}

    public LogSender(Run<?, ?> run) {
        this.run = run;
    }

    public OutputStream decorateLogger(Run build, OutputStream logger) throws IOException, InterruptedException {
        // Instantiate the Datadog Client
        DatadogClient client = DatadogUtilities.getDatadogDescriptor().leaseDatadogClient();

        if (build != null && build instanceof AbstractBuild<?, ?>) {
            return logger;
        }
        if (run != null) {
            LogsWriter datadoglogs = getLogsWriter(run, logger);
            return new LogsOutputStream(logger, datadoglogs);
        }
        else {
            return logger;
        }
        // TODO - figure out how to send logs to DD
        client.sendLogs(datadoglogs, site);
    }

    LogsWriter getLogsWriter(Run<?, ?> build, OutputStream errorStream) throws IOException, InterruptedException {
        return new LogsWriter(build, errorStream, null, build.getCharset());
    }
}
