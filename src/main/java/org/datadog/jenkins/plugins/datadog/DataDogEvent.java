package org.datadog.jenkins.plugins.datadog;

import net.sf.json.JSONObject;

/**
 *
 * Marker interface for DataDog events.
 */
public interface DataDogEvent  {
  /**
   *
   * @return The payload for the given event. Events usually have a custom message
   *
   */
  public JSONObject createPayload();
}
