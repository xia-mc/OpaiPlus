package asia.lira.opaiplus.modules.misc;

import asia.lira.opaiplus.OpaiPlus;
import asia.lira.opaiplus.internal.SecurityManager;
import asia.lira.opaiplus.internal.Module;
import asia.lira.opaiplus.modules.misc.partyct.PasswordHider;
import asia.lira.opaiplus.utils.ChatFormatting;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.enums.EnumNotificationType;
import today.opai.api.events.EventChatReceived;
import today.opai.api.interfaces.modules.values.BooleanValue;
import today.opai.api.interfaces.modules.values.LabelValue;
import today.opai.api.interfaces.modules.values.ModeValue;
import today.opai.api.interfaces.modules.values.TextValue;

import java.util.Map;

import static asia.lira.opaiplus.modules.misc.partyct.OpCode.*;

public class PartyCT extends Module {
    public static final String DEFAULT_PASSWORD = "Global";
    public static final String MESSAGE_PREFIX = "[PCT]";
    public static final String IRC_GLOBAL_PREFIX = "IRC Global >> ";
    public static final String IRC_PARTY_PREFIX = "IRC Party >> ";
    public static final long HEARTBEAT_DELAY = Long.MAX_VALUE - 1;
    public static final long HEARTBEAT_TIMEOUT = Long.MAX_VALUE - 1;

    @SuppressWarnings("unused")
    private final LabelValue modeLabel = createLabel("Global mode has been removed to suppress iq issue.");
    private final ModeValue mode = createModes("Mode", "Party", "Party");
    private final TextValue password = createText("Password", DEFAULT_PASSWORD);
    private final BooleanValue hidePCTMessage = createBoolean("Hide PCT Message", true);
    private final BooleanValue notification = createBoolean("Notification", true);
    private final BooleanValue warnInvalidMessage = createBoolean("Warn Invalid Message", false);

    private final byte[] GIL = new byte[0];
    private final Map<String, String> friends = new Object2ObjectOpenHashMap<>();  // 游戏名字：IRC名字
    private final Object2LongMap<String> lastHeartBeat = new Object2LongOpenHashMap<>();  // 游戏名字：上次心跳包时间
    private long selfLastHeartBeat = -1;
    private boolean initializing = true;
    private String lastPlayerName = "";
    @Getter
    private String lastMsg = "";  // TODO Opai没有event handler priory，所以目前用这种方式防止PartyCT因为NoIRC失效。未来可以重做event bus

    public PartyCT() {
        super("Party CT", "Auto cross-team if possible.", EnumModuleCategory.MISC);
        mode.setValueCallback(value -> {
            initializing = true;
            ensureInitialize();
        });
        password.setValueCallback(value -> {
            initializing = true;
            ensureInitialize();
        });
    }

    @Override
    public String getSuffix() {
        return String.valueOf(friends.size());
    }

    @SneakyThrows
    @Override
    public void onEnabled() {
        if (!SecurityManager.isSecuritySupported()) {
            OpaiPlus.error("SHA256 is unsupported on your device. PartyCT is not available.");
            setEnabled(false);
            return;
        }

        initializing = true;
        ensureInitialize();
    }

    @Override
    public void onDisabled() {
        synchronized (GIL) {
            OpaiPlus.getExecutor().execute(() -> send(LEAVE));
            friends.keySet().forEach(API::removeFriend);
            friends.clear();
            lastHeartBeat.clear();
        }
    }

    @Override
    public void onChat(@NotNull EventChatReceived event) {
        lastMsg = event.getMessage();
        if (!ensureInitialize()) return;

        String message = ChatFormatting.getTextWithoutFormattingCodes(event.getMessage());
        String prefix = mode.isCurrentMode("Global") ? IRC_GLOBAL_PREFIX : IRC_PARTY_PREFIX;
        if (!message.startsWith(prefix)) {
            return;
        }

        String[] splits = message.substring(prefix.length()).split(": ", 2);
        if (!splits[1].startsWith(MESSAGE_PREFIX)) {
            return;
        }

        if (nullCheck()) {
            OpaiPlus.getExecutor().execute(() -> handle(splits[1], splits[0]));
        }

        if (hidePCTMessage.getValue()) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onTick() {
        if (!ensureInitialize()) return;

        OpaiPlus.getExecutor().execute(() -> {
            synchronized (GIL) {
                long time = System.currentTimeMillis();

                String profileName = player.getProfileName();
                if (!profileName.equals(lastPlayerName)) {
                    send(LEAVE);
                    lastPlayerName = profileName;
                    selfLastHeartBeat = time;
                    send(HEARTBEAT | REQUEST);
                } else if (time - selfLastHeartBeat >= HEARTBEAT_DELAY) {
                    selfLastHeartBeat = time;
                    send(HEARTBEAT);
                }

                // watchdog
                lastHeartBeat.object2LongEntrySet().removeIf(entry -> {
                    if (time - entry.getLongValue() > HEARTBEAT_TIMEOUT) {
                        friends.remove(entry.getKey());
                        return true;
                    }
                    return false;
                });
            }
        });
    }

    private boolean ensureInitialize() {
        if (!initializing) return true;

        if (!nullCheck()) return false;
        synchronized (GIL) {
            selfLastHeartBeat = System.currentTimeMillis();
            lastPlayerName = player.getProfileName();
            OpaiPlus.getExecutor().execute(() -> send(HEARTBEAT | REQUEST));
            initializing = false;
        }
        return true;
    }

    private void send(int opCode) {
        String data = String.format("%d,%s", opCode, lastPlayerName);
        String message = String.format("%s%s", MESSAGE_PREFIX,
                SecurityManager.encrypt(data, PasswordHider.fixPassword(password.getValue())));

        switch (mode.getValue()) {
            case "Global":
                API.getIRC().sendMessage(" " + message);
                break;
            case "Party":
                API.getIRC().sendPartyMessage(" " + message);
                break;
        }
    }

    private void handle(@NotNull String message, String senderIRC) {
        assert message.startsWith(MESSAGE_PREFIX);

        message = message.substring(MESSAGE_PREFIX.length());
        try {
            String[] splits = SecurityManager.decrypt(message,
                    PasswordHider.fixPassword(password.getValue())).split(",");
            handle(Integer.parseUnsignedInt(splits[0]), senderIRC, splits[1]);
        } catch (Exception e) {
            OpaiPlus.log(e.getMessage());
        }
    }

    private void handle(int opCode, @NotNull String senderIRC, String senderIGN) {
        if (lastPlayerName.equals(senderIGN)) {
            return;
        }
        if (!API.getIRC().getUsername(senderIGN).filter(senderIRC::equals).isPresent()) {
            invalid(senderIRC);
            return;
        }

        long time = System.currentTimeMillis();
        synchronized (GIL) {
            if ((opCode & HEARTBEAT) != 0) {
                lastHeartBeat.put(senderIGN, time);
                if (friends.put(senderIGN, senderIRC) == null) {
                    API.addFriend(senderIGN);
                    info(String.format("%s joined PartyCT", senderIRC));
                }
            }
            if ((opCode & LEAVE) != 0) {
                String removed = friends.remove(senderIGN);
                lastHeartBeat.removeLong(senderIGN);
                if (removed == null || !removed.equals(senderIRC)) {
                    invalid(senderIRC);
                    return;
                }
                API.removeFriend(senderIGN);
                info(String.format("%s leaved PartyCT", senderIRC));
            }
            if ((opCode & REQUEST) != 0) {
                send(HEARTBEAT);
            }
        }
    }

    private void invalid(String senderIRC) {
        if (!warnInvalidMessage.getValue()) return;
        API.popNotification(
                EnumNotificationType.WARNING, "PartyCT",
                String.format("Received Invalid Message From '%s'", senderIRC), 8000
        );
    }

    private void info(String message) {
        if (!notification.getValue()) return;
        API.popNotification(
                EnumNotificationType.NEW, "PartyCT",
                message, 8000
        );
    }
}
