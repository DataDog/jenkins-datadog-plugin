/*
The MIT License

Copyright (c) 2015-Present Datadog, Inc <opensource@datadoghq.com>
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

package org.datadog.jenkins.plugins.datadog.events;

import hudson.XmlFile;
import hudson.model.Saveable;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.util.Map;
import java.util.Set;

public class ConfigChangedEventImpl extends AbstractDatadogSimpleEvent {

    public ConfigChangedEventImpl(Saveable config, XmlFile file, Map<String, Set<String>> tags) {
        super(tags);

        String fileName = DatadogUtilities.getFileName(file);
        String userId = DatadogUtilities.getUserId();
        setAggregationKey(fileName);

        String title = "User " + userId + " changed file " + fileName;
        setTitle(title);

        String text = "%%% \nUser " + userId + " changed file " + fileName + " \n%%%";
        setText(text);

        setEnums(userId);
    }

    public void setEnums(String userId){
        if (userId != null && "system".equals(userId.toLowerCase())){
            setPriority(Priority.LOW);
            setAlertType(AlertType.INFO);
        }else{
            setPriority(Priority.NORMAL);
            setAlertType(AlertType.WARNING);
        }
    }

}
