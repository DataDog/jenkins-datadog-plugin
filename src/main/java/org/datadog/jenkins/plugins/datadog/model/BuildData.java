package org.datadog.jenkins.plugins.datadog.model;

import hudson.EnvVars;
import hudson.model.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BuildData {
    private String buildNumber;
    private String buildId;
    private String buildUrl;
    private String nodeName;
    private String jobName;
    private String buildTag;
    private String jenkinsUrl;
    private String executorNumber;
    private String javaHome;
    private String workspace;
    // Branch contains either env variable - SVN_REVISION or CVS_BRANCH or GIT_BRANCH
    private String branch;
    private String gitUrl;
    private String gitCommit;
    // Environment variable from the promoted build plugin
    // - See https://plugins.jenkins.io/promoted-builds
    // - See https://wiki.jenkins.io/display/JENKINS/Promoted+Builds+Plugin
    private String promotedUrl;
    private String promotedJobName;
    private String promotedNumber;
    private String promotedId;
    private String promotedTimestamp;
    private String promotedUserName;
    private String promotedUserId;
    private String promotedJobFullName;

    private String result;
    private String hostname;

    private Long startTime;
    private Long endTime;
    private Long duration;

    public BuildData(Run run, TaskListener listener) throws IOException, InterruptedException {
        EnvVars envVars = run.getEnvironment(listener);
        // Populate instance using environment variables.
        populateEnvVariables(envVars);

        // Populate instance using run instance
        // Set StartTime, EndTime and Duration
        long startTimeInMs = run.getStartTimeInMillis();
        setStartTime(startTimeInMs);
        long durationInMs = run.getDuration();
        if (durationInMs == 0 && startTimeInMs != 0) {
            durationInMs = System.currentTimeMillis() - startTimeInMs;
        }
        setDuration(durationInMs);
        if (durationInMs != 0 && startTimeInMs != 0) {
            Long endTimeInMs = startTimeInMs + durationInMs;
            setEndTime(endTimeInMs);
        }

        // Set Result
        setResult(run.getResult() == null ? null : run.getResult().toString());
        // Set Build Number
        setBuildNumber(String.valueOf(run.getNumber()));
        // Set Hostname
        setHostname(DatadogUtilities.getHostname(envVars == null ? null : envVars.get("HOSTNAME")));
        // Set Job Name
        String jobName = run.getParent().getFullName();
        setJobName(jobName == null ?
                "" : jobName.replaceAll("»", "/").replaceAll(" ", ""));
    }

    private void populateEnvVariables(EnvVars envVars){
        if (envVars == null) {
            return;
        }
        setBuildId(envVars.get("BUILD_ID"));
        setBuildUrl(envVars.get("BUILD_URL"));
        setNodeName(envVars.get("NODE_NAME"));
        setBuildTag(envVars.get("BUILD_TAG"));
        setJenkinsUrl(envVars.get("JENKINS_URL"));
        setExecutorNumber(envVars.get("EXECUTOR_NUMBER"));
        setJavaHome(envVars.get("JAVA_HOME"));
        setWorkspace(envVars.get("WORKSPACE"));
        if (envVars.get("GIT_BRANCH") != null) {
            setBranch(envVars.get("GIT_BRANCH"));
            setGitUrl(envVars.get("GIT_URL"));
            setGitCommit(envVars.get("GIT_COMMIT"));
        } else if (envVars.get("CVS_BRANCH") != null) {
            setBranch(envVars.get("CVS_BRANCH"));
        } else if (envVars.get("SVN_REVISION") != null) {
            setBranch(envVars.get("SVN_REVISION"));
        }
        setPromotedUrl(envVars.get("PROMOTED_URL"));
        setPromotedJobName(envVars.get("PROMOTED_JOB_NAME"));
        setPromotedNumber(envVars.get("PROMOTED_NUMBER"));
        setPromotedId(envVars.get("PROMOTED_ID"));
        setPromotedTimestamp(envVars.get("PROMOTED_TIMESTAMP"));
        setPromotedUserName(envVars.get("PROMOTED_USER_NAME"));
        setPromotedUserId(envVars.get("PROMOTED_USER_ID"));
        setPromotedJobFullName(envVars.get("PROMOTED_JOB_FULL_NAME"));
    }

    /**
     * Assembles a {@link JSONArray} from metadata available in the
     * {@link JSONObject} builddata. Returns a {@link JSONArray} with the set
     * of tags.
     *
     * @param extra - A list of tags.
     * @return a JSONArray containing a specific subset of tags retrieved from a builds metadata.
     */
    public JSONArray getAssembledTags(Map<String, Set<String>> extra) {
        if(extra == null){
            extra = new HashMap<>();
        }
        JSONArray tags = new JSONArray();
        tags.add("job:" + getJobName("null"));
        if (nodeName != null) {
            tags.add("node:" + getNodeName("null"));
        }
        if (result != null) {
            tags.add("result:" + getResult("null"));
        }
        if (branch != null && !extra.containsKey("branch")) {
            tags.add("branch:" + getBranch("null"));
        }

        //Add the extra tags here
        for (String name : extra.keySet()) {
            Set<String> values = extra.get(name);
            for (String value : values){
                tags.add(String.format("%s:%s", name, value));
            }
        }

        return tags;
    }

    private <A> A defaultIfNull(A value, A defaultValue) {
        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
    }

    public String getJobName(String value) {
        return defaultIfNull(jobName, value);
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
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

    public String getNodeName(String value) {
        return defaultIfNull(nodeName, value);
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getBranch(String value) {
        return defaultIfNull(branch, value);
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getBuildNumber(String value) {
        return defaultIfNull(buildNumber, value);
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public Long getDuration(Long value) {
        return defaultIfNull(duration, value);
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Long getEndTime(Long value) {
        return defaultIfNull(endTime, value);
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Long getStartTime(Long value) {
        return defaultIfNull(startTime, value);
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public String getBuildId(String value) {
        return defaultIfNull(buildId, value);
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public String getBuildTag(String value) {
        return defaultIfNull(buildTag, value);
    }

    public void setBuildTag(String buildTag) {
        this.buildTag = buildTag;
    }

    public String getJenkinsUrl(String value) {
        return defaultIfNull(jenkinsUrl, value);
    }

    public void setJenkinsUrl(String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl;
    }

    public String getExecutorNumber(String value) {
        return defaultIfNull(executorNumber, value);
    }

    public void setExecutorNumber(String executorNumber) {
        this.executorNumber = executorNumber;
    }

    public String getJavaHome(String value) {
        return defaultIfNull(javaHome, value);
    }

    public void setJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }

    public String getWorkspace(String value) {
        return defaultIfNull(workspace, value);
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getGitUrl(String value) {
        return defaultIfNull(gitUrl, value);
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public String getGitCommit(String value) {
        return defaultIfNull(gitCommit, value);
    }

    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }

    public String getPromotedUrl(String value) {
        return defaultIfNull(promotedUrl, value);
    }

    public void setPromotedUrl(String promotedUrl) {
        this.promotedUrl = promotedUrl;
    }

    public String getPromotedJobName(String value) {
        return defaultIfNull(promotedJobName, value);
    }

    public void setPromotedJobName(String promotedJobName) {
        this.promotedJobName = promotedJobName;
    }

    public String getPromotedNumber(String value) {
        return defaultIfNull(promotedNumber, value);
    }

    public void setPromotedNumber(String promotedNumber) {
        this.promotedNumber = promotedNumber;
    }

    public String getPromotedId(String value) {
        return defaultIfNull(promotedId, value);
    }

    public void setPromotedId(String promotedId) {
        this.promotedId = promotedId;
    }

    public String getPromotedTimestamp(String value) {
        return defaultIfNull(promotedTimestamp, value);
    }

    public void setPromotedTimestamp(String promotedTimestamp) {
        this.promotedTimestamp = promotedTimestamp;
    }

    public String getPromotedUserName(String value) {
        return defaultIfNull(promotedUserName, value);
    }

    public void setPromotedUserName(String promotedUserName) {
        this.promotedUserName = promotedUserName;
    }

    public String getPromotedUserId(String value) {
        return defaultIfNull(promotedUserId, value);
    }

    public void setPromotedUserId(String promotedUserId) {
        this.promotedUserId = promotedUserId;
    }

    public String getPromotedJobFullName(String value) {
        return defaultIfNull(promotedJobFullName, value);
    }

    public void setPromotedJobFullName(String promotedJobFullName) {
        this.promotedJobFullName = promotedJobFullName;
    }
}
