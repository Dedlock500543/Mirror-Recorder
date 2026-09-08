package com.mirror.recorder.handler;
import com.mirror.recorder.config.RecorderConfig;
import net.minecraft.client.entity.EntityPlayerSP;
/** Следит только за уроном: по игроку попали — запись и повтор пора остановить. */
public final class SafetyGuard{
    private static final int GRACE_TICKS=20;
    private float lastHealth=-1f;private int grace=GRACE_TICKS;
    public void standby(EntityPlayerSP player){lastHealth=player==null?-1f:player.getHealth();grace=GRACE_TICKS;}
    public String check(EntityPlayerSP player,RecorderConfig config){
        if(player==null||config==null)return null;
        float health=player.getHealth(),before=lastHealth;lastHealth=health;
        if(!config.isGuardOnDamage())return null;
        if(grace>0){grace--;return null;}
        if(before>=0f&&health<before-0.01f)
            return com.mirror.recorder.gui.Lang.s("Остановлено: по вам попали","Stopped: you took damage","Зупинено: по вас влучили","Gestoppt: Sie wurden getroffen","Zatrzymano: otrzymano obrażenia");
        return null;
    }
}
