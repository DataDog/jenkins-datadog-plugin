package org.datadog.jenkins.plugins.datadog.model;

import hudson.EnvVars;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class BuildData {
    private String job; //jobName
    private String result;
    private String hostname;
    private String buildUrl;
    private String node;
    private String branch;
    private Integer number;
    private Long startTime;
    private Long duration;
    private Long timestamp;

    public BuildData(Run run, TaskListener listener) throws IOException, InterruptedException {
        String jobName = run.getParent().getFullName();
        EnvVars envVars = run.getEnvironment(listener);
        create(jobName,
                run.getStartTimeInMillis(),
                run.getDuration(),
                run.getResult(),
                run.getNumber(),
                envVars);
    }

    private void create(String jobName, long startTimeMs, Long durationMs, Result result, int runNumber,
                        EnvVars envVars) {
        setStartTime(startTimeMs / 1000); // ms to s
        long duration;
        if (durationMs == null || durationMs == 0) {
            duration = System.currentTimeMillis() - startTimeMs;
        }else{
            duration = durationMs;
        }
        setDuration(durationMs / 1000); //ms to s
        Long endTime = startTimeMs + duration;
        setTimestamp(endTime / 1000); //ms to s

        setNumber(runNumber);
        setResult(result == null ? null : result.toString());

        setJob(jobName == null ?
                "": jobName.replaceAll("»", "/").replaceAll(" ", ""));

        // Grab environment variables
        setHostname(DatadogUtilities.getHostname(envVars == null? null : envVars.get("HOSTNAME")));
        if (envVars != null) {
            setBuildUrl(envVars.get("BUILD_URL"));
            setNode(envVars.get("NODE_NAME"));
            if (envVars.get("GIT_BRANCH") != null) {
                setBranch(envVars.get("GIT_BRANCH"));
            } else if (envVars.get("CVS_BRANCH") != null) {
                setBranch(envVars.get("CVS_BRANCH"));
            }
        }
    }

    /**
     * Assembles a {@link JSONArray} from metadata available in the
     * {@link JSONObject} builddata. Returns a {@link JSONArray} with the set
     * of tags.
     *
     * @param extra - A list of tags.
     * @return a JSONArray containing a specific subset of tags retrieved from a builds metadata.
     */
    public JSONArray getAssembledTags(Map<String, String> extra) {
        JSONArray tags = new JSONArray();
        if(extra == null){
            extra = new HashMap<>();
        }
        tags.add("job:" + getJob("null"));
        if (node != null) {
            tags.add("node:" + getNode("null"));
        }

        if (result != null) {
            tags.add("result:" + getResult("null"));
        }

        if (branch != null && !extra.containsKey("branch")) {
            tags.add("branch:" + getBranch("null"));
        }

        //Add the extra tags here
        for (Map.Entry entry : extra.entrySet()) {
            tags.add(String.format("%s:%s", entry.getKey(), entry.getValue()));
        }

        return tags;
    }

    private <A> A defaultIfNull(A value, A defaultValue){
        if (value == null){
            return defaultValue;
        }else{
            return value;
        }
    }

    public String getJob(String value) {
        return defaultIfNull(job, value);
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getResult(String value) {
        return defaultIfNull(result, value);
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getHostname(String value) {
        return defaultIfNull(hostname, value);
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getBuildUrl(String value) {
        return defaultIfNull(buildUrl, value);
    }

    public void setBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
    }

    public String getNode(String value) {
        return defaultIfNull(node, value);
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getBranch(String value) {
        return defaultIfNull(branch, value);
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Integer getNumber(Integer value) {
        return defaultIfNull(number, value);
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Long getStartTime(Long value) {
        return defaultIfNull(startTime, value);
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getDuration(Long value) {
        return defaultIfNull(duration, value);
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Long getTimestamp(Long value) {
        return defaultIfNull(timestamp, value);
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

}
