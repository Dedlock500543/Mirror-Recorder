package com.mirror.recorder.gui;
import com.mirror.recorder.config.RecorderConfig;
import com.mirror.recorder.manager.RecorderManager;
import net.minecraft.client.gui.*;
import java.io.IOException;

/** Compact right-click menu for a slot. */
public class GuiMirrorSlotContext extends GuiScreen{

    private static final int OPEN=1,PRIMARY=2,LOOP=3,DELETE=4;
    private final GuiMirrorMain parent;private final RecorderManager manager;private final RecorderConfig config;private final int slot,anchorX,anchorY;private int x,y,w=198,h;private boolean filled;
    public GuiMirrorSlotContext(GuiMirrorMain p,RecorderManager m,RecorderConfig c,int s,int mx,int my){parent=p;manager=m;config=c;slot=s;anchorX=mx;anchorY=my;}
    @Override public void initGui(){buttonList.clear();filled=manager.getSlotFrameCount(slot)>0;h=filled?150:90;x=Math.max(8,Math.min(width-w-8,anchorX));y=Math.max(8,Math.min(height-h-8,anchorY));int by=y+34,bw=w-28;
        buttonList.add(new StyledButton(OPEN,x+14,by,bw,22,Lang.s("Открыть настройки слота","Open slot settings","Відкрити налаштування слота","Slot-Einstellungen öffnen","Otwórz ustawienia slotu")).accent(UiTheme.ACCENT).icon("\u2699").compact().tip(Lang.s("§fДанные слота\n§7Имя, описание, пауза перед стартом, копия и сохранение в файл.","§fSlot data\n§7Name, description, start pause, copy and save to a file.","§fДані слота\n§7Ім'я, опис, пауза перед стартом, копія та збереження у файл.","§fSlot-Daten\n§7Name, Beschreibung, Startpause, Kopie und Speichern in eine Datei.","§fDane slotu\n§7Nazwa, opis, pauza przed startem, kopia i zapis do pliku.")));by+=28;
        buttonList.add(new StyledButton(PRIMARY,x+14,by,bw,22,filled?(Lang.s("Воспроизвести","Play","Відтворити","Abspielen","Odtwórz")):(Lang.s("Начать запись","Start recording","Почати запис","Aufnahme starten","Rozpocznij nagrywanie"))).accent(filled?UiTheme.BLUE:UiTheme.GREEN).icon(filled?"\u25b6":"\u25cf").compact().tip(filled?(Lang.s("§fВоспроизвести\n§7Мод повторит ваши нажатия один раз.\n§8Прервать можно кнопкой «Остановить».","§fPlay\n§7The mod repeats your key presses once.\n§8You can stop it with the Stop button.","§fВідтворити\n§7Мод повторить ваші натискання один раз.\n§8Перервати можна кнопкою «Зупинити».","§fAbspielen\n§7Die Mod wiederholt deine Tastendrücke einmal.\n§8Du kannst sie mit «Stopp» abbrechen.","§fOdtwórz\n§7Mod powtórzy twoje naciśnięcia raz.\n§8Można przerwać przyciskiem «Zatrzymaj».")):(Lang.s("§fНачать запись\n§7Мод запомнит, какие клавиши вы нажимаете.\n§8Сначала пойдёт короткий отсчёт.","§fStart recording\n§7The mod remembers which keys you press.\n§8A short countdown runs first.","§fПочати запис\n§7Мод запам'ятає, які клавіші ви натискаєте.\n§8Спочатку піде короткий відлік.","§fAufnahme starten\n§7Die Mod merkt sich, welche Tasten du drückst.\n§8Zuerst läuft ein kurzer Countdown.","§fRozpocznij nagrywanie\n§7Mod zapamięta, które klawisze naciskasz.\n§8Najpierw krótkie odliczanie."))));
        if(filled){by+=28;buttonList.add(new StyledButton(LOOP,x+14,by,bw,22,Lang.s("Зациклить","Loop","Зациклити","Wiederholen","Zapętl")).accent(UiTheme.PURPLE).icon("\u21bb").compact().tip(Lang.s("§fЗациклить\n§7Повторять запись снова и снова.\n§8Пока не нажмёте «Остановить».","§fLoop\n§7Repeat the recording over and over.\n§8Until you press Stop.","§fЗациклити\n§7Повторювати запис знову і знову.\n§8Поки не натиснете «Зупинити».","§fWiederholen\n§7Spielt die Aufnahme immer wieder ab.\n§8Bis du «Stopp» drückst.","§fZapętl\n§7Powtarzaj nagranie w kółko.\n§8Dopóki nie naciśniesz «Zatrzymaj».")));by+=28;buttonList.add(new StyledButton(DELETE,x+14,by,bw,22,Lang.s("Удалить запись","Delete recording","Видалити запис","Aufnahme löschen","Usuń nagranie")).accent(UiTheme.RED).icon("\u00d7").compact().tip(Lang.s("§fУдалить запись\n§7Сначала спросим подтверждение.\n§7Запись уйдёт в корзину — её можно вернуть.","§fDelete recording\n§7You will be asked to confirm first.\n§7It goes to the trash and can be restored.","§fВидалити запис\n§7Спочатку запитаємо підтвердження.\n§7Запис піде у кошик — його можна повернути.","§fAufnahme löschen\n§7Du wirst vorher um Bestätigung gebeten.\n§7Sie wandert in den Papierkorb und kann zurückgeholt werden.","§fUsuń nagranie\n§7Najpierw poprosimy o potwierdzenie.\n§7Nagranie trafi do kosza — można je przywrócić.")));}
    }
    @Override public void drawScreen(int mx,int my,float pt){
        // Попап модальный: нижний экран рисуется без координат курсора, поэтому его hover не светит сквозь прозрачный фон.
        parent.drawScreen(-10000,-10000,pt);
        UiTheme.shadow(x,y,x+w,y+h);UiTheme.card(x,y,x+w,y+h,UiTheme.PANEL,UiTheme.BORDER_HI);
        fontRenderer.drawString("\u00a7l"+Lang.t("slot.title")+" "+slot,x+10,y+11,UiTheme.TEXT);
        super.drawScreen(mx,my,pt);
        if(config.isShowTooltips())for(GuiButton raw:buttonList)if(raw instanceof StyledButton){StyledButton b=(StyledButton)raw;if(b.isHover(mx,my)&&b.tooltip!=null){if(UiTheme.tipReady(b))UiTheme.tooltip(fontRenderer,b.tooltip,mx,my,width,height);break;}}
    }
    @Override protected void mouseClicked(int mx,int my,int b)throws IOException{if(mx<x||mx>=x+w||my<y||my>=y+h){mc.displayGuiScreen(parent);return;}super.mouseClicked(mx,my,b);}
    @Override protected void actionPerformed(GuiButton b)throws IOException{if(b.id==OPEN)mc.displayGuiScreen(new GuiMirrorSlotTools(parent,manager,config,slot));else if(b.id==PRIMARY){if(filled)parent.contextPlay(slot,false);else parent.contextRecord(slot);}else if(b.id==LOOP)parent.contextPlay(slot,true);else if(b.id==DELETE)parent.contextDelete(slot);}
    @Override protected void keyTyped(char c,int key)throws IOException{if(key==1){mc.displayGuiScreen(parent);return;}super.keyTyped(c,key);}@Override public boolean doesGuiPauseGame(){return false;}
}
