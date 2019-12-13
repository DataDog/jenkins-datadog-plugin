package org.datadog.jenkins.plugins.datadog.listeners;

import hudson.Extension;
import jenkins.security.SecurityListener;
import org.acegisecurity.userdetails.UserDetails;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.events.UserAuthenticationEventImpl;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This class registers an {@link SecurityListener} to trigger events and calculate metrics:
 * - When an user authenticates, the {@link #authenticated(UserDetails)} method will be invoked.
 * - When an user fails to authenticate, the {@link #failedToAuthenticate(String)} method will be invoked.
 * - When an user logout, the {@link #loggedOut(String)} method will be invoked.
 */
@Extension
public class DatadogSecurityListener extends SecurityListener {

    private static final Logger logger = Logger.getLogger(DatadogSecurityListener.class.getName());

    @Override
    protected void authenticated(@Nonnull UserDetails details) {
        try {
            final boolean emitSystemEvents = DatadogUtilities.getDatadogGlobalDescriptor().isEmitSecurityEvents();
            if (!emitSystemEvents) {
                return;
            }
            logger.fine("Start DatadogSecurityListener#authenticated");

            // Get Datadog Client Instance
            DatadogClient client = DatadogUtilities.getDatadogClient();

            // Get the list of global tags to apply
            Map<String, Set<String>> tags = DatadogUtilities.getTagsFromGlobalTags();

            // Send event
            DatadogEvent event = new UserAuthenticationEventImpl(details.getUsername(),
                    UserAuthenticationEventImpl.LOGIN, tags);
            client.event(event);

            // Submit counter
            String hostname = DatadogUtilities.getHostname("null");
            client.incrementCounter("jenkins.user.authenticated", hostname, tags);

            logger.fine("End DatadogSecurityListener#authenticated");
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }

    @Override
    protected void failedToAuthenticate(@Nonnull String username) {
        try {
            final boolean emitSystemEvents = DatadogUtilities.getDatadogGlobalDescriptor().isEmitSecurityEvents();
            if (!emitSystemEvents) {
                return;
            }
            logger.fine("Start DatadogSecurityListener#failedToAuthenticate");

            // Get Datadog Client Instance
            DatadogClient client = DatadogUtilities.getDatadogClient();

            // Get the list of global tags to apply
            Map<String, Set<String>> tags = DatadogUtilities.getTagsFromGlobalTags();

            // Send event
            DatadogEvent event = new UserAuthenticationEventImpl(username, UserAuthenticationEventImpl.ACCESS_DENIED, tags);
            client.event(event);

            // Submit counter
            String hostname = DatadogUtilities.getHostname("null");
            client.incrementCounter("jenkins.user.access_denied", hostname, tags);

            logger.fine("End DatadogSecurityListener#failedToAuthenticate");
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }

    @Override
    protected void loggedIn(@Nonnull String username) {
        //Covered by Authenticated
    }

    @Override
    protected void failedToLogIn(@Nonnull String username) {
        //Covered by failedToAuthenticate
    }

    @Override
    protected void loggedOut(@Nonnull String username) {
        try {
            final boolean emitSystemEvents = DatadogUtilities.getDatadogGlobalDescriptor().isEmitSecurityEvents();
            if (!emitSystemEvents) {
                return;
            }
            logger.fine("Start DatadogSecurityListener#loggedOut");

            // Get Datadog Client Instance
            DatadogClient client = DatadogUtilities.getDatadogClient();

            // Get the list of global tags to apply
            Map<String, Set<String>> tags = DatadogUtilities.getTagsFromGlobalTags();

            // Send event
            DatadogEvent event = new UserAuthenticationEventImpl(username, UserAuthenticationEventImpl.LOGOUT, tags);
            client.event(event);

            // Submit counter
            String hostname = DatadogUtilities.getHostname("null");
            client.incrementCounter("jenkins.user.logout", hostname, tags);

            logger.fine("End DatadogSecurityListener#loggedOut");
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }
}
