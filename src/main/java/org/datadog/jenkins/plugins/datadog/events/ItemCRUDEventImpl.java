package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Item;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.util.Map;
import java.util.Set;

public class ItemCRUDEventImpl extends AbstractDatadogSimpleEvent {

    public final static String CREATED = "Created";
    public final static String UPDATED = "Updated";
    public final static String DELETED = "Deleted";

    public ItemCRUDEventImpl(Item item, String action, Map<String, Set<String>> tags) {
        super(tags);

        if(action == null){
            action = "did something with";
        }

        String itemName = DatadogUtilities.getItemName(item);
        String userId = DatadogUtilities.getUserId();
        setAggregationKey(itemName);

        String title = "User " + userId + " " + action.toLowerCase() + " the item " + itemName;
        setTitle(title);

        String text = "%%% \nUser " + userId + " " + action.toLowerCase() + " the item " + itemName + " \n%%%";
        setText(text);

        setPriority(Priority.NORMAL);
        setAlertType(AlertType.INFO);
    }
}
