package org.datadog.jenkins.plugins.datadog;

import java.util.HashMap;
import net.sf.json.JSONObject;

/**
 *
 * This event should contain all the data to construct a build started event. With
 * the right message for Datadog.
 */
public class BuildStartedEventImpl implements DataDogEvent  {

  private JSONObject builddata;
  private HashMap<String,String> tags;

  public BuildStartedEventImpl(JSONObject buildData, HashMap<String,String> tags)  {
    this.builddata = buildData;
    this.tags = tags;
  }

   /**
   *
   * @return - A JSON payload. See {@link DataDogEvent#createPayload()}
   */
  @Override
  public JSONObject createPayload() {
    JSONObject payload = new JSONObject();
    // Add event_type to assist in roll-ups
    payload.put("event_type", "build start"); // string
    String hostname = DataDogUtilities.nullSafeGetString(builddata, "hostname");
    String number = DataDogUtilities.nullSafeGetString(builddata, "number");
    String buildurl = DataDogUtilities.nullSafeGetString(builddata, "buildurl");
    String job = DataDogUtilities.nullSafeGetString(builddata, "job");
    long timestamp = builddata.getLong("timestamp");
    String message = "";

    // Build title
    StringBuilder title = new StringBuilder();
    title.append(job).append(" build #").append(number);
    title.append(" started");
    payload.put("alert_type", "info");
    message = "%%% \n [Follow build #" + number + " progress](" + buildurl + ") ";

    title.append(" on ").append(hostname);
    // Add duration
    if (builddata.get("duration") != null) {
      message = message + DataDogUtilities.durationToString(builddata.getDouble("duration"));
    }

    // Close markdown
    message = message + " \n %%%";

    // Build payload
    payload.put("title", title.toString());
    payload.put("text", message);
    payload.put("date_happened", timestamp);
    payload.put("event_type", builddata.get("event_type"));
    payload.put("host", hostname);
    payload.put("result", builddata.get("result"));
    payload.put("tags", DataDogUtilities.assembleTags(builddata, tags));
    payload.put("aggregation_key", job);

    return payload;
  }
}
