package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Item;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;

public class ItemLocationChangedEventImpl implements DatadogEvent {

    private Item item;
    private String oldFullName;
    private String newFullName;
    private Map<String, Set<String>> tags;

    public ItemLocationChangedEventImpl(Item item, String oldFullName, String newFullName, Map<String, Set<String>> tags) {
        this.item = item;
        this.oldFullName = oldFullName;
        this.newFullName = newFullName;
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

        String title = userId + " changed the location of the item " + itemName;
        payload.put("title", title);

        String message = "%%% \n " + userId + " changed the location of the item " + itemName + " from " +
                oldFullName + " to " + newFullName + " \n %%%";
        payload.put("text", message);

        payload.put("priority", "normal");
        payload.put("alert_type", "info");

        return payload;
    }
}
