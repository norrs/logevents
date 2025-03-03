package org.logevents.formatting;

import org.junit.Test;
import org.logevents.config.Configuration;
import org.logevents.extend.junit.LogEventSampler;
import org.logevents.util.JsonParser;
import org.logevents.util.JsonUtil;
import org.slf4j.event.Level;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ConsoleJsonLogEventFormatterTest {

    private final ConsoleJsonLogEventFormatter formatter = new ConsoleJsonLogEventFormatter();

    @Test
    public void shouldLogMessage() {
        String loggerName = "com.example.LoggerName";
        Instant time = ZonedDateTime.of(2018, 8, 1, 10, 0, 0, 0, ZoneOffset.UTC).toInstant();

        String message = formatter.apply(new LogEventSampler()
                .withLevel(Level.INFO)
                .withTime(time)
                .withThread("main")
                .withLoggerName(loggerName)
                .withFormat("Hello {}").withArgs("there")
                .build());
        assertEquals("{\"@timestamp\": \"2018-08-01T10:00:00Z\",\"messageFormat\": \"Hello {}\",\"level\": \"INFO\",\"marker\": \"HTTP_ERROR\",\"logger\": \"com.example.LoggerName\",\"levelInt\": 20,\"thread\": \"main\",\"message\": \"Hello there\"}\n",
                message);
    }

    @Test
    public void shouldDisplayMdc() {
        Map<String, String> properties = new HashMap<>();
        properties.put("observer.console.includedMdcKeys", "operation,user");
        Configuration configuration = new Configuration(properties, "observer.console");
        formatter.configure(configuration);
        configuration.checkForUnknownFields();

        String message = formatter.apply(new LogEventSampler()
                .withMdc("operation", "op13")
                .withMdc("user", "userOne")
                .withMdc("secret", "secret value")
                .build());
        Map<String, Object> mdc = JsonUtil.getObject(JsonParser.parseObject(message), "mdc");
        assertEquals("op13", mdc.get("operation"));
        assertEquals("userOne", mdc.get("user"));
        assertNull(mdc.get("secret"));
    }

    @Test
    public void shouldExcludeMdc() {
        Map<String, String> properties = new HashMap<>();
        properties.put("observer.console.excludedMdcKeys", "secret");
        Configuration configuration = new Configuration(properties, "observer.console");
        formatter.configure(configuration);
        configuration.checkForUnknownFields();

        String message = formatter.apply(new LogEventSampler()
                .withMdc("operation", "op13")
                .withMdc("user", "userOne")
                .withMdc("secret", "secret value")
                .build());
        Map<String, Object> mdc = JsonUtil.getObject(JsonParser.parseObject(message), "mdc");
        assertEquals("op13", mdc.get("operation"));
        assertEquals("userOne", mdc.get("user"));
        assertNull(mdc.get("secret"));
    }

    @Test
    public void shouldIncludeBaseProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("observer.console.formatter.properties.environment", "staging");
        properties.put("observer.console.formatter.properties.dataCenter", "norway-east");
        Configuration configuration = new Configuration(properties, "observer.console.formatter");
        formatter.configure(configuration);
        configuration.checkForUnknownFields();

        Map<String, Object> json = JsonParser.parseObject(formatter.apply(new LogEventSampler().build()));
       assertEquals("staging", json.get("environment"));
       assertEquals("norway-east", json.get("dataCenter"));
    }
}
