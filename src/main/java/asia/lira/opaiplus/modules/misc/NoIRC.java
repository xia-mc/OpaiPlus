package asia.lira.opaiplus.modules.misc;

import asia.lira.opaiplus.OpaiPlus;
import asia.lira.opaiplus.internal.Module;
import asia.lira.opaiplus.utils.ChatFormatting;
import org.jetbrains.annotations.NotNull;
import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.events.EventChatReceived;
import today.opai.api.interfaces.modules.values.BooleanValue;

public class NoIRC extends Module {
    public static final String IRC_GLOBAL_PREFIX = "IRC Global >> ";
    public static final String IRC_PARTY_PREFIX = "IRC Party >> ";

    private final BooleanValue globalChat = createBoolean("Global Chat", true);
    private final BooleanValue partyChat = createBoolean("Party Chat", false);

    public NoIRC() {
        super("NoIRC", "Remove IRC features", EnumModuleCategory.MISC);
    }

    @Override
    public void onChat(@NotNull EventChatReceived event) {
        if (!globalChat.getValue() && !partyChat.getValue()) return;

        String message = ChatFormatting.getTextWithoutFormattingCodes(event.getMessage());
        if (globalChat.getValue() && message.startsWith(IRC_GLOBAL_PREFIX)) {
            cancel(event);
        } else if (partyChat.getValue() && message.startsWith(IRC_PARTY_PREFIX)) {
            cancel(event);
        }
    }

    private static void cancel(@NotNull EventChatReceived event) {
        // 防止PartyCT炸了
        PartyCT partyCT = OpaiPlus.getModule(PartyCT.class);
        if (!partyCT.getLastMsg().equals(event.getMessage())) {
            partyCT.onChat(event);
        }
        event.setCancelled(true);
    }
}
