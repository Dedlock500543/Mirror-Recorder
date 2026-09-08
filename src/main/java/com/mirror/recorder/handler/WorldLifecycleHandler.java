package com.mirror.recorder.handler;
import com.mirror.recorder.manager.RecorderManager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.*;
@SideOnly(Side.CLIENT)
public class WorldLifecycleHandler {
    private final RecorderManager manager;
    public WorldLifecycleHandler(RecorderManager manager){this.manager=manager;}
    /** Запись/повтор переживают смену мира (лобби -> арена): вместо остановки — промежуточное сохранение. */
    @SubscribeEvent public void onWorldUnload(WorldEvent.Unload event){if(!event.getWorld().isRemote)return;if(manager.isBusy())manager.checkpointRecording();else manager.stopAll();manager.releaseIdleFrames();}
}
