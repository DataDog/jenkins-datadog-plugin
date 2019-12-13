package org.datadog.jenkins.plugins.datadog.util;

import org.datadog.jenkins.plugins.datadog.clients.DatadogClientStub;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class TagsUtilTest {

    @Test
    public void testMerge(){
        Map<String, Set<String>> emptyTags = new HashMap<>();
        Assert.assertTrue(TagsUtil.merge(null, null).equals(new HashMap<String, Set<String>>()));
        Assert.assertTrue(TagsUtil.merge(emptyTags, null).equals(new HashMap<String, Set<String>>()));
        Assert.assertTrue(TagsUtil.merge(null, emptyTags).equals(new HashMap<String, Set<String>>()));

        Map<String, Set<String>> assertionTags = new HashMap<>();
        assertionTags.put("name1", new HashSet<String>());
        Map<String, Set<String>> nullTagValue = new HashMap<>();
        nullTagValue.put("name1", null);
        Assert.assertTrue(TagsUtil.merge(null, nullTagValue).equals(assertionTags));
        Assert.assertTrue(TagsUtil.merge(nullTagValue, nullTagValue).equals(assertionTags));
        Assert.assertTrue(TagsUtil.merge(nullTagValue, null).toString().equals(assertionTags.toString()));
        Assert.assertTrue(TagsUtil.merge(nullTagValue, emptyTags).toString().equals(assertionTags.toString()));
        Assert.assertTrue(TagsUtil.merge(emptyTags, nullTagValue).toString().equals(assertionTags.toString()));
        Map<String, Set<String>> emptyTagValue = new HashMap<>();
        emptyTagValue.put("name1", new HashSet<String>());
        Assert.assertTrue(TagsUtil.merge(emptyTagValue, null).toString().equals(assertionTags.toString()));
        Assert.assertTrue(TagsUtil.merge(null, emptyTagValue).toString().equals(assertionTags.toString()));
        Assert.assertTrue(TagsUtil.merge(emptyTagValue, emptyTags).toString().equals(assertionTags.toString()));
        Assert.assertTrue(TagsUtil.merge(emptyTags, emptyTagValue).toString().equals(assertionTags.toString()));
        Assert.assertTrue(TagsUtil.merge(nullTagValue, emptyTagValue).toString().equals(assertionTags.toString()));
        Assert.assertTrue(TagsUtil.merge(emptyTagValue, nullTagValue).toString().equals(assertionTags.toString()));
        Map<String, Set<String>> n1v1Tag = new HashMap<>();
        n1v1Tag = DatadogClientStub.addTagToMap(n1v1Tag, "name1", "value1");
        assertionTags = new HashMap<>();
        assertionTags = DatadogClientStub.addTagToMap(assertionTags, "name1", "value1");
        Assert.assertTrue(TagsUtil.merge(n1v1Tag, null).toString().equals(assertionTags.toString()));
        Assert.assertTrue(TagsUtil.merge(null, n1v1Tag).toString().equals(assertionTags.toString()));
        Assert.assertTrue(TagsUtil.merge(n1v1Tag, emptyTags).toString().equals(assertionTags.toString()));
        Assert.assertTrue(TagsUtil.merge(emptyTags, n1v1Tag).toString().equals(assertionTags.toString()));
        Assert.assertTrue(TagsUtil.merge(nullTagValue, n1v1Tag).toString().equals(assertionTags.toString()));
        Assert.assertTrue(TagsUtil.merge(n1v1Tag, nullTagValue).toString().equals(assertionTags.toString()));
        Assert.assertTrue(TagsUtil.merge(n1v1Tag, emptyTagValue).toString().equals(assertionTags.toString()));
        Assert.assertTrue(TagsUtil.merge(emptyTagValue, n1v1Tag).toString().equals(assertionTags.toString()));

        Map<String, Set<String>> n1v1TagCopy = new HashMap<>();
        n1v1TagCopy = DatadogClientStub.addTagToMap(n1v1TagCopy, "name1", "value1");
        Assert.assertTrue(TagsUtil.merge(n1v1TagCopy, n1v1Tag).toString().equals(assertionTags.toString()));

        Map<String, Set<String>> n1v2Tag = new HashMap<>();
        n1v2Tag = DatadogClientStub.addTagToMap(n1v2Tag, "name1", "value2");
        assertionTags = new HashMap<>();
        assertionTags = DatadogClientStub.addTagToMap(assertionTags, "name1", "value1");
        assertionTags = DatadogClientStub.addTagToMap(assertionTags, "name1", "value2");
        Assert.assertTrue(TagsUtil.merge(n1v2Tag, n1v1Tag).toString().equals(assertionTags.toString()));

        Map<String, Set<String>> n2v1Tag = new HashMap<>();
        n2v1Tag = DatadogClientStub.addTagToMap(n2v1Tag, "name2", "value1");
        assertionTags = new HashMap<>();
        assertionTags = DatadogClientStub.addTagToMap(assertionTags, "name1", "value1");
        assertionTags = DatadogClientStub.addTagToMap(assertionTags, "name2", "value1");
        Assert.assertTrue(TagsUtil.merge(n2v1Tag, n1v1Tag).toString().equals(assertionTags.toString()));

        n2v1Tag = new HashMap<>();
        n2v1Tag = DatadogClientStub.addTagToMap(n2v1Tag, "name2", "value1");
        Map<String, Set<String>> n2v1v2Tag = new HashMap<>();
        n2v1v2Tag = DatadogClientStub.addTagToMap(n2v1v2Tag, "name2", "value1");
        n2v1v2Tag = DatadogClientStub.addTagToMap(n2v1v2Tag, "name2", "value2");
        assertionTags = new HashMap<>();
        assertionTags = DatadogClientStub.addTagToMap(assertionTags, "name2", "value1");
        assertionTags = DatadogClientStub.addTagToMap(assertionTags, "name2", "value2");
        Assert.assertTrue(TagsUtil.merge(n2v1Tag, n2v1v2Tag).toString() + " - "+ assertionTags.toString(),
                TagsUtil.merge(n2v1Tag, n2v1v2Tag).toString().equals(assertionTags.toString()));

    }

}
