package org.datadog.jenkins.plugins.datadog;

import java.util.HashMap;

import hudson.model.Hudson;
import hudson.model.Result;
import net.sf.json.JSONObject;

/**
 * Class that implements the {@link DatadogEvent}. This event produces an event payload with a
 * with a proper description for a finished build.
 */
public class BuildFinishedEventImpl implements DatadogEvent {

  private JSONObject builddata;
  private HashMap<String,String> tags;

  public BuildFinishedEventImpl(JSONObject buildData, HashMap<String,String> buildTags)  {
    this.builddata = buildData;
    this.tags = buildTags;
  }

  //Creates the raw json payload for this event.
  @Override
  public JSONObject createPayload() {
    JSONObject payload = new JSONObject();
    // Add event_type to assist in roll-ups
    payload.put("event_type", "build result"); // string
    String hostname = DatadogUtilities.nullSafeGetString(builddata, "hostname");
    String number = DatadogUtilities.nullSafeGetString(builddata, "number");
    String buildurl = DatadogUtilities.nullSafeGetString(builddata, "buildurl");
    String job = DatadogUtilities.nullSafeGetString(builddata, "job");
    long timestamp = builddata.getLong("timestamp");
    String message = "";

    // Build title
    StringBuilder title = new StringBuilder();
    title.append(job).append(" build #").append(number);
    

    message = "%%% \n [See results for build #" + number + "](" + buildurl + ") ";
    title.append(" " + builddata.get("result").toString().toLowerCase());

    String buildResult = builddata.get("result") != null ? builddata.get("result").toString() : Result.NOT_BUILT.toString() ;


    if (Result.SUCCESS.toString().equals(buildResult)) {
      payload.put("alert_type", "success");
      payload.put("priority", "low");
    } else if (Result.UNSTABLE.toString().equals(buildResult)  || Result.ABORTED.toString().equals(buildResult) || Result.NOT_BUILT.toString().equals(buildResult) ) {
      payload.put("alert_type", "warning");
    } else if (Result.FAILURE.toString().equals(buildResult)) {
      payload.put("alert_type", "error");
    }
    
    title.append(" on ").append(hostname);
    // Add duration
    if (builddata.get("duration") != null) {
      message = message + DatadogUtilities.durationToString(builddata.getDouble("duration"));
    }

    // Close markdown
    message = message + " \n %%%";

    // Build payload
    payload.put("title", title.toString());
    payload.put("text", message);
    payload.put("date_happened", timestamp);
    payload.put("host", hostname);
    payload.put("result", builddata.get("result"));
    payload.put("tags", DatadogUtilities.assembleTags(builddata, tags));
    payload.put("aggregation_key", job);
    payload.put("source_type_name", "jenkins");

    return payload;
  }
}
