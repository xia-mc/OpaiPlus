package asia.lira.opaiplus;

import asia.lira.opaiplus.internal.NetworkManager;
import asia.lira.opaiplus.modules.misc.BugFixer;
import asia.lira.opaiplus.utils.ChatFormatting;
import asia.lira.opaiplus.modules.combat.VelocityPlus;
import asia.lira.opaiplus.utils.MathUtils;
import asia.lira.opaiplus.utils.RandomUtils;
import asia.lira.opaiplus.utils.Timer;
import org.jetbrains.annotations.NotNull;
import today.opai.api.Extension;
import today.opai.api.OpenAPI;
import today.opai.api.annotations.ExtensionInfo;
import today.opai.api.interfaces.dataset.Vector3d;
import today.opai.api.interfaces.game.network.server.SPacket12Velocity;

@ExtensionInfo(name = "OpaiPlus", author = "xia__mc", version = "0.0.1 (For b18.4Beta)")
public final class OpaiPlus extends Extension {
    private static OpenAPI API = null;

    @Override
    public void initialize(@NotNull OpenAPI api) {
        Timer.begin();
        try {
            API = api;
            checkCompatibility();

            NetworkManager.init();
            api.registerFeature(new VelocityPlus());
            api.registerFeature(new BugFixer());

            success(String.format("Initialize successful. [%dms]", Timer.end()));
        } catch (Throwable e) {
            Timer.end();
            error(String.format("Failed to initialize. [%dms]", Timer.end()));
            e.printStackTrace(System.out);
        }
    }

    @Override
    public void onUnload() {
        success("Unloaded! See you next time.");
    }

    public static @NotNull OpenAPI getAPI() {
        if (API == null) {
            return API = Extension.getAPI();
        }
        return API;
    }

    public static void info(String message) {
        API.printMessage(ChatFormatting.GRAY + "[OpaiPlus] " + ChatFormatting.WHITE + message);
    }

    public static void success(String message) {
        API.printMessage(ChatFormatting.GRAY + "[OpaiPlus] " + ChatFormatting.GREEN + message);
    }

    public static void error(String message) {
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
}
