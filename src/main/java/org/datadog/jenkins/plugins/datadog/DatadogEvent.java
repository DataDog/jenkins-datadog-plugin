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
