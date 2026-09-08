package com.mirror.recorder.handler;
import com.mirror.recorder.MirrorRecorder;
import com.mirror.recorder.gui.Lang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * Локализация названий клавиш мода в «Управлении».
 *
 * keyDescription трогать нельзя: ваниль хранит привязку в options.txt строкой
 * key_&lt;keyDescription&gt; и по ней же читает её при запуске. Подставленный туда
 * локализованный текст приводил к тому, что следующий запуск не находил свою строку
 * и клавиши мода сбрасывались. Поэтому keyDescription всегда возвращается к ключу
 * перевода (это же лечит options.txt, испорченный прошлыми версиями), а перевод
 * кладётся в таблицу локализации I18n под тем же ключом — «Управление» рисует текст
 * через неё, а файл настроек игры остаётся нетронутым.
 */
@SideOnly(Side.CLIENT)
public final class KeybindLocalizer{
    private static final String[] KEYS={"key.mirror_recorder.record","key.mirror_recorder.play","key.mirror_recorder.loop","key.mirror_recorder.stop","key.mirror_recorder.gui"};
    private static String lastLang=null,lastMode=null;
    private static boolean fieldsResolved=false;private static Field localeField=null,mapField=null;
    @SubscribeEvent public void onTick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getMinecraft();if(mc==null||mc.gameSettings==null)return;
        MirrorRecorder mod=MirrorRecorder.getInstance();if(mod==null||mod.getKeyRecord()==null)return;
        String lang=mc.gameSettings.language==null?"":mc.gameSettings.language,mode=Lang.get();
        Map<String,String> table=localeTable();
        boolean fresh=!lang.equals(lastLang)||!mode.equals(lastMode);
        // Таблицу переводов пересобирает любая перезагрузка ресурсов, поэтому сверяем её каждый тик: это одна выборка из HashMap.
        boolean missing=table!=null&&!kbName(0).equals(table.get(KEYS[0]));
        if(!fresh&&!missing)return;
        lastLang=lang;lastMode=mode;
        for(int i=0;i<KEYS.length;i++){
            restoreDescription(binding(mod,i),KEYS[i]);
            if(table!=null)try{table.put(KEYS[i],kbName(i));}catch(Throwable ignored){}
        }
    }
    private static KeyBinding binding(MirrorRecorder mod,int kind){
        switch(kind){case 0:return mod.getKeyRecord();case 1:return mod.getKeyPlay();case 2:return mod.getKeyLoop();case 3:return mod.getKeyStop();default:return mod.getKeyGui();}
    }
    /** Литеральные названия клавиш под фактический язык интерфейса. */
    private static String kbName(int kind){
        String r=Lang.resolved();
        if(Lang.RU.equals(r))return new String[]{"Запись","Воспроизведение","Зациклить","Стоп","Открыть интерфейс"}[kind];
        if(Lang.UK.equals(r))return new String[]{"Запис","Відтворення","Зациклити","Стоп","Відкрити інтерфейс"}[kind];
        if(Lang.DE.equals(r))return new String[]{"Aufnehmen","Wiedergabe","Schleife","Stopp","Oberfläche öffnen"}[kind];
        if(Lang.PL.equals(r))return new String[]{"Nagraj","Odtwórz","Zapętl","Stop","Otwórz interfejs"}[kind];
        return new String[]{"Record","Playback","Loop","Stop","Open Interface"}[kind];
    }
    /** Возврат keyDescription к ключу перевода: по нему ваниль хранит привязку клавиши. */
    private static void restoreDescription(KeyBinding kb,String key){
        if(kb==null)return;String current=kb.getKeyDescription();
        if(current==null||current.equals(key))return;
        try{
            for(Field f:KeyBinding.class.getDeclaredFields()){
                if(f.getType()!=String.class||Modifier.isStatic(f.getModifiers()))continue;
                f.setAccessible(true);
                if(current.equals(f.get(kb))){f.set(kb,key);return;}
            }
        }catch(Throwable ignored){}
    }
    /** Таблица переводов активной локали: поля ищем по типу, чтобы работать и в обфусцированной сборке. */
    @SuppressWarnings("unchecked")
    private static Map<String,String> localeTable(){
        try{
            if(!fieldsResolved){
                fieldsResolved=true;
                for(Field f:I18n.class.getDeclaredFields()){
                    if(!Modifier.isStatic(f.getModifiers())||f.getType()!=net.minecraft.client.resources.Locale.class)continue;
                    f.setAccessible(true);localeField=f;break;
                }
                if(localeField!=null)for(Field g:net.minecraft.client.resources.Locale.class.getDeclaredFields()){
                    if(Modifier.isStatic(g.getModifiers())||!Map.class.isAssignableFrom(g.getType()))continue;
                    g.setAccessible(true);mapField=g;break;
                }
            }
            if(localeField==null||mapField==null)return null;
            Object locale=localeField.get(null);if(locale==null)return null;
            Object map=mapField.get(locale);
            return map instanceof Map?(Map<String,String>)map:null;
        }catch(Throwable ignored){return null;}
    }
}
