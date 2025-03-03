package asia.lira.opaiplus.modules.visual;

import asia.lira.opaiplus.internal.Module;
import asia.lira.opaiplus.utils.ChatFormatting;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;
import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.enums.EnumNotificationType;
import today.opai.api.enums.EnumUseEntityAction;
import today.opai.api.events.EventChatReceived;
import today.opai.api.events.EventPacketSend;
import today.opai.api.interfaces.game.network.client.CPacket02UseEntity;
import today.opai.api.interfaces.modules.values.BooleanValue;

import java.util.Set;

public class SilenceSpoof extends Module {
    public static final String SILENCE_TEMPLATE = ChatFormatting.LIGHT_PURPLE + "SilenceFix > "
            + ChatFormatting.GOLD + "暴击提示>"
            + ChatFormatting.RED + ChatFormatting.BOLD;
    private final BooleanValue criticals = createBoolean("Criticals", true);

    private final Set<String> bypassedMessage = new ObjectOpenHashSet<>();
    private int lastAttack = -1;

    public SilenceSpoof() {
        super("SilenceSpoof", "Spoof some visuals like SilenceFix.", EnumModuleCategory.VISUAL);
    }

    @Override
    public void onDisabled() {
        bypassedMessage.clear();
        lastAttack = -1;
    }

    @Override
    public void onChat(@NotNull EventChatReceived event) {
        if (!criticals.getValue()) {
            return;
        }

        String message = event.getMessage();
        if (bypassedMessage.remove(message)) {
            return;
        }

        if (message.contains("[Criticals]")) {
            event.setCancelled(true);

            String target;
            if (lastAttack == -1) {
                target = "Unknown";
            } else {
                target = ChatFormatting.getTextWithoutFormattingCodes(
                        world.getEntityByID(lastAttack).getDisplayName());
            }
            String silenceMsg = SILENCE_TEMPLATE + target;
            bypassedMessage.add(silenceMsg);
            API.printMessage(silenceMsg);
        }
    }

    @Override
    public void onPacketSend(@NotNull EventPacketSend event) {
        if (event.getPacket() instanceof CPacket02UseEntity) {
            CPacket02UseEntity packet = (CPacket02UseEntity) event.getPacket();
            if (packet.getAction() == EnumUseEntityAction.ATTACK) {
                lastAttack = packet.getEntityId();
            }
        }
    }
}
