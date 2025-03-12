package asia.lira.opaiplus.internal;

import asia.lira.opaiplus.OpaiPlus;
import org.jetbrains.annotations.NotNull;
import today.opai.api.OpenAPI;
import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.features.ExtensionModule;
import today.opai.api.interfaces.EventHandler;
import today.opai.api.interfaces.game.entity.LocalPlayer;
import today.opai.api.interfaces.game.world.World;
import today.opai.api.interfaces.modules.Value;
import today.opai.api.interfaces.modules.values.*;

import java.awt.*;
import java.util.Arrays;

public abstract class Module extends ExtensionModule implements EventHandler {
    protected final OpenAPI API = OpaiPlus.getAPI();
    protected final LocalPlayer player = API.getLocalPlayer();
    protected final World world = API.getWorld();

    public Module(String name, String description, EnumModuleCategory category) {
        super(name, description, category);

        setEventHandler(this);
    }

    public final void unload() {
        setEventHandler(null);
        if (isEnabled()) {
            onDisabled();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        try {
            super.setEnabled(enabled);
        } catch (Throwable e) {
            e.printStackTrace(System.out);
        }
    }

    @Override
    @Deprecated
    public void addValues(Value<?>... values) {
        throw new UnsupportedOperationException();
    }

    private <E, T extends Value<E>> T onCreateValue(T result) {
        super.addValues(result);
        return result;
    }

    protected boolean nullCheck() {
        return !API.isNull();
    }

    protected ModeValue createModes(String name, String defaultValue, String... modes) {
        assert Arrays.asList(modes).contains(defaultValue);
        return onCreateValue(API.getValueManager().createModes(name, defaultValue, modes));
    }

    protected NumberValue createNumber(String name, double defaultValue, double min, double max, double inc) {
        return onCreateValue(API.getValueManager().createDouble(name, defaultValue, min, max, inc));
    }

    protected BooleanValue createBoolean(String name, boolean defaultValue) {
        return onCreateValue(API.getValueManager().createBoolean(name, defaultValue));
    }

    protected TextValue createText(String name, String defaultValue) {
        return onCreateValue(API.getValueManager().createInput(name, defaultValue));
    }

    protected LabelValue createLabel(String string) {
        return onCreateValue(API.getValueManager().createLabel(string));
    }

    protected ColorValue createColor(String name, Color color) {
        return onCreateValue(API.getValueManager().createColor(name, color));
    }

    protected void setDepends(Value<?> value, BooleanValue @NotNull ... deps) {
        if (deps.length == 0) return;
        value.setHiddenPredicate(() -> !Arrays.stream(deps).allMatch(Value::getValue));
    }

    protected void setDepends(@NotNull Value<?> value, ModeValue dep, String depValue) {
        value.setHiddenPredicate(() -> !dep.isCurrentMode(depValue));
    }
}
