package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Item;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;

public class ItemCopiedEventImpl implements DatadogEvent {

    private Item src;
    private Item item;
    private Map<String, Set<String>> tags;

    public ItemCopiedEventImpl(Item src, Item item, Map<String, Set<String>> tags) {
        this.src = src;
        this.item = item;
        this.tags = tags;
    }

    @Override
    public JSONObject createPayload() {
        String hostname = DatadogUtilities.getHostname(null);
        String srcName = DatadogUtilities.getItemName(src);
        String itemName = DatadogUtilities.getItemName(item);
        String userId = DatadogUtilities.getUserId();

        JSONObject payload = new JSONObject();
        payload.put("host", hostname);
        payload.put("aggregation_key", itemName);
        payload.put("date_happened", System.currentTimeMillis() / 1000);
        payload.put("tags", TagsUtil.convertTagsToJSONArray(tags));
        payload.put("source_type_name", "jenkins");

        String title = userId + " copied the item " + itemName + " from " + srcName;
        payload.put("title", title);

        String message = "%%% \n " + userId + " copied the item " + itemName + " from " + srcName + " \n %%%";
        payload.put("text", message);

        payload.put("priority", "normal");
        payload.put("alert_type", "low");

        return payload;
    }
}
