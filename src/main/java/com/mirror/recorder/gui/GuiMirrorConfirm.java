package com.mirror.recorder.gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import java.io.IOException;
public class GuiMirrorConfirm extends GuiScreen{

    private static final int YES=1,NO=2;private final GuiMirrorMain parent;private final int slot;private final String name;
    private int winX,winY,winW,winH;
    public GuiMirrorConfirm(GuiMirrorMain parent,int slot,String name){this.parent=parent;this.slot=slot;this.name=name==null?"":name;}
    @Override public void initGui(){
        buttonList.clear();winW=Math.min(width-24,326);winH=Math.min(126,height-8);winX=(width-winW)/2;winY=Math.max(0,(height-winH)/2);int y=winY+winH-32,half=(winW-30)/2;
        buttonList.add(new StyledButton(NO,winX+12,y,half,20,Lang.s("Отмена","Cancel","Скасувати","Abbrechen","Anuluj")).accent(UiTheme.GRAY).tip(Lang.s("§fОтмена","§fCancel","§fСкасувати","§fAbbrechen","§fAnuluj"),Lang.s("§7Ничего не удаляем, запись остаётся на месте.","§7Nothing is deleted, the recording stays in place.","§7Нічого не видаляємо, запис залишається на місці.","§7Nichts wird gelöscht, die Aufnahme bleibt, wo sie ist.","§7Nic nie usuwamy, nagranie zostaje na miejscu.")));
        buttonList.add(new StyledButton(YES,winX+18+half,y,half,20,Lang.s("Удалить","Delete","Видалити","Löschen","Usuń")).primary().accent(UiTheme.RED).tip(Lang.s("§fДа, удалить","§fYes, delete","§fТак, видалити","§fJa, löschen","§fTak, usuń"),Lang.s("§7Запись уйдёт в корзину — её можно вернуть.","§7The recording goes to the trash and can be restored.","§7Запис піде у кошик — його можна повернути.","§7Die Aufnahme wandert in den Papierkorb und kann zurückgeholt werden.","§7Nagranie trafi do kosza — można je przywrócić.")));
    }
    @Override public void drawScreen(int mx,int my,float pt){
        {int bd=UiTheme.GLASS_BACKDROP;drawGradientRect(0,0,width,height,bd,bd);}UiTheme.window(winX,winY,winX+winW,winY+winH,30);
        UiTheme.drawFitted(fontRenderer,Lang.s("§lУдаление записи","§lDelete recording","§lВидалення запису","§lAufnahme löschen","§lUsuwanie nagrania"),winX+14,winY+11,winW-40,UiTheme.TEXT);UiTheme.dot(winX+winW-22,winY+12,UiTheme.RED);
        UiTheme.drawCenteredFitted(fontRenderer,Lang.s("Удалить слот "+slot+"?","Delete slot "+slot+"?","Видалити слот "+slot+"?","Slot "+slot+" löschen?","Usunąć slot "+slot+"?"),winX+winW/2f,winY+43,winW-28,0xFFFFFFFF,false);
        UiTheme.drawCenteredFitted(fontRenderer,name,winX+winW/2f,winY+57,winW-36,UiTheme.TEXT_DIM,false);
        UiTheme.drawCenteredFitted(fontRenderer,Lang.s("Запись уйдёт в корзину — её можно вернуть.","The recording goes to the trash and can be restored.","Запис піде у кошик — його можна повернути.","Die Aufnahme wandert in den Papierkorb und kann zurückgeholt werden.","Nagranie trafi do kosza — można je przywrócić."),winX+winW/2f,winY+70,winW-30,UiTheme.TEXT_MUTE,false);super.drawScreen(mx,my,pt);UiTheme.buttonTooltip(fontRenderer,buttonList,mx,my,width,height);
    }
    @Override protected void actionPerformed(GuiButton b)throws IOException{if(b.id==YES)parent.confirmDelete(slot,true);else if(b.id==NO)parent.confirmDelete(slot,false);}
    @Override protected void keyTyped(char c,int key)throws IOException{if(key==1){parent.confirmDelete(slot,false);return;}super.keyTyped(c,key);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
