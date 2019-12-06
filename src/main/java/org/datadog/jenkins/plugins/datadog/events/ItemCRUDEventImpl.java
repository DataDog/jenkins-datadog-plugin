package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Item;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;

public class ItemCRUDEventImpl implements DatadogEvent {
    public final static String CREATED = "Created";
    public final static String UPDATED = "Updated";
    public final static String DELETED = "Deleted";

    private Item item;
    private String action;
    private Map<String, Set<String>> tags;

    public ItemCRUDEventImpl(Item item, String action, Map<String, Set<String>> tags) {
        this.item = item;
        this.action = action;
        this.tags = tags;
    }

    @Override
    public JSONObject createPayload() {
        String hostname = DatadogUtilities.getHostname(null);
        String itemName = DatadogUtilities.getItemName(item);
        String userId = DatadogUtilities.getUserId();

        JSONObject payload = new JSONObject();
        payload.put("host", hostname);
        payload.put("aggregation_key", itemName);
        payload.put("date_happened", System.currentTimeMillis() / 1000);
        payload.put("tags", TagsUtil.convertTagsToJSONArray(tags));
        payload.put("source_type_name", "jenkins");

        String title = userId + " " + action.toLowerCase() + " the item " + itemName;
        payload.put("title", title);

        String message = "%%% \n " + userId + " " + action.toLowerCase() + " the item " + itemName + " \n %%%";
        payload.put("text", message);

        payload.put("priority", "normal");
        payload.put("alert_type", "info");

        return payload;
    }
}
