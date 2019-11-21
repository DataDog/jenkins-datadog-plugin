package org.datadog.jenkins.plugins.datadog.logs;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.clients.DatadogHttpClient;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.logging.Logger;


@Extension
public class LogSender extends ConsoleLogFilter implements Serializable {
    public final static Logger LOG = Logger.getLogger(LogSender.class.getName());

    private static final long serialVersionUID = 1L;

    private transient Run<?, ?> run;
    public LogSender() {}

    @Override
    public OutputStream decorateLogger(AbstractBuild abstractBuild, OutputStream outputStream) throws IOException, InterruptedException {
        return null;
    }

    public LogSender(Run<?, ?> run) {
        this.run = run;
    }

    // Based on https://github.com/jenkinsci/jenkins/blob/360029e9d52152425dbabca9f1072fcd919772b6/core/src/main/java/hudson/console/ConsoleLogFilter.java#L76-L95
    public OutputStream decorateLogger(Run build, OutputStream logger) throws IOException, InterruptedException {
        // Instantiate the Datadog Client
        DatadogUtilities.getDatadogDescriptor().leaseDatadogClient();
        DatadogHttpClient client = null;

        if (build != null && build instanceof AbstractBuild<?, ?>) {
            return logger;
        }
        if (run != null) {
            LogsWriter datadoglogs = getLogsWriter(run, logger);
            // client.sendLogs(datadoglogs);
            return new LogsOutputStream(logger, datadoglogs);
        }
        else {
            return logger;
        }
    }

    LogsWriter getLogsWriter(Run<?, ?> build, OutputStream errorStream) throws IOException, InterruptedException {
        return new LogsWriter(build, errorStream, null, build.getCharset());
    }
}
