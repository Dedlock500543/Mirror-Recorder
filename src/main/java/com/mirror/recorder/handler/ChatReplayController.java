package com.mirror.recorder.handler;
import com.mirror.recorder.config.RecorderConfig;
import com.mirror.recorder.debug.MirrorDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import java.lang.reflect.*;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
/** Очередь сообщений, посимвольный ввод в GuiChat, fallback-отправка, антифлуд, блокировка импортированных. */
public class ChatReplayController{
    private static final int CHAT_OPEN_WAIT=20,CHAT_QUEUE_LIMIT=256;
    private static final long CHAT_FALLBACK_CHAR_NS=85000000L;
    private static final String[] CHAT_FIELDS={"field_146415_a","inputField"};
    private final Minecraft mc;private final RecorderConfig config;
    private final Set<Integer> replayedKeysRef;
    private final ArrayDeque<String> chatQueue=new ArrayDeque<String>();
    private String typeText=null;private int typeAt=0;private long typeNextNs=0L;private int typeWait=0;
    private boolean chatWarned=false,chatKeyReplay=false,prevChatOpen=false,importedChatWarned=false;
    private long lastChatSentAt=0L;private Field chatField=null;
    public ChatReplayController(Minecraft mc,RecorderConfig config,Set<Integer> replayedKeysRef){this.mc=mc;this.config=config;this.replayedKeysRef=replayedKeysRef;}
    /** Сообщение в очередь (не для импортированных). */
    public void enqueue(String msg){if(msg!=null&&!msg.isEmpty()&&chatQueue.size()<CHAT_QUEUE_LIMIT)chatQueue.add(msg);}
    /** Очередь полна? */
    public boolean isQueueFull(){return chatQueue.size()>=CHAT_QUEUE_LIMIT;}
    /** Есть ли сообщения в очереди или активная печать. */
    public boolean hasWork(){return typeText!=null||!chatQueue.isEmpty();}
    /** Может ли быть отправлено следующее сообщение с учётом антифлуда. */
    public boolean canSendNow(){long nowMs=System.currentTimeMillis();if(nowMs<lastChatSentAt)lastChatSentAt=0L;return nowMs-lastChatSentAt>=config.getChatSendIntervalMs();}
    /** Видимая печать: открытие GuiChat и ввод по буквам. */
    public void startVisibleTyping(String msg){
        if(msg==null||msg.isEmpty())return;typeText=msg;typeAt=0;typeNextNs=0L;typeWait=CHAT_OPEN_WAIT;
        MirrorDebug.log("CHAT","visible typing started: "+msg);openChatScreen();}
    /** Попытка отправить следующее сообщение из очереди. Возвращает отправленное сообщение или null. */
    /** Попытка отправить следующее сообщение. player нужен для fallback-отправки. */
    public String trySendNext(boolean isVisibleChat,EntityPlayerSP player){
        if(chatQueue.isEmpty())return null;
        long nowMs=System.currentTimeMillis();if(nowMs<lastChatSentAt)lastChatSentAt=0L;
        if(nowMs-lastChatSentAt<config.getChatSendIntervalMs())return null;
        if(isVisibleChat&&(typeText!=null||chatBlockingScreen()||(chatKeyReplay&&mc.currentScreen instanceof net.minecraft.client.gui.GuiChat)))return null;
        String msg=chatQueue.poll();if(msg!=null&&!msg.isEmpty()){
            if(chatKeyReplay)chatKeyReplay=false;
            else if(isVisibleChat)startVisibleTyping(msg);
            else if(player!=null)player.sendChatMessage(msg);
            lastChatSentAt=nowMs;}return msg;}
    /** Тик посимвольной печати. tickWait=true на клиентском тике, false на render tick. */
    public void tickTyping(boolean tickWait){
        if(typeText==null)return;if(mc.player==null){typeText=null;typeNextNs=0L;return;}
        if(!(mc.currentScreen instanceof net.minecraft.client.gui.GuiChat)){
            if(typeWait>0){if(tickWait)typeWait--;openChatScreen();return;}
            MirrorDebug.log("CHAT","chat screen did not open in "+CHAT_OPEN_WAIT+" ticks");warnVisibleChat();failTyping(typeText);return;}
        long now=System.nanoTime();if(typeNextNs>0L&&now<typeNextNs)return;
        if(typeAt<typeText.length()){
            typeWait=CHAT_OPEN_WAIT;char tc=typeText.charAt(typeAt);typeAt++;
            if(!typeChar(tc)&&!setChatText(typeText.substring(0,typeAt))){MirrorDebug.log("CHAT","chat input field is not reachable");warnVisibleChat();failTyping(typeText);return;}
            typeNextNs=now+CHAT_FALLBACK_CHAR_NS;return;}
        String done=typeText;typeText=null;typeNextNs=0L;
        if(!sendChatKey()&&mc.player!=null)mc.player.sendChatMessage(done);
        if(mc.currentScreen instanceof net.minecraft.client.gui.GuiChat)mc.displayGuiScreen(null);}
    public boolean isTyping(){return typeText!=null;}
    public boolean isChatKeyReplay(){return chatKeyReplay;}
    public void setChatKeyReplay(boolean v){chatKeyReplay=v;}
    public boolean wasPrevChatOpen(){return prevChatOpen;}
    public void setPrevChatOpen(boolean v){prevChatOpen=v;}
    /** Предупреждение: чат из импортированной записи. */
    public void warnImported(){
        if(importedChatWarned)return;importedChatWarned=true;
        MirrorDebug.log("CHAT","imported recording: chat and commands are not sent");
        sendMsg(L("§eСообщения и команды из импортированной записи не отправляются.","§eChat and commands from an imported recording are not sent.","§eПовідомлення та команди з імпортованого запису не надсилаються.","§eChat und Befehle aus einer importierten Aufnahme werden nicht gesendet.","§eCzat i komendy z zaimportowanego nagrania nie są wysyłane."));}
    /** Сброс состояния при остановке. */
    public void reset(){
        typeText=null;typeNextNs=0L;chatKeyReplay=false;chatQueue.clear();lastChatSentAt=0L;
        importedChatWarned=false;prevChatOpen=false;}
    // --- Внутренние методы ---
    private void failTyping(String msg){typeText=null;if(mc.player!=null&&msg!=null&&!msg.isEmpty())mc.player.sendChatMessage(msg);}
    private void warnVisibleChat(){
        if(chatWarned)return;chatWarned=true;
        sendMsg(L("§eЖивой чат недоступен: сообщение отправлено обычным способом.","§eVisible chat is unavailable: the message was sent the plain way.","§eЖивий чат недоступний: повідомлення надіслано звичайним способом.","§eSichtbarer Chat ist nicht verfügbar: Die Nachricht wurde normal gesendet.","§eWidoczny czat jest niedostępny: wiadomość wysłano zwykłą metodą."));}
    private boolean chatBlockingScreen(){GuiScreen s=mc.currentScreen;return s!=null&&!(s instanceof net.minecraft.client.gui.GuiChat);}
    private void openChatScreen(){try{if(!(mc.currentScreen instanceof net.minecraft.client.gui.GuiChat))mc.displayGuiScreen(new net.minecraft.client.gui.GuiChat());}catch(Exception e){}}
    private boolean typeChar(char c){
        try{GuiScreen sc=mc.currentScreen;if(!(sc instanceof net.minecraft.client.gui.GuiChat))return false;
            net.minecraft.client.gui.GuiScreen gsc=(net.minecraft.client.gui.GuiScreen)sc;
            Method km=null;for(Method m:GuiScreen.class.getDeclaredMethods()){Class<?>[] a=m.getParameterTypes();String n=m.getName();if(a.length==2&&a[0]==char.class&&a[1]==int.class&&(n.equals("keyTyped")||n.equals("func_73869_a"))){km=m;break;}}
            if(km==null)return false;km.setAccessible(true);km.invoke(gsc,Character.valueOf(c),Integer.valueOf(0));return true;
        }catch(Exception e){return false;}}
    private boolean sendChatKey(){
        try{GuiScreen sc=mc.currentScreen;if(!(sc instanceof net.minecraft.client.gui.GuiChat))return false;
            Method km=null;for(Method m:GuiScreen.class.getDeclaredMethods()){Class<?>[] a=m.getParameterTypes();String n=m.getName();if(a.length==2&&a[0]==char.class&&a[1]==int.class&&(n.equals("keyTyped")||n.equals("func_73869_a"))){km=m;break;}}
            if(km==null)return false;km.setAccessible(true);km.invoke(sc,Character.valueOf('\r'),Integer.valueOf(28));
            return !(mc.currentScreen instanceof net.minecraft.client.gui.GuiChat);
        }catch(Exception e){return false;}}
    private boolean setChatText(String text){
        try{GuiScreen sc=mc.currentScreen;if(!(sc instanceof net.minecraft.client.gui.GuiChat))return false;
            if(chatField==null){for(String nm:CHAT_FIELDS){try{Field f=net.minecraft.client.gui.GuiChat.class.getDeclaredField(nm);f.setAccessible(true);chatField=f;break;}catch(Exception e){}}}
            if(chatField==null)return false;Object o=chatField.get(sc);
            if(!(o instanceof net.minecraft.client.gui.GuiTextField))return false;((net.minecraft.client.gui.GuiTextField)o).setText(text);return true;
        }catch(Exception e){return false;}}
    private static String L(String ru,String en,String uk,String de,String pl){return com.mirror.recorder.gui.Lang.s(ru,en,uk,de,pl);}
    private void sendMsg(String msg){com.mirror.recorder.handler.TransientChat.show(msg);}
}
