package org.datadog.jenkins.plugins.datadog.listeners;

import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import net.sf.json.JSONArray;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.events.BuildAbortedEventImpl;
import org.datadog.jenkins.plugins.datadog.events.BuildFinishedEventImpl;
import org.datadog.jenkins.plugins.datadog.events.BuildStartedEventImpl;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


/**
 * This class registers an {@link RunListener} to trigger events and calculate metrics:
 * - When a build starts, the {@link #onStarted(Run, TaskListener)} method will be invoked.
 * - When a build finishes, the {@link #onCompleted(Run, TaskListener)} method will be invoked.
 */
@Extension
public class DatadogBuildListener extends RunListener<Run>  {

    private static final Logger logger = Logger.getLogger(DatadogBuildListener.class.getName());

    /**
     * Called when a build is first started.
     *
     * @param run      - A Run object representing a particular execution of Job.
     * @param listener - A TaskListener object which receives events that happen during some
     *                 operation.
     */
    @Override
    public void onStarted(final Run run, final TaskListener listener) {
        try {
            // Process only if job is NOT in blacklist and is in whitelist
            if (!DatadogUtilities.isJobTracked(run.getParent().getFullName())) {
                return;
            }
            logger.fine("End DatadogBuildListener#onStarted");

            // Get Datadog Client Instance
            DatadogClient client = DatadogUtilities.getDatadogClient();

            // Collect Build Data
            BuildData buildData;
            try {
                buildData = new BuildData(run, listener);
            } catch (IOException | InterruptedException e) {
                logger.severe(e.getMessage());
                return;
            }

            // Send an event
            DatadogEvent event = new BuildStartedEventImpl(buildData);
            client.sendEvent(event.createPayload());

            // Send an metric
            // item.getInQueueSince() may raise a NPE if a worker node is spinning up to run the job.
            // This could be expected behavior with ec2 spot instances/ecs containers, meaning no waiting
            // queue times if the plugin is spinning up an instance/container for one/first job.
            Queue queue = Queue.getInstance();
            Queue.Item item = queue.getItem(run.getQueueId());
            Map<String, Set<String>> tags = buildData.getTags();
            String hostname = buildData.getHostname("null");
            try {
                long waiting = (currentTimeMillis() - item.getInQueueSince()) / 1000;
                client.gauge("jenkins.job.waiting", waiting, hostname, tags);
            } catch (NullPointerException e) {
                logger.warning("Unable to compute 'waiting' metric. " +
                        "item.getInQueueSince() unavailable, possibly due to worker instance provisioning");
            }

            // Submit counter
            client.incrementCounter("jenkins.job.started", hostname, tags);

            logger.fine("End DatadogBuildListener#onStarted");
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }

    /**
     * Called when a build is completed.
     *
     * @param run      - A Run object representing a particular execution of Job.
     * @param listener - A TaskListener object which receives events that happen during some
     *                 operation.
     */

    @Override
    public void onCompleted(final Run run, @Nonnull final TaskListener listener) {
        try {
            // Process only if job in NOT in blacklist and is in whitelist
            if (!DatadogUtilities.isJobTracked(run.getParent().getFullName())) {
                return;
            }
            logger.fine("Start DatadogBuildListener#onCompleted");

            // Get Datadog Client Instance
            DatadogClient client = DatadogUtilities.getDatadogClient();

            // Collect Build Data
            BuildData buildData;
            try {
                buildData = new BuildData(run, listener);
            } catch (IOException | InterruptedException e) {
                logger.severe(e.getMessage());
                return;
            }

            // Send an event
            DatadogEvent event = new BuildFinishedEventImpl(buildData);
            client.sendEvent(event.createPayload());

            // Send a metric
            Map<String, Set<String>> tags = buildData.getTags();
            String hostname = buildData.getHostname("null");
            client.gauge("jenkins.job.duration", buildData.getDuration(0L) / 1000, hostname, tags);

            // Submit counter
            client.incrementCounter("jenkins.job.completed", hostname, tags);

            // Send a service check
            String buildResult = buildData.getResult(Result.NOT_BUILT.toString());
            int statusCode = DatadogClient.UNKNOWN;
            if (Result.SUCCESS.toString().equals(buildResult)) {
                statusCode = DatadogClient.OK;
            } else if (Result.UNSTABLE.toString().equals(buildResult) ||
                    Result.ABORTED.toString().equals(buildResult) ||
                    Result.NOT_BUILT.toString().equals(buildResult)) {
                statusCode = DatadogClient.WARNING;
            } else if (Result.FAILURE.toString().equals(buildResult)) {
                statusCode = DatadogClient.CRITICAL;
            }
            client.serviceCheck("jenkins.job.status", statusCode, hostname, tags);

            if (run.getResult() == Result.SUCCESS) {
                long mttr = getMeanTimeToRecovery(run);
                long cycleTime = getCycleTime(run);
                long leadTime = run.getDuration() + mttr;

                client.gauge("jenkins.job.leadtime", leadTime / 1000, hostname, tags);
                if (cycleTime > 0) {
                    client.gauge("jenkins.job.cycletime", cycleTime / 1000, hostname, tags);
                }
                if (mttr > 0) {
                    client.gauge("jenkins.job.mttr", mttr / 1000, hostname, tags);
                }
            } else {
                long feedbackTime = run.getDuration();
                long mtbf = getMeanTimeBetweenFailure(run);

                client.gauge("jenkins.job.feedbacktime", feedbackTime / 1000, hostname, tags);
                if (mtbf > 0) {
                    client.gauge("jenkins.job.mtbf", mtbf / 1000, hostname, tags);
                }
            }

            logger.fine("End DatadogBuildListener#onCompleted");
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }

    @Override
    public void onDeleted(Run run) {
        try {
            // Process only if job is NOT in blacklist and is in whitelist
            if (!DatadogUtilities.isJobTracked(run.getParent().getFullName())) {
                return;
            }
            logger.fine("Start DatadogBuildListener#onDeleted");

            // Get Datadog Client Instance
            DatadogClient client = DatadogUtilities.getDatadogClient();

            // Collect Build Data
            BuildData buildData;
            try {
                buildData = new BuildData(run, null);
            } catch (IOException | InterruptedException e) {
                logger.severe(e.getMessage());
                return;
            }

            // Get the list of global tags to apply
            String hostname = buildData.getHostname("null");

            // Send an event
            DatadogEvent event = new BuildAbortedEventImpl(buildData);
            client.sendEvent(event.createPayload());

            // Submit counter
            Map<String, Set<String>> tags = buildData.getTags();
            client.incrementCounter("jenkins.job.aborted", hostname, tags);

            logger.fine("End DatadogBuildListener#onDeleted");
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }

    public long currentTimeMillis(){
        // This method exist so we can mock System.currentTimeMillis in unit tests
        return System.currentTimeMillis();
    }

    private long getMeanTimeBetweenFailure(Run<?, ?> run) {
        Run<?, ?> lastGreenRun = run.getPreviousNotFailedBuild();
        if (lastGreenRun != null) {
            return getStartTimeInMillis(run) - getStartTimeInMillis(lastGreenRun);
        }
        return 0;
    }

    private long getCycleTime(Run<?, ?> run) {
        Run<?, ?> previousSuccessfulBuild = run.getPreviousSuccessfulBuild();
        if (previousSuccessfulBuild != null) {
            return (getStartTimeInMillis(run) + run.getDuration()) -
                    (getStartTimeInMillis(previousSuccessfulBuild) + previousSuccessfulBuild.getDuration());
        }
        return 0;
    }

    private long getMeanTimeToRecovery(Run<?, ?> run) {
        if (isFailedBuild(run.getPreviousBuiltBuild())) {
            Run<?, ?> firstFailedRun = run.getPreviousBuiltBuild();

            while (firstFailedRun != null && isFailedBuild(firstFailedRun.getPreviousBuiltBuild())) {
                firstFailedRun = firstFailedRun.getPreviousBuiltBuild();
            }
            if (firstFailedRun != null) {
                return getStartTimeInMillis(run) - getStartTimeInMillis(firstFailedRun);
            }
        }
        return 0;
    }

    public long getStartTimeInMillis(Run run) {
        // getStartTimeInMillis wrapper in order to mock it in unit tests
        return run.getStartTimeInMillis();
    }

    private boolean isFailedBuild(Run<?, ?> run) {
        return run != null && run.getResult() != Result.SUCCESS;
    }

}
