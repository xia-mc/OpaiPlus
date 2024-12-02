package today.opai.example;

import today.opai.api.Extension;
import today.opai.api.OpenAPI;
import today.opai.api.annotations.ExtensionInfo;
import today.opai.example.commands.BindsCommand;
import today.opai.example.modules.AutoWool;
import today.opai.example.modules.CustomScoreboard;
import today.opai.example.widgets.MyScoreboard;

// Required @ExtensionInfo annotation
@ExtensionInfo(name = "Example Extension",author = "cubk",version = "1.0")
public class ExampleExtension extends Extension {
    public static OpenAPI openAPI;

    @Override
    public void initialize(OpenAPI openAPI) {
        ExampleExtension.openAPI = openAPI;
        // Command
        openAPI.registerFeature(new BindsCommand());

        // Modules
        openAPI.registerFeature(new CustomScoreboard());
        openAPI.registerFeature(new AutoWool());

        // Widgets
        openAPI.registerFeature(new MyScoreboard());
    }
}
