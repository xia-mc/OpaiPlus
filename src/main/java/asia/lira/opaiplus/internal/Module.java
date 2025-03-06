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

import java.util.Arrays;

public abstract class Module extends ExtensionModule implements EventHandler {
    protected final OpenAPI API = OpaiPlus.getAPI();
    protected final LocalPlayer player = API.getLocalPlayer();
    protected final World world = API.getWorld();

    public Module(String name, String description, EnumModuleCategory category) {
        super(name, description, category);
        setEventHandler(this);
    }

    @Override
    @Deprecated
    public void addValues(Value<?>... values) {
        throw new UnsupportedOperationException();
    }

    protected boolean nullCheck() {
        try {
            int id = player.getEntityId();
            world.getEntityByID(id);
            return true;
        } catch (NullPointerException e) {
            return false;
        }
    }

    protected ModeValue createModes(String name, String defaultValue, String... modes) {
        assert Arrays.asList(modes).contains(defaultValue);
        ModeValue result = API.getValueManager().createModes(name, defaultValue, modes);
        super.addValues(result);
        return result;
    }

    protected NumberValue createNumber(String name, double defaultValue, double min, double max, double inc) {
        NumberValue result = API.getValueManager().createDouble(name, defaultValue, min, max, inc);
        super.addValues(result);
        return result;
    }

    protected BooleanValue createBoolean(String name, boolean defaultValue) {
        BooleanValue result = API.getValueManager().createBoolean(name, defaultValue);
        super.addValues(result);
        return result;
    }

    protected TextValue createText(String name, String defaultValue) {
        TextValue result = API.getValueManager().createInput(name, defaultValue);
        super.addValues(result);
        return result;
    }

    protected void setDepends(Value<?> value, BooleanValue @NotNull ... deps) {
        if (deps.length == 0) return;
        value.setHiddenPredicate(() -> !Arrays.stream(deps).allMatch(Value::getValue));
    }

    protected void setDepends(@NotNull Value<?> value, ModeValue dep, String depValue) {
        value.setHiddenPredicate(() -> !dep.isCurrentMode(depValue));
    }
}
