package today.opai.example.widgets;

import org.lwjgl.opengl.GL11;
import today.opai.api.features.ExtensionWidget;
import today.opai.example.modules.CustomScoreboard;

import java.awt.*;

import static today.opai.example.ExampleExtension.openAPI;

public class MyScoreboard extends ExtensionWidget {
    public MyScoreboard() {
        super("Scoreboard");
    }

    @Override
    public void render() {
        int height = 0;

        int renderWidth;

        if(openAPI.getWorld().getScoreboardLines().isEmpty() || openAPI.getWorld().getScoreboardTitle() == null)
            return;

        openAPI.getRenderUtil().drawRoundRect(getX(), getY(), getWidth(), getHeight(), 5, new Color(0, 0, 0, 150));

        height += 10;

        openAPI.getFontUtil().getTahoma18().drawCenteredString(openAPI.getWorld().getScoreboardTitle(), getX() + (this.getWidth() / 2) - 1, getY() + 4, -1);

        renderWidth = openAPI.getFontUtil().getTahoma18().getWidth(openAPI.getWorld().getScoreboardTitle());

        for (String s1 : openAPI.getWorld().getScoreboardLines()) {
            renderWidth = Math.max(openAPI.getFontUtil().getTahoma18().getWidth(s1), renderWidth);
            openAPI.getFontUtil().getTahoma18().drawString(s1, getX() + 3, getY() + (height += 10), 0xffffffff);
        }

        setWidth(renderWidth + 8);
        setHeight(height + 12);
    }

    @Override
    public boolean renderPredicate() {
        return CustomScoreboard.INSTANCE.isEnabled();
    }
}
