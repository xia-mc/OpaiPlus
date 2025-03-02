package today.opai.example.commands;

import org.lwjgl.input.Keyboard;
import today.opai.api.features.ExtensionCommand;
import today.opai.api.interfaces.modules.PresetModule;
import today.opai.example.ExampleExtension;

public class BindsCommand extends ExtensionCommand {
    public BindsCommand() {
        super(new String[]{"binds"}, "Show binds list", ".binds");
    }

    @Override
    public void onExecute(String[] strings) {
        ExampleExtension.openAPI.printMessage("§aBinds:");
        for (PresetModule module : ExampleExtension.openAPI.getModuleManager().getModules()) {
            if(module.getKey() != -1){
                ExampleExtension.openAPI.printMessage("§7" + module.getName() + ": §f" + Keyboard.getKeyName(module.getKey()));
            }
        }
    }
}
