package com.mirror.recorder.handler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.util.*;

/** Independently removes each Mirror Recorder chat message after five seconds. */
public final class TransientChat{
    private static final class Entry{final int id;int ticks=100;Entry(int value){id=value;}}
    private static final List<Entry> entries=new ArrayList<Entry>();private static int nextId=730000;
    public static synchronized void show(String text){Minecraft mc=Minecraft.getMinecraft();if(mc.ingameGUI==null||text==null)return;int id=nextId++;if(nextId>739999)nextId=730000;mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new TextComponentString(text),id);entries.add(new Entry(id));}
    @SubscribeEvent public void onTick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;Minecraft mc=Minecraft.getMinecraft();synchronized(TransientChat.class){Iterator<Entry> it=entries.iterator();while(it.hasNext()){Entry entry=it.next();if(--entry.ticks<=0){if(mc.ingameGUI!=null)mc.ingameGUI.getChatGUI().deleteChatLine(entry.id);it.remove();}}}}
}
