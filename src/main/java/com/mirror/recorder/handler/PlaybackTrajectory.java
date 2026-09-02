package com.mirror.recorder.handler;
import com.mirror.recorder.manager.RecorderManager;
import com.mirror.recorder.model.Frame;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.relauncher.*;
@SideOnly(Side.CLIENT)
public final class PlaybackTrajectory{
    private final RecorderManager manager;private long sessionId=-1L,runId=-1L;private boolean originSet=false,ready=false;private double originX,originY,originZ,originMotionX,originMotionZ,recordedBaseX,recordedBaseY,recordedBaseZ;
    public PlaybackTrajectory(RecorderManager manager){this.manager=manager;}
    /** Each playback run (including an internal loop wrap) is calibrated from the current position and the recorded first frame. Live drift of the previous cycle is not reused. */
    public void updateCalibration(EntityPlayerSP player){
        if(player==null||!manager.isPlaying()){reset();return;}
        long currentRun=manager.getPlaybackRunId(),currentSession=manager.getPlaybackSessionId();
        if(currentRun!=runId||currentSession!=sessionId){reset();runId=currentRun;sessionId=currentSession;}
        Frame first=manager.getPlaybackFirstFrame();
        if(!originSet){
            // Кадр 0 записи — это состояние ПОСЛЕ его тика: first.x уже содержит перемещение первого тика.
            // Поэтому точку отсчёта снимаем на тик позже, когда тик кадра 0 отработал и в повторе.
            // Иначе вся ожидаемая линия сдвинута на перемещение первого тика (~0.22 блока шагом,
            // ~0.29 спринтом) и стабилизатор бесконечно давит на этот сдвиг. Старт записи стоя: сдвиг нулевой.
            if(manager.getCurrentFrameIndex()<1)return;
            originX=player.posX;originY=player.posY;originZ=player.posZ;originMotionX=player.motionX;originMotionZ=player.motionZ;originSet=true;
            if(first!=null){recordedBaseX=first.x;recordedBaseY=first.y;recordedBaseZ=first.z;ready=true;}
            return;
        }
        if(!ready&&first!=null){recordedBaseX=first.x;recordedBaseY=first.y;recordedBaseZ=first.z;ready=true;}
    }
    public boolean isReady(){return ready&&originSet&&manager.isPlaying()&&manager.getPlaybackRunId()==runId;}
    public double expectedX(Frame frame,int cycle){return originX+(frame.x-recordedBaseX);}public double expectedY(Frame frame,int cycle){return originY+(frame.y-recordedBaseY);}public double expectedZ(Frame frame,int cycle){return originZ+(frame.z-recordedBaseZ);}
    public double expectedPreX(){int idx=manager.getCurrentFrameIndex();Frame previous=idx>0?manager.getPreviousPlaybackFrame():null;return previous==null?originX:expectedX(previous,manager.getPlaybackCycle());}public double expectedPreY(){int idx=manager.getCurrentFrameIndex();Frame previous=idx>0?manager.getPreviousPlaybackFrame():null;return previous==null?originY:expectedY(previous,manager.getPlaybackCycle());}public double expectedPreZ(){int idx=manager.getCurrentFrameIndex();Frame previous=idx>0?manager.getPreviousPlaybackFrame():null;return previous==null?originZ:expectedZ(previous,manager.getPlaybackCycle());}
    public double expectedVelocityX(){Frame previous=manager.getCurrentFrameIndex()>0?manager.getPreviousPlaybackFrame():null;return previous==null?originMotionX:previous.motionX;}public double expectedVelocityZ(){Frame previous=manager.getCurrentFrameIndex()>0?manager.getPreviousPlaybackFrame():null;return previous==null?originMotionZ:previous.motionZ;}
    public void reset(){sessionId=-1L;runId=-1L;originSet=false;ready=false;originX=originY=originZ=originMotionX=originMotionZ=recordedBaseX=recordedBaseY=recordedBaseZ=0d;}
}
