package com.mirror.recorder.gui;
import com.mirror.recorder.config.RecorderConfig;import com.mirror.recorder.manager.RecorderManager;import net.minecraft.client.gui.*;import java.io.*;import java.text.SimpleDateFormat;import java.util.*;
/** Карточка слота: шапка с состоянием, сетка действий и блок экспорта. */
public class GuiMirrorSlotTools extends GuiScreen{

    private static final int EDIT=1,DUP=2,EXPORT=3,IMPORT=4,BACK=5,DELETE=6;
    private final GuiMirrorMain parent;private final RecorderManager manager;private final RecorderConfig config;private final int slot;
    private int wx,wy,ww,wh,headH,padX,innerW,infoY,infoH,pathY,pathH;private boolean hasExportsCache=false;
    private StyledButton edit,dup,export,imp,delete;
    public GuiMirrorSlotTools(GuiMirrorMain p,RecorderManager m,RecorderConfig c,int s){parent=p;manager=m;config=c;slot=s;}
    private String s(String ru,String en,String uk,String de,String pl){return Lang.s(ru,en,uk,de,pl);}
    private String[] tt(String ru,String en,String uk,String de,String pl){return Lang.s(ru,en,uk,de,pl).split("\n");}

    @Override public void initGui(){
        buttonList.clear();hasExportsCache=!manager.getExportFiles().isEmpty();
        ww=Math.min(width-16,470);wh=Math.min(height-10,318);wx=(width-ww)/2;wy=(height-wh)/2;
        headH=Math.min(34,Math.max(26,wh/9));padX=12;innerW=ww-padX*2;
        int x=wx+padX,half=(innerW-6)/2;
        int backH=20,backY=wy+wh-12-backH,bh=32,delH=22,avail=wh-headH-10;
        int fixed=20+bh*2+6+12+delH+10+backH+12;
        if(avail-fixed<46){bh=24;delH=20;fixed=20+bh*2+6+12+delH+10+backH+12;}
        if(avail-fixed<46){bh=20;delH=16;fixed=20+bh*2+6+12+delH+10+backH+12;}
        pathH=avail-fixed-50>=46?40:0;
        infoY=wy+headH+10;infoH=Math.max(28,avail-fixed-(pathH>0?pathH+10:0));
        int gridY=infoY+infoH+20;
        edit=add(new StyledButton(EDIT,x,gridY,half,bh,s("Изменить","Rename","Змінити","Umbenennen","Zmień")).accent(UiTheme.ACCENT).icon("\u270e")
            .sub(s("имя и описание","name and description","ім'я та опис","Name und Beschreibung","nazwa i opis")));
        dup=add(new StyledButton(DUP,x+half+6,gridY,half,bh,s("Дублировать","Duplicate","Дублювати","Duplizieren","Duplikuj")).accent(UiTheme.PURPLE).icon("copy")
            .sub(s("копия в пустой слот","copy to a free slot","копія в порожній слот","Kopie in einen freien Slot","kopia do wolnego slotu")));
        int row2=gridY+bh+6;
        export=add(new StyledButton(EXPORT,x,row2,half,bh,s("Экспорт","Export","Експорт","Export","Eksport")).accent(UiTheme.BLUE).icon("\u2191")
            .sub(s("файл .mrr",".mrr file","файл .mrr",".mrr-Datei","plik .mrr")));
        imp=add(new StyledButton(IMPORT,x+half+6,row2,half,bh,s("Импорт","Import","Імпорт","Import","Import")).accent(UiTheme.GREEN).icon("\u2193")
            .sub(s("из папки exports","from exports folder","з папки exports","aus dem exports-Ordner","z folderu exports")));
        int delY=row2+bh+12;
        delete=add(new StyledButton(DELETE,x,delY,innerW,delH,s("Удалить запись","Delete recording","Видалити запис","Aufnahme löschen","Usuń nagranie")).accent(UiTheme.RED).icon("\u00d7"));
        pathY=delY+delH+10;
        add(new StyledButton(BACK,x,backY,innerW,backH,Lang.t("common.back")).accent(UiTheme.GRAY)
            .tip(tt("§fНазад\n§7Вернуться к списку записей.\n§8Здесь ничего не потеряется.","§fBack\n§7Go back to the list of recordings.\n§8Nothing here gets lost.","§fНазад\n§7Повернутися до списку записів.\n§8Тут нічого не загубиться.","§fZurück\n§7Zurück zur Aufnahmeliste.\n§8Hier geht nichts verloren.","§fWstecz\n§7Wróć do listy nagrań.\n§8Nic się tu nie zgubi.")));
        sync();
    }
    private StyledButton add(StyledButton b){buttonList.add(b);return b;}

    private void sync(){
        boolean ready=!manager.isBusy(),filled=manager.getSlotFrameCount(slot)>0,hasExports=hasExportsCache;
        edit.enabled=ready&&filled;dup.enabled=ready&&filled;export.enabled=ready&&filled;
        imp.enabled=ready&&!filled&&hasExports;delete.enabled=ready&&filled&&!manager.isSlotBusy(slot);
        edit.tooltip=filled?tt("§fИзменить\n§7Дать записи имя и короткое описание.\n§8Так проще найти её в списке.","§fRename\n§7Give the recording a name and a short description.\n§8That makes it easier to find in the list.","§fЗмінити\n§7Дати запису ім'я та короткий опис.\n§8Так легше знайти його у списку.","§fUmbenennen\n§7Gib der Aufnahme einen Namen und eine kurze Beschreibung.\n§8So findest du sie leichter in der Liste.","§fZmień\n§7Nadaj nagraniu nazwę i krótki opis.\n§8Tak łatwiej je znaleźć na liście.")
            :tt("§fИзменить\n§cСначала нужно что-то записать в этот слот.","§fRename\n§cRecord something into this slot first.","§fЗмінити\n§cСпочатку треба щось записати в цей слот.","§fUmbenennen\n§cNimm zuerst etwas in diesem Slot auf.","§fZmień\n§cNajpierw nagraj coś w tym slocie.");
        dup.tooltip=filled?tt("§fДублировать\n§7Сделает такую же запись в свободном слоте.\n§7Удобно, если хотите что-то поменять, но оригинал сохранить.\n§8Оригинал останется на месте.","§fDuplicate\n§7Makes the same recording in a free slot.\n§7Handy when you want to change something but keep the original.\n§8The original stays where it is.","§fДублювати\n§7Зробить такий самий запис у вільному слоті.\n§7Зручно, якщо хочете щось змінити, але оригінал зберегти.\n§8Оригінал залишиться на місці.","§fDuplizieren\n§7Erstellt dieselbe Aufnahme in einem freien Slot.\n§7Praktisch, wenn du etwas ändern, aber das Original behalten willst.\n§8Das Original bleibt, wo es ist.","§fDuplikuj\n§7Tworzy takie samo nagranie w wolnym slocie.\n§7Wygodne, gdy chcesz coś zmienić, ale zachować oryginał.\n§8Oryginał zostaje na miejscu.")
            :tt("§fДублировать\n§cСначала нужно что-то записать в этот слот.","§fDuplicate\n§cRecord something into this slot first.","§fДублювати\n§cСпочатку треба щось записати в цей слот.","§fDuplizieren\n§cNimm zuerst etwas in diesem Slot auf.","§fDuplikuj\n§cNajpierw nagraj coś w tym slocie.");
        export.tooltip=filled?tt("§fСохранить в файл\n§7Создаст файл .mrr — его можно скопировать или отдать другу.\n§7Запись в слоте останется.\n§b"+manager.getExportDirectoryPath(),"§fSave to a file\n§7Creates an .mrr file you can copy or share.\n§7The recording stays in the slot too.\n§b"+manager.getExportDirectoryPath(),"§fЗберегти у файл\n§7Створить файл .mrr — його можна скопіювати або віддати другу.\n§7Запис у слоті залишиться.\n§b"+manager.getExportDirectoryPath(),"§fIn Datei speichern\n§7Erstellt eine .mrr-Datei, die du kopieren oder weitergeben kannst.\n§7Die Aufnahme im Slot bleibt ebenfalls.\n§b"+manager.getExportDirectoryPath(),"§fZapisz do pliku\n§7Utworzy plik .mrr — można go skopiować lub udostępnić znajomemu.\n§7Nagranie w slocie też zostanie.\n§b"+manager.getExportDirectoryPath())
            :tt("§fСохранить в файл\n§cВ этом слоте пока нет записи.","§fSave to a file\n§cThis slot has no recording yet.","§fЗберегти у файл\n§cУ цьому слоті поки немає запису.","§fIn Datei speichern\n§cIn diesem Slot ist noch keine Aufnahme.","§fZapisz do pliku\n§cW tym slocie nie ma jeszcze nagrania.");
        imp.tooltip=!ready?tt("§fЗагрузить из файла\n§cПодождите: сейчас идёт запись или повтор.","§fLoad from a file\n§cPlease wait: recording or playback is running.","§fЗавантажити з файлу\n§cЗачекайте: зараз іде запис або повтор.","§fAus Datei laden\n§cBitte warten: Aufnahme oder Wiedergabe läuft.","§fWczytaj z pliku\n§cPoczekaj: trwa nagrywanie lub powtórka.")
            :filled?tt("§fЗагрузить из файла\n§cНужен пустой слот, а этот занят.","§fLoad from a file\n§cAn empty slot is needed, this one is taken.","§fЗавантажити з файлу\n§cПотрібен порожній слот, а цей зайнятий.","§fAus Datei laden\n§cEin leerer Slot wird benötigt, dieser ist belegt.","§fWczytaj z pliku\n§cPotrzebny pusty slot, a ten jest zajęty.")
            :!hasExports?tt("§fЗагрузить из файла\n§cВ папке exports пока нет файлов .mrr.","§fLoad from a file\n§cThere are no .mrr files in the exports folder yet.","§fЗавантажити з файлу\n§cУ папці exports поки немає файлів .mrr.","§fAus Datei laden\n§cIm exports-Ordner gibt es noch keine .mrr-Dateien.","§fWczytaj z pliku\n§cW folderze exports nie ma jeszcze plików .mrr.")
            :tt("§fЗагрузить из файла\n§7Выберите файл .mrr — запись появится в этом слоте.","§fLoad from a file\n§7Pick an .mrr file and the recording lands in this slot.","§fЗавантажити з файлу\n§7Виберіть файл .mrr — запис з'явиться в цьому слоті.","§fAus Datei laden\n§7Wähle eine .mrr-Datei — die Aufnahme landet in diesem Slot.","§fWczytaj z pliku\n§7Wybierz plik .mrr — nagranie pojawi się w tym slocie.");
        delete.tooltip=filled?tt("§fУдалить запись\n§7Запись уйдёт в корзину, оттуда её можно вернуть.\n§7Сначала спросим подтверждение.\n§8Слот, который сейчас играет, удалить не выйдет.","§fDelete recording\n§7The recording goes to the trash and can be restored from there.\n§7You will be asked to confirm first.\n§8A slot that is playing right now cannot be deleted.","§fВидалити запис\n§7Запис піде у кошик, звідти його можна повернути.\n§7Спочатку запитаємо підтвердження.\n§8Слот, який зараз грає, видалити не вийде.","§fAufnahme löschen\n§7Die Aufnahme wandert in den Papierkorb und kann von dort zurückgeholt werden.\n§7Du wirst vorher um Bestätigung gebeten.\n§8Ein Slot, der gerade spielt, kann nicht gelöscht werden.","§fUsuń nagranie\n§7Nagranie trafi do kosza, skąd można je przywrócić.\n§7Najpierw poprosimy o potwierdzenie.\n§8Slotu, który teraz gra, nie da się usunąć.")
            :tt("§fУдалить\n§cЭтот слот и так пустой.","§fDelete\n§cThis slot is already empty.","§fВидалити\n§cЦей слот і так порожній.","§fLöschen\n§cDieser Slot ist bereits leer.","§fUsuń\n§cTen slot jest już pusty.");
    }

    @Override public void drawScreen(int mx,int my,float pt){
        {int bd=UiTheme.GLASS_BACKDROP;drawGradientRect(0,0,width,height,bd,bd);}
        UiTheme.window(wx,wy,wx+ww,wy+wh,headH);
        int frames=manager.getSlotFrameCount(slot);boolean filled=frames>0;
        String name=manager.getSlotName(slot);
        if(name==null||name.isEmpty())name=filled?s("Запись ","Recording ","Запис ","Aufnahme ","Nagranie ")+slot:s("Пустой слот","Empty slot","Порожній слот","Leerer Slot","Pusty slot");
        UiTheme.header(fontRenderer,wx,wy,wx+ww,headH,s("Слот ","Slot ","Слот ","Slot ","Slot ")+slot,name,filled?UiTheme.ACCENT:UiTheme.GRAY);
        int l=wx+padX,r=wx+ww-padX;
        UiTheme.card(l,infoY,r,infoY+infoH,UiTheme.CARD,UiTheme.BORDER);
        if(filled){
            String desc=manager.getSlotDescription(slot);if(desc==null||desc.isEmpty())desc=s("Описание не задано","No description","Опис не задано","Keine Beschreibung","Brak opisu");
            String[] labels=Lang.pick(new String[]{"Тип","Описание","Кадры","Длительность","Размер","Создано"},new String[]{"Type","Description","Frames","Duration","Size","Created"},new String[]{"Тип","Опис","Кадри","Тривалість","Розмір","Створено"},new String[]{"Typ","Beschreibung","Frames","Dauer","Größe","Erstellt"},new String[]{"Typ","Opis","Klatki","Czas","Rozmiar","Utworzono"});
            String[] values=new String[]{config.isPreferredLoopMode()?s("Цикл","Loop","Цикл","Schleife","Pętla"):s("Один раз","Once","Один раз","Einmal","Raz"),desc,frames+s(" кадров"," frames"," кадрів"," Frames"," klatek"),sizeDur(frames),size(manager.getSlotFileSize(slot)),date(manager.getSlotModified(slot))};
            UiTheme.infoTable(fontRenderer,labels,values,l+10,infoY+8,r-10,infoY+infoH-6);
        }else UiTheme.drawFitted(fontRenderer,s("Записи нет — доступен импорт","No recording — import available","Запису немає — доступний імпорт","Keine Aufnahme — Import verfügbar","Brak nagrania — dostępny import"),l+10,infoY+infoH-16,r-l-20,UiTheme.TEXT_DIM);

        UiTheme.sectionLabel(fontRenderer,s("ДЕЙСТВИЯ","ACTIONS","ДІЇ","AKTIONEN","AKCJE"),l,infoY+infoH+6,innerW);
        if(pathH>0){
            UiTheme.card(l,pathY,r,pathY+pathH,UiTheme.CARD,UiTheme.BORDER);
            UiTheme.drawFitted(fontRenderer,s("Экспорт и импорт","Export and import","Експорт і імпорт","Export und Import","Eksport i import"),l+10,pathY+6,innerW-20,UiTheme.TEXT);
            UiTheme.drawFitted(fontRenderer,manager.getExportDirectoryPath(),l+10,pathY+17,innerW-20,UiTheme.BLUE);
            if(pathH>=34)UiTheme.drawFitted(fontRenderer,s(".mrr: маршрут, название и описание. Импорт только в пустой слот.",".mrr: route, name and description. Import into an empty slot only.",".mrr: маршрут, назва та опис. Імпорт лише в порожній слот.",".mrr: Route, Name und Beschreibung. Import nur in einen leeren Slot.",".mrr: trasa, nazwa i opis. Import tylko do pustego slotu."),l+10,pathY+pathH-12,innerW-20,UiTheme.TEXT_MUTE);
        }
        sync();super.drawScreen(mx,my,pt);drawTips(mx,my);
    }
    private void drawTips(int mx,int my){if(!config.isShowTooltips())return;for(GuiButton g:buttonList)if(g instanceof StyledButton){StyledButton b=(StyledButton)g;if(b.visible&&b.tooltip!=null&&mx>=b.x&&my>=b.y&&mx<b.x+b.width&&my<b.y+b.height){if(UiTheme.tipReady(b))UiTheme.tooltip(fontRenderer,b.tooltip,mx,my,width,height);return;}}}
    private String dec(double v){String out=String.format(java.util.Locale.US,"%.1f",v);return Lang.dec(out);}
    private String sizeDur(int frames){return dec(Math.max(0,frames)/20d)+s(" сек"," sec"," сек"," s"," s");}
    private String size(long b){if(b<1024)return b+" B";if(b<1048576)return dec(b/1024d)+" KB";return dec(b/1048576d)+" MB";}
    private String date(long t){return t<=0?"--":new SimpleDateFormat("dd.MM.yyyy, HH:mm",java.util.Locale.ROOT).format(new Date(t));}
    @Override protected void actionPerformed(GuiButton b)throws IOException{
        if(b.id==EDIT)mc.displayGuiScreen(new GuiMirrorEdit(this,manager,slot));
        else if(b.id==DUP)mc.displayGuiScreen(new GuiMirrorDuplicate(this,manager,slot));
        else if(b.id==EXPORT){String f=manager.exportSlot(slot);finish(f==null?s("§cНе удалось экспортировать слот","§cExport failed","§cНе вдалося експортувати слот","§cExport fehlgeschlagen","§cNie udało się wyeksportować slotu"):(Lang.s("§aЭкспорт сохранён: §f","§aExport saved: §f","§aЕкспорт збережено: §f","§aExport gespeichert: §f","§aEksport zapisany: §f"))+manager.getExportDirectoryPath()+File.separator+f);}
        else if(b.id==IMPORT)mc.displayGuiScreen(new GuiMirrorImport(this,manager,slot));
        else if(b.id==DELETE){String n=manager.getSlotName(slot);if(n==null||n.isEmpty())n=s("Запись ","Recording ","Запис ","Aufnahme ","Nagranie ")+slot;mc.displayGuiScreen(new GuiMirrorConfirm(parent,slot,n));}
        else if(b.id==BACK)mc.displayGuiScreen(parent);
    }
    public void finish(String message){mc.displayGuiScreen(this);if(message!=null)parent.notifyResult(message);}
    @Override protected void keyTyped(char c,int k)throws IOException{if(k==1){mc.displayGuiScreen(parent);return;}super.keyTyped(c,k);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
