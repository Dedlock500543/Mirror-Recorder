package com.mirror.recorder.gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import java.io.IOException;
/**
 * Подтверждение сброса всех настроек мода до заводских значений.
 * Записи в слотах не затрагиваются: сбрасываются только параметры.
 */
public class GuiResetConfirm extends GuiScreen{

    private static final int YES=1,NO=2;
    private final GuiMirrorSettings parent;private int winX,winY,winW,winH;
    public GuiResetConfirm(GuiMirrorSettings parent){this.parent=parent;}
    private static String s(String ru,String en,String uk,String de,String pl){return Lang.s(ru,en,uk,de,pl);}
    @Override public void initGui(){
        buttonList.clear();winW=Math.min(width-24,344);winH=Math.min(132,height-8);winX=(width-winW)/2;winY=Math.max(0,(height-winH)/2);
        int y=winY+winH-32,half=(winW-30)/2;
        buttonList.add(new StyledButton(NO,winX+12,y,half,20,Lang.t("common.cancel")).accent(UiTheme.GRAY).tip(Lang.s("§fОтмена","§fCancel","§fСкасувати","§fAbbrechen","§fAnuluj"),Lang.s("§7Настройки останутся такими, как сейчас.","§7Settings stay as they are now.","§7Налаштування залишаться такими, як зараз.","§7Die Einstellungen bleiben, wie sie sind.","§7Ustawienia pozostaną takie, jak teraz.")));
        buttonList.add(new StyledButton(YES,winX+18+half,y,half,20,s("Сбросить","Reset","Скинути","Zurücksetzen","Resetuj")).primary().accent(UiTheme.RED).tip(Lang.s("§fДа, сбросить","§fYes, reset","§fТак, скинути","§fJa, zurücksetzen","§fTak, zresetuj"),Lang.s("§7Вернёт все настройки к заводским.","§7Resets every setting to factory defaults.","§7Поверне всі налаштування до заводських.","§7Setzt alle Einstellungen auf die Werkseinstellungen zurück.","§7Przywraca wszystkie ustawienia do fabrycznych."),Lang.s("§8Записи в слотах останутся.","§8Recordings in the slots are kept.","§8Записи в слотах залишаться.","§8Aufnahmen in den Slots bleiben.","§8Nagrania w slotach zostaną.")));
    }
    @Override public void drawScreen(int mx,int my,float pt){
        {int bd=UiTheme.GLASS_BACKDROP;drawGradientRect(0,0,width,height,bd,bd);}
        UiTheme.window(winX,winY,winX+winW,winY+winH,30);
        fontRenderer.drawString("\u00a7l"+s("Заводские настройки","Factory settings","Заводські налаштування","Werkseinstellungen","Ustawienia fabryczne"),winX+14,winY+11,UiTheme.TEXT);
        UiTheme.dot(winX+winW-22,winY+12,UiTheme.RED);
        drawCenteredString(fontRenderer,s("Сбросить все настройки до заводских?","Reset every setting to factory defaults?","Скинути всі налаштування до заводських?","Alle Einstellungen auf Werkseinstellungen zurücksetzen?","Zresetować wszystkie ustawienia do fabrycznych?"),winX+winW/2,winY+45,0xFFFFFFFF);
        drawCenteredString(fontRenderer,s("Это действие нельзя отменить.","This action cannot be undone.","Цю дію не можна скасувати.","Diese Aktion kann nicht rückgängig gemacht werden.","Tej akcji nie można cofnąć."),winX+winW/2,winY+60,UiTheme.TEXT_DIM);
        drawCenteredString(fontRenderer,s("Записи в слотах останутся на месте.","Recordings in the slots are kept.","Записи в слотах залишаться на місці.","Aufnahmen in den Slots bleiben erhalten.","Nagrania w slotach zostają na miejscu."),winX+winW/2,winY+73,UiTheme.TEXT_MUTE);
        super.drawScreen(mx,my,pt);UiTheme.buttonTooltip(fontRenderer,buttonList,mx,my,width,height);
    }
    @Override protected void actionPerformed(GuiButton b)throws IOException{parent.finishFactoryReset(b.id==YES);}
    @Override protected void keyTyped(char c,int key)throws IOException{if(key==1){parent.finishFactoryReset(false);return;}super.keyTyped(c,key);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
