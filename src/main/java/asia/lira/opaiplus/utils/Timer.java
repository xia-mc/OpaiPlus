package asia.lira.opaiplus.utils;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongStack;

public class Timer {
    private static final LongStack startTimes = new LongArrayList();

    public static void begin() {
        synchronized (startTimes) {
            startTimes.push(System.nanoTime());
        }
    }

    public static long end() {
        synchronized (startTimes) {
            return (System.nanoTime() - startTimes.popLong()) / 1000000;
        }
    }
}
