package org.logevents.observers;

import org.logevents.LogEvent;
import org.logevents.LogEventObserver;

import java.util.Collections;
import java.util.List;

/**
 * A {@link LogEventObserver} that does nothing. Useful to avoid null
 * checks and null pointer exception.
 *
 * @author Johannes Brodwall
 */
public class NullLogEventObserver implements LogEventObserver {

    @Override
    public void logEvent(LogEvent logEvent) {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NullLogEventObserver;
    }

    @Override
    public List<LogEventObserver> toList() {
        return Collections.emptyList();
    }
}
