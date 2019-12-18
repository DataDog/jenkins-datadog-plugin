/*
The MIT License

Copyright (c) 2010-2019, Datadog <info@datadoghq.com>
All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

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
