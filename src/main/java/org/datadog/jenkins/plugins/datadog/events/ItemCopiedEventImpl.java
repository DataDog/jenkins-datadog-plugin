package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Item;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.util.Map;
import java.util.Set;

public class ItemCopiedEventImpl extends AbstractDatadogSimpleEvent {

    public ItemCopiedEventImpl(Item src, Item item, Map<String, Set<String>> tags) {
        super(tags);

        String srcName = DatadogUtilities.getItemName(src);
        String itemName = DatadogUtilities.getItemName(item);
        String userId = DatadogUtilities.getUserId();
        setAggregationKey(itemName);

        String title = userId + " copied the item " + itemName + " from " + srcName;
        setTitle(title);

        String text = "%%% \nUser " + userId + " copied the item " + itemName + " from " + srcName + " \n%%%";
        setText(text);

        setPriority(Priority.NORMAL);
        setAlertType(AlertType.INFO);
    }
}
