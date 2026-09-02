package com.mirror.recorder.gui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

/**
 * Текстовое поле без ванильной рамки, которое рисует текст строго по вертикальному
 * центру своей области. Ванильный GuiTextField с выключенной рамкой рисует текст от
 * верхнего края (y), из-за чего в тонких полях текст визуально приподнят.
 * Приём: на время отрисовки временно сдвигаем y вниз и возвращаем обратно —
 * область клика (x/y/width/height) при этом не меняется.
 */
public class CenteredTextField extends GuiTextField{
    private final int fontHeight;
    public CenteredTextField(int id,FontRenderer fr,int x,int y,int w,int h){
        super(id,fr,x,y,w,h);
        fontHeight=fr.FONT_HEIGHT;
        setEnableBackgroundDrawing(false);
    }
    @Override public void drawTextBox(){
        int realY=y;
        // Строго по вертикальному центру поля при любой его высоте: зазоры сверху и снизу равны (округление вверх).
        y=realY+Math.max(0,(height-fontHeight+1)/2);
        super.drawTextBox();
        y=realY;
    }
}
