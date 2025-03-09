package asia.lira.opaiplus.internal.unsafe;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Files;
import java.nio.file.Paths;

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
            AntiCrack.THROW(e);
        }
    }
}
