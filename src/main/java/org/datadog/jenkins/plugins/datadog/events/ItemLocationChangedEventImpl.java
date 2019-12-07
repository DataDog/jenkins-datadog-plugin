package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Item;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.util.Map;
import java.util.Set;

public class ItemLocationChangedEventImpl extends AbstractDatadogSimpleEvent {

    private Item item;
    private String oldFullName;
    private String newFullName;

    public ItemLocationChangedEventImpl(Item item, String oldFullName, String newFullName, Map<String, Set<String>> tags) {
        super(tags);
        this.item = item;
        this.oldFullName = oldFullName;
        this.newFullName = newFullName;
    }

    @Override
    public JSONObject createPayload() {
        String itemName = DatadogUtilities.getItemName(item);
        String userId = DatadogUtilities.getUserId();
        JSONObject payload = super.createPayload(itemName);

        String title = userId + " changed the location of the item " + itemName;
        payload.put("title", title);

        String message = "%%% \nUser" + userId + " changed the location of the item " + itemName + " from " +
                oldFullName + " to " + newFullName + " \n%%%";
        payload.put("text", message);

        payload.put("priority", "normal");
        payload.put("alert_type", "info");

        return payload;
    }
}
