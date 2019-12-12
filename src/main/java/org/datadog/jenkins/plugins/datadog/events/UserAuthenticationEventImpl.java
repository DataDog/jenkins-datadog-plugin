package org.datadog.jenkins.plugins.datadog.events;

import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;

public class UserAuthenticationEventImpl extends AbstractDatadogSimpleEvent {

    public final static String LOGIN = "authenticated";
    public final static String ACCESS_DENIED = "failed to authenticate";
    public final static String LOGOUT = "logout";

    private String username;
    private String action;

    public UserAuthenticationEventImpl(String username, String action, Map<String, Set<String>> tags) {
        super(tags);
        this.username = username;
        this.action = action;
    }

    @Override
    public JSONObject createPayload() {
        JSONObject payload = super.createPayload(username);

        String title = username + " " + action.toLowerCase();
        payload.put("title", title);

        String message = "%%% \nUser " + username + " " + action.toLowerCase() +" \n%%%";
        payload.put("text", message);

        if (LOGIN.equals(action) || LOGOUT.equals(action)){
            payload.put("priority", "low");
            payload.put("alert_type", "success");
        } else {
            payload.put("priority", "normal");
            payload.put("alert_type", "error");
        }

        return payload;
    }
}