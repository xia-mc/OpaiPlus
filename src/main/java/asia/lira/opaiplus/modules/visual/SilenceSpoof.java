package asia.lira.opaiplus.modules.visual;

import asia.lira.opaiplus.internal.Module;
import asia.lira.opaiplus.modules.visual.silence.BWHUDHeader;
import asia.lira.opaiplus.modules.visual.silence.SWHUDHeader;
import asia.lira.opaiplus.utils.ChatFormatting;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.Display;
import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.enums.EnumUseEntityAction;
import today.opai.api.events.EventChatReceived;
import today.opai.api.events.EventPacketSend;
import today.opai.api.interfaces.game.entity.Entity;
import today.opai.api.interfaces.game.entity.LivingEntity;
import today.opai.api.interfaces.game.network.client.CPacket02UseEntity;
import today.opai.api.interfaces.game.network.client.CPacket03Player;
import today.opai.api.interfaces.modules.values.BooleanValue;
import today.opai.api.interfaces.modules.values.ModeValue;
import today.opai.api.interfaces.modules.values.TextValue;

import java.util.Objects;
import java.util.Set;

public class SilenceSpoof extends Module {
    public static final String SILENCE_TEMPLATE = ChatFormatting.LIGHT_PURPLE + "SilenceFix > "
            + ChatFormatting.GOLD + "暴击提示>"
            + ChatFormatting.RED + ChatFormatting.BOLD;

    private final ModeValue criticals = createModes("Criticals", "Replace", "None", "Replace", "Silence", "Replace + Silence");
    private final ModeValue hudHeader = createModes("HUD Header", "None", "None", "BedWars", "SkyWars");
    private final BooleanValue title = createBoolean("Title", false);
    private final TextValue titleVersion = createText("Title Version", "18.60");
    private final TextValue titleWebsite = createText("Title Website", "xinxin.cam");

    private final Set<String> bypassedMessage = new ObjectOpenHashSet<>();
    private int lastAttack = -1;
    private boolean serverOnGroundState = true;
    private static BWHUDHeader bwHeaderModule = null;
    private static SWHUDHeader swHeaderModule = null;
    private String lastTitle = null;
    private String cachedTitle = null;

    public SilenceSpoof() {
        super("SilenceSpoof", "Spoof some visuals like SilenceFix.", EnumModuleCategory.VISUAL);
    }

    @Override
    public void onDisabled() {
        bypassedMessage.clear();
        if (bwHeaderModule != null) {
            bwHeaderModule.setHidden(true);
            bwHeaderModule.setEnabled(false);
        }
        if (swHeaderModule != null) {
            swHeaderModule.setHidden(true);
            swHeaderModule.setEnabled(false);
        }

        if (lastTitle != null) {
            Display.setTitle(lastTitle);
            lastTitle = null;
            cachedTitle = null;
        }
    }

    @Override
    public void onEnabled() {
        lastAttack = -1;
        serverOnGroundState = true;
    }

    @Override
    public void onChat(@NotNull EventChatReceived event) {
        if (criticals.getValue().equals("None")) {
            return;
        }

        String message = event.getMessage();
        if (bypassedMessage.remove(message)) {
            return;
        }

        if (ChatFormatting.getTextWithoutFormattingCodes(message).startsWith("[Criticals]")) {
            event.setCancelled(true);

            if (criticals.getValue().equals("Replace") || criticals.getValue().equals("Replace + Silence")) {
                onCriticals();
            }
        }
    }

    private void onCriticals() {
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

    @Override
    public void onPacketSend(@NotNull EventPacketSend event) {
        if (event.getPacket() instanceof CPacket02UseEntity) {
            CPacket02UseEntity packet = (CPacket02UseEntity) event.getPacket();
            if (packet.getAction() == EnumUseEntityAction.ATTACK) {
                int id = packet.getEntityId();
                Entity entity = world.getEntityByID(id);
                if (!(entity instanceof LivingEntity)) return;
                LivingEntity livingEntity = (LivingEntity) entity;

                if (lastAttack != id) {
                    lastAttack = id;
                }

                if (!(criticals.getValue().equals("Silence") || criticals.getValue().equals("Replace + Silence"))) return;
                if (!(!serverOnGroundState && player.getFallDistance() > 0)) return;
                if (livingEntity.getHurtTime() > 1) return;

                onCriticals();
            }
        } else if (event.getPacket() instanceof CPacket03Player) {
            CPacket03Player packet = (CPacket03Player) event.getPacket();
            serverOnGroundState = packet.isOnGround();
        }
    }

    @Override
    public void onLoop() {
        if (hudHeader.getValue().equals("BedWars")) {
            if (bwHeaderModule == null) {
                bwHeaderModule = new BWHUDHeader();
                API.registerFeature(bwHeaderModule);
            }
            bwHeaderModule.setHidden(false);
            bwHeaderModule.setEnabled(true);
        } else if (bwHeaderModule != null && bwHeaderModule.isEnabled()) {
            bwHeaderModule.setHidden(true);
            bwHeaderModule.setEnabled(false);
        }

        if (hudHeader.getValue().equals("SkyWars")) {
            if (swHeaderModule == null) {
                swHeaderModule = new SWHUDHeader();
                API.registerFeature(swHeaderModule);
            }
            swHeaderModule.setHidden(false);
            swHeaderModule.setEnabled(true);
        } else if (swHeaderModule != null && swHeaderModule.isEnabled()) {
            swHeaderModule.setHidden(true);
            swHeaderModule.setEnabled(false);
        }

        if (title.getValue()) {
            if (lastTitle == null) {
                lastTitle = Display.getTitle();
            }
            String formatted = String.format(
                    "XinXin SilenceFix-%s Welcome User 免费获取请用浏览器搜索%s",
                    titleVersion.getValue(), titleWebsite.getValue()
            );
            if (!Objects.equals(cachedTitle, formatted)) {
                Display.setTitle(formatted);
                cachedTitle = formatted;
            }
        } else if (lastTitle != null) {
            Display.setTitle(lastTitle);
            lastTitle = null;
            cachedTitle = null;
        }
    }
}
