package com.example;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.management.ManagementFactory.getThreadMXBean;

class PyroscopeAggregator {
    private final ThreadMXBean threadMXBean;
    private long from;
    private Map<String, Integer> result = new ConcurrentHashMap<>();

    public PyroscopeAggregator() {
        this.threadMXBean = getThreadMXBean();

        this.from = System.currentTimeMillis() / 1000;
    }

    public void aggregate() {
        for (ThreadInfo threadInfo : threadMXBean.dumpAllThreads(false, false)) {
            Thread.State threadState = threadInfo.getThreadState();
            if (threadState != Thread.State.RUNNABLE) {
                continue;
            }

            StackTraceElement[] traces = threadInfo.getStackTrace();
            StringBuilder builder = new StringBuilder();
            for (int i = traces.length - 1; i >= 0; i--) {
                builder.append(traces[i].getClassName());
                builder.append(".");
                builder.append(traces[i].getMethodName());
                if (i != 0) {
                    builder.append(";");
                }
            }
            String key = builder.toString();
            result.compute(key, (s, i) -> i == null ? 1 : i + 1);
        }
    }

    public Snapshot snapshot() {
        long from = this.from;
        long until = System.currentTimeMillis() / 1000;
        Map<String, Integer> retval = this.result;
        this.result = new ConcurrentHashMap<>();
        this.from = until;
        return new Snapshot(from, until, retval);
    }

    public static class Snapshot {
        private final long from;
        private final long until;
        private final Map<String, Integer> snapshot;

        public Snapshot(long from, long until, Map<String, Integer> snapshot) {
            this.from = from;
            this.until = until;
            this.snapshot = snapshot;
        }

        public long getFrom() {
            return from;
        }

        public long getUntil() {
            return until;
        }

        public Map<String, Integer> getSnapshot() {
            return snapshot;
        }
    }
}
