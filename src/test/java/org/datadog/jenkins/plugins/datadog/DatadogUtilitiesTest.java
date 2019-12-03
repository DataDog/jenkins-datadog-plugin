package org.datadog.jenkins.plugins.datadog;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class DatadogUtilitiesTest {

    @Test
    public void testCstrToList(){
        Assert.assertTrue(DatadogUtilities.cstrToList(null).isEmpty());
        Assert.assertTrue(DatadogUtilities.cstrToList("").isEmpty());
        Assert.assertTrue(DatadogUtilities.cstrToList(" , ").isEmpty());

        List<String> items = new ArrayList<>();
        items.add("item1");
        Assert.assertTrue(DatadogUtilities.cstrToList("item1").equals(items));
        Assert.assertTrue(DatadogUtilities.cstrToList(" item1 ").equals(items));
        Assert.assertTrue(DatadogUtilities.cstrToList(" , item1 , ").equals(items));

        items = new ArrayList<>();
        items.add("item1");
        items.add("item2");
        Assert.assertTrue(DatadogUtilities.cstrToList("item1,item2").equals(items));
        Assert.assertTrue(DatadogUtilities.cstrToList("  item1 , item2 ").equals(items));
        Assert.assertTrue(DatadogUtilities.cstrToList(" , item1 , item2 , ").equals(items));
    }

    @Test
    public void testLinesToList(){
        Assert.assertTrue(DatadogUtilities.linesToList(null).isEmpty());
        Assert.assertTrue(DatadogUtilities.linesToList("").isEmpty());

        List<String> items = new ArrayList<>();
        items.add("item1");
        Assert.assertTrue(DatadogUtilities.linesToList("item1").equals(items));
        Assert.assertTrue(DatadogUtilities.linesToList(" item1 ").equals(items));
        Assert.assertTrue(DatadogUtilities.linesToList(" \n item1 \n ").equals(items));

        items = new ArrayList<>();
        items.add("item1");
        items.add("item2");
        Assert.assertTrue(DatadogUtilities.linesToList("item1\nitem2").equals(items));
        Assert.assertTrue(DatadogUtilities.linesToList("  item1 \n item2 ").equals(items));
        Assert.assertTrue(DatadogUtilities.linesToList(" \n item1 \n item2 \n ").equals(items));
    }

    @Test
    public void testMerge(){
        Map<String, Set<String>> emptyTags = new HashMap<>();
        Assert.assertTrue(DatadogUtilities.merge(null, null).equals(new HashMap<String, Set<String>>()));
        Assert.assertTrue(DatadogUtilities.merge(emptyTags, null).equals(new HashMap<String, Set<String>>()));
        Assert.assertTrue(DatadogUtilities.merge(null, emptyTags).equals(new HashMap<String, Set<String>>()));

        Map<String, Set<String>> assertionTags = new HashMap<>();
        assertionTags.put("name1", new HashSet<String>());
        Map<String, Set<String>> nullTagValue = new HashMap<>();
        nullTagValue.put("name1", null);
        Assert.assertTrue(DatadogUtilities.merge(null, nullTagValue).equals(assertionTags));
        Assert.assertTrue(DatadogUtilities.merge(nullTagValue, nullTagValue).equals(assertionTags));
        Assert.assertTrue(DatadogUtilities.merge(nullTagValue, null).toString().equals(assertionTags.toString()));
        Assert.assertTrue(DatadogUtilities.merge(nullTagValue, emptyTags).toString().equals(assertionTags.toString()));
        Assert.assertTrue(DatadogUtilities.merge(emptyTags, nullTagValue).toString().equals(assertionTags.toString()));
        Map<String, Set<String>> emptyTagValue = new HashMap<>();
        emptyTagValue.put("name1", new HashSet<String>());
        Assert.assertTrue(DatadogUtilities.merge(emptyTagValue, null).toString().equals(assertionTags.toString()));
        Assert.assertTrue(DatadogUtilities.merge(null, emptyTagValue).toString().equals(assertionTags.toString()));
        Assert.assertTrue(DatadogUtilities.merge(emptyTagValue, emptyTags).toString().equals(assertionTags.toString()));
        Assert.assertTrue(DatadogUtilities.merge(emptyTags, emptyTagValue).toString().equals(assertionTags.toString()));
        Assert.assertTrue(DatadogUtilities.merge(nullTagValue, emptyTagValue).toString().equals(assertionTags.toString()));
        Assert.assertTrue(DatadogUtilities.merge(emptyTagValue, nullTagValue).toString().equals(assertionTags.toString()));
        Map<String, Set<String>> n1v1Tag = new HashMap<>();
        Set<String> v1 = new HashSet<>();
        v1.add("value1");
        n1v1Tag.put("name1", v1);
        assertionTags = new HashMap<>();
        Set<String> assertionV1 = new HashSet<>();
        assertionV1.add("value1");
        assertionTags.put("name1", assertionV1);
        Assert.assertTrue(DatadogUtilities.merge(n1v1Tag, null).toString().equals(assertionTags.toString()));
        Assert.assertTrue(DatadogUtilities.merge(null, n1v1Tag).toString().equals(assertionTags.toString()));
        Assert.assertTrue(DatadogUtilities.merge(n1v1Tag, emptyTags).toString().equals(assertionTags.toString()));
        Assert.assertTrue(DatadogUtilities.merge(emptyTags, n1v1Tag).toString().equals(assertionTags.toString()));
        Assert.assertTrue(DatadogUtilities.merge(nullTagValue, n1v1Tag).toString().equals(assertionTags.toString()));
        Assert.assertTrue(DatadogUtilities.merge(n1v1Tag, nullTagValue).toString().equals(assertionTags.toString()));
        Assert.assertTrue(DatadogUtilities.merge(n1v1Tag, emptyTagValue).toString().equals(assertionTags.toString()));
        Assert.assertTrue(DatadogUtilities.merge(emptyTagValue, n1v1Tag).toString().equals(assertionTags.toString()));

        Map<String, Set<String>> n1v1TagCopy = new HashMap<>();
        Set<String> v1Copy = new HashSet<>();
        v1Copy.add("value1");
        n1v1TagCopy.put("name1", v1Copy);
        Assert.assertTrue(DatadogUtilities.merge(n1v1TagCopy, n1v1Tag).toString().equals(assertionTags.toString()));

        Map<String, Set<String>> n1v2Tag = new HashMap<>();
        Set<String> v2 = new HashSet<>();
        v2.add("value2");
        n1v2Tag.put("name1", v2);
        assertionTags = new HashMap<>();
        Set<String> assertionValues = new HashSet<>();
        assertionValues.add("value1");
        assertionValues.add("value2");
        assertionTags.put("name1", assertionValues);
        Assert.assertTrue(DatadogUtilities.merge(n1v2Tag, n1v1Tag).toString().equals(assertionTags.toString()));

        Map<String, Set<String>> n2v1Tag = new HashMap<>();
        v1 = new HashSet<>();
        v1.add("value1");
        n2v1Tag.put("name2", v1);
        assertionTags = new HashMap<>();
        assertionV1 = new HashSet<>();
        assertionV1.add("value1");
        assertionTags.put("name1", assertionV1);
        assertionV1.add("value1");
        assertionTags.put("name2", assertionV1);
        Assert.assertTrue(DatadogUtilities.merge(n2v1Tag, n1v1Tag).toString().equals(assertionTags.toString()));

        n2v1Tag = new HashMap<>();
        v1 = new HashSet<>();
        v1.add("value1");
        n2v1Tag.put("name2", v1);
        Map<String, Set<String>> n2v1v2Tag = new HashMap<>();
        Set<String> v1v2 = new HashSet<>();
        v1v2.add("value1");
        v1v2.add("value2");
        n2v1v2Tag.put("name2", v1v2);
        assertionTags = new HashMap<>();
        assertionValues = new HashSet<>();
        assertionValues.add("value1");
        assertionValues.add("value2");
        assertionTags.put("name2", assertionValues);
        Assert.assertTrue(DatadogUtilities.merge(n2v1Tag, n2v1v2Tag).toString() + " - "+ assertionTags.toString(),
                DatadogUtilities.merge(n2v1Tag, n2v1v2Tag).toString().equals(assertionTags.toString()));

    }

}
