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

package org.datadog.jenkins.plugins.datadog;

import com.timgroup.statsd.Event;

import java.util.Map;
import java.util.Set;

/**
 * Interface for Datadog events.
 */
public interface DatadogEvent {

    public static enum AlertType {
        ERROR,
        WARNING,
        INFO,
        SUCCESS;

        private AlertType() {
        }

        public Event.AlertType toEventAlertType(){
            return Event.AlertType.valueOf(this.name());
        }
    }

    public static enum Priority {
        LOW,
        NORMAL;

        private Priority() {
        }

        public Event.Priority toEventPriority(){
            return Event.Priority.valueOf(this.name());
        }
    }

    public String getTitle();

    public String getText();

    public String getHost();

    public Priority getPriority();

    public AlertType getAlertType();

    public String getAggregationKey();

    public Long getDate();

    public Map<String, Set<String>> getTags();


}
