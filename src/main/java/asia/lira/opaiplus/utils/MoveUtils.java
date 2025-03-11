package asia.lira.opaiplus.utils;

import asia.lira.opaiplus.OpaiPlus;
import today.opai.api.dataset.Vec3Data;
import today.opai.api.interfaces.dataset.Vector3d;
import today.opai.api.interfaces.game.entity.LocalPlayer;
import today.opai.api.interfaces.game.item.PotionEffect;
import today.opai.api.interfaces.modules.PresetModule;
import today.opai.api.interfaces.modules.special.ModuleScaffold;

public class MoveUtils {
    private static final LocalPlayer player = OpaiPlus.getAPI().getLocalPlayer();
    public static final double WALK_SPEED = 0.221;
    public static final double BUNNY_SLOPE = 0.66;
    public static final double MOD_SPRINTING = 1.3F;
    public static final double MOD_SNEAK = 0.3F;
    public static final double MOD_ICE = 2.5F;
    public static final double MOD_WEB = 0.105 / WALK_SPEED;
    public static final double JUMP_HEIGHT = 0.42F;
    public static final double BUNNY_FRICTION = 159.9F;
    public static final double Y_ON_GROUND_MIN = 0.00001;
    public static final double Y_ON_GROUND_MAX = 0.0626;
    public static final double LILYPAD_HEIGHT = 0.015625F;
    public static final double AIR_FRICTION = 0.9800000190734863D;
    public static final double WATER_FRICTION = 0.800000011920929D;
    public static final double LAVA_FRICTION = 0.5D;
    public static final double MOD_SWIM = 0.115F / WALK_SPEED;
    public static final double[] MOD_DEPTH_STRIDER = {
            1.0F,
            0.1645F / MOD_SWIM / WALK_SPEED,
            0.1995F / MOD_SWIM / WALK_SPEED,
            1.0F / MOD_SWIM,
    };

    public static final double UNLOADED_CHUNK_MOTION = -0.09800000190735147;
    public static final double HEAD_HITTER_MOTION = -0.0784000015258789;

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isMoving() {
        return player.getMoveForward() != 0 || player.getMoveStrafing() != 0;
    }

    public static void strafe() {
        strafe(speed());
    }

    public static void strafe(final double speed) {
        final double yaw = direction();
        Vector3d motion = player.getMotion();
        player.setMotion(new Vec3Data(
                -Math.sin((float) yaw) * speed,
                motion.getY(),
                Math.cos((float) yaw) * speed
        ));
    }

    public static void strafe(final double speed, float yaw) {
        yaw = (float) Math.toRadians(yaw);
        Vector3d motion = player.getMotion();
        player.setMotion(new Vec3Data(
                -Math.sin(yaw) * speed,
                motion.getY(),
                Math.cos(yaw) * speed
        ));
    }

    public static double speed() {
        Vector3d motion = player.getMotion();
        return Math.hypot(motion.getX(), motion.getZ());
    }

    public static double direction() {
        return Math.toRadians(directionYaw());
    }

    public static float directionYaw() {
        float rotationYaw = player.getRotation().getYaw();

        if (player.getMoveForward() < 0) {
            rotationYaw += 180;
        }

        float forward = 1;

        if (player.getMoveForward() < 0) {
            forward = -0.5F;
        } else if (player.getMoveForward() > 0) {
            forward = 0.5F;
        }

        if (player.getMoveStrafing() > 0) {
            rotationYaw -= 70 * forward;
        }

        if (player.getMoveStrafing() < 0) {
            rotationYaw += 70 * forward;
        }
        return rotationYaw;
    }

    /**
     * Gets the players' depth strider modifier
     *
     * @return depth strider modifier
     */
    public static int depthStriderLevel() {
        // TODO 重新造轮子或是deobf都非常困难，等cubk更新API
//        return EnchantmentHelper.getDepthStriderModifier(mc.thePlayer);
        return 0;
    }

    /**
     * Checks if the player has enough movement input for sprinting
     *
     * @return movement input enough for sprinting
     */
    public static boolean enoughMovementForSprinting() {
        return Math.abs(player.getMoveForward()) >= 0.8F || Math.abs(player.getMoveStrafing()) >= 0.8F;
    }

    /**
     * Checks if the player is allowed to sprint
     *
     * @param legit should the player follow vanilla sprinting rules?
     * @return player able to sprint
     */
    public static boolean canSprint(final boolean legit) {
        return (legit ? player.getMoveForward() >= 0.8F
                && !player.isCollidedHorizontally()
                && (player.getFoodLevel() > 6 || player.canFlying())
                && player.getPotionEffects().stream().noneMatch(effect -> effect.getId() == 15)  // Potion.blindness
                && !player.isUsingItem()
                && !player.isSneaking()
                : enoughMovementForSprinting());
    }

    /**
     * Basically calculates allowed horizontal distance just like NCP does
     *
     * @return allowed horizontal distance in one tick
     */
    public static double getAllowedHorizontalDistance() {
        double horizontalDistance;
        boolean useBaseModifiers = false;

        //noinspection ConstantValue
        if (false) {  // TODO player.isInWeb()
            horizontalDistance = MOD_WEB * WALK_SPEED;
        } else if (PlayerUtils.inLiquid()) {
            horizontalDistance = MOD_SWIM * WALK_SPEED;

            final int depthStriderLevel = depthStriderLevel();
            if (depthStriderLevel > 0) {
                horizontalDistance *= MOD_DEPTH_STRIDER[depthStriderLevel];
                useBaseModifiers = true;
            }

        } else if (player.isSneaking()) {
            horizontalDistance = MOD_SNEAK * WALK_SPEED;
        } else {
            horizontalDistance = WALK_SPEED;
            useBaseModifiers = true;
        }

        if (useBaseModifiers) {
            if (canSprint(false)) {
                horizontalDistance *= MOD_SPRINTING;
            }

            PotionEffect speedEffect = PlayerUtils.getPotionEffect(1);
            if (speedEffect != null && speedEffect.getDuration() > 0) {
                horizontalDistance *= 1 + (0.2 * (speedEffect.getAmplifier() + 1));
            }

            if (PlayerUtils.isPotionActive(2)) {  // Potion.moveSlowdown
                horizontalDistance = 0.29;
            }
        }

        return horizontalDistance;
    }

    public static void stop() {
        Vector3d motion = player.getMotion();
        player.setMotion(new Vec3Data(0, motion.getY(), 0));
    }
}
