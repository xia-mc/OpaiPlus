package asia.lira.opaiplus.utils;

import java.util.LinkedList;

public class Timer {
    private static final ThreadLocal<LinkedList<Long>> startTimes = ThreadLocal.withInitial(LinkedList::new);

    public static void begin() {
        startTimes.get().push(System.nanoTime());
    }

    public static long end() {
        return (System.nanoTime() - startTimes.get().pop()) / 1000000;
    }
}
