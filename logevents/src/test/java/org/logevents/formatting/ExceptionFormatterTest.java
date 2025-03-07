package org.logevents.formatting;

import org.junit.Test;
import org.logevents.config.Configuration;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExceptionFormatterTest {

    private final StackTraceElement mainMethod = new StackTraceElement("org.logeventsdemo.MyApplication", "main", "MyApplication.java", 20);
    private final StackTraceElement publicMethod = new StackTraceElement("org.logeventsdemo.internal.MyClassName", "publicMethod", "MyClassName.java", 31);
    private final StackTraceElement internalMethod = new StackTraceElement("org.logeventsdemo.internal.MyClassName", "internalMethod", "MyClassName.java", 311);
    private final StackTraceElement nioApiMethod = new StackTraceElement("java.nio.file.Files", "write", "Files.java", 3292);
    private final StackTraceElement nioInternalMethod = new StackTraceElement("sun.nio.fs.WindowsException", "translateToIOException", "WindowsException.java", 79);
    private final StackTraceElement ioApiMethod = new StackTraceElement("java.io.FilterOutputStream", "close", "FilterOutputStream.java", 180);
    private final StackTraceElement ioInternalMethod = new StackTraceElement("java.io.FileOutputStream", "close", "FileOutputStream.java", 323);
    private final Map<String, String> properties = new HashMap<>();
    {
        properties.put("observer.file.formatter.exceptionFormatter",
                ExceptionFormatter.class.getName());
    }

    @Test
    public void shouldFormatSimpleException() {
        IOException simpleException = new IOException("This went wrong");
        simpleException.setStackTrace(new StackTraceElement[] { publicMethod, mainMethod });

        String nl = ExceptionFormatter.newLine();
        assertEquals("java.io.IOException: This went wrong" + nl +  "\tat " + publicMethod + nl + "\tat " + mainMethod + nl,
                getFormatter().format(simpleException));
    }

    @Test
    public void shouldNoException() {
        assertEquals("", getFormatter().format(null));
    }

    @Test
    public void shouldFormatStackTrace() {
        RuntimeException exception = new RuntimeException("This is an error message");
        exception.setStackTrace(new StackTraceElement[] {
                internalMethod, publicMethod, mainMethod
        });

        String[] lines = getFormatter().format(exception).split("\r?\n");
        assertEquals("java.lang.RuntimeException: This is an error message", lines[0]);
        assertEquals("\tat org.logeventsdemo.internal.MyClassName.internalMethod(MyClassName.java:311)", lines[1]);
        assertEquals("\tat org.logeventsdemo.internal.MyClassName.publicMethod(MyClassName.java:31)", lines[2]);
        assertEquals("\tat org.logeventsdemo.MyApplication.main(MyApplication.java:20)", lines[3]);
        assertEquals(4, lines.length);
    }

    @Test
    public void shouldOutputNestedExceptions() {
        IOException ioException = new IOException("An IO exception happened");
        ioException.setStackTrace(new StackTraceElement[] {
                nioInternalMethod, nioApiMethod, internalMethod, publicMethod, mainMethod
        });

        RuntimeException exception = new RuntimeException("This is an error message", ioException);
        exception.setStackTrace(new StackTraceElement[] {
                internalMethod, publicMethod, mainMethod
        });

        String[] lines = getFormatter().format(exception).split("\r?\n");

        assertEquals("java.lang.RuntimeException: This is an error message", lines[0]);
        assertEquals("\tat " + internalMethod, lines[1]);
        assertEquals("Caused by: " + ioException, lines[4]);
        assertEquals("\tat " + nioInternalMethod, lines[5]);
        assertEquals("\tat " + nioApiMethod, lines[6]);
        assertEquals("\t... 3 more", lines[7]);
    }


    @Test
    public void shouldOutputSuppressedExceptions() {
        IOException nested = new IOException("Nested");
        nested.setStackTrace(new StackTraceElement[] {
                nioInternalMethod, nioInternalMethod, internalMethod, publicMethod, mainMethod
        });
        IOException nestedSuppressed = new IOException("Nested suppressed");
        nestedSuppressed.setStackTrace(new StackTraceElement[] {
                ioApiMethod, nioInternalMethod, internalMethod, publicMethod, mainMethod
        });
        nested.addSuppressed(nestedSuppressed);
        IOException suppressedSuppressed = new IOException("Suppressed, suppressed");
        suppressedSuppressed.setStackTrace(new StackTraceElement[] {
                ioInternalMethod, ioApiMethod, nioInternalMethod, internalMethod, publicMethod, mainMethod
        });
        nestedSuppressed.addSuppressed(suppressedSuppressed);

        String[] lines = getFormatter().format(nested).split("\r?\n");

        assertEquals(8, lines.length);
        assertEquals(nested.toString(), lines[0]);
        assertEquals("\tat " + nioInternalMethod, lines[1]);

        assertEquals("\tSuppressed: " + nestedSuppressed, lines[5]);
        assertEquals("\t\tat " + ioApiMethod, lines[6]);
        assertEquals("\t\t... 4 more", lines[7]);
    }

    @Test
    public void shouldLimitStackTrace() {
        IOException nestedNested = new IOException("Nested nested");
        nestedNested.setStackTrace(new StackTraceElement[] {
                ioInternalMethod, ioApiMethod, nioInternalMethod, nioInternalMethod, internalMethod, publicMethod, mainMethod
        });
        IOException nested = new IOException("Nested", nestedNested);
        nested.setStackTrace(new StackTraceElement[] {
                ioApiMethod, nioInternalMethod, nioInternalMethod, internalMethod, publicMethod, mainMethod
        });
        RuntimeException exception = new RuntimeException("This is an error message", nested);
        exception.setStackTrace(new StackTraceElement[] {
                internalMethod, publicMethod, mainMethod
        });

        properties.put("observer.file.formatter.exceptionFormatter.maxLength", "2");
        String[] lines = getFormatter().format(exception).split("\r?\n");

        assertEquals(exception.toString(), lines[0]);
        assertEquals("\tat " + internalMethod, lines[1]);
        assertEquals("\tat " + publicMethod, lines[2]);
        assertEquals("Caused by: " + nested, lines[3]);
        assertEquals("\tat " + ioApiMethod, lines[4]);
        assertEquals("\t[2 skipped]", lines[5]);
        assertEquals("Caused by: " + nestedNested, lines[6]);
        assertEquals("\tat " + ioInternalMethod, lines[7]);
        assertEquals("\t... 6 more", lines[8]);
        assertEquals(1 + 2 + 1 + 2 + 1 + 2, lines.length);
    }

    @Test
    public void shouldFilterStackTrace() {
        IOException exceptions = new IOException("Nested nested");
        exceptions.setStackTrace(new StackTraceElement[] {
                ioInternalMethod, ioApiMethod,
                nioInternalMethod, nioInternalMethod, nioInternalMethod, nioInternalMethod, nioInternalMethod,
                internalMethod, publicMethod, mainMethod
        });

        properties.put("observer.file.formatter.exceptionFormatter.packageFilter",
                "sun.nio.fs, java.nio");
        properties.put("observer.file.formatter.exceptionFormatter.maxLength",
                "4");
        String[] lines = getFormatter().format(exceptions).split("\r?\n");

        assertEquals(exceptions.toString(), lines[0]);
        assertEquals("\tat " + ioInternalMethod, lines[1]);
        assertEquals("\tat " + ioApiMethod, lines[2]);
        assertEquals("\tat " + internalMethod + " [5 skipped]", lines[3]);
        assertEquals("\tat " + publicMethod, lines[4]);
        assertEquals(1+4, lines.length);
    }

    @Test
    public void shouldConfigureDefaultPackageFilter() {
        IOException exceptions = new IOException("Nested nested");
        exceptions.setStackTrace(new StackTraceElement[] {
                ioInternalMethod, ioApiMethod,
                nioInternalMethod, nioInternalMethod,
                internalMethod
        });

        properties.put("observer.*.packageFilter", "sun.nio.fs, java.nio");
        String[] lines = getFormatter().format(exceptions).split("\r?\n");

        assertEquals(exceptions.toString(), lines[0]);
        assertEquals("\tat " + ioInternalMethod, lines[1]);
        assertEquals("\tat " + ioApiMethod, lines[2]);
        assertEquals("\tat " + internalMethod + " [2 skipped]", lines[3]);
        assertEquals(1+3, lines.length);
    }

    @Test
    public void shouldOutputFinalIgnoredLineCount() {
        IOException exceptions = new IOException("Nested nested");
        exceptions.setStackTrace(new StackTraceElement[] {
                ioInternalMethod, ioApiMethod,
                nioInternalMethod, nioInternalMethod, nioInternalMethod, nioInternalMethod, nioInternalMethod
        });

        properties.put("observer.file.formatter.exceptionFormatter.packageFilter",
                "sun.nio.fs, java.nio");
        String[] lines = getFormatter().format(exceptions).split("\r?\n");

        assertEquals(exceptions.toString(), lines[0]);
        assertEquals("\tat " + ioInternalMethod, lines[1]);
        assertEquals("\tat " + ioApiMethod, lines[2]);
        assertEquals("\t[5 skipped]", lines[3]);
        assertEquals(4, lines.length);
    }

    @Test
    public void shouldNotFailOnExceptionCycles() throws Exception {
        IOException exception = new IOException("Nested nested");
        exception.setStackTrace(new StackTraceElement[] {
                ioInternalMethod, ioApiMethod, nioInternalMethod
        });
        Field causeField = Throwable.class.getDeclaredField("cause");
        causeField.setAccessible(true);
        causeField.set(exception, exception);

        String[] lines = getFormatter().format(exception).split("\r?\n");
        assertEquals(Arrays.asList(exception.toString(),
                "\tat " + ioInternalMethod, "\tat " + ioApiMethod, "\t[1 skipped]"),
                Arrays.asList(lines));
    }

    @Test
    public void shouldNotFailOnSuppressedExceptionCycles() {
        IOException exception = new IOException("Nested nested");
        exception.setStackTrace(new StackTraceElement[] {
                ioInternalMethod, ioApiMethod
        });
        RuntimeException suppressed = new RuntimeException(exception);
        suppressed.setStackTrace(new StackTraceElement[] { mainMethod });
        exception.addSuppressed(suppressed);

        String[] lines = getFormatter().format(exception).split("\r?\n");
        assertEquals(Arrays.asList(exception.toString(),
                "\tat " + ioInternalMethod, "\tat " + ioApiMethod, "\tSuppressed: " + suppressed, "\t\tat " + mainMethod),
                Arrays.asList(lines));
    }

    @Test
    public void shouldFindPackagingInformation() {
        RuntimeException exception = new RuntimeException("Something went wrong");
        StackTraceElement[] stackTrace = new StackTraceElement[] {
            new StackTraceElement("java.nio.file.Files", "write", "Files.java", 3292),
            new StackTraceElement("org.logevents.formatting.ExceptionFormatterTest", "shouldFindPackagingInformation", "ExceptionFormatterTest.java", 175),
            new StackTraceElement("org.logevents.formatting.NoSuchClass", "unknownMethod", "NoSuchClass.java", 17),
            new StackTraceElement("org.junit.runners.model.FrameworkMethod$1", "runReflectiveCall", "FrameworkMethod.java", 50),
            new StackTraceElement("org.junit.internal.runners.model.ReflectiveCallable", "run", ".ReflectiveCallable.java", 12),
        };
        exception.setStackTrace(stackTrace);

        properties.put("observer.file.formatter.exceptionFormatter.packageFilter", "none");
        properties.put("observer.file.formatter.exceptionFormatter.includePackagingData", "true");

        String[] lines = getFormatter().format(exception).split("\r?\n");

        String javaVersion = System.getProperty("java.version");

        assertEquals(Arrays.asList(exception.toString(),
                "\tat " + stackTrace[0] + " [rt.jar:" + javaVersion + "]",
                "\tat " + stackTrace[1] + " [test-classes:na]",
                "\tat " + stackTrace[2] + " [na:na]",
                "\tat " + stackTrace[3] + " [junit-4.13.2.jar:4.13.2]",
                "\tat " + stackTrace[4] + " [junit-4.13.2.jar:4.13.2]"),
                Arrays.asList(lines));
    }

    private ExceptionFormatter getFormatter() {
        Configuration configuration = new Configuration(properties, "observer.file.formatter");
        return configuration.createInstance("exceptionFormatter", ExceptionFormatter.class);
    }
}
