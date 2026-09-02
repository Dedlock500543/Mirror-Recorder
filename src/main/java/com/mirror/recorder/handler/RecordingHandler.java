package com.mirror.recorder.handler;
import com.mirror.recorder.MirrorRecorder;
import com.mirror.recorder.model.Frame;
import com.mirror.recorder.manager.RecorderManager;
import com.mirror.recorder.config.RecorderConfig;
import com.mirror.recorder.debug.MirrorDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.*;
import net.minecraftforge.fml.relauncher.*;
import net.minecraftforge.event.world.WorldEvent;
import org.lwjgl.input.Keyboard;import org.lwjgl.input.Mouse;
import org.apache.logging.log4j.*;import java.lang.reflect.*;
@SideOnly(Side.CLIENT)
public class RecordingHandler {
    private static final Logger LOG=LogManager.getLogger("MirrorRecorder/Input");
    private boolean sndRec=false,sndPlay=false;private int sndCycle=0;
    private final SafetyGuard guard=new SafetyGuard();
    private final Minecraft mc=Minecraft.getMinecraft();
    private final RecorderManager manager; private final RecorderConfig config;
    private volatile String pendingChatMessage=null;
    private boolean keysNeedReset=false;
    private static final int CHAT_QUEUE_LIMIT=256,GUI_CLICK_TTL=40,GUI_QUEUE_LIMIT=64;
    /** Сколько тиков повтор может стоять на месте без применённого ввода, прежде чем остановиться самому. */
    private static final int STALL_LIMIT=10;
    private int stallTicks=0;
    private long rotRun=-1L;private int rotFrame=-1;private float lookDYaw=0f,lookDPitch=0f,savedLookYaw=0f,savedLookPitch=0f,savedPrevLookYaw=0f,savedPrevLookPitch=0f;private boolean lookRenderPushed=false;
    private long appliedRun=-1L,blendRun=-1L;
    /** Опора вращения на прогон: при старте повтора камера ставится на записанный стартовый угол, дальше повтор идёт по дельтам с плавным доворотом к записанному углу (не более MAX_ROT_CORRECTION за тик). */
    private long rotAnchorRun=-1L;private boolean rotAnchorSet=false;private static final float MAX_ROT_CORRECTION=10.0f;
    private final java.util.ArrayDeque<String> chatQueue=new java.util.ArrayDeque<String>();
    /** Записанные сообщения отправляются по одному: залп в один тик ведёт к кику за флуд. */
    private long lastChatSentAt=0L;private static final long CHAT_SEND_INTERVAL_MS=1200L;
    private int appliedFrame=-1,interactionFrame=-1;private boolean heldAttack=false,heldUse=false,recRotInit=false;private int lastMask=0;private float lastRecYaw=0f,lastRecPitch=0f;private long interactionRun=-1L;private float adjustedForward,adjustedStrafe;private final PlaybackTrajectory trajectory;private boolean clickMethodsResolved=false,guiClickMethodResolved=false,clickMethodWarning=false;private Method clickMouseMethod,rightClickMouseMethod,guiMouseClickedMethod;private Field leftClickCounterField;private Method sendClickBlockMethod;private Field rightClickDelayField;private volatile boolean pendingGuiClick=false,pendingGuiCenter=false;private volatile int pendingGuiCX=0,pendingGuiCY=0;private boolean importedChatWarned=false;private volatile float pendingGuiX=0f,pendingGuiY=0f;private volatile String pendingGuiScreen=null;private String lastNotice="";private long lastNoticeAt=0L;private Method guiMouseReleasedMethod,guiKeyTypedMethod;private int lastHotbar=-1;private final java.util.ArrayDeque<GuiClickEvent> guiQueue=new java.util.ArrayDeque<GuiClickEvent>();private boolean speedNoticeShown=false;private boolean backgroundPolicyHeld=false,savedPauseOnLostFocus=true;private int useHoldFallbackDelay=0;private boolean wantSprint=false;private boolean sprintAssist=false;private boolean useTimerAlign=false;private long maskRun=-1L;private volatile boolean pendingGuiShift=false;private int screenMismatchTicks=0;private static final int SCREEN_CLOSE_DELAY=10;private boolean quickMoveResolved=false;private Method handleMouseClickMethod,slotAtPositionMethod;private final Frame[] lookAhead=new Frame[100];private static final int GA_TYPE=0,GA_DROP=1,GA_PICK=2,GA_CLOSE=3,GA_SWAP=4,GUI_KEY_TTL=40,GUI_KEY_LIMIT=64;private final java.util.List<int[]> pendingGuiKeys=new java.util.ArrayList<int[]>();private final java.util.ArrayDeque<GuiKeyEvent> guiKeyQueue=new java.util.ArrayDeque<GuiKeyEvent>();private volatile int pendingGuiButton=0;private final java.util.Map<String,Field[]> textFieldCache=new java.util.HashMap<String,Field[]>();private int worldSuspendTicks=0;private static final int WORLD_SUSPEND_LIMIT=600;private volatile boolean worldResetPending=false;
    public RecordingHandler(RecorderManager manager,RecorderConfig config,PlaybackTrajectory trajectory){this.manager=manager;this.config=config;this.trajectory=trajectory;}
    /** Аварийная остановка: гасит запись, повтор, возврат и все удерживаемые клавиши. */
    private void emergencyStop(String reason){
        MirrorRecorder mod=MirrorRecorder.getInstance();
        if(mod!=null&&mod.getReturnController()!=null)mod.getReturnController().cancel();
        resetPlayback();manager.stopAll();forceResetKeys();guard.standby(mc.player);
        SoundFx.error();com.mirror.recorder.debug.MirrorDebug.log("GUARD",reason);sendMsg("\u00a7c[!] "+reason);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void onInputUpdate(InputUpdateEvent event){
        if(!manager.isPlaying())return;
        EntityPlayerSP player=mc.player;if(player==null)return;
        Frame frame=manager.getCurrentPlaybackFrame();if(frame==null)return;
        boolean replaySneak=config.isPlaybackSneak()&&(frame.hasKeyMask?frame.key(Frame.K_SNEAK):frame.sneak);
        trajectory.updateCalibration(player);event.getMovementInput().sneak=replaySneak;
        if(config.isPlaybackMovement()){
            float rawForward=frame.hasKeyMask?((frame.key(Frame.K_FORWARD)?1f:0f)-(frame.key(Frame.K_BACK)?1f:0f)):frame.moveForward,rawStrafe=frame.hasKeyMask?((frame.key(Frame.K_LEFT)?1f:0f)-(frame.key(Frame.K_RIGHT)?1f:0f)):frame.moveStrafe;
            float forward=MathHelper.clamp(rawForward,-1.0f,1.0f),strafe=MathHelper.clamp(rawStrafe,-1.0f,1.0f);if(!config.isApplyRotation()&&(forward!=0f||strafe!=0f)){double rec=Math.toRadians(frame.yaw),cur=Math.toRadians(player.rotationYaw),worldX=-forward*Math.sin(rec)+strafe*Math.cos(rec),worldZ=forward*Math.cos(rec)+strafe*Math.sin(rec),nf=-worldX*Math.sin(cur)+worldZ*Math.cos(cur),ns=worldX*Math.cos(cur)+worldZ*Math.sin(cur),peak=Math.max(Math.abs(nf),Math.abs(ns));if(peak>1d){nf/=peak;ns/=peak;}forward=(float)nf;strafe=(float)ns;}if(!stabilizeRoute(player,frame,forward,strafe)){event.getMovementInput().moveForward=0f;event.getMovementInput().moveStrafe=0f;event.getMovementInput().jump=false;event.getMovementInput().sneak=false;return;}
            // Ваниль замедляет красться x0.3 внутри updatePlayerMoveState до нашего хука — доводим вручную, иначе повтор крадётся в 3.3 раза быстрее записи.
            if(replaySneak){adjustedForward*=0.3f;adjustedStrafe*=0.3f;}
            event.getMovementInput().moveForward=adjustedForward;event.getMovementInput().moveStrafe=adjustedStrafe;
        }
        event.getMovementInput().jump=config.isPlaybackJump()&&(frame.hasKeyMask?frame.key(Frame.K_JUMP):frame.jump);
        // Replay the actual recorded sprint state: opening chat clears key states, and double-tap sprint never uses the sprint key bit.
        wantSprint=config.isPlaybackSprint()&&(frame.sprint||(frame.hasKeyMask&&frame.key(Frame.K_SPRINT)))&&!replaySneak;if(!config.isPlaybackMovement())sprintAssist=false;player.setSprinting(wantSprint||sprintAssist);
        applyPlaybackRotation();
        appliedRun=manager.getPlaybackRunId();appliedFrame=manager.getCurrentFrameIndex();keysNeedReset=true;
    }
    /** Новый угол ставится до кликов. prevRotation не трогаем: его перезапишет тик сущности, а плавность руки восстанавливается на кадре отрисовки. */
    private void applyPlaybackRotation(){
        EntityPlayerSP player=mc.player;if(player==null||!manager.isPlaying())return;
        Frame frame=manager.getCurrentPlaybackFrame();if(frame==null)return;
        long run=manager.getPlaybackRunId();int index=manager.getCurrentFrameIndex();
        if(rotRun==run&&rotFrame==index)return;
        rotRun=run;rotFrame=index;
        float carryYaw=manager.consumeCarryYaw(),carryPitch=manager.consumeCarryPitch();
        if(blendRun!=run){blendRun=run;guiQueue.clear();}
        if(rotAnchorRun!=run){rotAnchorRun=run;rotAnchorSet=false;if(frame.hasKeyMask&&config.isApplyRotation()){float baseYaw=frame.yaw-frame.dYaw,basePitch=MathHelper.clamp(frame.pitch-frame.dPitch,-90.0f,90.0f);player.rotationYaw=baseYaw;player.rotationPitch=basePitch;player.prevRotationYaw=baseYaw;player.prevRotationPitch=basePitch;player.rotationYawHead=baseYaw;rotAnchorSet=true;}}
        if(!config.isApplyRotation()){lookDYaw=0f;lookDPitch=0f;return;}
        float targetYaw,targetPitch;
        if(frame.hasKeyMask){
            float stepYaw=player.rotationYaw+carryYaw+frame.dYaw,stepPitch=player.rotationPitch+carryPitch+frame.dPitch;
            // Доворот к записанному абсолютному углу, но не более MAX_ROT_CORRECTION градусов за тик: накопленная ошибка дельт гасится плавно, без рывков камеры.
            if(rotAnchorSet){
                stepYaw+=MathHelper.clamp(MathHelper.wrapDegrees(frame.yaw-stepYaw),-MAX_ROT_CORRECTION,MAX_ROT_CORRECTION);
                stepPitch+=MathHelper.clamp(frame.pitch-stepPitch,-MAX_ROT_CORRECTION,MAX_ROT_CORRECTION);
            }
            targetYaw=stepYaw;targetPitch=MathHelper.clamp(stepPitch,-90.0f,90.0f);
        }
        else{targetYaw=frame.yaw;targetPitch=MathHelper.clamp(frame.pitch,-90.0f,90.0f);}
        lookDYaw=MathHelper.wrapDegrees(targetYaw-player.rotationYaw);
        lookDPitch=MathHelper.clamp(targetPitch,-90.0f,90.0f)-player.rotationPitch;
        player.rotationYaw=player.rotationYaw+lookDYaw;
        player.rotationPitch=player.rotationPitch+lookDPitch;
        player.rotationYawHead=player.rotationYaw;
    }
    /** Ваниль рисует руку через partialTicks. После тика сущности prevRotation==rotation, поэтому на кадр отрисовки подставляем угол между тиками. */
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void onRenderTick(TickEvent.RenderTickEvent event){
        if(!manager.isPlaying()||!config.isApplyRotation()){popInterpolatedLook();return;}
        if(event.phase==TickEvent.Phase.START)pushInterpolatedLook(event.renderTickTime);else popInterpolatedLook();
    }
    private void pushInterpolatedLook(float partialTicks){
        EntityPlayerSP player=mc.player;if(player==null||lookRenderPushed)return;
        savedLookYaw=player.rotationYaw;savedLookPitch=player.rotationPitch;savedPrevLookYaw=player.prevRotationYaw;savedPrevLookPitch=player.prevRotationPitch;
        float pt=partialTicks<0f?0f:(partialTicks>1f?1f:partialTicks);
        float yaw=savedLookYaw-lookDYaw*(1f-pt),pitch=savedLookPitch-lookDPitch*(1f-pt);
        player.rotationYaw=yaw;player.rotationPitch=pitch;player.prevRotationYaw=yaw;player.prevRotationPitch=pitch;
        player.rotationYawHead=yaw;player.prevRotationYawHead=yaw;lookRenderPushed=true;
    }
    private void popInterpolatedLook(){
        if(!lookRenderPushed)return;EntityPlayerSP player=mc.player;lookRenderPushed=false;if(player==null)return;
        player.rotationYaw=savedLookYaw;player.rotationPitch=savedLookPitch;player.prevRotationYaw=savedPrevLookYaw;player.prevRotationPitch=savedPrevLookPitch;
        player.rotationYawHead=savedLookYaw;player.prevRotationYawHead=savedPrevLookYaw;
    }
    private boolean stabilizeRoute(EntityPlayerSP player,Frame frame,float forward,float strafe){
        adjustedForward=forward;adjustedStrafe=strafe;boolean prevAssist=sprintAssist;sprintAssist=false;if(!trajectory.isReady())return true;if(Math.abs(config.getPlaybackSpeed()-1f)>0.001f){if(!speedNoticeShown){speedNoticeShown=true;MirrorDebug.log("ROUTE","stabilization disabled: playback speed "+config.getPlaybackSpeed()+" != 1.00");sendMsg(L("§eСтабилизация маршрута выключена: скорость повтора не 1.00.","§eRoute stabilization is off: playback speed is not 1.00.","§eСтабілізація маршруту вимкнена: швидкість відтворення не 1.00.","§eRoutenstabilisierung ist aus: Wiedergabegeschwindigkeit ist nicht 1.00.","§eStabilizacja trasy wyłączona: prędkość odtwarzania nie wynosi 1.00."));}return true;}double errorX=trajectory.expectedPreX()-player.posX,errorZ=trajectory.expectedPreZ()-player.posZ,errorSq=errorX*errorX+errorZ*errorZ;
        MirrorDebug.route(manager.getPlaybackRunId(),manager.getCurrentFrameIndex(),trajectory.expectedPreX(),trajectory.expectedPreZ(),player.posX,player.posZ,Math.sqrt(errorSq),player.onGround);
        if(errorSq>9d){MirrorDebug.log("ROUTE","ABORT: deviation "+String.format(java.util.Locale.ROOT,"%.2f",Double.valueOf(Math.sqrt(errorSq)))+" blocks > 3 at frame "+manager.getCurrentFrameIndex()+" (run "+manager.getPlaybackRunId()+", onGround="+player.onGround+", water="+player.isInWater()+", riding="+player.isRiding()+")");MirrorDebug.csvClose();sendMsg(L("\u00a7c\u0412\u043e\u0441\u043f\u0440\u043e\u0438\u0437\u0432\u0435\u0434\u0435\u043d\u0438\u0435 \u043e\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u043e: \u043e\u0442\u043a\u043b\u043e\u043d\u0435\u043d\u0438\u0435 \u043c\u0430\u0440\u0448\u0440\u0443\u0442\u0430 \u0431\u043e\u043b\u044c\u0448\u0435 3 \u0431\u043b\u043e\u043a\u043e\u0432.","§cPlayback stopped: route deviation is over 3 blocks.","§cВідтворення зупинено: відхилення маршруту більше 3 блоків.","§cWiedergabe gestoppt: Routenabweichung über 3 Blöcke.","§cOdtwarzanie zatrzymane: odchylenie trasy większe niż 3 bloki."));manager.stopPlayback();forceResetKeys();return false;}
        // Отклонение >3 блоков останавливает повтор всегда, даже когда коррекция маршрута выключена.
        if(!config.isRouteStabilization())return true;if(player.isRiding()||player.capabilities.isFlying||player.isElytraFlying()||player.isInWater()||player.isInLava()||player.isOnLadder())return true;double velocityX=trajectory.expectedVelocityX()-player.motionX,velocityZ=trajectory.expectedVelocityZ()-player.motionZ,velocitySq=velocityX*velocityX+velocityZ*velocityZ;if(errorSq<1.0E-4d&&velocitySq<6.4E-5d)return true;
        float positionGain=player.onGround?0.45f:0.12f,velocityGain=player.onGround?0.18f:0.05f,limit=player.onGround?0.28f:0.08f;double worldX=errorX*positionGain+velocityX*velocityGain,worldZ=errorZ*positionGain+velocityZ*velocityGain,yaw=Math.toRadians(player.rotationYaw),localForward=-worldX*Math.sin(yaw)+worldZ*Math.cos(yaw),localStrafe=worldX*Math.cos(yaw)+worldZ*Math.sin(yaw);
        adjustedForward=MathHelper.clamp(forward+MathHelper.clamp((float)localForward,-limit,limit),-1f,1f);adjustedStrafe=MathHelper.clamp(strafe+MathHelper.clamp((float)localStrafe,-limit,limit),-1f,1f);
        // Ваниль срывает спринт при moveForward<0.8 — не даём коррекции опустить ввод ниже этого порога.
        boolean sneakNow=config.isPlaybackSneak()&&(frame.hasKeyMask?frame.key(Frame.K_SNEAK):frame.sneak);
        boolean recSprint=config.isPlaybackSprint()&&(frame.sprint||(frame.hasKeyMask&&frame.key(Frame.K_SPRINT)))&&!sneakNow;
        if(recSprint&&forward>=0.8f&&adjustedForward<0.8f)adjustedForward=0.8f;
        // Отставание на полном ходу вперёд нечем догнать: ввод уже 1.0. Кратковременный спринт — единственный запас скорости.
        double fwdError=-errorX*Math.sin(yaw)+errorZ*Math.cos(yaw);
        boolean canAssist=config.isPlaybackSprint()&&!recSprint&&!sneakNow&&player.onGround&&adjustedForward>=0.8f&&!(frame.hasKeyMask&&frame.key(Frame.K_ATTACK))&&!(prevAssist&&!player.isSprinting());
        sprintAssist=canAssist&&fwdError>(prevAssist?0.05d:0.15d);
        return true;
    }
    private void resetStabilizer(){trajectory.reset();speedNoticeShown=false;sprintAssist=false;}
    /** Пока идёт операция мода, не даём ванилле открыть паузу через 500 мс после потери фокуса. Исходную настройку пользователя возвращаем без сохранения в options.txt. */
    private void updateBackgroundPolicy(){
        if(mc.gameSettings==null)return;MirrorRecorder mod=MirrorRecorder.getInstance();BaritoneReturnController r=mod==null?null:mod.getReturnController();boolean active=manager.isBusy()||(r!=null&&(r.isBusy()||r.isRecovering()));
        if(active){if(!backgroundPolicyHeld){savedPauseOnLostFocus=mc.gameSettings.pauseOnLostFocus;backgroundPolicyHeld=true;}mc.gameSettings.pauseOnLostFocus=false;}
        else releaseBackgroundPolicy();
    }
    private void releaseBackgroundPolicy(){if(backgroundPolicyHeld&&mc.gameSettings!=null)mc.gameSettings.pauseOnLostFocus=savedPauseOnLostFocus;backgroundPolicyHeld=false;}
    /** Ваниль возвращает захват мыши только по клику (runTickMouse), да и тот лишь при активном окне
     *  (setIngameFocus проверяет Display.isActive). Если в мире без открытого экрана захвата нет —
     *  возвращаем сами: эффект обязательного клика, но без клика. Чужие экраны, чат, пауза, смерть и
     *  неактивное окно не трогаем — там свободный курсор законен. */
    private void ensureIngameGrab(){if(mc.player==null||mc.world==null||mc.currentScreen!=null||mc.inGameHasFocus)return;mc.setIngameFocus();}
    @SubscribeEvent public void onClientTick(TickEvent.ClientTickEvent event){
        updateBackgroundPolicy();ensureIngameGrab();
        if(event.phase==TickEvent.Phase.START){if(manager.isPlaying()&&!mc.isGamePaused()){popInterpolatedLook();long run=manager.getPlaybackRunId();int index=manager.getCurrentFrameIndex();if(rotRun==run&&rotFrame==index){lookDYaw=0f;lookDPitch=0f;}applyPlaybackRotation();handlePlaybackInteractions();}return;}
        // Мира нет: либо переход между мирами (ждём), либо выход в меню/дисконнект (30 с без мира — стоп с сохранением).
        if(mc.world==null){if(manager.isBusy()&&++worldSuspendTicks>=WORLD_SUSPEND_LIMIT){worldSuspendTicks=0;boolean wasRec=manager.isRecording();manager.stopAll();forceResetKeys();sendMsg(wasRec?L("§eЗапись остановлена и сохранена: выход из мира.","§eRecording stopped and saved: left the world.","§eЗапис зупинено і збережено: вихід зі світу.","§eAufnahme gestoppt und gespeichert: Welt verlassen.","§eNagrywanie zatrzymane i zapisane: opuszczenie świata."):L("§eПовтор остановлен: выход из мира.","§ePlayback stopped: left the world.","§eВідтворення зупинено: вихід зі світу.","§eWiedergabe gestoppt: Welt verlassen.","§eOdtwarzanie zatrzymane: opuszczenie świata."));}return;}
        if(worldSuspendTicks>0)worldSuspendTicks=0;
        EntityPlayerSP player=mc.player;if(player==null||mc.world==null)return;if(!manager.isRecording())pendingChatMessage=null;
        int failedSlot=manager.consumeUnsavedWarningSlot();if(failedSlot>0)SoundFx.error();
        if(failedSlot>0)sendMsg(L("\u00a7c\u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u044f: \u0441\u043b\u043e\u0442 ","§cSave error: slot ","§cПомилка збереження: слот ","§cSpeicherfehler: Slot ","§cBłąd zapisu: slot ")+failedSlot+L(" \u043e\u0441\u0442\u0430\u043b\u0441\u044f \u0442\u043e\u043b\u044c\u043a\u043e \u0432 \u043f\u0430\u043c\u044f\u0442\u0438. \u041d\u0435 \u0437\u0430\u043a\u0440\u044b\u0432\u0430\u0439\u0442\u0435 \u0438\u0433\u0440\u0443."," is kept only in memory. Do not close the game."," залишився лише в пам'яті. Не закривайте гру."," ist nur noch im Speicher. Schließe das Spiel nicht."," pozostał tylko w pamięci. Nie zamykaj gry."));
        boolean nowRec=manager.isRecording(),nowPlay=manager.isPlaying();
        int limitSlot=manager.consumeLimitReachedSlot();
        if(limitSlot>0)sendMsg(L("§eЗапись остановлена: достигнут лимит ","§eRecording stopped: the frame limit of ","§eЗапис зупинено: досягнуто ліміт ","§eAufnahme gestoppt: das Limit von ","§eNagranie zatrzymane: osiągnięto limit ")+com.mirror.recorder.storage.StorageManager.MAX_FRAMES+L(" кадров (1 час). Слот сохранён."," frames (1 hour) was reached. The slot is saved."," кадрів (1 годину). Слот збережено."," Frames (1 Stunde) wurde erreicht. Der Slot ist gespeichert."," klatek (1 godzina) został osiągnięty. Slot został zapisany."));
        if(nowRec&&!sndRec)SoundFx.recordStart();
        if(!nowRec&&sndRec)SoundFx.recordStop();
        if(nowPlay&&!sndPlay){SoundFx.playStart();sndCycle=manager.getPlaybackCycle();}
        else if(nowPlay){int cyc=manager.getPlaybackCycle();if(cyc>sndCycle){sndCycle=cyc;SoundFx.cycle();}}
        sndRec=nowRec;sndPlay=nowPlay;
        if(nowRec||nowPlay||manager.hasPendingAction()){String threat=guard.check(player,config);if(threat!=null){emergencyStop(threat);return;}}else guard.standby(player);
        if((manager.isPlaying()||manager.isRecording()||manager.hasPendingAction())&&(player.isDead||player.getHealth()<=0f)){manager.stopAll();forceResetKeys();return;}
        if(manager.hasPendingAction()){
            int prev=manager.getDelaySecondsRemaining();int sec=manager.tickDelay();
            if(sec>0&&sec!=prev){sendMsg("\u00a7e"+sec+"...");SoundFx.countdown(sec);}else if(sec==0)sendMsg(L("\u00a7a\u0421\u0442\u0430\u0440\u0442!","§aStart!","§aСтарт!","§aStart!","§aStart!"));return;
        }
        // Удержание с открытым экраном гоняем в конце тика: Esc стирает состояния клавиш в середине тика (unPressAllKeys), а ванильные повторы при экране заглушены контекстом IN_GAME и проверкой currentScreen. END-фаза покрывает и тик, в котором экран открыли. Таймер/внутренние проверки не дают двойных срабатываний.
        if(manager.isPlaying()&&mc.currentScreen!=null){if(heldAttack)driveScreenAttackHold();if(heldUse)driveScreenUseHold();}
        if(useTimerAlign){useTimerAlign=false;if(rightClickDelayField!=null)try{if(rightClickDelayField.getInt(mc)==3)rightClickDelayField.setInt(mc,4);}catch(Exception e){warnClickFailure("Cannot align use timer",e);}}
        // Мир заморожен (одиночная игра + пауза): запись и повтор обязаны замереть вместе с ним, иначе кадры жрутся в замороженном мире и точность уходит в ноль. На сервере мир не замирает — там ничего не меняется.
        if(manager.isRecording()){if(!mc.isGamePaused())handleRecord(player);}else if(manager.isPlaying()){if(!mc.isGamePaused())handlePlayback(player);}else if(keysNeedReset)forceResetKeys();
    }
    private void handlePlaybackInteractions(){
        if(mc.player==null||mc.world==null||mc.player.isDead||mc.player.getHealth()<=0f)return;
        if(useHoldFallbackDelay>0)--useHoldFallbackDelay;
        boolean interact=config.isPlaybackInteraction();
        if(!interact&&(heldAttack||heldUse)){heldAttack=false;heldUse=false;lastMask=0;forceReleaseClicks();}
        long run=manager.getPlaybackRunId();int index=manager.getCurrentFrameIndex();
        boolean fresh=interactionRun!=run||interactionFrame!=index;
        boolean blocked=config.isStopOnMove()&&manager.getSimulateCooldown()<=0&&isRealMovement();
        Frame frame=manager.getCurrentPlaybackFrame();
        if(fresh&&frame!=null&&!blocked){
            interactionRun=run;interactionFrame=index;
            java.util.List<Frame> pending=manager.consumeSkippedFrames();
            // Первый кадр запуска: состояние кнопок берём из него самого. При lastMask=0 зажатая в записи
            // кнопка выглядит как новое нажатие и на нулевом кадре стреляет лишний клик. Настоящее нажатие
            // ровно на этом кадре приходит через frame.leftClick/attackClicks и не теряется.
            if(maskRun!=run){Frame seed=pending.isEmpty()?frame:pending.get(0);maskRun=run;lastMask=seed.hasKeyMask?seed.keyMask:0;}
            for(int i=0;i<pending.size();i++)replayFrameEvents(pending.get(i),false,interact);
            replayFrameEvents(frame,true,interact);
        }
        if(interact){processGuiQueue();processGuiKeyQueue();closeUnexpectedScreen(frame);}
    }
    /** Один кадр записи: сначала дискретные события, затем клики и чат. При ускорении пропущенные кадры проходят тем же путём, поэтому события не теряются. */
    private void replayFrameEvents(Frame frame,boolean current,boolean interact){
        if(frame==null)return;
        // Граница мира: опоры поворота и стабилизации от старого мира невалидны, очереди окон мертвы.
        if(frame.worldReset){rotAnchorSet=false;resetStabilizer();guiQueue.clear();guiKeyQueue.clear();}
        if(config.isPlaybackChat()&&frame.chatMessage!=null&&!frame.chatMessage.isEmpty()){
            if(manager.isSlotImported(manager.getActiveOperationSlot()))warnImportedChat();
            else if(chatQueue.size()<CHAT_QUEUE_LIMIT)chatQueue.add(frame.chatMessage);
            else{LOG.warn("Chat queue overflow, recorded message dropped");MirrorDebug.log("CHAT","queue overflow at limit "+CHAT_QUEUE_LIMIT+", dropped a message of "+frame.chatMessage.length()+" chars");}
        }
        if(!interact||mc.gameSettings==null)return;
        KeyBinding attack=mc.gameSettings.keyBindAttack,use=mc.gameSettings.keyBindUseItem;
        if(!frame.hasKeyMask){
            heldAttack=false;heldUse=false;lastMask=0;
            KeyBinding.setKeyBindState(attack.getKeyCode(),false);KeyBinding.setKeyBindState(use.getKeyCode(),false);
            for(int i=0;i<frame.attackClicks;i++)fireRecordedClick(frame,attack,false);
            for(int i=0;i<frame.useClicks;i++)fireRecordedClick(frame,use,true);
            return;
        }
        int mask=frame.keyMask;
        // A held-state bit cannot distinguish a hold from another press on the next tick. The explicit mouse events can.
        boolean attackPress=frame.leftClick||rising(mask,Frame.K_ATTACK),usePress=frame.rightClick||rising(mask,Frame.K_USE);
        int attackShots=frame.attackClicks>0?frame.attackClicks:(attackPress?1:0),useShots=frame.useClicks>0?frame.useClicks:(usePress?1:0);
        // Маска пропущенных кадров не нужна: их события доставляются по порядку через consumeSkippedFrames.
        applyHotbar(frame.hotbarSlot);
        // При открытом окне ваниль не читает хоткеи: накопленное нажатие выстреливало после закрытия окна (фантомный дроп/инвентарь). Действия окна повторяются через guiKeys, мировые клавиши — только когда окна нет ни в записи, ни сейчас.
        boolean guiKeyCtx=frame.guiKeys.length>0||(frame.hasScreenState&&frame.openScreen!=null),hasGuiClose=false;
        for(int i=0;i+6<=frame.guiKeys.length;i+=6)if(frame.guiKeys[i]==GA_CLOSE){hasGuiClose=true;break;}
        if(mc.currentScreen==null&&!guiKeyCtx){if((mask&Frame.K_DROP)!=0&&(lastMask&Frame.K_DROP)==0&&mc.player!=null&&!mc.player.isSpectator())mc.player.dropItem(frame.dropAll);pulse(mc.gameSettings.keyBindSwapHands,(mask&Frame.K_SWAP)!=0,(lastMask&Frame.K_SWAP)!=0);pulse(mc.gameSettings.keyBindPickBlock,(mask&Frame.K_PICK)!=0,(lastMask&Frame.K_PICK)!=0);}
        if(mc.currentScreen==null&&!hasGuiClose)pulse(mc.gameSettings.keyBindInventory,(mask&Frame.K_INVENTORY)!=0,(lastMask&Frame.K_INVENTORY)!=0);
        boolean guiFrame=frame.guiClick&&frame.guiScreen!=null&&!frame.guiScreen.isEmpty();
        if(guiFrame){
            KeyBinding.setKeyBindState(attack.getKeyCode(),false);KeyBinding.setKeyBindState(use.getKeyCode(),false);
            if(frame.guiButton==2)queueGuiClick(frame,2);
            else{if(attackPress)queueGuiClick(frame,0);if(usePress)queueGuiClick(frame,1);}
            heldAttack=false;heldUse=false;
        }else if(current){
            boolean wantAttack=(mask&Frame.K_ATTACK)!=0,wantUse=(mask&Frame.K_USE)!=0;
            applyHold(attack,wantAttack);applyHold(use,wantUse);
            for(int i=0;i<attackShots;i++)fireRecordedClick(frame,attack,false);
            for(int i=0;i<useShots;i++)fireRecordedClick(frame,use,true);
            heldAttack=wantAttack;heldUse=wantUse;
        }else{
            for(int i=0;i<attackShots;i++)fireRecordedClick(frame,attack,false);
            for(int i=0;i<useShots;i++)fireRecordedClick(frame,use,true);
            heldAttack=(mask&Frame.K_ATTACK)!=0;heldUse=(mask&Frame.K_USE)!=0;
        }
        if(frame.guiKeys.length>0&&frame.openScreen!=null)queueGuiKeys(frame);
        lastMask=mask;
    }
    private boolean rising(int mask,int bit){return (mask&bit)!=0&&(lastMask&bit)==0;}
    /** Смена предмета повторяется как нажатие клавиши слота, а не как запись состояния инвентаря. */
    private void applyHotbar(int slot){
        if(slot<0||slot>8||lastHotbar==slot)return;lastHotbar=slot;
        if(mc.player!=null&&mc.player.inventory!=null&&mc.player.inventory.currentItem!=slot){
            mc.player.inventory.currentItem=slot;
            if(mc.player.connection!=null)mc.player.connection.sendPacket(new net.minecraft.network.play.client.CPacketHeldItemChange(slot));
        }
    }
    private static final class GuiClickEvent{
        final String screen;final float x,y;final int button,cx,cy;final boolean hasCenter,shift;int waited=0;
        GuiClickEvent(String screen,float x,float y,int button,boolean hasCenter,int cx,int cy,boolean shift){this.screen=screen;this.x=x;this.y=y;this.button=button;this.hasCenter=hasCenter;this.cx=cx;this.cy=cy;this.shift=shift;}
    }
    /** Любой реально открытый экран, включая чат и интерфейс мода, может принять записанный GUI-клик. */
    /** Клавиша, нажатая в чужом окне: ждёт своего экрана так же, как клик. */
    private static final class GuiKeyEvent{
        final String screen;final int[] data;int waited=0;
        GuiKeyEvent(String screen,int[] data){this.screen=screen;this.data=data;}
    }
    private static boolean replayableScreen(String className){return className!=null&&!className.isEmpty();}
    private void queueGuiClick(Frame frame,int button){
        if(frame==null||!replayableScreen(frame.guiScreen))return;
        if(guiQueue.size()>=GUI_QUEUE_LIMIT){MirrorDebug.log("GUI","click queue overflow, dropped click for "+frame.guiScreen);return;}
        guiQueue.add(new GuiClickEvent(frame.guiScreen,frame.guiX,frame.guiY,button,frame.hasGuiCenter,frame.guiCenterX,frame.guiCenterY,frame.guiShift));
    }
    /** Клики в GUI отдаются строго по порядку: если нужный экран ещё не открылся, клик ждёт его и не пропадает. */
    private void processGuiQueue(){
        while(!guiQueue.isEmpty()){
            GuiClickEvent c=guiQueue.peek();
            GuiScreen screen=mc.currentScreen;
            if(screen!=null&&screen.getClass().getName().equals(c.screen)){guiQueue.poll();if(replayableScreen(c.screen))deliverGuiClick(screen,c);continue;}
            c.waited++;
            if(c.waited>GUI_CLICK_TTL){guiQueue.poll();MirrorDebug.log("GUI","dropped recorded click: "+c.screen+" did not open");continue;}
            break;
        }
    }
    private void deliverGuiClick(GuiScreen screen,GuiClickEvent c){
        if(!guiClickMethodResolved){resolveGuiClickMethod();MirrorDebug.probe("GuiScreen.mouseClicked",guiMouseClickedMethod!=null,"GUI click replay");MirrorDebug.probe("GuiScreen.mouseReleased",guiMouseReleasedMethod!=null,"GUI release replay");}
        if(guiMouseClickedMethod==null)return;
        int x,y;
        if(c.hasCenter){x=MathHelper.clamp(screen.width/2+c.cx,0,Math.max(0,screen.width-1));y=MathHelper.clamp(screen.height/2+c.cy,0,Math.max(0,screen.height-1));}
        else{x=MathHelper.clamp((int)(c.x*Math.max(1,screen.width)),0,Math.max(0,screen.width-1));y=MathHelper.clamp((int)(c.y*Math.max(1,screen.height))-1,0,Math.max(0,screen.height-1));}
        if(c.button==2&&deliverClone(screen,x,y))return;
        if(c.shift&&deliverQuickMove(screen,x,y,c.button))return;
        try{guiMouseClickedMethod.invoke(screen,Integer.valueOf(x),Integer.valueOf(y),Integer.valueOf(c.button));}
        catch(Exception e){warnClickFailure("Recorded GUI click failed",e);return;}
        if(guiMouseReleasedMethod==null)return;
        try{guiMouseReleasedMethod.invoke(screen,Integer.valueOf(x),Integer.valueOf(y),Integer.valueOf(c.button));}
        catch(Exception e){warnClickFailure("Recorded GUI release failed",e);}
    }
    private void queueGuiKeys(Frame frame){
        // Как и в очереди кликов: при переполнении теряем новое событие, а не старейшее — порядок уже стоящих не ломается.
        if(guiKeyQueue.size()>=GUI_KEY_LIMIT){MirrorDebug.log("GUI","key queue overflow, dropped key event for "+frame.openScreen);return;}
        guiKeyQueue.add(new GuiKeyEvent(frame.openScreen,frame.guiKeys));
    }
    private void processGuiKeyQueue(){
        if(guiKeyQueue.isEmpty())return;
        GuiScreen screen=mc.currentScreen;int guard=0;
        while(!guiKeyQueue.isEmpty()&&guard++<8){
            GuiKeyEvent e=guiKeyQueue.peek();
            if(screen!=null&&screen.getClass().getName().equals(e.screen)){guiKeyQueue.poll();deliverGuiKeys(screen,e);screen=mc.currentScreen;continue;}
            if(++e.waited>GUI_KEY_TTL){guiKeyQueue.poll();continue;}
            break;
        }
    }
    /** Повтор нажатий в окне: печать идёт через keyTyped, действия слотов — напрямую в handleMouseClick, потому что ваниль смотрит на физический курсор и физический Ctrl. */
    private void deliverGuiKeys(GuiScreen screen,GuiKeyEvent e){
        if(!guiClickMethodResolved){resolveGuiClickMethod();MirrorDebug.probe("GuiScreen.keyTyped",guiKeyTypedMethod!=null,"GUI key replay");}
        int[] d=e.data;
        for(int i=0;i+6<=d.length;i+=6){
            int action=d[i],key=d[i+1],aux=d[i+2],flags=d[i+3],cx=d[i+4],cy=d[i+5];
            if(action==GA_TYPE){if(guiKeyTypedMethod!=null)try{guiKeyTypedMethod.invoke(screen,Character.valueOf((char)aux),Integer.valueOf(key));}catch(Exception ex){warnClickFailure("Recorded key replay failed",ex);}}
            else if(action==GA_CLOSE){closeRecordedScreen(screen);return;}
            else deliverContainerKey(screen,action,aux,(flags&2)!=0,cx,cy);
        }
    }
    private void deliverContainerKey(GuiScreen screen,int action,int aux,boolean ctrl,int cx,int cy){
        if(!(screen instanceof net.minecraft.client.gui.inventory.GuiContainer))return;
        if(!quickMoveResolved)resolveQuickMove();
        if(handleMouseClickMethod==null||slotAtPositionMethod==null||mc.player==null)return;
        try{
            int x=screen.width/2+cx,y=screen.height/2+cy;
            Object slot=slotAtPositionMethod.invoke(screen,Integer.valueOf(x),Integer.valueOf(y));
            if(!(slot instanceof net.minecraft.inventory.Slot))return;
            net.minecraft.inventory.Slot s=(net.minecraft.inventory.Slot)slot;
            if(action==GA_DROP){if(s.getHasStack())handleMouseClickMethod.invoke(screen,s,Integer.valueOf(s.slotNumber),Integer.valueOf(ctrl?1:0),net.minecraft.inventory.ClickType.THROW);}
            else if(action==GA_PICK){if(s.getHasStack())handleMouseClickMethod.invoke(screen,s,Integer.valueOf(s.slotNumber),Integer.valueOf(0),net.minecraft.inventory.ClickType.CLONE);}
            else if(action==GA_SWAP){if(mc.player.inventory.getItemStack().isEmpty())handleMouseClickMethod.invoke(screen,s,Integer.valueOf(s.slotNumber),Integer.valueOf(aux),net.minecraft.inventory.ClickType.SWAP);}
        }catch(Exception e2){warnClickFailure("Recorded container key failed",e2);}
    }
    /** Средний клик в контейнере ваниль определяет через привязку pick-block, поэтому вызываем CLONE напрямую: привязка могла быть переназначена. */
    private boolean deliverClone(GuiScreen screen,int x,int y){
        if(!quickMoveResolved)resolveQuickMove();
        if(handleMouseClickMethod==null||slotAtPositionMethod==null)return false;
        try{
            Object slot=slotAtPositionMethod.invoke(screen,Integer.valueOf(x),Integer.valueOf(y));
            if(!(slot instanceof net.minecraft.inventory.Slot))return false;
            net.minecraft.inventory.Slot s=(net.minecraft.inventory.Slot)slot;
            if(s.getHasStack())handleMouseClickMethod.invoke(screen,s,Integer.valueOf(s.slotNumber),Integer.valueOf(0),net.minecraft.inventory.ClickType.CLONE);
            return true;
        }catch(Exception e){warnClickFailure("Recorded middle click failed",e);return false;}
    }
    /** Закрытие как в ванили: контейнер шлёт пакет на сервер, книга и табличка закрываются локально (табличка при этом отправляет текст в onGuiClosed). */
    private void closeRecordedScreen(GuiScreen screen){
        if(screen instanceof net.minecraft.client.gui.inventory.GuiContainer){if(mc.player!=null)mc.player.closeScreen();}
        else mc.displayGuiScreen(null);
    }
    private int[] flattenGuiKeys(){
        int n=pendingGuiKeys.size();if(n==0)return null;
        int[] flat=new int[n*6];
        for(int i=0;i<n;i++)System.arraycopy(pendingGuiKeys.get(i),0,flat,i*6,6);
        return flat;
    }
    /** Shift-клик в контейнере ваниль определяет по физической клавише, а при повторе она не нажата: без этого предмет не перекладывается, а зависает на курсоре. */
    private boolean deliverQuickMove(GuiScreen screen,int x,int y,int button){
        if(!(screen instanceof net.minecraft.client.gui.inventory.GuiContainer))return false;
        if(!quickMoveResolved){resolveQuickMove();MirrorDebug.probe("GuiContainer.handleMouseClick",handleMouseClickMethod!=null,"shift-click replay");MirrorDebug.probe("GuiContainer.getSlotAtPosition",slotAtPositionMethod!=null,"shift-click slot lookup");}
        if(handleMouseClickMethod==null||slotAtPositionMethod==null)return false;
        try{
            Object slot=slotAtPositionMethod.invoke(screen,Integer.valueOf(x),Integer.valueOf(y));
            if(!(slot instanceof net.minecraft.inventory.Slot))return false;
            int id=((net.minecraft.inventory.Slot)slot).slotNumber;
            handleMouseClickMethod.invoke(screen,slot,Integer.valueOf(id),Integer.valueOf(button),net.minecraft.inventory.ClickType.QUICK_MOVE);
            return true;
        }catch(Exception e){warnClickFailure("Recorded shift-click failed",e);return false;}
    }
    private void resolveQuickMove(){
        quickMoveResolved=true;
        for(Method m:net.minecraft.client.gui.inventory.GuiContainer.class.getDeclaredMethods()){String n=m.getName();Class<?>[] a=m.getParameterTypes();
            if(a.length==4&&a[0]==net.minecraft.inventory.Slot.class&&a[1]==int.class&&a[2]==int.class&&a[3]==net.minecraft.inventory.ClickType.class&&(n.equals("handleMouseClick")||n.equals("func_184098_a")))handleMouseClickMethod=m;
            else if(a.length==2&&a[0]==int.class&&a[1]==int.class&&m.getReturnType()==net.minecraft.inventory.Slot.class&&(n.equals("getSlotAtPosition")||n.equals("func_146975_c")))slotAtPositionMethod=m;}
        try{if(handleMouseClickMethod!=null)handleMouseClickMethod.setAccessible(true);if(slotAtPositionMethod!=null)slotAtPositionMethod.setAccessible(true);}
        catch(Exception e){handleMouseClickMethod=null;slotAtPositionMethod=null;warnClickFailure("Cannot access container click methods",e);}
    }
    /** Чистый повтор состояния экрана: окно, которого в записи в этот момент не было, закрываем сами — иначе интерфейс жителя или сундука висит до конца повтора и глушит остальное. */
    private void closeUnexpectedScreen(Frame frame){
        GuiScreen screen=mc.currentScreen;
        boolean container=screen instanceof net.minecraft.client.gui.inventory.GuiContainer;
        boolean closable=container||screen instanceof net.minecraft.client.gui.GuiScreenBook||screen instanceof net.minecraft.client.gui.inventory.GuiEditSign;
        if(frame==null||screen==null||mc.player==null||!guiQueue.isEmpty()||!guiKeyQueue.isEmpty()||!closable){screenMismatchTicks=0;return;}
        String name=screen.getClass().getName();
        if(name.startsWith("com.mirror.recorder.gui.")){screenMismatchTicks=0;return;}
        if(frame.hasScreenState?name.equals(frame.openScreen):screenClickAhead(name)){screenMismatchTicks=0;return;}
        if(++screenMismatchTicks<SCREEN_CLOSE_DELAY)return;
        screenMismatchTicks=0;MirrorDebug.log("GUI","closing screen absent from the recording: "+name);
        if(container)mc.player.closeScreen();else mc.displayGuiScreen(null);
    }
    /** Записи формата 2 открытый экран не хранят: там ориентир — записанные клики этого же окна впереди. */
    private boolean screenClickAhead(String name){
        int count=manager.copyPlaybackFramesAhead(lookAhead);
        for(int i=0;i<count;i++){Frame f=lookAhead[i];if(f!=null&&f.guiClick&&name.equals(f.guiScreen))return true;}
        return false;
    }
    /** Взмах ровно как в ваниле: одно нажатие — один взмах; удержание обрабатывает сама игра (копание идёт со своим ритмом). */
    private void applyHold(KeyBinding key,boolean down){if(key!=null)KeyBinding.setKeyBindState(key.getKeyCode(),down);}
    private void forceReleaseClicks(){if(mc.gameSettings==null)return;restorePhysical(mc.gameSettings.keyBindAttack);restorePhysical(mc.gameSettings.keyBindUseItem);}
    private void pulse(KeyBinding key,boolean down,boolean was){if(key==null||!down||was)return;KeyBinding.onTick(key.getKeyCode());}
    /** Дискретный клик вызываем напрямую: очередь нажатий KeyBinding стирается unPressAllKeys при открытии паузы/инвентаря, а на сервере мир в этот момент продолжает тикать. Прицел обновляем перед кликом, чтобы попадание соответствовало текущему кадру. */
    private void fireRecordedClick(Frame frame,KeyBinding key,boolean right){
        drainPressQueue(key);if(frame.guiClick&&replayableScreen(frame.guiScreen)){queueGuiClick(frame,right?1:0);return;}
        if(mc.player==null||mc.world==null)return;
        if(!clickMethodsResolved){resolveClickMethods();MirrorDebug.probe("Minecraft.clickMouse",clickMouseMethod!=null,"world click replay");MirrorDebug.probe("Minecraft.rightClickMouse",rightClickMouseMethod!=null,"world use replay");MirrorDebug.probe("Minecraft.leftClickCounter",leftClickCounterField!=null,"left click unblock");MirrorDebug.probe("Minecraft.sendClickBlockToController",sendClickBlockMethod!=null,"held attack with open screen");}
        if(mc.entityRenderer!=null)mc.entityRenderer.getMouseOver(1.0F);
        if(mc.currentScreen!=null&&!right&&leftClickCounterField!=null)try{leftClickCounterField.setInt(mc,0);}catch(Exception e){warnClickFailure("Cannot unblock recorded left click",e);}
        Method method=right?rightClickMouseMethod:clickMouseMethod;
        if(method!=null)try{method.invoke(mc);if(right)useTimerAlign=true;}catch(Exception e){warnClickFailure("Recorded world click failed",e);}
    }

    private void resolveClickMethods(){
        clickMethodsResolved=true;
        for(Method m:Minecraft.class.getDeclaredMethods()){String n=m.getName();Class<?>[] a=m.getParameterTypes();
            if(a.length==0&&(n.equals("clickMouse")||n.equals("func_147116_af")))clickMouseMethod=m;
            else if(a.length==0&&(n.equals("rightClickMouse")||n.equals("func_147121_ag")))rightClickMouseMethod=m;
            else if(a.length==1&&a[0]==boolean.class&&(n.equals("sendClickBlockToController")||n.equals("func_147115_a")))sendClickBlockMethod=m;}
        for(Field f:Minecraft.class.getDeclaredFields()){String n=f.getName();if(f.getType()!=int.class)continue;
            if(n.equals("leftClickCounter")||n.equals("field_71429_W"))leftClickCounterField=f;
            else if(n.equals("rightClickDelayTimer")||n.equals("field_71467_ac"))rightClickDelayField=f;}
        try{if(clickMouseMethod!=null)clickMouseMethod.setAccessible(true);if(rightClickMouseMethod!=null)rightClickMouseMethod.setAccessible(true);if(sendClickBlockMethod!=null)sendClickBlockMethod.setAccessible(true);if(leftClickCounterField!=null)leftClickCounterField.setAccessible(true);if(rightClickDelayField!=null)rightClickDelayField.setAccessible(true);}catch(Exception e){clickMouseMethod=null;rightClickMouseMethod=null;sendClickBlockMethod=null;leftClickCounterField=null;rightClickDelayField=null;warnClickFailure("Cannot access Minecraft click methods",e);}
    }
    /** Открытый экран глушит ванильное ломание блоков (проверка currentScreen). На сервере мир тикает, поэтому повторяем вызов напрямую. */
    private void driveScreenAttackHold(){
        if(mc.isGamePaused()||mc.player==null||mc.world==null)return;
        if(!clickMethodsResolved)resolveClickMethods();
        if(leftClickCounterField!=null)try{leftClickCounterField.setInt(mc,0);}catch(Exception e){warnClickFailure("Cannot unblock held left click",e);}
        if(mc.entityRenderer!=null)mc.entityRenderer.getMouseOver(1.0F);
        if(sendClickBlockMethod!=null)try{sendClickBlockMethod.invoke(mc,Boolean.TRUE);}catch(Exception e){warnClickFailure("Held attack with open screen failed",e);}
    }
    /** Открытый экран глушит ванильный повтор ПКМ через контекст клавиши. Повторяем тот же ритм вручную: вызов, когда таймер задержки дошёл до нуля и рука свободна. */
    private void driveScreenUseHold(){
        EntityPlayerSP player=mc.player;if(mc.isGamePaused()||player==null||mc.world==null||player.isHandActive())return;
        if(!clickMethodsResolved)resolveClickMethods();
        int delay;
        if(rightClickDelayField!=null){try{delay=rightClickDelayField.getInt(mc);}catch(Exception e){delay=useHoldFallbackDelay;}}
        else delay=useHoldFallbackDelay;
        if(delay>0)return;
        if(mc.entityRenderer!=null)mc.entityRenderer.getMouseOver(1.0F);
        if(rightClickMouseMethod!=null){try{rightClickMouseMethod.invoke(mc);useHoldFallbackDelay=4;}catch(Exception e){warnClickFailure("Held use with open screen failed",e);}}
    }
    private void resolveGuiClickMethod(){guiClickMethodResolved=true;
        for(Method m:GuiScreen.class.getDeclaredMethods()){String n=m.getName();Class<?>[] a=m.getParameterTypes();
            if(a.length!=3||a[0]!=int.class||a[1]!=int.class||a[2]!=int.class)continue;
            if(n.equals("mouseClicked")||n.equals("func_73864_a"))guiMouseClickedMethod=m;
            else if(n.equals("mouseReleased")||n.equals("func_146286_i"))guiMouseReleasedMethod=m;
        }
        for(Method m:GuiScreen.class.getDeclaredMethods()){String n=m.getName();Class<?>[] a=m.getParameterTypes();
            if(a.length==2&&a[0]==char.class&&a[1]==int.class&&(n.equals("keyTyped")||n.equals("func_73869_a")))guiKeyTypedMethod=m;}
        try{if(guiMouseClickedMethod!=null)guiMouseClickedMethod.setAccessible(true);if(guiMouseReleasedMethod!=null)guiMouseReleasedMethod.setAccessible(true);if(guiKeyTypedMethod!=null)guiKeyTypedMethod.setAccessible(true);}
        catch(Exception e){guiMouseClickedMethod=null;guiMouseReleasedMethod=null;guiKeyTypedMethod=null;warnClickFailure("Cannot access GuiScreen click methods",e);}
    }
    private void warnClickFailure(String text,Exception e){MirrorDebug.log("CLICK","failure: "+text+" ("+(e==null?"unknown":e.toString())+")");if(!clickMethodWarning){clickMethodWarning=true;LOG.error(text,e);}}
    private void handleRecord(EntityPlayerSP player){
        long idx=manager.getRecordingTickCounter();boolean jump=false;float fwd=0,str=0;boolean sneak=false;
        if(player.movementInput!=null){
            jump=config.isRecordJump()&&player.movementInput.jump;sneak=config.isRecordSneak()&&player.movementInput.sneak;
            if(config.isRecordMovement()){
                fwd=(mc.gameSettings.keyBindForward.isKeyDown()?1f:0f)-(mc.gameSettings.keyBindBack.isKeyDown()?1f:0f);
                str=(mc.gameSettings.keyBindLeft.isKeyDown()?1f:0f)-(mc.gameSettings.keyBindRight.isKeyDown()?1f:0f);
            }
        }
        String chat=null;if(config.isRecordChat())chat=pendingChatMessage;pendingChatMessage=null;
        boolean sprint=config.isRecordSprint()&&player.isSprinting();
        boolean lmb=config.isRecordInteraction()&&manager.getLeftClickState();int lmbN=lmb?Math.max(1,manager.getLeftClickCount()):0;
        boolean rmb=config.isRecordInteraction()&&manager.getRightClickState();int rmbN=rmb?Math.max(1,manager.getRightClickCount()):0;boolean guiClick=config.isRecordInteraction()&&(lmb||rmb||pendingGuiButton==2)&&pendingGuiClick;int guiBtn=guiClick?pendingGuiButton:0;int[] guiKeys=config.isRecordInteraction()?flattenGuiKeys():null;float guiX=pendingGuiX,guiY=pendingGuiY;String guiScreen=pendingGuiScreen;boolean guiCenter=pendingGuiCenter;int guiCX=pendingGuiCX,guiCY=pendingGuiCY;boolean guiShift=guiClick&&pendingGuiShift;String openScreen=openScreenName();
        if(idx<=0){recRotInit=false;pendingGuiClick=false;pendingGuiCenter=false;pendingGuiShift=false;pendingGuiScreen=null;pendingGuiButton=0;pendingGuiKeys.clear();}
        int mask=0;net.minecraft.client.settings.GameSettings gs=mc.gameSettings;
        if(gs!=null){if(gs.keyBindAttack.isKeyDown()||lmb)mask|=Frame.K_ATTACK;if(gs.keyBindUseItem.isKeyDown()||rmb)mask|=Frame.K_USE;if(gs.keyBindForward.isKeyDown())mask|=Frame.K_FORWARD;if(gs.keyBindBack.isKeyDown())mask|=Frame.K_BACK;if(gs.keyBindLeft.isKeyDown())mask|=Frame.K_LEFT;if(gs.keyBindRight.isKeyDown())mask|=Frame.K_RIGHT;if(gs.keyBindJump.isKeyDown())mask|=Frame.K_JUMP;if(gs.keyBindSneak.isKeyDown())mask|=Frame.K_SNEAK;if(gs.keyBindSprint.isKeyDown())mask|=Frame.K_SPRINT;if(gs.keyBindDrop.isKeyDown())mask|=Frame.K_DROP;if(gs.keyBindSwapHands.isKeyDown())mask|=Frame.K_SWAP;if(gs.keyBindInventory.isKeyDown())mask|=Frame.K_INVENTORY;if(gs.keyBindPickBlock.isKeyDown())mask|=Frame.K_PICK;}
        boolean dropAll=(mask&Frame.K_DROP)!=0&&GuiScreen.isCtrlKeyDown();
        int hotbar=player.inventory!=null?MathHelper.clamp(player.inventory.currentItem,0,8):-1;
        float dy=0f,dp=0f;if(recRotInit){dy=MathHelper.wrapDegrees(player.rotationYaw-lastRecYaw);dp=player.rotationPitch-lastRecPitch;}lastRecYaw=player.rotationYaw;lastRecPitch=player.rotationPitch;recRotInit=true;
        boolean worldReset=worldResetPending;worldResetPending=false;
        Frame frame=Frame.builder().x(player.posX).y(player.posY).z(player.posZ).yaw(player.rotationYaw).pitch(player.rotationPitch)
            .motionX(player.motionX).motionY(player.motionY).motionZ(player.motionZ)
            .onGround(player.onGround).sneak(sneak).sprint(sprint).leftClick(lmb).rightClick(rmb).attackClicks(lmbN).useClicks(rmbN).jump(jump)
            .moveForward(fwd).moveStrafe(str).keyMask(mask).hotbarSlot(hotbar).dYaw(dy).dPitch(dp).guiClick(guiClick).guiX(guiX).guiY(guiY).guiScreen(guiScreen).guiCenter(guiClick&&guiCenter,guiCX,guiCY).guiShift(guiShift).guiButton(guiBtn).guiKeys(guiKeys).openScreen(openScreen).screenState(true).dropAll(dropAll).worldReset(worldReset).chatMessage(chat).tickIndex(idx).build();
        manager.setLeftClickState(false);manager.setRightClickState(false);pendingGuiClick=false;pendingGuiCenter=false;pendingGuiShift=false;pendingGuiScreen=null;pendingGuiButton=0;pendingGuiKeys.clear();manager.recordTick(frame);
    }
    /** Экран этого тика для записи: собственный интерфейс мода в запись не попадает. */
    private String openScreenName(){GuiScreen s=mc.currentScreen;if(s==null)return null;String n=s.getClass().getName();return n.startsWith("com.mirror.recorder.gui.")?null:n;}
    private void handlePlayback(EntityPlayerSP player){
        if(config.isStopOnMove()&&manager.getSimulateCooldown()<=0&&isRealMovement()){MirrorDebug.log("PLAYBACK","STOP: manual movement detected at frame "+manager.getCurrentFrameIndex()+" (run "+manager.getPlaybackRunId()+")");MirrorDebug.csvClose();resetPlayback();manager.stopPlayback();SoundFx.stop();sendMsg(L("\u00a7e\u0412\u043e\u0441\u043f\u0440\u043e\u0438\u0437\u0432\u0435\u0434\u0435\u043d\u0438\u0435 \u043e\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u043e: \u043e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d\u043e \u0440\u0443\u0447\u043d\u043e\u0435 \u0434\u0432\u0438\u0436\u0435\u043d\u0438\u0435.","§ePlayback stopped: manual movement detected.","§eВідтворення зупинено: виявлено ручний рух.","§eWiedergabe gestoppt: manuelle Bewegung erkannt.","§eOdtwarzanie zatrzymane: wykryto ręczny ruch."));return;}
        long run=manager.getPlaybackRunId();int frameIndex=manager.getCurrentFrameIndex();
        if(appliedRun!=run||appliedFrame!=frameIndex){
            // Ввод не применяется к игроку (например, спектатор за другой сущностью: InputUpdateEvent не приходит).
            // Короткие расхождения терпимы, но вечно висящий повтор — баг: через полсекунды честно останавливаем.
            if(++stallTicks>=STALL_LIMIT){stallTicks=0;MirrorDebug.log("PLAYBACK","STOP: player input not applied for "+STALL_LIMIT+" ticks at frame "+frameIndex);resetPlayback();manager.stopPlayback();SoundFx.stop();sendMsg(L("§eПовтор остановлен: управление не применяется к игроку (например, наблюдение за другой сущностью).","§ePlayback stopped: input is not reaching the player (e.g. spectating another entity).","§eВідтворення зупинено: керування не застосовується до гравця (наприклад, спостереження за іншою сутністю).","§eWiedergabe gestoppt: Die Eingabe erreicht den Spieler nicht (z. B. Beobachten einer anderen Entität).","§eOdtwarzanie zatrzymane: sterowanie nie dociera do gracza (np. obserwacja innej istoty)."));}
            return;
        }
        stallTicks=0;
        appliedRun=-1L;appliedFrame=-1;
        if(!chatQueue.isEmpty()){long nowMs=System.currentTimeMillis();if(nowMs-lastChatSentAt>=CHAT_SEND_INTERVAL_MS){String msg=chatQueue.poll();if(msg!=null&&!msg.isEmpty()){player.sendChatMessage(msg);lastChatSentAt=nowMs;}}}
        if(!manager.advancePlayback()){SoundFx.finish();resetPlayback();}
    }
    /** Чужая запись не говорит от вашего имени: чат и команды из импортированного слота не отправляются. */
    private void warnImportedChat(){
        if(importedChatWarned)return;importedChatWarned=true;
        MirrorDebug.log("CHAT","imported recording: chat and commands are not sent");
        sendMsg(L("§eСообщения и команды из импортированной записи не отправляются.","§eChat and commands from an imported recording are not sent.","§eПовідомлення та команди з імпортованого запису не надсилаються.","§eChat und Befehle aus einer importierten Aufnahme werden nicht gesendet.","§eCzat i komendy z zaimportowanego nagrania nie są wysyłane."));
    }
    private void resetPlayback(){forceResetKeys();}
    private void forceResetKeys(){
        stallTicks=0;
        if(mc.gameSettings!=null){restorePhysical(mc.gameSettings.keyBindAttack);restorePhysical(mc.gameSettings.keyBindUseItem);if(mc.gameSettings.keyBindsHotbar!=null)for(KeyBinding h:mc.gameSettings.keyBindsHotbar)drainPressQueue(h);drainPressQueue(mc.gameSettings.keyBindDrop);drainPressQueue(mc.gameSettings.keyBindSwapHands);drainPressQueue(mc.gameSettings.keyBindPickBlock);drainPressQueue(mc.gameSettings.keyBindInventory);}if(mc.player!=null)mc.player.setSprinting(false);wantSprint=false;sprintAssist=false;chatQueue.clear();lastChatSentAt=0L;appliedRun=-1L;appliedFrame=-1;interactionRun=-1L;interactionFrame=-1;guiQueue.clear();lastHotbar=-1;heldAttack=false;heldUse=false;lastMask=0;rotRun=-1L;rotFrame=-1;lookDYaw=0f;lookDPitch=0f;popInterpolatedLook();recRotInit=false;pendingChatMessage=null;pendingGuiClick=false;pendingGuiCenter=false;pendingGuiScreen=null;importedChatWarned=false;resetStabilizer();keysNeedReset=false;useHoldFallbackDelay=0;wantSprint=false;useTimerAlign=false;maskRun=-1L;screenMismatchTicks=0;pendingGuiShift=false;guiKeyQueue.clear();pendingGuiKeys.clear();pendingGuiButton=0;worldResetPending=false;worldSuspendTicks=0;
    }
    /** Clears presses buffered inside a KeyBinding: every isPressed() call consumes one. The guard prevents an endless loop if the counter never drains. */
    private void drainPressQueue(KeyBinding key){if(key==null)return;for(int guard=0;guard<64&&key.isPressed();guard++){}}
    private void restorePhysical(KeyBinding key){if(key==null)return;drainPressQueue(key);KeyBinding.setKeyBindState(key.getKeyCode(),physicalDown(key.getKeyCode()));}
    private boolean physicalDown(int code){try{if(code>=0)return Keyboard.isKeyDown(code);int button=code+100;return button>=0&&button<Mouse.getButtonCount()&&Mouse.isButtonDown(button);}catch(Exception e){return false;}}
    @SubscribeEvent public void onWorldUnload(WorldEvent.Unload event){if(event.getWorld().isRemote){releaseBackgroundPolicy();pendingChatMessage=null;forceResetKeys();}}
    @SubscribeEvent public void onWorldLoad(WorldEvent.Load event){if(event.getWorld().isRemote&&manager.isBusy()){worldResetPending=true;if(manager.isRecording())sendMsg(L("§aНовый мир: запись продолжается.","§aNew world: recording continues.","§aНовий світ: запис триває.","§aNeue Welt: Aufnahme läuft weiter.","§aNowy świat: nagrywanie trwa."));else if(manager.isPlaying())sendMsg(L("§aНовый мир: повтор продолжается.","§aNew world: playback continues.","§aНовий світ: відтворення триває.","§aNeue Welt: Wiedergabe läuft weiter.","§aNowy świat: odtwarzanie trwa."));}}
    @SubscribeEvent public void onClientChat(ClientChatEvent event){if(manager.isRecording()){String msg=event.getMessage();if(msg!=null&&!msg.isEmpty())pendingChatMessage=msg;}}
    @SubscribeEvent public void onMouseInput(InputEvent.MouseInputEvent event){
        if(!manager.isRecording()||mc.currentScreen!=null||!Mouse.getEventButtonState())return;int btn=Mouse.getEventButton();pendingGuiClick=false;pendingGuiCenter=false;pendingGuiShift=false;pendingGuiScreen=null;pendingGuiButton=0;if(btn==0)manager.addLeftClick();else if(btn==1)manager.addRightClick();
    }
    @SubscribeEvent public void onGuiMouseInput(GuiScreenEvent.MouseInputEvent.Pre event){if(!manager.isRecording()||!Mouse.getEventButtonState())return;GuiScreen screen=event.getGui();if(screen==null)return;String name=screen.getClass().getName();
        // Тот же фильтр, что у клавиш: собственные окна мода и окно чата в запись не попадают — их клики не должны повторяться.
        if(!replayableScreen(name)||name.startsWith("com.mirror.recorder.gui.")||screen instanceof net.minecraft.client.gui.GuiChat)return;int btn=Mouse.getEventButton();if(btn<0||btn>2)return;pendingGuiButton=btn;pendingGuiClick=true;pendingGuiX=MathHelper.clamp((float)Mouse.getEventX()/Math.max(1,mc.displayWidth),0f,1f);pendingGuiY=MathHelper.clamp(1f-(float)Mouse.getEventY()/Math.max(1,mc.displayHeight),0f,1f);int gw=Math.max(1,screen.width),gh=Math.max(1,screen.height);pendingGuiCX=Math.round(pendingGuiX*gw)-gw/2;pendingGuiCY=Math.round(pendingGuiY*gh)-gh/2;pendingGuiCenter=true;pendingGuiShift=GuiScreen.isShiftKeyDown();pendingGuiScreen=name;if(btn==0)manager.setLeftClickState(true);else if(btn==1)manager.setRightClickState(true);}

    /** Клавиши в чужих окнах: классифицируем сразу (действие слота, закрытие, печать), чтобы повтор не зависел от текущих привязок клавиш. Чат и окна мода не пишем. */
    @SubscribeEvent public void onGuiKeyInput(GuiScreenEvent.KeyboardInputEvent.Pre event){
        if(!manager.isRecording()||!Keyboard.getEventKeyState())return;
        GuiScreen screen=event.getGui();if(screen==null||mc.gameSettings==null)return;
        String name=screen.getClass().getName();
        if(!replayableScreen(name)||name.startsWith("com.mirror.recorder.gui.")||screen instanceof net.minecraft.client.gui.GuiChat)return;
        int key=Keyboard.getEventKey();char chr=Keyboard.getEventCharacter();
        net.minecraft.client.settings.GameSettings gs=mc.gameSettings;
        boolean containerKey=screen instanceof net.minecraft.client.gui.inventory.GuiContainer;
        boolean textFocus=containerKey&&hasFocusedTextField(screen);
        boolean printable=chr>=32&&chr!=127,nav=key==14||key==199||key==203||key==205||key==207||key==211;
        boolean textWins=textFocus&&(printable||nav);
        int action=-1,aux=chr;
        if(key==1)action=GA_CLOSE;
        else if(!containerKey)action=GA_TYPE;
        else if(gs.keyBindInventory.isActiveAndMatches(key)){if(!textWins)action=GA_CLOSE;}
        else if(gs.keyBindDrop.isActiveAndMatches(key)){if(!textWins)action=GA_DROP;}
        else if(gs.keyBindPickBlock.isActiveAndMatches(key)){if(!textWins)action=GA_PICK;}
        else{int hb=hotbarKeyIndex(gs,key);if(hb>=0&&!textWins){action=GA_SWAP;aux=hb;}}
        if(action<0){if(printable||nav)action=GA_TYPE;else return;}
        if(pendingGuiKeys.size()>=GUI_KEY_LIMIT)return;
        int cx=0,cy=0;
        if(action==GA_DROP||action==GA_PICK||action==GA_SWAP){
            int gw=Math.max(1,screen.width),gh=Math.max(1,screen.height);
            float fx=MathHelper.clamp((float)Mouse.getX()/Math.max(1,mc.displayWidth),0f,1f),fy=MathHelper.clamp(1f-(float)Mouse.getY()/Math.max(1,mc.displayHeight),0f,1f);
            cx=Math.round(fx*gw)-gw/2;cy=Math.round(fy*gh)-gh/2;
        }
        pendingGuiKeys.add(new int[]{action,key,aux,(GuiScreen.isShiftKeyDown()?1:0)|(GuiScreen.isCtrlKeyDown()?2:0),cx,cy});
    }
    private int hotbarKeyIndex(net.minecraft.client.settings.GameSettings gs,int key){
        if(gs.keyBindsHotbar==null)return -1;
        for(int i=0;i<gs.keyBindsHotbar.length&&i<9;i++)if(gs.keyBindsHotbar[i].isActiveAndMatches(key))return i;
        return -1;
    }
    /** Фокус текстового поля решает, что клавиша — это печать (как ванильная наковальня: textboxKeyTyped идёт первым). */
    private boolean hasFocusedTextField(GuiScreen screen){
        try{for(Field f:textFieldsOf(screen.getClass())){f.setAccessible(true);Object o=f.get(screen);if(o instanceof net.minecraft.client.gui.GuiTextField&&((net.minecraft.client.gui.GuiTextField)o).isFocused())return true;}}catch(Throwable ignored){}
        return false;
    }
    private Field[] textFieldsOf(Class<?> c){
        Field[] cached=textFieldCache.get(c.getName());if(cached!=null)return cached;
        java.util.List<Field> out=new java.util.ArrayList<Field>();
        for(Class<?> k=c;k!=null&&k!=Object.class;k=k.getSuperclass())for(Field f:k.getDeclaredFields())if(net.minecraft.client.gui.GuiTextField.class.isAssignableFrom(f.getType()))out.add(f);
        Field[] arr=out.toArray(new Field[out.size()]);textFieldCache.put(c.getName(),arr);return arr;
    }
    @SubscribeEvent public void onKeyInput(InputEvent.KeyInputEvent event){
        MirrorRecorder mod=MirrorRecorder.getInstance();if(mod==null)return;
        if(mod.getKeyRecord().isPressed()){if(mod.getReturnController()!=null&&mod.getReturnController().isBusy())sendMsg(L("\u00a7e\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u043e\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u0435 \u0432\u043e\u0437\u0432\u0440\u0430\u0442 Baritone.","§eStop the Baritone return first.","§eСпочатку зупиніть повернення Baritone.","§eStoppe zuerst die Baritone-Rückkehr.","§eNajpierw zatrzymaj powrót Baritone."));else{int slot=config.getActiveSlot();if(manager.scheduleRecording(slot))sendMsg(L("\u00a7a[+] \u0417\u0430\u043f\u0438\u0441\u044c \u0447\u0435\u0440\u0435\u0437 ","§a[+] Recording in ","§a[+] Запис через ","§a[+] Aufnahme in ","§a[+] Nagranie za ")+config.getStartDelay()+L(" \u0441\u0435\u043a..."," sec..."," сек..."," Sek..."," s..."));else sendMsg(L("\u00a7c\u0421\u043b\u043e\u0442 ","§cSlot ","§cСлот ","§cSlot ","§cSlot ")+slot+L(" \u0437\u0430\u043d\u044f\u0442 \u0438\u043b\u0438 \u0438\u0434\u0451\u0442 \u0434\u0440\u0443\u0433\u043e\u0435 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0435"," is occupied or another action is running"," зайнятий або йде інша дія"," ist belegt oder eine andere Aktion läuft"," zajęty lub trwa inna akcja"));}}
        if(mod.getKeyPlay().isPressed()){int slot=config.getActiveSlot();BaritoneReturnController r=mod.getReturnController();if(r!=null){manager.setPreferredLoopMode(slot,false);r.request(slot,false);}}
        if(mod.getKeyLoop().isPressed()){int slot=config.getActiveSlot();BaritoneReturnController r=mod.getReturnController();if(r!=null){manager.setPreferredLoopMode(slot,true);r.request(slot,true);}}
        if(mod.getKeyStop().isPressed()){SoundFx.stop();resetPlayback();if(mod.getReturnController()!=null)mod.getReturnController().cancel();manager.stopAll();sendMsg(L("\u00a7e[=] \u041e\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u043e","§e[=] Stopped","§e[=] Зупинено","§e[=] Gestoppt","§e[=] Zatrzymano"));}
        if(mod.getKeyGui().isPressed())mc.displayGuiScreen(new com.mirror.recorder.gui.GuiMirrorMain(manager,config));
    }
    private boolean isRealMovement(){return mc.player!=null&&(mc.gameSettings.keyBindForward.isKeyDown()||mc.gameSettings.keyBindBack.isKeyDown()||mc.gameSettings.keyBindLeft.isKeyDown()||mc.gameSettings.keyBindRight.isKeyDown()||mc.gameSettings.keyBindJump.isKeyDown());}
    private static String L(String ru,String en,String uk,String de,String pl){return com.mirror.recorder.gui.Lang.s(ru,en,uk,de,pl);}
    private void sendMsg(String msg){long now=System.currentTimeMillis();if(msg!=null&&msg.equals(lastNotice)&&now-lastNoticeAt<2000L)return;lastNotice=msg==null?"":msg;lastNoticeAt=now;TransientChat.show(msg);}
}
