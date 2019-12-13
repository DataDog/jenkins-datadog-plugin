package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Item;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.util.Map;
import java.util.Set;

public class ItemLocationChangedEventImpl extends AbstractDatadogSimpleEvent {

    public ItemLocationChangedEventImpl(Item item, String oldFullName, String newFullName, Map<String, Set<String>> tags) {
        super(tags);

        String itemName = DatadogUtilities.getItemName(item);
        String userId = DatadogUtilities.getUserId();
        setAggregationKey(itemName);

        String title = userId + " changed the location of the item " + itemName;
        setTitle(title);

        String text = "%%% \nUser " + userId + " changed the location of the item " + itemName + " from " +
                oldFullName + " to " + newFullName + " \n%%%";
        setText(text);

        setPriority(Priority.NORMAL);
        setAlertType(AlertType.INFO);
    }
}
