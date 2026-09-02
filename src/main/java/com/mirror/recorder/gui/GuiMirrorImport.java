package com.mirror.recorder.gui;

import com.mirror.recorder.manager.RecorderManager;
import net.minecraft.client.gui.*;
import java.io.IOException;
import java.util.*;

public class GuiMirrorImport extends GuiScreen{

    private static final int PREV=1,NEXT=2,IMPORT=3,CANCEL=4,PER_PAGE=5;
    private final GuiMirrorSlotTools parent;
    private final RecorderManager manager;
    private final int target;
    private List<String> files=new ArrayList<String>();
    private int page=0,selected=-1,wx,wy,ww,wh;
    private StyledButton importButton;

    public GuiMirrorImport(GuiMirrorSlotTools p,RecorderManager m,int t){parent=p;manager=m;target=t;}
    @Override public void initGui(){files=manager.getExportFiles();build();}

    private void build(){
        buttonList.clear();ww=Math.min(width-20,470);wh=Math.min(height-14,255);wx=(width-ww)/2;wy=(height-wh)/2;
        int x=wx+18,w=ww-36,pages=Math.max(1,(files.size()+PER_PAGE-1)/PER_PAGE),nav=22,pageY=wy+32;
        StyledButton prev=new StyledButton(PREV,x+w-nav*2-5,pageY,nav,19,"\u2039").accent(UiTheme.GRAY)
            .tip(Lang.s("§fНазад","§fBack","§fНазад","§fZurück","§fWstecz"),Lang.s("§7Предыдущая страница списка.","§7Previous page of the list.","§7Попередня сторінка списку.","§7Vorherige Seite der Liste.","§7Poprzednia strona listy."));
        StyledButton next=new StyledButton(NEXT,x+w-nav,pageY,nav,19,"\u203a").accent(UiTheme.GRAY)
            .tip(Lang.s("§fДальше","§fNext","§fДалі","§fWeiter","§fDalej"),Lang.s("§7Следующая страница списка.","§7Next page of the list.","§7Наступна сторінка списку.","§7Nächste Seite der Liste.","§7Następna strona listy."));
        prev.enabled=page>0;next.enabled=page+1<pages;buttonList.add(prev);buttonList.add(next);

        int footerY=wy+wh-31,pathY=wy+wh-52,listY=wy+55;
        int step=Math.max(17,Math.min(26,(pathY-5-listY)/PER_PAGE)),rowH=Math.max(14,step-5),start=page*PER_PAGE;
        for(int i=0;i<PER_PAGE;i++){
            int idx=start+i;if(idx>=files.size())break;
            StyledButton b=new StyledButton(100+i,x,listY+i*step,w,rowH,files.get(idx)).accent(idx==selected?UiTheme.GREEN:UiTheme.BLUE)
                .tip(Lang.s("§fВыбрать этот файл","§fPick this file","§fВибрати цей файл","§fDiese Datei wählen","§fWybierz ten plik"),Lang.s("§7После выбора нажмите зелёную кнопку «Импорт».","§7Then press the green «Import» button.","§7Після вибору натисніть зелену кнопку «Імпорт».","§7Danach den grünen Knopf «Import» drücken.","§7Po wybraniu naciśnij zielony przycisk «Import»."));
            buttonList.add(b);
        }

        int gap=6,cancelW=Math.min(105,(w-gap)*2/5),importW=w-gap-cancelW;
        importButton=new StyledButton(IMPORT,x,footerY,importW,20,Lang.s("Импорт","Import","Імпорт","Import","Import")).primary().accent(UiTheme.GREEN)
            .tip(Lang.s("§fЗагрузить","§fLoad","§fЗавантажити","§fLaden","§fWczytaj"),Lang.s("§7Положит выбранную запись в пустой слот.","§7Puts the chosen recording into a free slot.","§7Покладе вибраний запис у порожній слот.","§7Legt die gewählte Aufnahme in einen leeren Slot.","§7Umieści wybrane nagranie w pustym slocie."),Lang.s("§8Файл останется в папке exports.","§8The file stays in the exports folder.","§8Файл залишиться в папці exports.","§8Die Datei bleibt im exports-Ordner.","§8Plik zostanie w folderze exports."));
        importButton.enabled=selected>=0&&selected<files.size();buttonList.add(importButton);
        buttonList.add(new StyledButton(CANCEL,x+importW+gap,footerY,cancelW,20,Lang.s("Отмена","Cancel","Скасувати","Abbrechen","Anuluj")).accent(UiTheme.GRAY)
            .tip(Lang.s("§fОтмена","§fCancel","§fСкасувати","§fAbbrechen","§fAnuluj"),Lang.s("§7Закрыть окно. Слот останется как был.","§7Close the window. The slot stays as it was.","§7Закрити вікно. Слот залишиться як був.","§7Fenster schließen. Der Slot bleibt, wie er war.","§7Zamknij okno. Slot zostanie jak był.")));
    }

    @Override public void drawScreen(int mx,int my,float pt){
        {int bd=UiTheme.GLASS_BACKDROP;drawGradientRect(0,0,width,height,bd,bd);}
        UiTheme.window(wx,wy,wx+ww,wy+wh,32);
        UiTheme.drawFitted(fontRenderer,Lang.s("§lИмпорт в слот "+target,"§lImport into slot "+target,"§lІмпорт у слот "+target,"§lImport in Slot "+target,"§lImport do slotu "+target),wx+15,wy+11,ww-30,UiTheme.TEXT);
        int pages=Math.max(1,(files.size()+PER_PAGE-1)/PER_PAGE);
        if(files.isEmpty())drawCenteredString(fontRenderer,Lang.s("В папке exports нет файлов .mrr","No .mrr files in the exports folder","У папці exports немає файлів .mrr","Keine .mrr-Dateien im exports-Ordner","W folderze exports nie ma plików .mrr"),wx+ww/2,wy+93,UiTheme.TEXT_MUTE);
        else UiTheme.drawFitted(fontRenderer,(Lang.s("Выберите файл  ·  Страница ","Pick a file  ·  Page ","Виберіть файл  ·  Сторінка ","Wähle eine Datei  ·  Seite ","Wybierz plik  ·  Strona "))+(page+1)+"/"+pages,wx+18,wy+38,Math.max(50,ww-104),UiTheme.TEXT_DIM);
        UiTheme.drawFitted(fontRenderer,manager.getExportDirectoryPath(),wx+18,wy+wh-52,ww-36,UiTheme.TEXT_MUTE);
        super.drawScreen(mx,my,pt);UiTheme.buttonTooltip(fontRenderer,buttonList,mx,my,width,height);
    }

    @Override protected void actionPerformed(GuiButton b)throws IOException{
        if(b.id>=100&&b.id<100+PER_PAGE){selected=page*PER_PAGE+(b.id-100);build();return;}
        if(b.id==PREV&&page>0){page--;selected=-1;build();return;}
        if(b.id==NEXT&&(page+1)*PER_PAGE<files.size()){page++;selected=-1;build();return;}
        if(b.id==CANCEL){mc.displayGuiScreen(parent);return;}
        if(b.id==IMPORT&&selected>=0&&selected<files.size()){
            String file=files.get(selected);
            if(manager.importSlot(target,file))parent.finish(Lang.s("§aФайл импортирован в слот ","§aFile imported into slot ","§aФайл імпортовано в слот ","§aDatei importiert in Slot ","§aPlik zaimportowany do slotu ")+target);
            else parent.finish(Lang.s("§cИмпорт отменён: файл повреждён или слот занят","§cImport cancelled: the file is damaged or the slot is taken","§cІмпорт скасовано: файл пошкоджено або слот зайнятий","§cImport abgebrochen: Datei beschädigt oder Slot belegt","§cImport anulowany: plik uszkodzony lub slot zajęty"));
        }
    }
    @Override protected void keyTyped(char c,int k)throws IOException{if(k==1){mc.displayGuiScreen(parent);return;}super.keyTyped(c,k);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
