package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Item;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;

public class ItemCRUDEventImpl extends AbstractDatadogSimpleEvent {

    public final static String CREATED = "Created";
    public final static String UPDATED = "Updated";
    public final static String DELETED = "Deleted";

    private Item item;
    private String action;

    public ItemCRUDEventImpl(Item item, String action, Map<String, Set<String>> tags) {
        super(tags);
        this.item = item;
        this.action = action;
    }

    @Override
    public JSONObject createPayload() {
        String itemName = DatadogUtilities.getItemName(item);
        String userId = DatadogUtilities.getUserId();
        JSONObject payload = super.createPayload(itemName);

        String title = userId + " " + action.toLowerCase() + " the item " + itemName;
        payload.put("title", title);

        String message = "%%% \nUser" + userId + " " + action.toLowerCase() + " the item " + itemName + " \n%%%";
        payload.put("text", message);

        payload.put("priority", "normal");
        payload.put("alert_type", "info");

        return payload;
    }
}
