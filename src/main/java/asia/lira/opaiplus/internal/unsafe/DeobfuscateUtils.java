package asia.lira.opaiplus.internal.unsafe;

import asia.lira.opaiplus.OpaiPlus;
import asia.lira.opaiplus.utils.ReflectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

@SuppressWarnings("unused")
public class DeobfuscateUtils {

    public static void exportClass(@NotNull Class<?> aClass) {
        try {
            ClassNode classNode = new ClassNode();
            new ClassReader(aClass.getName()).accept(classNode, 0);
            ClassWriter writer = new ClassWriter(0);
            classNode.accept(writer);
            Files.write(
                    Paths.get(String.format("D:/OpaiPlus/export/%s.class", aClass.getSimpleName())),
                    writer.toByteArray()
            );
        } catch (Throwable e) {
            e.printStackTrace(System.out);
            Unsafe.THROW(e);
        }
    }

    public static void findField(@NotNull Class<?> aClass, @Nullable Object object,
                                 Class<?> fieldType, int fieldModifier, Object fieldValue) {
        try {
            for (Field field : aClass.getDeclaredFields()) {
                if (!field.getType().equals(fieldType)) {
                    continue;
                }

                if ((field.getModifiers() & fieldModifier) == 0) {
                    continue;
                }

                if (!field.get(object).equals(fieldValue)) {
                    continue;
                }

                OpaiPlus.log(String.format("Found '%s'", field.getName()));
            }
        } catch (Throwable e) {
            e.printStackTrace(System.out);
            Unsafe.THROW(e);
        }
    }

    @SuppressWarnings("BusyWait")
    public static void trackValue(@NotNull Object object, String fieldName, long delay) {
        OpaiPlus.getExecutor().execute(() -> {
            Object lastValue = ReflectionUtils.get(object, fieldName);
            OpaiPlus.log(String.format("Field '%s' has value '%s'", fieldName, lastValue));

            Object newValue;
            while (true) {
                try {
                    newValue = ReflectionUtils.get(object, fieldName);
                    if (!Objects.equals(newValue, lastValue)) {
                        lastValue = newValue;
                        OpaiPlus.log(String.format("Field '%s' has value '%s'", fieldName, lastValue));
                    }
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }
}
