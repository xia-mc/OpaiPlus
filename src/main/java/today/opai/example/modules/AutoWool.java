package today.opai.example.modules;

import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.enums.EnumResource;
import today.opai.api.enums.EnumShopItem;
import today.opai.api.features.ExtensionModule;
import today.opai.api.interfaces.EventHandler;
import today.opai.api.interfaces.game.item.ItemStack;

import static today.opai.example.ExampleExtension.openAPI;

public class AutoWool extends ExtensionModule implements EventHandler {
    public AutoWool() {
        super("Auto Wool", "Auto purchase wools when you don't have 64 wools", EnumModuleCategory.MISC);
        setEventHandler(this);
    }

    @Override
    public void onPlayerUpdate() {
        if(openAPI.getLocalPlayer().isBedWarsShopScreen() && countWool() < 65 && openAPI.getLocalPlayer().countResource(EnumResource.IRON) >= 4){
            openAPI.getLocalPlayer().purchase(EnumShopItem.WOOL);
        }
    }

    private int countWool(){
        int count = 0;
        for (ItemStack itemStack : openAPI.getLocalPlayer().getInventory().getMainInventory()) {
            if(itemStack == null) continue;
            if(itemStack.getName().equals("tile.cloth")){
                count += itemStack.getStackSize();
            }
        }
        return count;
    }
}
