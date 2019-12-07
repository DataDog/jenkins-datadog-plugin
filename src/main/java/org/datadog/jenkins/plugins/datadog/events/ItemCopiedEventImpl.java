package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Item;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;

public class ItemCopiedEventImpl extends AbstractDatadogSimpleEvent {

    private Item src;
    private Item item;

    public ItemCopiedEventImpl(Item src, Item item, Map<String, Set<String>> tags) {
        super(tags);
        this.src = src;
        this.item = item;
    }

    @Override
    public JSONObject createPayload() {
        String srcName = DatadogUtilities.getItemName(src);
        String itemName = DatadogUtilities.getItemName(item);
        String userId = DatadogUtilities.getUserId();
        JSONObject payload = super.createPayload(itemName);

        String title = userId + " copied the item " + itemName + " from " + srcName;
        payload.put("title", title);

        String message = "%%% \nUser" + userId + " copied the item " + itemName + " from " + srcName + " \n%%%";
        payload.put("text", message);

        payload.put("priority", "normal");
        payload.put("alert_type", "low");

        return payload;
    }
}
