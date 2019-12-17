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

}
