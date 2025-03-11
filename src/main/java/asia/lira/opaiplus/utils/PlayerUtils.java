package asia.lira.opaiplus.utils;

import asia.lira.opaiplus.OpaiPlus;
import org.jetbrains.annotations.Nullable;
import today.opai.api.dataset.PositionData;
import today.opai.api.interfaces.game.entity.LocalPlayer;
import today.opai.api.interfaces.game.item.PotionEffect;

public class PlayerUtils {
    private static final LocalPlayer player = OpaiPlus.getAPI().getLocalPlayer();

    public static boolean isTargetNearby() {
        return isTargetNearby(6);
    }

    public static boolean isTargetNearby(double dist) {
        int selfId = player.getEntityId();
        PositionData position = player.getPosition();
        return OpaiPlus.getAPI().getWorld().getLoadedPlayerEntities().stream()
                .filter(target -> target.getEntityId() != selfId)
                .anyMatch(target -> Vec3Utils.distanceTo(position, target.getPosition()) < dist);
    }

    /**
     * Checks if the player is in a liquid
     *
     * @return in liquid
     */
    public static boolean inLiquid() {
        // TODO 计算这个复杂度很高且太麻烦了，等cubk加API
        return false;
    }

    public static boolean isPotionActive(int id) {
        return player.getPotionEffects().stream().anyMatch(effect -> effect.getId() == id);
    }

    public static @Nullable PotionEffect getPotionEffect(int id) {
        return player.getPotionEffects().stream().filter(effect -> effect.getId() == id).findAny().orElse(null);
    }
}
