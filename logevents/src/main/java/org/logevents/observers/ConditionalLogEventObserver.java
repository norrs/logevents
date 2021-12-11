package org.logevents.observers;

import org.logevents.LogEvent;
import org.logevents.LogEventObserver;
import org.logevents.core.LogEventPredicate;
import org.slf4j.Marker;

/**
 * An observer that filters log messages by a given conditions, forwarding those
 * that match the condition to a delegate observer
 */
public class ConditionalLogEventObserver implements LogEventObserver {

    private final LogEventObserver delegate;
    private final LogEventPredicate condition;

    public ConditionalLogEventObserver(LogEventObserver delegate, LogEventPredicate predicate) {
        this.delegate = delegate;
        this.condition = predicate;
    }

    @Override
    public void logEvent(LogEvent logEvent) {
        if (condition.test(logEvent)) {
            this.delegate.logEvent(logEvent);
        }
    }

    @Override
    public boolean isEnabled() {
        return condition.test() && delegate.isEnabled();
    }

    @Override
    public boolean isEnabled(Marker marker) {
        return condition.test(marker) && delegate.isEnabled(marker);
    }

    public LogEventPredicate getCondition() {
        return condition;
    }

    public LogEventObserver getObserver() {
        return delegate;
    }
}
