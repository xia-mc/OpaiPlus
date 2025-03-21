package asia.lira.opaiplus;

import asia.lira.opaiplus.internal.Module;
import asia.lira.opaiplus.internal.NetworkManager;
import asia.lira.opaiplus.internal.SecurityManager;
import asia.lira.opaiplus.internal.unsafe.DeobfuscateUtils;
import asia.lira.opaiplus.internal.unsafe.Unsafe;
import asia.lira.opaiplus.modules.combat.TimerRange;
import asia.lira.opaiplus.modules.combat.VelocityPlus;
import asia.lira.opaiplus.modules.misc.Fixes;
import asia.lira.opaiplus.modules.misc.NoIRC;
import asia.lira.opaiplus.modules.misc.PartyCT;
import asia.lira.opaiplus.modules.movement.SaveMoveKeys;
import asia.lira.opaiplus.modules.player.ChestAura;
import asia.lira.opaiplus.modules.visual.SilenceSpoof;
import asia.lira.opaiplus.utils.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import today.opai.api.Extension;
import today.opai.api.OpenAPI;
import today.opai.api.annotations.ExtensionInfo;
import today.opai.api.dataset.BlockPosition;
import today.opai.api.enums.EnumNotificationType;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@ExtensionInfo(name = "OpaiPlus", author = "xia__mc", version = "0.8 (For b19 WIP4)")
public final class OpaiPlus extends Extension {
    private static OpenAPI API = null;
    @Getter
    private static ThreadPoolExecutor executor;
    private static Map<Class<? extends Module>, Module> modules;

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
            API.isNull();
            API.getPacketUtil();

            Object ignored = NetworkManager.createC08(
                    new BlockPosition(-1, -1, -1), 255, null, 0, 0, 0
            );
            Unsafe.getCurrentScreen();

            return;
        } catch (Throwable ignored) {
        }

        error("Compatibility test failed. Some features may won't work correctly.");
    }

    private static void addModules(Module @NotNull ... modules) {
        for (Module module : modules) {
            OpaiPlus.modules.put(module.getClass(), module);
            API.registerFeature(module);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T getModule(Class<T> moduleClass) {
        return (T) modules.get(moduleClass);
    }

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
            modules = new Object2ObjectOpenHashMap<>();

            API = api;
            checkCompatibility();

            SecurityManager.init();
            NetworkManager.init();
            addModules(
                    new Fixes(), new SilenceSpoof(),
                    new PartyCT(), new NoIRC(),
                    new VelocityPlus(), new TimerRange(), new ChestAura(), new SaveMoveKeys()
            );

            success(String.format("Initialize successful. [%dms]", Timer.end()));
        } catch (Throwable e) {
            error(String.format("Failed to initialize. [%dms]", Timer.end()));
            e.printStackTrace(System.out);
        }
    }

    @Override
    public void onUnload() {
        try {
            try {
                Timer.begin();
                try {
                    // 要求各模块清理资源
                    modules.values().forEach(Module::unload);

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

                    executor = null;
                    modules = null;

                    System.gc();
                    success(String.format("Unloaded! See you next time. [%dms]", Timer.end()));
                } catch (Throwable e) {
                    error(String.format("Failed to release resources. [%dms]", Timer.end()));
                    e.printStackTrace(System.out);
                } finally {
                    API = null;
                }
            } catch (Throwable e) {
                System.out.println("Error while unloading OpaiPlus.\n");
                e.printStackTrace(System.out);
            }
        } catch (Throwable ignored) {
        }
    }
}
