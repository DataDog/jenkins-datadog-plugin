package org.datadog.jenkins.plugins.datadog.logs;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.clients.DatadogHttpClient;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.logging.Logger;


@Extension
public class LogSender extends ConsoleLogFilter implements Serializable {
    private static final Logger logger = Logger.getLogger(LogSender.class.getName());

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
    public final void onCompleted(final Run run, @Nonnull final TaskListener listener) {
        // Instantiate the Datadog Client
        DatadogUtilities.getDatadogDescriptor().leaseDatadogClient();
        DatadogHttpClient client = null;

        if (run != null) {
            // LogsWriter datadoglogs = getLogsWriter(run);
            logger.info("Sending logs from build...");
            // client.sendLogs(datadoglogs);
            // return new LogsOutputStream(loggerOutputStream, datadoglogs);
        }
    }

    LogsWriter getLogsWriter(Run<?, ?> build, OutputStream errorStream) throws IOException, InterruptedException {
        return new LogsWriter(build, errorStream, null, build.getCharset());
    }
}
