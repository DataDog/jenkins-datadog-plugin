package org.datadog.jenkins.plugins.datadog.util;

import net.sf.json.JSONArray;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TagsUtil {

    public static Map<String, Set<String>> merge(Map<String, Set<String>> dest, Map<String, Set<String>> orig) {
        if (dest == null) {
            dest = new HashMap<>();
        }
        if (orig == null) {
            orig = new HashMap<>();
        }
        for (String oName: orig.keySet()){
            Set<String> dValues = dest.containsKey(oName) ? dest.get(oName) : new HashSet<String>();
            if (dValues == null) {
                dValues = new HashSet<>();
            }
            Set<String> oValues = orig.get(oName);
            if (oValues != null) {
                dValues.addAll(oValues);
            }
            dest.put(oName, dValues);
        }
        return dest;
    }

    public static JSONArray convertTagsToJSONArray(Map<String, Set<String>> tags){
        JSONArray result = new JSONArray();
        for (String name : tags.keySet()) {
            Set<String> values = tags.get(name);
            for (String value : values){
                if ("".equals(value)){
                    result.add(name); // Tag with no value
                }else{
                    result.add(String.format("%s:%s", name, value));
                }
            }
        }
        return result;
    }
}
