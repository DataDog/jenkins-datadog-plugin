package org.datadog.jenkins.plugins.datadog.logs;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.*;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;


@Extension
public class LogSender extends ConsoleLogFilter implements Serializable {

    private static final Logger logger = Logger.getLogger(LogSender.class.getName());
    public transient Run<?,?> run;

    private static final long serialVersionUID = 1L;

    BuildData buildData;

    public LogSender() {}

    public LogSender(Run<?, ?> run) {
        this.run = run;
    }

    public OutputStream decorateLogger(Run run, OutputStream loggerOutputstream) throws IOException, InterruptedException {
        if (loggerOutputstream == null) {
            logger.info("No logger..");
            return null;
        }
        /*
        try {
            buildData = new BuildData(run, listener);
        } catch (IOException | InterruptedException e) {
            logger.severe(e.getMessage());
        }
         */
        if (run != null) {
            LogsWriter logswriter = getLogsWriter(run, loggerOutputstream);
            return new LogsOutputStream(loggerOutputstream, logswriter);
        }
        else {
            return loggerOutputstream;
        }

        /*
        try {
            logger.info(run.getLog());
            write(Collections.singletonList(run.getLog()));
            logger.info("write method completed");
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
         */
    }

    LogsWriter getLogsWriter(Run<?, ?> run, OutputStream errorStream) throws IOException, InterruptedException {
        return new LogsWriter(run, errorStream, null, run.getCharset());
    }

    private void write(List<String> lines) throws IOException {
        JSONObject payload = buildPayload(buildData, lines);
        logger.info("Sending logs from write");
        logger.info(payload.toString());
    }

    public JSONObject buildPayload(BuildData buildData, List<String> logLines) {
        JSONObject payload = new JSONObject();
        payload.put("job", String.valueOf(buildData));
        payload.put("message", logLines);
        payload.put("ddsource", "jenkins");
        logger.finer(payload.toString());
        logger.finer("created the logs payload");
        return payload;
    }

    @Override
    public OutputStream decorateLogger(AbstractBuild abstractBuild, OutputStream outputStream) {
        return null;
    }
}
