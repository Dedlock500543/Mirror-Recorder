package com.mirror.recorder.gui;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
/**
 * Локализация интерфейса. Пять языков: ru / en / uk / de / pl + режим AUTO (по умолчанию).
 * AUTO определяет фактический язык клиента Minecraft: сперва спрашивает LanguageManager
 * (им игра реально рисует текст), запасной вариант — gameSettings.language; неподдержанные
 * языки получают английский. Проверка идёт при каждом обращении — смена языка игры
 * подхватывается сразу, без перезапуска.
 * Ключ отсутствует -> английский -> русский -> сам ключ: интерфейс не падает.
 */
public final class Lang{
    private Lang(){}
    public static final String RU="ru",EN="en",UK="uk",DE="de",PL="pl",AUTO="auto";
    private static final String[] SUPPORTED={RU,EN,UK,DE,PL};
    private static String current=AUTO;
    private static final Map<String,String> R=new HashMap<String,String>(),E=new HashMap<String,String>(),U=new HashMap<String,String>(),D=new HashMap<String,String>(),P=new HashMap<String,String>();
    public static String get(){return current;}
    public static String[] supported(){return SUPPORTED.clone();}
    /** Фактический язык интерфейса: явный выбор, а при AUTO — язык игры. */
    public static String resolved(){return AUTO.equals(current)?detectGameLanguage():current;}
    public static boolean isRu(){return RU.equals(resolved());}
    /** Язык клиента Minecraft: сперва LanguageManager (то, чем игра рисует текст), запасной — gameSettings.language. */
    private static String detectGameLanguage(){
        String code=null;
        try{
            net.minecraft.client.Minecraft mc=net.minecraft.client.Minecraft.getMinecraft();
            if(mc!=null){
                if(mc.getLanguageManager()!=null&&mc.getLanguageManager().getCurrentLanguage()!=null)code=mc.getLanguageManager().getCurrentLanguage().getLanguageCode();
                if((code==null||code.isEmpty())&&mc.gameSettings!=null)code=mc.gameSettings.language;
            }
        }catch(Throwable ignored){}
        return mapCode(code);
    }
    private static String mapCode(String code){
        if(code==null)return EN;
        String c=code.toLowerCase(Locale.ROOT);
        if(c.startsWith("ru"))return RU;
        if(c.startsWith("uk"))return UK;
        if(c.startsWith("de"))return DE;
        if(c.startsWith("pl"))return PL;
        return EN;
    }
    public static void set(String code){
        if(code!=null){for(String s:SUPPORTED)if(s.equalsIgnoreCase(code)){current=s;return;}}
        current=AUTO;
    }
    /** Цикл кнопки «Язык»: AUTO -> ru -> en -> uk -> de -> pl -> AUTO. */
    public static String next(){
        if(AUTO.equals(current))return RU;
        for(int i=0;i<SUPPORTED.length;i++)if(SUPPORTED[i].equals(current))return i+1<SUPPORTED.length?SUPPORTED[i+1]:AUTO;
        return AUTO;
    }
    private static String nativeName(String code){
        if(RU.equals(code))return"Русский";if(EN.equals(code))return"English";if(UK.equals(code))return"Українська";
        if(DE.equals(code))return"Deutsch";if(PL.equals(code))return"Polski";return code;
    }
    private static String autoWord(String code){return RU.equals(code)||UK.equals(code)?"Авто":"Auto";}
    public static String name(){
        if(AUTO.equals(current)){String r=resolved();return autoWord(r)+" ("+nativeName(r)+")";}
        return nativeName(current);
    }
    public static String t(String key){
        String r=resolved();
        Map<String,String> m=RU.equals(r)?R:UK.equals(r)?U:DE.equals(r)?D:PL.equals(r)?P:E;
        String v=m.get(key);
        if(v==null)v=E.get(key);
        if(v==null)v=R.get(key);
        return v==null?key:v;
    }
    /** Подсказка: строки хранятся через \n в одном значении. */
    public static String[] tip(String key){String v=t(key);return v.split("\n");}
    /** Строка на всех пяти языках. Порядок аргументов: ru, en, uk, de, pl. */
    public static String s(String ru,String en,String uk,String de,String pl){String r=resolved();return RU.equals(r)?ru:UK.equals(r)?uk:DE.equals(r)?de:PL.equals(r)?pl:en;}
    /** То же для нестроковых значений (например, массивов). */
    public static <T> T pick(T ru,T en,T uk,T de,T pl){String r=resolved();return RU.equals(r)?ru:UK.equals(r)?uk:DE.equals(r)?de:PL.equals(r)?pl:en;}
    /** Десятичный разделитель: точка только в английском, в ru/uk/de/pl — запятая. */
    public static String dec(String s){return EN.equals(resolved())?s:s.replace('.',',');}
    private static void p(String key,String ru,String en){R.put(key,ru);E.put(key,en);}
    private static void p3(String key,String uk,String de,String pl){U.put(key,uk);D.put(key,de);P.put(key,pl);}
    static{
        // --- Общее ---
        p("common.save","Сохранить","Save");
        p("common.cancel","Отмена","Cancel");
        p("common.back","Назад","Back");
        p("common.close","Закрыть","Close");
        // --- Главный экран ---
        p("main.record","Записать","Record");
        p("main.play","Воспроизвести","Play");
        p("main.loop","Зациклить","Loop");
        p("main.stop","Остановить","Stop");
        p("main.settings","Настройки","Settings");
        p("tip.record","§fЗаписать\n§7Мод запомнит, какие клавиши и кнопки мыши вы нажимаете.\n§7Сначала пойдёт отсчёт — успеете убрать руки с управления.\n§8Нужен пустой слот. Закончить — кнопкой «Остановить».",
                      "§fRecord\n§7The mod remembers which keys and mouse buttons you press.\n§7A countdown runs first, so you can let go of the controls.\n§8Needs an empty slot. Press Stop when you are done.");
        p("tip.play","§fВоспроизвести\n§7Мод сам нажмёт те же клавиши, в том же порядке и с той же скоростью.\n§7Проиграет один раз и остановится.\n§8Мир вокруг мог измениться, поэтому итог не всегда тот же.",
                    "§fPlay\n§7The mod presses the same keys, in the same order and at the same speed.\n§7It plays once and stops.\n§8The world may have changed, so the result can differ.");
        p("tip.loop","§fЗациклить\n§7То же самое, что «Воспроизвести», но по кругу — снова и снова.\n§7Сколько кругов сделать, задаётся в настройках (0 — без конца).\n§8Между кругами мод может сам возвращаться к точке старта.",
                    "§fLoop\n§7Same as Play, but over and over again.\n§7The number of rounds is set in settings (0 means endless).\n§8Between rounds the mod can walk back to the start point.");
        p("tip.stop","§fОстановить\n§7Прекращает то, что идёт прямо сейчас: отсчёт, запись или повтор.\n§8Все зажатые клавиши сразу отпускаются.",
                    "§fStop\n§7Ends whatever is running now: countdown, recording or playback.\n§8All held keys are released right away.");
        p("tip.settings","§fНастройки\n§7Что повторять, сколько ждать перед стартом и как всё выглядит.\n§8Всё разложено по вкладкам слева.",
                        "§fSettings\n§7What to replay, how long to wait before the start, and how it looks.\n§8Everything is grouped into tabs on the left.");
        p("tip.close","§fЗакрыть\n§7Просто убирает это окно с экрана.\n§8Если идёт запись или повтор — они продолжатся.",
                     "§fClose\n§7Simply hides this window.\n§8Recording or playback keeps running in the background.");
        p("tip.lang","§fЯзык\n§7Меняет язык всех надписей.\n§7По умолчанию стоит «Авто» — язык повторяет язык самой игры.\n§8Названия клавиш в «Управлении» переключаются сразу, без перезапуска.",
                    "§fLanguage\n§7Switches the language of all text.\n§7The default is Auto: the mod follows the game's own language.\n§8Key names in Controls switch right away, no restart needed.");
        // --- Категории настроек ---
        // --- Справка: разделы ---
        p("help.title","Справка по функциям","Feature guide");
        p("help.of","из","of");
        p("help.sections","РАЗДЕЛЫ","SECTIONS");
        p("slot.title","Слот","Slot");
        // --- Кнопки, которые раньше оставались без пояснения ---
        p("tip.back","§fНазад\n§7Возвращает на прошлый экран.\n§8Ничего не отменяет и не сохраняет.",
                     "§fBack\n§7Returns to the previous screen.\n§8Nothing is saved or cancelled.");
        p("tip.helpPrev","§fПредыдущий раздел\n§7Листает справку на один раздел назад.\n§8То же самое делает стрелка влево на клавиатуре.",
                         "§fPrevious section\n§7Flips the guide one section back.\n§8The left arrow key does the same.");
        p("tip.helpNext","§fСледующий раздел\n§7Листает справку на один раздел вперёд.\n§8То же самое делает стрелка вправо на клавиатуре.",
                         "§fNext section\n§7Flips the guide one section forward.\n§8The right arrow key does the same.");
        p("tip.helpList","§fРазделы справки\n§7Нажмите на название, чтобы открыть раздел.\n§8Длинный текст листается колесом мыши.",
                         "§fGuide sections\n§7Click a title to open that section.\n§8Long text scrolls with the mouse wheel.");

        // ================= Українська / Deutsch / Polski =================
        p3("common.save","Зберегти","Speichern","Zapisz");
        p3("common.cancel","Скасувати","Abbrechen","Anuluj");
        p3("common.back","Назад","Zurück","Wstecz");
        p3("common.close","Закрити","Schließen","Zamknij");
        p3("main.record","Записати","Aufnehmen","Nagraj");
        p3("main.play","Відтворити","Abspielen","Odtwórz");
        p3("main.loop","Зациклити","Schleife","Zapętl");
        p3("main.stop","Зупинити","Stopp","Zatrzymaj");
        p3("main.settings","Налаштування","Einstellungen","Ustawienia");
        p3("tip.record","§fЗаписати\n§7Мод запам'ятає, які клавіші та кнопки миші ви натискаєте.\n§7Спершу йде відлік — встигнете прибрати руки з керування.\n§8Потрібен порожній слот. Завершити — кнопкою «Зупинити».",
                        "§fAufnehmen\n§7Der Mod merkt sich, welche Tasten und Maustasten du drückst.\n§7Zuerst läuft ein Countdown — du hast Zeit, die Finger von den Tasten zu nehmen.\n§8Braucht einen leeren Slot. Beenden mit „Stopp“.",
                        "§fNagraj\n§7Mod zapamiętuje, które klawisze i przyciski myszy naciskasz.\n§7Najpierw odliczanie — zdążysz zabrać ręce od sterowania.\n§8Potrzebny pusty slot. Zakończ przyciskiem „Zatrzymaj”.");
        p3("tip.play","§fВідтворити\n§7Мод сам натисне ті самі клавіші, у тому ж порядку та з тією ж швидкістю.\n§7Програє один раз і зупиниться.\n§8Світ навколо міг змінитися, тому підсумок не завжди той самий.",
                      "§fAbspielen\n§7Der Mod drückt dieselben Tasten, in derselben Reihenfolge und Geschwindigkeit.\n§7Spielt einmal ab und stoppt.\n§8Die Welt kann sich geändert haben, das Ergebnis kann abweichen.",
                      "§fOdtwórz\n§7Mod sam naciśnie te same klawisze, w tej samej kolejności i z tą samą prędkością.\n§7Odtworzy raz i się zatrzyma.\n§8Świat mógł się zmienić, więc wynik może być inny.");
        p3("tip.loop","§fЗациклити\n§7Те саме, що «Відтворити», але по колу — знову і знову.\n§7Кількість кіл задається в налаштуваннях (0 — без кінця).\n§8Між колами мод може сам повертатися до точки старту.",
                      "§fSchleife\n§7Wie „Abspielen“, aber immer wieder von vorn.\n§7Die Zahl der Runden stellst du in den Einstellungen ein (0 — endlos).\n§8Zwischen den Runden kann der Mod selbst zum Startpunkt zurücklaufen.",
                      "§fZapętl\n§7To samo co „Odtwórz”, ale w kółko — raz za razem.\n§7Liczba powtórzeń jest w ustawieniach (0 — bez końca).\n§8Między powtórzeniami mod może sam wracać do punktu startu.");
        p3("tip.stop","§fЗупинити\n§7Припиняє те, що йде просто зараз: відлік, запис або повтор.\n§8Усі затиснуті клавіші одразу відпускаються.",
                      "§fStopp\n§7Beendet, was gerade läuft: Countdown, Aufnahme oder Wiedergabe.\n§8Alle gehaltenen Tasten werden sofort losgelassen.",
                      "§fZatrzymaj\n§7Kończy to, co trwa: odliczanie, nagrywanie lub odtwarzanie.\n§8Wszystkie wciśnięte klawisze są od razu zwalniane.");
        p3("tip.settings","§fНалаштування\n§7Що повторювати, скільки чекати перед стартом і як все виглядає.\n§8Все розкладено за вкладками ліворуч.",
                          "§fEinstellungen\n§7Was wiederholt wird, wie lange vor dem Start gewartet wird und wie alles aussieht.\n§8Alles ist links in Tabs sortiert.",
                          "§fUstawienia\n§7Co powtarzać, ile czekać przed startem i jak to wygląda.\n§8Wszystko pogrupowane w zakładki po lewej.");
        p3("tip.close","§fЗакрити\n§7Просто прибирає це вікно з екрана.\n§8Якщо йде запис або повтор — вони триватимуть.",
                       "§fSchließen\n§7Blendet dieses Fenster einfach aus.\n§8Aufnahme oder Wiedergabe laufen im Hintergrund weiter.",
                       "§fZamknij\n§7Po prostu chowa to okno.\n§8Nagrywanie lub odtwarzanie trwa w tle.");
        p3("tip.lang","§fМова\n§7Змінює мову всіх написів.\n§7Типово стоїть «Авто» — мова повторює мову самої гри.\n§8Ваші записи від цього не змінюються.",
                      "§fSprache\n§7Ändert die Sprache aller Texte.\n§7Standard ist „Auto“ — der Mod folgt der Sprache des Spiels.\n§8Deine Aufnahmen bleiben unverändert.",
                      "§fJęzyk\n§7Zmienia język wszystkich napisów.\n§7Domyślnie „Auto” — mod podąża za językiem gry.\n§8Twoje nagrania pozostają bez zmian.");
        p3("help.title","Довідка за функціями","Funktionsübersicht","Przewodnik po funkcjach");
        p3("help.of","з","von","z");
        p3("help.sections","РОЗДІЛИ","ABSCHNITTE","SEKCJE");
        p3("slot.title","Слот","Slot","Slot");
        p3("tip.back","§fНазад\n§7Повертає на попередній екран.\n§8Нічого не скасовує і не зберігає.",
                      "§fZurück\n§7Geht zum vorherigen Bildschirm.\n§8Nichts wird gespeichert oder verworfen.",
                      "§fWstecz\n§7Wraca do poprzedniego ekranu.\n§8Nic nie zostaje zapisane ani anulowane.");
        p3("tip.helpPrev","§fПопередній розділ\n§7Гортає довідку на один розділ назад.\n§8Те саме робить стрілка вліво на клавіатурі.",
                          "§fVorheriger Abschnitt\n§7Blättert die Hilfe einen Abschnitt zurück.\n§8Das macht auch die Pfeiltaste nach links.",
                          "§fPoprzednia sekcja\n§7Przewija przewodnik o jedną sekcję wstecz.\n§8To samo robi strzałka w lewo.");
        p3("tip.helpNext","§fНаступний розділ\n§7Гортає довідку на один розділ уперед.\n§8Те саме робить стрілка вправо на клавіатурі.",
                          "§fNächster Abschnitt\n§7Blättert die Hilfe einen Abschnitt weiter.\n§8Das macht auch die Pfeiltaste nach rechts.",
                          "§fNastępna sekcja\n§7Przewija przewodnik o jedną sekcję naprzód.\n§8To samo robi strzałka w prawo.");
        p3("tip.helpList","§fРозділи довідки\n§7Натисніть на назву, щоб відкрити розділ.\n§8Довгий текст гортайте колесом миші.",
                          "§fHilfe-Abschnitte\n§7Titel anklicken, um den Abschnitt zu öffnen.\n§8Langer Text scrollt mit dem Mausrad.",
                          "§fSekcje pomocy\n§7Kliknij tytuł, aby otworzyć sekcję.\n§8Długi tekst przewija się kółkiem myszy.");
    }
}
