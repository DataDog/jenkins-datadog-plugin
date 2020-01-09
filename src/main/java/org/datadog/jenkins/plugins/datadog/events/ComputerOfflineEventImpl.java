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

import hudson.model.Computer;
import hudson.slaves.OfflineCause;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.util.Map;
import java.util.Set;

public class ComputerOfflineEventImpl extends AbstractDatadogSimpleEvent {

    public ComputerOfflineEventImpl(Computer computer, OfflineCause cause, Map<String, Set<String>> tags, boolean isTemporarily) {
        super(tags);

        String nodeName = DatadogUtilities.getNodeName(computer);
        setAggregationKey(nodeName);

        String title = "Jenkins node " + nodeName + " is" + (isTemporarily? " temporarily ": " ") + "offline";
        setTitle(title);

        // TODO: Add more info about the case in the event in message.
        String text = "%%% \nJenkins node " + nodeName + " is" + (isTemporarily? " temporarily ": " ") +
                "offline \n%%%";
        setText(text);

        setPriority(Priority.NORMAL);
        setAlertType(AlertType.WARNING);
    }
}
