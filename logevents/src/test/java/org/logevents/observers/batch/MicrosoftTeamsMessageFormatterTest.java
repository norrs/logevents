package org.logevents.observers.batch;


import org.junit.Test;
import org.logevents.LogEvent;
import org.logevents.config.Configuration;
import org.logevents.extend.servlets.LogEventSampler;
import org.logevents.observers.MicrosoftTeamsLogEventObserver;
import org.logevents.util.JsonUtil;
import org.slf4j.event.Level;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MicrosoftTeamsMessageFormatterTest {

    @Test
    public void shouldIncludeLevelInTeamsMessage() {
        LogEventBatch batch = new LogEventBatch();
        LogEvent event1 = new LogEventSampler().build();
        batch.add(event1);
        LogEvent event2 = new LogEventSampler().build();
        batch.add(event2);
        batch.add(new LogEventSampler().withLevel(Level.ERROR).build());
        batch.add(new LogEventSampler().build());

        Map<String, Object> teamsMessage = new MicrosoftTeamsMessageFormatter(
                new Properties(), "observer.teams.formatter"
        ).createMessage(batch);

        assertEquals(MicrosoftTeamsMessageFormatter.getLevelColor(Level.ERROR), teamsMessage.get("themeColor"));

        Map<String, Object> detailsSection = JsonUtil.getObject(JsonUtil.getList(teamsMessage, "sections"), 0);
        assertEquals(new Configuration().getApplicationNode(), detailsSection.get("activitySubtitle"));
    }

    @Test
    public void shouldIncludeStackTrace() {
        RuntimeException exception = new RuntimeException(new IOException("Something went wrong"));
        LogEvent event = new LogEventSampler().withThrowable(exception).build();

        Map<String, Object> teamsMessage = new MicrosoftTeamsMessageFormatter(
                new Properties(), "observer.teams.formatter"
        ).createMessage(new LogEventBatch().add(event));

        Map<String, Object> exceptionSection = JsonUtil.getObject(JsonUtil.getList(teamsMessage, "sections"), 1);
        assertEquals("**" + exception + "**", exceptionSection.get("title").toString());
        assertContains("MicrosoftTeamsMessageFormatterTest.shouldIncludeStackTrace", exceptionSection.get("text").toString());
        assertContains("java.lang.reflect.Method.invoke", exceptionSection.get("text").toString());
    }


    private Map<String, Object> postedJson;

    @Test
    public void shouldCreateObserver() {
        Properties properties = new Properties();
        properties.put("observer.teams.url", "http://example.com/webhook");
        MicrosoftTeamsLogEventObserver observer = new MicrosoftTeamsLogEventObserver(properties, "observer.teams") {
            @Override
            protected String postJson(Map<String, Object> jsonMessage) {
                postedJson = jsonMessage;
                return "OK";
            }
        };
        LogEvent event = new LogEventSampler().build();
        observer.processBatch(new LogEventBatch().add(event));

        assertContains(event.getMessage(), JsonUtil.getField(postedJson, "text").toString());
    }

    private void assertContains(String expected, String actual) {
        assertTrue("Expected to find <" + expected + "> in <" + actual + ">",
                actual.contains(expected));
    }

}
