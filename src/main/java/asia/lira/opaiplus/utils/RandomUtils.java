package asia.lira.opaiplus.utils;

import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomUtils {

    public static int randInt(int min, int max) {
        if (min == max) {
            return min;
        }
        if (min > max) {
            return ThreadLocalRandom.current().nextInt(max, min);
        }
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    public static int randInt() {
        return randInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static int randIntExcept(int except) {
        int result = ThreadLocalRandom.current().nextInt();
        return result == except ? result + 1 : result;
    }

    public static double randDouble(double min, double max) {
        if (min == max) {
            return min;
        }
        if (min > max) {
            return ThreadLocalRandom.current().nextDouble(max, min);
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static double randDouble() {
        return randDouble(Double.MIN_VALUE, Double.MAX_VALUE);
    }

    public static <T> T choice(@NotNull List<T> list) {
        assert !list.isEmpty();
        return list.get(randInt(0, list.size()));
    }

    public static <T> T choiceOrElse(@NotNull List<T> list, T defaultValue) {
        if (list.isEmpty())
            return defaultValue;
        return choice(list);
    }

    public static <T> T choiceExcept(@NotNull List<T> list, T except) {
        assert !list.isEmpty();

        int size = list.size();
        int index = randInt(0, size);
        T result = list.get(index);
        if (result == except) {
            index++;
            if (index == size)
                index = 0;
            return list.get(index);
        }
        return result;
    }

    public static <T> T choiceExcept(@NotNull Collection<T> collection, T except) {
        return choiceExcept(new ObjectImmutableList<>(collection), except);
    }
}
