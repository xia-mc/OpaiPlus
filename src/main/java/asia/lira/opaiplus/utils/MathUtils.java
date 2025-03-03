package asia.lira.opaiplus.utils;

public class MathUtils {

    public static boolean posEquals(double first, double second) {
        if (first == second) {
            return true;
        }

        return Math.abs(first - second) < 0.001;
    }

    public static double limit(double value, double min, double max) {
        return value > max ? max : Math.max(value, min);
    }
}
