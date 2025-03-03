package asia.lira.opaiplus.utils;

import asia.lira.opaiplus.OpaiPlus;
import today.opai.api.interfaces.game.entity.LocalPlayer;

public class MoveUtil {
    public static boolean isMoving() {
        LocalPlayer player = OpaiPlus.getAPI().getLocalPlayer();
        return player.getMoveForward() != 0 || player.getMoveStrafing() != 0;
    }
}
