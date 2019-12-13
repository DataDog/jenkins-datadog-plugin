package org.datadog.jenkins.plugins.datadog.events;

import java.util.Map;
import java.util.Set;

public class UserAuthenticationEventImpl extends AbstractDatadogSimpleEvent {

    public final static String LOGIN = "authenticated";
    public final static String ACCESS_DENIED = "failed to authenticate";
    public final static String LOGOUT = "logout";

    public UserAuthenticationEventImpl(String username, String action, Map<String, Set<String>> tags) {
        super(tags);

        setAggregationKey(username);
        String title = username + " " + action.toLowerCase();
        setTitle(title);

        String text = "%%% \nUser " + username + " " + action.toLowerCase() +" \n%%%";
        setText(text);

        if (LOGIN.equals(action) || LOGOUT.equals(action)){
            setPriority(Priority.LOW);
            setAlertType(AlertType.SUCCESS);
        } else {
            setPriority(Priority.NORMAL);
            setAlertType(AlertType.ERROR);
        }
    }

}