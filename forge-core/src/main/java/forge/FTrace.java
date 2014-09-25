package forge;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FTrace {
    private static long appStartTime;
    private static Map<String, FTrace> traces = new HashMap<String, FTrace>();

    public static void initialize() {
        appStartTime = new Date().getTime();
    }

    public static FTrace get(String name0) {
        FTrace trace = traces.get(name0);
        if (trace == null) {
            trace = new FTrace(name0);
            traces.put(name0, trace);
        }
        return trace;
    }

    public static String formatTimestamp(Date timestamp) {
        return new SimpleDateFormat("hh:mm:ss.SSS").format(timestamp);
    }

    //dump total time of all traces into log file
    public static void dump() {
        if (traces.isEmpty()) { return; }

        long appTotalTime = new Date().getTime() - appStartTime;
        NumberFormat percent = NumberFormat.getPercentInstance();

        System.out.println();
        System.out.println("Forge total time - " + appTotalTime + "ms");
        for (FTrace trace : traces.values()) {
            System.out.println(trace.name + " total time - " + trace.totalTime + "ms (" + percent.format((double)trace.totalTime / (double)appTotalTime) + ")");
        }
        traces.clear();
    }

    private final String name;
    private long startTime;
    private long totalTime;

    private FTrace(String name0) {
        name = name0;
    }

    public void start() {
        if (startTime > 0) { return; }

        Date now = new Date();
        startTime = now.getTime();
        System.out.println(name + " start - " + formatTimestamp(now));
    }

    public void end() {
        if (startTime == 0) { return; }

        Date now = new Date();
        long elapsed = now.getTime() - startTime;
        startTime = 0;
        totalTime += elapsed;
        System.out.println(name + " end - " + formatTimestamp(now) + " (" + elapsed  + "ms)");
    }
}
