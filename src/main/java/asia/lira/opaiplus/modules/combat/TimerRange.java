package asia.lira.opaiplus.modules.combat;

import asia.lira.opaiplus.internal.Module;
import asia.lira.opaiplus.utils.MathUtils;
import asia.lira.opaiplus.utils.MoveUtil;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.Nullable;
import today.opai.api.dataset.RotationData;
import today.opai.api.dataset.Vec3Data;
import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.events.EventMove;
import today.opai.api.events.EventPacketSend;
import today.opai.api.interfaces.dataset.Vector3d;
import today.opai.api.interfaces.game.entity.LivingEntity;
import today.opai.api.interfaces.game.entity.Player;
import today.opai.api.interfaces.game.network.NetPacket;
import today.opai.api.interfaces.game.network.client.CPacket03Player;
import today.opai.api.interfaces.modules.PresetModule;
import today.opai.api.interfaces.modules.special.ModuleAntiBot;
import today.opai.api.interfaces.modules.special.ModuleKillAura;
import today.opai.api.interfaces.modules.special.ModuleScaffold;
import today.opai.api.interfaces.modules.special.ModuleTeams;
import today.opai.api.interfaces.modules.values.BooleanValue;
import today.opai.api.interfaces.modules.values.LabelValue;
import today.opai.api.interfaces.modules.values.NumberValue;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TimerRange extends Module {
    private final NumberValue lagTicks = createNumber("Lag Ticks", 2, 0, 10, 1);
    private final NumberValue timerTicks = createNumber("Timer Ticks", 2, 0, 10, 1);
    private final NumberValue minRange = createNumber("Min Range", 3.6, 2, 8, 0.1);
    private final NumberValue maxRange = createNumber("Max Range", 5, 2, 8, 0.1);
    private final NumberValue delay = createNumber("Delay", 500, 100, 5000, 50);
    private final NumberValue fov = createNumber("FOV", 180, 15, 360, 15);
    private final BooleanValue onlyOnGround = createBoolean("Only On Ground", true);
    private final BooleanValue clearMotion = createBoolean("Clear Motion", false);
    private final BooleanValue keepRotation = createBoolean("Keep rotation", false);
    private final BooleanValue onlyKillAuraTarget = createBoolean("Only KillAura Target", false);
    private final BooleanValue notWhileCombat = createBoolean("Not While Combat", true);
    private final BooleanValue notWhileScaffold = createBoolean("Not While Scaffold", true);
    @SuppressWarnings("unused")
    private final LabelValue advanced = createLabel("Advanced");
    private final NumberValue timerSpeed = createNumber("Timer Speed", 20, 2, 20, 0.1);

    private final PresetModule moduleBlink = API.getModuleManager().getModule("Blink");
    private final ModuleScaffold moduleScaffold = (ModuleScaffold) API.getModuleManager().getModule("Scaffold");
    private final ModuleTeams moduleTeams = (ModuleTeams) API.getModuleManager().getModule("Teams");
    private final ModuleAntiBot moduleAntiBot = (ModuleAntiBot) API.getModuleManager().getModule("AntiBot");
    private final ModuleKillAura moduleKillAura = (ModuleKillAura) API.getModuleManager().getModule("KillAura");

    private final Queue<NetPacket> delayedPackets = new ConcurrentLinkedQueue<>();
    private volatile State state = State.NONE;
    private int hasLag = 0;
    private int hasTimer = 0;
    private long lastTimerTime = -1;
    private float yaw, pitch;
    private double motionX, motionY, motionZ;

    public TimerRange() {
        super("TimerRange", "Use timer help you to beat opponent.", EnumModuleCategory.COMBAT);
    }

    @Override
    public String getSuffix() {
        int tick;
        if (hasLag != 0) {
            tick = hasLag;
        } else if (hasTimer != 0) {
            tick = timerTicks.getValue().intValue() - hasTimer;
        } else {
            tick = timerTicks.getValue().intValue();
        }
        return tick * 50 + "ms";
    }

    @Override
    public void onDisabled() {
        done();
    }

    @Override
    public void onPlayerUpdate() {
        assert nullCheck();

        switch (state) {
            case NONE:
                if (shouldStart())
                    state = State.TIMER;
                break;
            case TIMER:
                if (hasTimer < timerTicks.getValue().intValue()) {
                    // 没有player.onUpdate。这个timerSpeed方法不一定安全
                    API.getOptions().setTimerSpeed(timerSpeed.getValue().floatValue());
                    hasTimer++;
                    break;
                } else {
                    API.getOptions().setTimerSpeed(1);
                }

                RotationData rotation = API.getRotationManager().getCurrentRotation();
                Vector3d motion = player.getMotion();

                yaw = rotation.getYaw();
                pitch = rotation.getPitch();
                motionX = motion.getX();
                motionY = motion.getY();
                motionZ = motion.getZ();
                state = State.LAG;
                break;
            case LAG:
                if (hasLag >= lagTicks.getValue()) {
                    done();
                } else {
                    hasLag++;
                    if (keepRotation.getValue()) {
                        API.getRotationManager().applyRotation(new RotationData(yaw, pitch), 180, true);
                    }
                }
                break;
        }
    }

    @Override
    public void onPacketSend(EventPacketSend event) {
        switch (state) {
            case TIMER:
                synchronized (delayedPackets) {
                    delayedPackets.add(event.getPacket());
                    event.setCancelled(true);
                }
                break;
            case LAG:
                if (event.getPacket() instanceof CPacket03Player) {
                    event.setCancelled(true);
                } else {
                    synchronized (delayedPackets) {
                        delayedPackets.add(event.getPacket());
                        event.setCancelled(true);
                    }
                }
                break;
        }
    }

    @Override
    public void onMove(EventMove event) {
        if (state == State.LAG) {
            event.setX(0);
            event.setY(0);
            event.setZ(0);
            player.setMotion(new Vec3Data(motionX, motionY, motionZ));
        }
    }

    private boolean shouldStart() {
        assert nullCheck();
        if (moduleBlink.isEnabled()) return false;
        if (onlyOnGround.getValue() && !player.isOnGround()) return false;
        if (notWhileCombat.getValue() && player.getHurtTime() > 0) return false;
        if (notWhileScaffold.getValue() && moduleScaffold.isEnabled()) return false;
        if (!MoveUtil.isMoving()) return false;
        assert fov.getValue() != 0;
        if (System.currentTimeMillis() - lastTimerTime < delay.getValue()) return false;

        @Nullable IntSet filter = getFilter();

        Player target = world.getLoadedPlayerEntities().parallelStream()
                .filter(Objects::nonNull)
                .filter(p -> p.getEntityId() != player.getEntityId())
                .distinct()
                .filter(p -> filter == null || filter.contains(p.getEntityId()))
                .filter(p -> !API.isFriend(p.getProfileName()))
                .filter(p -> !moduleTeams.isEnabled() || !moduleTeams.isTeammate(p))
                .filter(p -> !moduleAntiBot.isEnabled() || !moduleAntiBot.isBot(p))
                .min(Comparator.comparing(p -> p.getDistanceToPosition(player.getPosition())))
                .orElse(null);

        if (target == null) return false;

        if (fov.getValue() < 360 && !MathUtils.inFov(fov.getValue().floatValue(), target)) return false;

        double distance = target.getDistanceToPosition(player.getPosition());
        return distance >= minRange.getValue() && distance <= maxRange.getValue();
    }

    private @Nullable IntSet getFilter() {
        if (onlyKillAuraTarget.getValue()) {
            List<LivingEntity> targets = moduleKillAura.getTargets();
            IntSet filter = new IntOpenHashSet(targets.size() + 1);
            for (LivingEntity target : targets) {
                if (target == null) continue;
                filter.add(target.getEntityId());
            }
            LivingEntity blockTarget = moduleKillAura.getBlockTarget();
            if (blockTarget != null) {
                filter.add(blockTarget.getEntityId());
            }
            return filter;
        }
        return null;
    }

    private void done() {
        state = State.NONE;
        hasLag = 0;
        hasTimer = 0;
        lastTimerTime = System.currentTimeMillis();
        API.getOptions().setTimerSpeed(1);

        if (!nullCheck()) {
            delayedPackets.clear();
            return;
        }

        synchronized (delayedPackets) {
            for (NetPacket p : delayedPackets) {
                p.sendPacket();
            }
            delayedPackets.clear();
        }

        if (state != State.NONE) {
            if (clearMotion.getValue()) {
                player.setMotion(new Vec3Data(0, 0, 0));
            } else {
                player.setMotion(new Vec3Data(motionX, motionY, motionZ));
            }
        }
    }

    enum State {
        NONE,
        TIMER,
        LAG
    }
}
