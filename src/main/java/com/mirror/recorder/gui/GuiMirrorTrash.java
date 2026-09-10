package com.mirror.recorder.gui;

import com.mirror.recorder.manager.RecorderManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Корзина: сначала выбор записи, затем одно главное действие — вернуть.
 * Удаление и очистка спрятаны как вторичные, чтобы не спорить с восстановлением.
 */
public class GuiMirrorTrash extends GuiScreen{

    private static final int PREV=1,NEXT=2,RESTORE=3,DELETE=4,UNDO=5,CLEAR=6,BACK=7,PER_PAGE=4;
    private final GuiMirrorSettings parent;
    private final RecorderManager manager;
    private List<String> files=new ArrayList<String>();
    private int[] slots=new int[0],frames=new int[0];
    private long[] times=new long[0];
    private String[] titles=new String[0];
    private int page=0,selected=-1,wx,wy,ww,wh,listTop,listBottom,statusY;
    private String notice="";
    private int noticeColor=UiTheme.TEXT_MUTE;
    private boolean confirmClear=false;

    public GuiMirrorTrash(GuiMirrorSettings p,RecorderManager m){parent=p;manager=m;}
    private static String s(String ru,String en,String uk,String de,String pl){return Lang.s(ru,en,uk,de,pl);}
    @Override public void initGui(){reload();}

    private void reload(){
        files=manager.getTrashFiles();int n=files.size();
        slots=new int[n];frames=new int[n];times=new long[n];titles=new String[n];
        for(int i=0;i<n;i++){
            String f=files.get(i);slots[i]=manager.getTrashSlot(f);frames[i]=manager.getTrashFrames(f);
            times[i]=manager.getTrashTime(f);titles[i]=manager.getTrashTitle(f);
        }
        confirmClear=false;if(selected>=n)selected=-1;
        if(n>0&&page*PER_PAGE>=n)page=(n-1)/PER_PAGE;if(n==0)page=0;
        build();
    }

    private String titleOf(int i){
        if(titles[i]!=null&&!titles[i].isEmpty())return titles[i];
        return Lang.s("Запись слота "+slots[i],"Slot "+slots[i]+" recording","Запис слота "+slots[i],"Aufnahme von Slot "+slots[i],"Nagranie slota "+slots[i]);
    }
    private String metaOf(int i){
        StringBuilder b=new StringBuilder();
        b.append(s("Слот ","Slot ","Слот ","Slot ","Slot ")).append(slots[i]).append("  ·  ")
            .append(String.format(Locale.ROOT,"%.1f",frames[i]/20f)).append(s(" с"," s"," с"," s"," s"));
        if(times[i]>0L)b.append("  ·  ").append(new SimpleDateFormat("dd.MM HH:mm",java.util.Locale.ROOT).format(new Date(times[i])));
        return b.toString();
    }
    private String countLabel(){
        int n=files.size();
        if(n==0)return s("Пусто","Empty","Порожньо","Leer","Pusto");
        String r=Lang.resolved();
        if(Lang.RU.equals(r))return n%10==1&&n%100!=11?n+" копия":(n%10>=2&&n%10<=4&&(n%100<12||n%100>14)?n+" копии":n+" копий");
        if(Lang.UK.equals(r))return n%10==1&&n%100!=11?n+" копія":(n%10>=2&&n%10<=4&&(n%100<12||n%100>14)?n+" копії":n+" копій");
        if(Lang.PL.equals(r))return n%10==1&&n%100!=11?n+" kopia":(n%10>=2&&n%10<=4&&(n%100<12||n%100>14)?n+" kopie":n+" kopii");
        if(Lang.DE.equals(r))return n==1?"1 Kopie":n+" Kopien";
        return n==1?"1 copy":n+" copies";
    }

    private void build(){
        buttonList.clear();
        ww=Math.min(width-20,460);wh=Math.min(height-14,300);wx=(width-ww)/2;wy=(height-wh)/2;
        int x=wx+16,w=ww-32,pages=Math.max(1,(files.size()+PER_PAGE-1)/PER_PAGE);
        int nav=22,pageY=wy+34;
        StyledButton prev=new StyledButton(PREV,x+w-nav*2-6,pageY,nav,20,"\u2039").accent(UiTheme.GRAY).compact()
            .tip(s("§fПредыдущая страница","§fPrevious page","§fПопередня сторінка","§fVorherige Seite","§fPoprzednia strona"),s("§7Показать более ранние копии.","§7Show earlier copies.","§7Показати ранніші копії.","§7Frühere Kopien anzeigen.","§7Pokaż wcześniejsze kopie."));
        StyledButton next=new StyledButton(NEXT,x+w-nav,pageY,nav,20,"\u203a").accent(UiTheme.GRAY).compact()
            .tip(s("§fСледующая страница","§fNext page","§fНаступна сторінка","§fNächste Seite","§fNastępna strona"),s("§7Показать следующие копии.","§7Show later copies.","§7Показати наступні копії.","§7Spätere Kopien anzeigen.","§7Pokaż późniejsze kopie."));
        prev.enabled=page>0;next.enabled=page+1<pages;prev.visible=files.size()>PER_PAGE;next.visible=files.size()>PER_PAGE;
        buttonList.add(prev);buttonList.add(next);

        int footerY=wy+wh-32,mainY=files.isEmpty()?footerY:footerY-26;
        listTop=wy+58;listBottom=mainY-18;statusY=mainY-14;
        int slotStep=Math.max(22,Math.min(32,(listBottom-listTop)/Math.max(1,PER_PAGE)));
        int slotH=Math.max(18,slotStep-6),start=page*PER_PAGE;
        for(int i=0;i<PER_PAGE;i++){
            int idx=start+i;if(idx>=files.size())break;
            boolean sel=idx==selected;
            StyledButton card=new StyledButton(100+i,x,listTop+i*slotStep,w,slotH,titleOf(idx))
                .accent(sel?UiTheme.GREEN:UiTheme.BLUE).sub(sel?s("Выбрано  ·  ","Selected  ·  ","Вибрано  ·  ","Gewählt  ·  ","Wybrano  ·  ")+metaOf(idx):metaOf(idx))
                .tip(s("§fВыбрать копию","§fSelect this copy","§fВибрати копію","§fDiese Kopie wählen","§fWybierz kopię"),
                     s("§7После выбора внизу появится кнопка «Вернуть».","§7After this the Restore button appears below.","§7Після вибору внизу з'явиться кнопка «Повернути».","§7Danach erscheint unten der Knopf «Wiederherstellen».","§7Po wybraniu na dole pojawi się przycisk «Przywróć»."));
            if(sel)card.primary();
            buttonList.add(card);
        }

        if(!files.isEmpty()){
            int gap=6,restoreW=selected>=0?(w-gap)*3/4:w;
            String restoreLabel=selected>=0?s("Вернуть в слот ","Restore to slot ","Повернути в слот ","Wiederherstellen in Slot ","Przywróć do slotu ")+slots[selected]:s("Сначала выберите запись","Select a recording first","Спочатку виберіть запис","Wähle zuerst eine Aufnahme","Najpierw wybierz nagranie");
            StyledButton restore=new StyledButton(RESTORE,x,mainY,restoreW,20,restoreLabel).primary().accent(UiTheme.GREEN)
                .tip(s("§fВернуть запись","§fRestore recording","§fПовернути запис","§fAufnahme wiederherstellen","§fPrzywróć nagranie"),s("§7Положит копию обратно в её слот. Слот должен быть пустым.","§7Puts the copy back into its own slot. The slot must be empty.","§7Покладе копію назад у її слот. Слот має бути порожнім.","§7Legt die Kopie zurück in ihren Slot. Der Slot muss leer sein.","§7Odłoży kopię z powrotem do jej slotu. Slot musi być pusty."));
            restore.enabled=selected>=0&&selected<files.size()&&slots[selected]>=1;buttonList.add(restore);
            if(selected>=0){
                StyledButton del=new StyledButton(DELETE,x+restoreW+gap,mainY,w-restoreW-gap,20,s("Удалить","Delete","Видалити","Löschen","Usuń"))
                    .accent(UiTheme.RED).tip(s("§fУдалить навсегда","§fDelete forever","§fВидалити назавжди","§fEndgültig löschen","§fUsuń na zawsze"),s("§cЭта копия исчезнет без возврата.","§cThis copy disappears for good.","§cЦя копія зникне безповоротно.","§cDiese Kopie verschwindet für immer.","§cTa kopia zniknie bezpowrotnie."));
                del.enabled=true;buttonList.add(del);
            }
        }

        int third=(w-gapOf())/3;
        StyledButton undo=new StyledButton(UNDO,x,footerY,third,20,s("Последнее удаление","Last deletion","Останнє видалення","Letzte Löschung","Ostatnie usunięcie")).accent(UiTheme.AMBER).compact()
            .tip(s("§fВернуть последнее","§fUndo last","§fПовернути останнє","§fLetztes rückgängig","§fPrzywróć ostatnie"),s("§7Вернёт запись, которую удалили только что.","§7Restores the recording you just deleted.","§7Поверне запис, який видалили щойно.","§7Stellt die Aufnahme wieder her, die du gerade gelöscht hast.","§7Przywraca nagranie, które właśnie usunąłeś."));
        undo.enabled=!files.isEmpty();buttonList.add(undo);
        StyledButton clear=new StyledButton(CLEAR,x+third+6,footerY,third,20,
            confirmClear?s("Нажмите ещё раз","Click again","Натисніть ще раз","Nochmal klicken","Kliknij ponownie"):s("Очистить всё","Empty all","Очистити все","Alles leeren","Opróżnij wszystko"))
            .accent(confirmClear?UiTheme.RED:UiTheme.GRAY).compact()
            .tip(s("§fОчистить корзину","§fEmpty trash","§fОчистити кошик","§fPapierkorb leeren","§fOpróżnij kosz"),s("§cУдалит все копии навсегда.","§cDeletes every copy for good.","§cВидалить усі копії назавжди.","§cLöscht jede Kopie endgültig.","§cUsunie wszystkie kopie na zawsze."));
        clear.enabled=!files.isEmpty();buttonList.add(clear);
        buttonList.add(new StyledButton(BACK,x+(third+6)*2,footerY,w-third*2-12,20,s("Назад","Back","Назад","Zurück","Wstecz")).accent(UiTheme.GRAY).compact()
            .tip(s("§fНазад","§fBack","§fНазад","§fZurück","§fWstecz"),s("§7Вернуться в настройки.","§7Return to settings.","§7Повернутися в налаштування.","§7Zurück zu den Einstellungen.","§7Wróć do ustawień.")));
    }
    private static int gapOf(){return 12;}

    @Override public void drawScreen(int mx,int my,float pt){
        {int bd=UiTheme.GLASS_BACKDROP;drawGradientRect(0,0,width,height,bd,bd);}
        UiTheme.window(wx,wy,wx+ww,wy+wh,32);
        int cw=fontRenderer.getStringWidth(countLabel())+10;
        UiTheme.drawFitted(fontRenderer,s("§lКорзина","§lTrash","§lКошик","§lPapierkorb","§lKosz"),wx+16,wy+11,Math.max(40,ww-16-cw-20),UiTheme.TEXT);
        UiTheme.chip(fontRenderer,countLabel(),wx+ww-16-cw,wy+11,files.isEmpty()?UiTheme.GRAY:UiTheme.AMBER);
        int pages=Math.max(1,(files.size()+PER_PAGE-1)/PER_PAGE);
        if(files.isEmpty()){
            drawCenteredString(fontRenderer,s("Здесь появятся удалённые записи","Deleted recordings will show up here","Тут з'являться видалені записи","Gelöschte Aufnahmen erscheinen hier","Tu pojawią się usunięte nagrania"),wx+ww/2,wy+wh/2-18,UiTheme.TEXT_DIM);
            drawCenteredString(fontRenderer,s("Их можно вернуть в свой слот","You can put them back into their slot","Їх можна повернути у свій слот","Du kannst sie zurück in ihren Slot holen","Można je przywrócić do swojego slotu"),wx+ww/2,wy+wh/2-6,UiTheme.TEXT_MUTE);
        }else{
            String hint=files.size()>PER_PAGE?s("Страница ","Page ","Сторінка ","Seite ","Strona ")+(page+1)+"/"+pages+s("  ·  нажмите карточку, затем «Вернуть»","  ·  tap a card, then Restore","  ·  натисніть картку, потім «Повернути»","  ·  tippe eine Karte an, dann «Wiederherstellen»","  ·  kliknij kartę, potem «Przywróć»")
                :s("Нажмите карточку, затем «Вернуть»","Tap a card, then Restore","Натисніть картку, потім «Повернути»","Tippe eine Karte an, dann «Wiederherstellen»","Kliknij kartę, potem «Przywróć»");
            UiTheme.drawFitted(fontRenderer,hint,wx+16,wy+38,Math.max(40,ww-(files.size()>PER_PAGE?90:36)),UiTheme.TEXT_MUTE);
        }
        if(!notice.isEmpty())UiTheme.drawFitted(fontRenderer,notice,wx+16,statusY,ww-32,noticeColor);
        super.drawScreen(mx,my,pt);UiTheme.buttonTooltip(fontRenderer,buttonList,mx,my,width,height);
    }

    private void setNotice(String text,int color){notice=text==null?"":text;noticeColor=color;}

    @Override protected void actionPerformed(GuiButton b)throws IOException{
        if(b.id!=CLEAR)confirmClear=false;
        if(b.id>=100&&b.id<100+PER_PAGE){selected=page*PER_PAGE+(b.id-100);setNotice("",UiTheme.TEXT_MUTE);build();return;}
        if(b.id==PREV&&page>0){page--;selected=-1;build();return;}
        if(b.id==NEXT&&(page+1)*PER_PAGE<files.size()){page++;selected=-1;build();return;}
        if(b.id==BACK){parent.finishTrash();return;}
        if(b.id==UNDO){
            int slot=manager.undoLastDelete();
            if(slot>0)setNotice(s("§aВернули последнюю запись в слот ","§aRestored the last recording into slot ","§aПовернули останній запис у слот ","§aLetzte Aufnahme wiederhergestellt in Slot ","§aPrzywrócono ostatnie nagranie do slotu ")+slot,UiTheme.GREEN);
            else if(manager.isBusy())setNotice(s("§cПодождите: мод сейчас работает","§cPlease wait: the mod is busy","§cЗачекайте: мод зараз працює","§cBitte warten: Die Mod arbeitet gerade","§cPoczekaj: mod teraz pracuje"),UiTheme.RED);
            else setNotice(s("§cФайл повреждён или слот занят","§cFile is damaged or the slot is taken","§cФайл пошкоджено або слот зайнятий","§cDatei beschädigt oder Slot belegt","§cPlik uszkodzony lub slot zajęty"),UiTheme.RED);
            selected=-1;reload();return;
        }
        if(b.id==CLEAR&&!confirmClear){confirmClear=true;setNotice(s("§eЕщё раз — и все копии исчезнут","§eClick again to delete every copy","§eЩе раз — і всі копії зникнуть","§eNochmal klicken, um jede Kopie zu löschen","§eJeszcze raz — i wszystkie kopie znikną"),UiTheme.AMBER);build();return;}
        if(b.id==CLEAR){
            confirmClear=false;
            if(manager.clearTrash())setNotice(s("§eКорзина очищена","§eTrash emptied","§eКошик очищено","§ePapierkorb geleert","§eKosz opróżniony"),UiTheme.AMBER);
            else setNotice(s("§cНе все файлы удалились","§cSome files could not be deleted","§cНе всі файли видалилися","§cEinige Dateien konnten nicht gelöscht werden","§cNie wszystkie pliki udało się usunąć"),UiTheme.RED);
            selected=-1;reload();return;
        }
        if(selected<0||selected>=files.size())return;
        String file=files.get(selected);
        if(b.id==RESTORE){
            int origSlot=slots[selected];
            if(origSlot<1){setNotice(s("§cНеизвестный слот исходной записи: файл не содержит метку слота","§cUnknown original slot: the file has no slot tag","§cНевідомий слот початкового запису: файл не містить мітки слота","§cUnbekannter Ursprungsslot: Die Datei enthält keinen Slot-Tag","§cNieznany oryginalny slot: plik nie zawiera tagu slotu"),UiTheme.RED);selected=-1;reload();return;}
            if(manager.isBusy()){setNotice(s("§cПодождите: мод сейчас работает","§cPlease wait: the mod is busy","§cЗачекайте: мод зараз працює","§cBitte warten: Die Mod arbeitet gerade","§cPoczekaj: mod teraz pracuje"),UiTheme.RED);selected=-1;reload();return;}
            if(manager.getSlotFrameCount(origSlot)>0){setNotice(s("§cСлот "+origSlot+" занят: удалите или перенесите запись","§cSlot "+origSlot+" is taken: delete or move the recording first","§cСлот "+origSlot+" зайнятий: видаліть або перенесіть запис","§cSlot "+origSlot+" ist belegt: Lösche oder verschiebe die Aufnahme zuerst","§cSlot "+origSlot+" jest zajęty: usuń lub przenieś nagranie"),UiTheme.RED);selected=-1;reload();return;}
            int slot=manager.restoreTrashToOwnSlot(file);
            if(slot>0)setNotice(s("§aЗапись вернулась в слот ","§aRecording restored into slot ","§aЗапис повернувся у слот ","§aAufnahme wiederhergestellt in Slot ","§aNagranie wróciło do slotu ")+slot,UiTheme.GREEN);
            else setNotice(s("§cФайл повреждён: восстановление невозможно","§cFile is damaged: restoration is not possible","§cФайл пошкоджено: відновлення неможливе","§cDatei beschädigt: Wiederherstellung nicht möglich","§cPlik uszkodzony: przywrócenie niemożliwe"),UiTheme.RED);
            selected=-1;reload();return;
        }
        if(b.id==DELETE){
            if(manager.deleteTrash(file))setNotice(s("§eКопия удалена навсегда","§eCopy deleted for good","§eКопію видалено назавжди","§eKopie endgültig gelöscht","§eKopia usunięta na zawsze"),UiTheme.AMBER);
            else setNotice(s("§cФайл не удалился","§cThe file could not be deleted","§cФайл не видалився","§cDie Datei konnte nicht gelöscht werden","§cPliku nie udało się usunąć"),UiTheme.RED);
            selected=-1;reload();
        }
    }
    @Override protected void keyTyped(char c,int k)throws IOException{if(k==1){parent.finishTrash();return;}super.keyTyped(c,k);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
