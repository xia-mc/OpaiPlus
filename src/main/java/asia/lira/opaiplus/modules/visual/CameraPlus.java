package asia.lira.opaiplus.modules.visual;

import asia.lira.opaiplus.OpaiPlus;
import asia.lira.opaiplus.internal.Module;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.events.EventRender3D;
import today.opai.api.interfaces.modules.values.BooleanValue;
import today.opai.api.interfaces.modules.values.NumberValue;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class CameraPlus extends Module {
    private final BooleanValue modifyRatio = createBoolean("Modify Ratio", false);
    private final NumberValue fov = createNumber("FOV", 75, 30, 120, 1);
    private final NumberValue ratio = createNumber("Ratio", 1, 0.1, 5, 0.1);

    public CameraPlus() {
        super("Camera+", "A set of additional Camera effect implementations", EnumModuleCategory.VISUAL);
        setDepends(fov, modifyRatio);
        setDepends(ratio, modifyRatio);
    }

    @Override
    public void onRender3D(EventRender3D event) {
        if (!modifyRatio.getValue()) return;
        IntBuffer intBuffer = BufferUtils.createIntBuffer(16);

        GL11.glGetInteger(GL11.GL_VIEWPORT, intBuffer);
        intBuffer.rewind();
        int width = intBuffer.get(2);
        int height = intBuffer.get(3);
        float aspect = width / (float) height * ratio.getValue().floatValue();

        // 读取所有像素
        ByteBuffer pixels = BufferUtils.createByteBuffer(Math.max(width * height * 4, 16));
        GL11.glReadBuffer(GL11.GL_BACK);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);

//        GL11.glGetInteger(GL11.GL_MATRIX_MODE, intBuffer);
//        intBuffer.rewind();
//
//        GL11.glMatrixMode(GL11.GL_PROJECTION);
//
//        GL11.glLoadIdentity();
//        GLU.gluPerspective(fov.getValue().floatValue(), aspect, 0.05F, 1000.0F);
//
        // 重新绘制一切🔥
//        GL11.glMatrixMode(GL11.GL_MODELVIEW);
//        GL11.glLoadIdentity();
//        GL11.glRasterPos2i(0, 0);
//        GL11.glDrawPixels(width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
//
//        // 恢复Matrix mode
//        GL11.glMatrixMode(intBuffer.get());
    }
}
