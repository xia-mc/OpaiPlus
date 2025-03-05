package asia.lira.opaiplus;

import asia.lira.opaiplus.internal.Module;
import asia.lira.opaiplus.internal.NetworkManager;
import asia.lira.opaiplus.internal.SecurityManager;
import asia.lira.opaiplus.modules.combat.VelocityPlus;
import asia.lira.opaiplus.modules.misc.BugFixer;
import asia.lira.opaiplus.modules.misc.PartyCT;
import asia.lira.opaiplus.modules.visual.SilenceSpoof;
import asia.lira.opaiplus.utils.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import today.opai.api.Extension;
import today.opai.api.OpenAPI;
import today.opai.api.annotations.ExtensionInfo;
import today.opai.api.enums.EnumNotificationType;
import today.opai.api.interfaces.dataset.Vector3d;
import today.opai.api.interfaces.game.network.server.SPacket12Velocity;

import java.util.HashSet;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@ExtensionInfo(name = "OpaiPlus", author = "xia__mc", version = "0.1 (For b18.4Beta)")
public final class OpaiPlus extends Extension {
    private static OpenAPI API = null;
    @Getter
    private static ThreadPoolExecutor executor;
    private static Module[] modules;

    public static @NotNull OpenAPI getAPI() {
        if (API == null) {
            return API = Extension.getAPI();
        }
        return API;
    }

    public static void log(String message) {
        if (API == null) {
            System.out.printf("[OpaiPlus][Log] %s", message);
        }
        API.printMessage(ChatFormatting.GRAY + "[OpaiPlus] " + ChatFormatting.WHITE + message);
    }

    public static void success(String message) {
        if (API == null) {
            System.out.printf("[OpaiPlus][Success] %s", message);
        }
        API.popNotification(EnumNotificationType.SUCCESSFULLY, "OpaiPlus", message, 5000);
    }

    public static void error(String message) {
        if (API == null) {
            System.out.printf("[OpaiPlus][Error] %s", message);
        }
        API.popNotification(EnumNotificationType.ERROR, "OpaiPlus", message, 8000);
        API.printMessage(ChatFormatting.GRAY + "[OpaiPlus] " + ChatFormatting.RED + message);
    }

    private static void checkCompatibility() {
        try {
            int id = RandomUtils.randInt();
            double motionX = RandomUtils.randDouble(0, 3.9);
            double motionY = RandomUtils.randDouble(0, 3.9);
            double motionZ = RandomUtils.randDouble(0, 3.9);
            SPacket12Velocity s12 = NetworkManager.createS12(id, motionX, motionY, motionZ);

            Vector3d motion = s12.getMotion();
            if (s12.getEntityId() == id
                    && MathUtils.posEquals(motion.getX() / 8000, motionX)
                    && MathUtils.posEquals(motion.getY() / 8000, motionY)
                    && MathUtils.posEquals(motion.getZ() / 8000, motionZ)) {  // 除以8000因为opai的bug
                return;
            }
        } catch (Throwable ignored) {
        }

        error("Compatibility test failed. Some features may won't work correctly.");
    }

    public static void UNREACHABLE() {
        String alwaysNull = ReflectionUtils.callDeclared(
                ReflectionUtils.getClass("java.lang.System"), "exit",
                new Class[]{int.class}, new Object[]{RandomUtils.randInt()}
        );
        System.loadLibrary(alwaysNull);
        naive();
    }

    private static native void naive();

    @Override
    public void initialize(@NotNull OpenAPI api) {
        Timer.begin();
        try {
            executor = new ThreadPoolExecutor(
                    0, Integer.MAX_VALUE,
                    60L, TimeUnit.SECONDS,
                    new SynchronousQueue<>(),
                    r -> {
                        Thread t = new Thread(r);
                        t.setUncaughtExceptionHandler((thread, e) -> e.printStackTrace(System.out));
                        return t;
                    }, new ThreadPoolExecutor.DiscardPolicy()
            );

            API = api;
            checkCompatibility();

            SecurityManager.init();
            NetworkManager.init();
            modules = new Module[]{new VelocityPlus(), new BugFixer(), new SilenceSpoof(), new PartyCT()};
            for (Module module : modules) {
                API.registerFeature(module);
            }

            success(String.format("Initialize successful. [%dms]", Timer.end()));
        } catch (Throwable e) {
            error(String.format("Failed to initialize. [%dms]", Timer.end()));
            e.printStackTrace(System.out);
        }
    }

    @Override
    public void onUnload() {
        Timer.begin();
        try {
            // 要求各模块清理资源
            for (Module module : modules) {
                module.onDisabled();
            }

            // 要求关闭线程池
            executor.shutdown();
            executor.setKeepAliveTime(1, TimeUnit.MILLISECONDS);
            executor.allowCoreThreadTimeOut(true);
            try {
                boolean ignored = executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
            }
            executor.shutdownNow();

            // 强制清理资源
            HashSet<Object> workers = ReflectionUtils.get(executor, "workers");
            assert workers != null;
            workers.forEach(worker -> {
                Thread thread = ReflectionUtils.get(workers, "thread");
                if (thread == null) return;
                thread.interrupt();
                try {
                    thread.join(100);
                } catch (InterruptedException ignored) {
                }
                if (!thread.isAlive()) return;

                ReflectionUtils.call(thread, "stop0",
                        new Class[]{Object.class}, new Object[]{new ThreadDeath()});
            });
            System.gc();

            success(String.format("Unloaded! See you next time. [%dms]", Timer.end()));
        } catch (Throwable e) {
            error(String.format("Failed to release resources. [%dms]", Timer.end()));
            e.printStackTrace(System.out);
        } finally {
            API = null;
            executor = null;
        }
    }
}
