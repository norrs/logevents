package org.logevents.observers;

import java.util.ArrayList;
import java.util.List;

import org.logevents.LogEvent;
import org.logevents.LogEventObserver;

public class CompositeLogEventObserver implements LogEventObserver {

    private List<LogEventObserver> observers;

    private CompositeLogEventObserver(List<LogEventObserver> observers) {
        this.observers = observers;
    }

    @Override
    public void logEvent(LogEvent e) {
        observers.forEach(o -> o.logEvent(e));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + observers + "}";
    }

    public static LogEventObserver combine(LogEventObserver... args) {
        List<LogEventObserver> observers = new ArrayList<>();
        for (LogEventObserver o : args) {
            if (o == null || o instanceof NullLogEventObserver) {
                continue;
            } else if (o instanceof CompositeLogEventObserver) {
                observers.addAll(((CompositeLogEventObserver)o).observers);
            } else {
                observers.add(o);
            }
        }
        if (observers.isEmpty()) {
            return new NullLogEventObserver();
        } else if (observers.size() == 1) {
            return observers.get(0);
        } else {
            return new CompositeLogEventObserver(observers);
        }
    }
}
