package com.mirror.recorder.handler;
import com.mirror.recorder.config.RecorderConfig;
import com.mirror.recorder.manager.RecorderManager;
import com.mirror.recorder.debug.MirrorDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import com.mirror.recorder.MirrorRecorder;
/** Безопасность воспроизведения: stop on WASD, stop on mouse, stall detection, потеря фокуса, аварийная остановка. */
public class PlaybackSafetyController{
    private static final int MOUSE_GRACE_TICKS=20,STALL_LIMIT=10,WORLD_SUSPEND_LIMIT=600;
    private static final double MOUSE_DECAY=0.75d;
    private static final int MOUSE_REGRAB_GRACE=6;
    private final Minecraft mc;private final RecorderManager manager;private final RecorderConfig config;
    private double mouseMovePixels=0d;private int mouseMoveGrace=MOUSE_GRACE_TICKS;
    private boolean mouseFree=false;private int mouseFreeX=0,mouseFreeY=0;private long mouseRun=-1L;
    private int stallTicks=0;private boolean lostFocus=false,keysNeedReset=false;
    private boolean backgroundPolicyHeld=false;private boolean savedPauseOnLostFocus=true;
    private int worldSuspendTicks=0;private boolean worldResetPending=false;
    public PlaybackSafetyController(Minecraft mc,RecorderManager manager,RecorderConfig config){this.mc=mc;this.manager=manager;this.config=config;}
    /** Обновление фокуса: потеря → отжать клавиши (только в покое). */
    public void focusGuard(){
        boolean active=true;try{active=Display.isActive();}catch(Exception e){active=true;}
        if(active){lostFocus=false;return;}if(lostFocus)return;lostFocus=true;
        if(!manager.isPlaying()&&!manager.isRecording())try{KeyBinding.unPressAllKeys();}catch(Exception e){}}
    /** Накопление движения мыши за кадр. */
    public void sampleMouseMovement(){
        if(!config.isStopOnMouseMove()||!manager.isPlaying()||manager.isPaused()){mouseMovePixels=0d;mouseFree=false;return;}
        if(manager.getSimulateCooldown()>0){mouseMovePixels=0d;mouseFree=false;return;}
        if(mc.currentScreen==null&&mc.inGameHasFocus){mouseFree=false;if(mc.mouseHelper!=null)mouseMovePixels+=Math.abs(mc.mouseHelper.deltaX)+Math.abs(mc.mouseHelper.deltaY);return;}
        boolean active=true;try{active=Display.isActive();}catch(Exception e){active=true;}
        if(!active){mouseFree=false;return;}
        int x,y;try{x=Mouse.getX();y=Mouse.getY();}catch(Exception e){mouseFree=false;return;}
        if(!mouseFree){mouseFree=true;mouseFreeX=x;mouseFreeY=y;return;}
        mouseMovePixels+=Math.abs(x-mouseFreeX)+Math.abs(y-mouseFreeY);mouseFreeX=x;mouseFreeY=y;}
    /** Проверка порога мышиного стопа. */
    public boolean mouseStopTriggered(){
        long run=manager.getPlaybackRunId();if(mouseRun!=run){mouseRun=run;mouseMoveGrace=MOUSE_GRACE_TICKS;mouseMovePixels=0d;return false;}
        if(!config.isStopOnMouseMove()){mouseMovePixels=0d;return false;}
        if(mouseMoveGrace>0){--mouseMoveGrace;mouseMovePixels=0d;return false;}
        if(mouseMovePixels>=config.getStopOnMouseThreshold()){mouseMovePixels=0d;return true;}
        mouseMovePixels*=MOUSE_DECAY;return false;}
    /** Стоп по мыши. */
    public boolean stopByMouseMove(){
        if(!manager.isPlaying()||manager.isPaused())return false;
        if(manager.getSimulateCooldown()>0){mouseMovePixels=0d;return false;}
        if(!mouseStopTriggered())return false;
        MirrorDebug.log("PLAYBACK","STOP: manual mouse movement at frame "+manager.getCurrentFrameIndex()+" (run "+manager.getPlaybackRunId()+", screen "+(mc.currentScreen==null?"none":mc.currentScreen.getClass().getSimpleName())+")");
        return true;}
    /** Реальное движение WASD. */
    public boolean isRealMovement(){return mc.player!=null&&mc.gameSettings!=null&&(mc.gameSettings.keyBindForward.isKeyDown()||mc.gameSettings.keyBindBack.isKeyDown()||mc.gameSettings.keyBindLeft.isKeyDown()||mc.gameSettings.keyBindRight.isKeyDown()||mc.gameSettings.keyBindJump.isKeyDown());}
    /** Stall detection: вход не применяется к игроку. */
    public boolean checkStall(long appliedRun,int appliedFrame,long expectedRun,int expectedFrame){
        if(appliedRun==expectedRun&&appliedFrame==expectedFrame){stallTicks=0;return false;}
        if(++stallTicks>=STALL_LIMIT){stallTicks=0;MirrorDebug.log("PLAYBACK","STOP: player input not applied for "+STALL_LIMIT+" ticks at frame "+expectedFrame);return true;}return false;}
    /** Мир заморожен (одиночная пауза): запись и повтор замерзают вместе. */
    public boolean isGamePaused(){return mc.isGamePaused();}
    /** Обработка отсутствия мира (переход между мирами). */
    public boolean handleWorldMissing(boolean isBusy){
        if(mc.world!=null){worldSuspendTicks=0;return false;}
        if(isBusy&&++worldSuspendTicks>=WORLD_SUSPEND_LIMIT){worldSuspendTicks=0;return true;}
        return false;}
    /** Сброс при смене мира. */
    public void onWorldLoad(){if(manager.isBusy())worldResetPending=true;}
    public boolean consumeWorldReset(){boolean v=worldResetPending;worldResetPending=false;return v;}
    /** Политика фокуса: не даём ванилле открывать паузу при потере фокуса во время операции. */
    public void updateBackgroundPolicy(){
        if(mc.gameSettings==null)return;
        MirrorRecorder mod=MirrorRecorder.getInstance();BaritoneReturnController r=mod==null?null:mod.getReturnController();
        boolean active=manager.isBusy()||(r!=null&&(r.isBusy()||r.isRecovering()));
        if(active){if(!backgroundPolicyHeld){savedPauseOnLostFocus=mc.gameSettings.pauseOnLostFocus;backgroundPolicyHeld=true;}mc.gameSettings.pauseOnLostFocus=false;}
        else releaseBackgroundPolicy();}
    public void releaseBackgroundPolicy(){if(backgroundPolicyHeld&&mc.gameSettings!=null)mc.gameSettings.pauseOnLostFocus=savedPauseOnLostFocus;backgroundPolicyHeld=false;}
    /** Grace period после возврата захвата мыши. */
    public int getMouseRegabGrace(){return MOUSE_REGRAB_GRACE;}
    public void resetMouseGrace(){mouseMoveGrace=MOUSE_GRACE_TICKS;mouseMovePixels=0d;mouseFree=false;}
    public void resetStall(){stallTicks=0;}
    public int getStallTicks(){return stallTicks;}
    public boolean needsKeyReset(){return keysNeedReset;}
    public void setKeyReset(boolean v){keysNeedReset=v;}
}
