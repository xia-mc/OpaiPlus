package asia.lira.opaiplus.utils;

import asia.lira.opaiplus.OpaiPlus;
import today.opai.api.dataset.PositionData;
import today.opai.api.interfaces.game.entity.LocalPlayer;

public class PlayerUtils {

    public static boolean isTargetNearby() {
        return isTargetNearby(6);
    }

    public static boolean isTargetNearby(double dist) {
        LocalPlayer player = OpaiPlus.getAPI().getLocalPlayer();
        PositionData position = player.getPosition();
        return OpaiPlus.getAPI().getWorld().getLoadedPlayerEntities().stream()
                .filter(target -> target != player)
                .anyMatch(target -> Vec3Utils.distanceTo(position, target.getPosition()) < dist);
    }
}
