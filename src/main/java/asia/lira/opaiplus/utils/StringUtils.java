package asia.lira.opaiplus.utils;

import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

public class StringUtils {

    public static @NotNull String join(String spilt, @NotNull Iterable<String> strings) {
        return join(spilt, strings.iterator());
    }

    public static @NotNull String join(String spilt, @NotNull Iterator<String> strings) {
        if (!strings.hasNext())
            return "";

        StringBuilder result = new StringBuilder();
        while (true) {
            result.append(strings.next());
            if (!strings.hasNext())
                break;
            result.append(spilt);
        }

        return result.toString();
    }

    public static String getStackTraceAsString(@NotNull Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    public static @NotNull String limitLength(@NotNull String string, int length) {
        if (length >= string.length()) {
            return string;
        }
        return string.substring(0, length);
    }
}
