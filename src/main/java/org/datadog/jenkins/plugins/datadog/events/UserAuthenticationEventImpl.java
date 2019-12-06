package org.datadog.jenkins.plugins.datadog.events;

import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;

public class UserAuthenticationEventImpl implements DatadogEvent {
    public final static String LOGIN = "authenticated";
    public final static String ACCESS_DENIED = "failed to authenticate";
    public final static String LOGOUT = "logout";

    private String username;
    private String action;
    private Map<String, Set<String>> tags;

    public UserAuthenticationEventImpl(String username, String action, Map<String, Set<String>> tags) {
        this.username = username;
        this.action = action;
        this.tags = tags;
    }

    @Override
    public JSONObject createPayload() {
        String hostname = DatadogUtilities.getHostname(null);

        JSONObject payload = new JSONObject();
        payload.put("host", hostname);
        payload.put("aggregation_key", username);
        payload.put("date_happened", System.currentTimeMillis() / 1000);
        payload.put("tags", TagsUtil.convertTagsToJSONArray(tags));
        payload.put("source_type_name", "jenkins");

        String title = username + " " + action.toLowerCase();
        payload.put("title", title);

        String message = "%%% \n " + username + " " + action.toLowerCase() +" \n %%%";
        payload.put("text", message);

        if (LOGIN.equals(action) || LOGOUT.equals(action)){
            payload.put("alert_type", "info");
        } else {
            payload.put("alert_type", "error");
        }
        payload.put("priority", "normal");

        return payload;
    }
}