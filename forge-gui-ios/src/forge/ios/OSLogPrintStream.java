package forge.ios;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.robovm.apple.foundation.Foundation;
import org.robovm.apple.uikit.UIDevice;

/**
 * A PrintStream that redirects output to os_log (iOS 26+) or NSLog (older iOS)
 * so all logging is visible via idevicesyslog on any iOS version.
 *
 * iOS 26+ redacts NSLog output as {@code <private>}, so os_log with
 * {@code %{public}s} is required. Older iOS versions expose NSLog via
 * idevicesyslog but may not surface os_log entries.
 */
public class OSLogPrintStream extends PrintStream {
    private final String prefix;
    private final boolean isError;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    // Lazy-initialized: UIDevice may not be available during early class loading
    static volatile Boolean useOSLog;

    static boolean shouldUseOSLog() {
        if (useOSLog == null) {
            try {
                String ver = UIDevice.getCurrentDevice().getSystemVersion();
                int major = Integer.parseInt(ver.split("\\.")[0]);
                useOSLog = Boolean.valueOf(major >= 26);
            } catch (Throwable t) {
                // Default to NSLog if we can't detect version
                useOSLog = Boolean.FALSE;
            }
        }
        return useOSLog.booleanValue();
    }

    public OSLogPrintStream(String prefix, boolean isError) {
        super(new OutputStream() {
            @Override
            public void write(int b) {
                // Dummy - we override the print methods
            }
        });
        this.prefix = prefix;
        this.isError = isError;
    }

    @Override
    public void println(String x) {
        log(x);
    }

    @Override
    public void println(Object x) {
        log(String.valueOf(x));
    }

    @Override
    public void println() {
        log("");
    }

    @Override
    public void println(boolean x) {
        log(String.valueOf(x));
    }

    @Override
    public void println(char x) {
        log(String.valueOf(x));
    }

    @Override
    public void println(int x) {
        log(String.valueOf(x));
    }

    @Override
    public void println(long x) {
        log(String.valueOf(x));
    }

    @Override
    public void println(float x) {
        log(String.valueOf(x));
    }

    @Override
    public void println(double x) {
        log(String.valueOf(x));
    }

    @Override
    public void println(char[] x) {
        log(new String(x));
    }

    @Override
    public void print(String s) {
        if (s == null) s = "null";
        synchronized (buffer) {
            // Buffer UTF-8 bytes, not raw chars: ByteArrayOutputStream.write(int)
            // truncates a char to its low 8 bits, garbling any non-ASCII text
            // (localized strings, card names with accents).
            int start = 0;
            int nl;
            while ((nl = s.indexOf('\n', start)) != -1) {
                writeUtf8(s.substring(start, nl));
                flushBuffer();
                start = nl + 1;
            }
            writeUtf8(s.substring(start));
        }
    }

    private void writeUtf8(String part) {
        if (part.isEmpty()) {
            return;
        }
        byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
        buffer.write(bytes, 0, bytes.length);
    }

    @Override
    public void print(Object obj) {
        print(String.valueOf(obj));
    }

    @Override
    public void print(boolean b) {
        print(String.valueOf(b));
    }

    @Override
    public void print(char c) {
        print(String.valueOf(c));
    }

    @Override
    public void print(int i) {
        print(String.valueOf(i));
    }

    @Override
    public void print(long l) {
        print(String.valueOf(l));
    }

    @Override
    public void print(float f) {
        print(String.valueOf(f));
    }

    @Override
    public void print(double d) {
        print(String.valueOf(d));
    }

    @Override
    public void print(char[] s) {
        print(new String(s));
    }

    @Override
    public void write(int b) {
        synchronized (buffer) {
            if (b == '\n') {
                flushBuffer();
            } else {
                buffer.write(b);
            }
        }
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        synchronized (buffer) {
            for (int i = off; i < off + len; i++) {
                write(buf[i]);
            }
        }
    }

    @Override
    public void flush() {
        synchronized (buffer) {
            if (buffer.size() > 0) {
                flushBuffer();
            }
        }
    }

    private void flushBuffer() {
        // Decode explicitly as UTF-8 (the buffer holds UTF-8 bytes from print()
        // and write()); the no-arg toString() uses the platform default charset.
        String line = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        buffer.reset();
        log(line);
    }

    private void log(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        String fullMessage = prefix + message;
        try {
            if (shouldUseOSLog()) {
                if (isError) {
                    ForgeOSLog.logError(fullMessage);
                } else {
                    ForgeOSLog.logPublic(fullMessage);
                }
            } else {
                Foundation.log("%@", new org.robovm.apple.foundation.NSString(fullMessage));
            }
        } catch (Throwable t) {
            // If logging fails, we can't do much - avoid infinite recursion
        }
    }
}
