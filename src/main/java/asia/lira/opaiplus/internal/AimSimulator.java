package asia.lira.opaiplus.internal;

import asia.lira.opaiplus.OpaiPlus;
import asia.lira.opaiplus.utils.MathUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.lwjgl.util.vector.Vector2f;

public class AimSimulator {

    private static float random(double multiple) {
        return (float) ((Math.random() - 0.5) * 2 * multiple);
    }

    @Contract(pure = true)
    private static double normal(double max, double min, double current) {
        if (current >= max) return max;
        return Math.max(current, min);
    }

    @Contract(pure = true)
    public static float rotMove(double target, double current, @Range(from = 0, to = 180) double diff) {
        return rotMove(target, current, diff, 180);
    }

    @Contract(pure = true)
    public static float rotMove(double target, double current, @Range(from = 0, to = 180) double diff,
                                @Range(from = 0, to = 180) double acc) {
        double yawDiff = Math.abs(MathUtils.normalize((float) target) - MathUtils.normalize((float) current));
        if (yawDiff <= diff) {
            double overMove = yawDiff * (180 - acc) / 180;
            if (overMove > diff / 10) {
                if (target > current) {
                    target += overMove;
                } else {
                    target -= overMove;
                }
            }
        }

        // do rotMove
        float delta;
        if ((float) target > (float) current) {
            float dist1 = (float) target - (float) current;
            float dist2 = (float) current + 360 - (float) target;
            if (dist1 > dist2) {  // 另一边移动更近
                delta = -(float) current - 360 + (float) target;
            } else {
                delta = dist1;
            }
        } else if ((float) target < (float) current) {
            float dist1 = (float) current - (float) target;
            float dist2 = (float) target + 360 - (float) current;
            if (dist1 > dist2) {  // 另一边移动更近
                delta = (float) current + 360 + (float) target;
            } else {
                delta = -dist1;
            }
        } else {
            return (float) current;
        }

        delta = fixGCD(MathUtils.normalize(delta));

        if (Math.abs(delta) < 0.1 * Math.random() + 0.1) {
            return (float) current;
        } else if (Math.abs(delta) <= (float) diff) {
            return (float) current + delta;
        } else {
            if (delta < 0) {
                return (float) current - (float) diff;
            } else if (delta > 0) {
                return (float) current + (float) diff;
            } else {
                return (float) current;
            }
        }
    }

    public static boolean yawEquals(float yaw1, float yaw2) {
        return Math.abs(fixGCD(MathUtils.normalize(yaw1)) - fixGCD(MathUtils.normalize(yaw2))) <= getGCD();
    }

    public static boolean equals(@NotNull Vector2f rot1, @NotNull Vector2f rot2) {
        return yawEquals(rot1.x, rot2.x) && Math.abs(fixGCD(rot1.y) - fixGCD(rot2.y)) <= getGCD();
    }

    public static float fixGCD(float value) {
        double gcd = getGCD();
        return (float) (Math.round(value / gcd) * gcd);
    }

    private static double getGCD() {
        double f = OpaiPlus.getAPI().getOptions().getMouseSensitivity() * 0.6 + 0.2;
        return f * f * f * 8.0 * 0.15;
    }
}
