package com.mirror.recorder.debug;
import com.mirror.recorder.manager.PlaybackClock;
import com.mirror.recorder.model.Frame;
import net.minecraft.nbt.NBTTagCompound;
import java.util.*;

/** Автотест движка записи/повтора: сравнение идёт по событиям и временным меткам, а не «на глаз».
 *
 *  Проверяется всё, что можно проверить без запуска в мире и без зависимости от FPS, фокуса окна,
 *  масштаба интерфейса и языка игры:
 *   1) временная шкала повтора — ноль накопленной погрешности за час прогона на любой скорости;
 *   2) порядок кликов внутри тика — ЛКМ/ПКМ не переставляются местами;
 *   3) капы событий — переполнение обрезается предсказуемо, а не ломает кадр;
 *   4) сериализация кадра — поток событий после записи на диск и чтения совпадает бит в бит;
 *   5) координаты окон — смещение от центра в единицах интерфейса не зависит от GUI Scale и разрешения.
 *  Запуск: /mirror selftest. */
public final class MirrorSelfTest{
    private MirrorSelfTest(){}
    private static final int TICKS_10_MIN=12000,TICKS_HOUR=72000,STREAM_FRAMES=5000;
    /** Результат прогона: строки отчёта готовы к выводу в чат. */
    public static final class Result{
        public final List<String> lines=new ArrayList<String>();public int passed=0,failed=0;
        void check(String name,boolean condition,String why){
            if(condition){passed++;lines.add("\u00a7a+ \u00a77"+name);}
            else{failed++;lines.add("\u00a7c- \u00a7f"+name+" \u00a78\u2014 \u00a7c"+why);}
        }
        public boolean isGreen(){return failed==0;}
        public int total(){return passed+failed;}
    }
    public static Result run(){
        Result r=new Result();
        timeline(r);clickOrder(r);caps(r);serialization(r);eventStream(r);guiScale(r);
        MirrorDebug.log("SELFTEST","passed="+r.passed+" failed="+r.failed);
        return r;
    }
    /** Шкала времени: за N тиков должно пройти ровно N*speed кадров, остаток не копится. */
    private static void timeline(Result r){
        float[] speeds={0.25f,0.5f,0.85f,1f,1.15f,1.5f,2f,3.35f,4f};
        for(int s=0;s<speeds.length;s++){
            int milli=PlaybackClock.toMilli(speeds[s]);PlaybackClock clock=new PlaybackClock();long steps=0L;
            for(int i=0;i<TICKS_HOUR;i++)steps+=clock.advance(milli);
            long expected=(long)TICKS_HOUR*(long)milli/(long)PlaybackClock.UNIT;
            r.check("\u0448\u043a\u0430\u043b\u0430 x"+speeds[s]+": 72000 \u0442\u0438\u043a\u043e\u0432 \u2192 "+expected+" \u043a\u0430\u0434\u0440\u043e\u0432",
                steps==expected&&clock.getRemainder()<PlaybackClock.UNIT,"steps="+steps+" expected="+expected+" remainder="+clock.getRemainder());
        }
        PlaybackClock ten=new PlaybackClock();long slow=0L;
        for(int i=0;i<TICKS_10_MIN;i++)slow+=ten.advance(PlaybackClock.toMilli(1f));
        r.check("10 \u043c\u0438\u043d\u0443\u0442 x1.00 \u0431\u0435\u0437 \u0434\u0440\u0435\u0439\u0444\u0430",slow==TICKS_10_MIN&&ten.getRemainder()==0,"steps="+slow+" remainder="+ten.getRemainder());
        PlaybackClock back=new PlaybackClock();int taken=back.advance(PlaybackClock.UNIT*3);back.giveBack(taken);
        r.check("\u0432\u043e\u0437\u0432\u0440\u0430\u0442 \u043d\u0435\u043f\u0440\u043e\u0439\u0434\u0435\u043d\u043d\u044b\u0445 \u0448\u0430\u0433\u043e\u0432",back.getRemainder()==PlaybackClock.UNIT*3,"remainder="+back.getRemainder());
        r.check("\u0437\u0430\u0436\u0438\u043c \u0441\u043a\u043e\u0440\u043e\u0441\u0442\u0438 0.25\u20134.00",
            PlaybackClock.toMilli(0.01d)==PlaybackClock.MIN_SPEED&&PlaybackClock.toMilli(99d)==PlaybackClock.MAX_SPEED&&PlaybackClock.toMilli(1d)==PlaybackClock.UNIT,"clamp");
    }
    /** Порядок кликов внутри одного тика обязан сохраняться: состояние «нажато» его не хранит. */
    private static void clickOrder(Result r){
        int[] seq={1,0,0,1,1,0};
        Frame source=Frame.builder().keyMask(Frame.K_ATTACK|Frame.K_USE).leftClick(true).rightClick(true).attackClicks(3).useClicks(3).clickSeq(seq).build();
        Frame copy=Frame.fromNBT(source.toNBT(),true);
        r.check("\u043f\u043e\u0440\u044f\u0434\u043e\u043a \u043a\u043b\u0438\u043a\u043e\u0432 \u0432 \u0442\u0438\u043a\u0435 \u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d",Arrays.equals(copy.clickSeq,seq),"got "+Arrays.toString(copy.clickSeq));
        int[] dirty={0,1,7,-3,1};
        Frame filtered=Frame.builder().clickSeq(dirty).build();
        r.check("\u043c\u0443\u0441\u043e\u0440\u043d\u044b\u0435 \u043a\u043e\u0434\u044b \u043a\u043b\u0438\u043a\u043e\u0432 \u043e\u0442\u0431\u0440\u043e\u0448\u0435\u043d\u044b",
            filtered.clickSeq.length==3&&filtered.clickSeq[0]==0&&filtered.clickSeq[1]==1&&filtered.clickSeq[2]==1,"got "+Arrays.toString(filtered.clickSeq));
        Frame legacy=Frame.builder().leftClick(true).build();
        r.check("\u0441\u0442\u0430\u0440\u044b\u0435 \u0437\u0430\u043f\u0438\u0441\u0438 \u0431\u0435\u0437 \u043f\u043e\u0440\u044f\u0434\u043a\u0430",legacy.clickSeq.length==0&&legacy.attackClicks==1,"clickSeq="+legacy.clickSeq.length+" attack="+legacy.attackClicks);
    }
    /** Капы: 100 кликов и 200 записей порядка на тик, 8 — верхний слот хотбара. */
    private static void caps(Result r){
        int[] flood=new int[400];for(int i=0;i<flood.length;i++)flood[i]=i%2;
        Frame capped=Frame.builder().attackClicks(250).useClicks(250).clickSeq(flood).build();
        r.check("\u043a\u0430\u043f \u043a\u043b\u0438\u043a\u043e\u0432 100/\u0442\u0438\u043a",capped.attackClicks==Frame.MAX_CLICKS&&capped.useClicks==Frame.MAX_CLICKS,"attack="+capped.attackClicks+" use="+capped.useClicks);
        r.check("\u043a\u0430\u043f \u043f\u043e\u0440\u044f\u0434\u043a\u0430 "+Frame.MAX_CLICK_SEQ+"/\u0442\u0438\u043a",capped.clickSeq.length==Frame.MAX_CLICK_SEQ,"len="+capped.clickSeq.length);
        r.check("\u043e\u0442\u0440\u0438\u0446\u0430\u0442\u0435\u043b\u044c\u043d\u044b\u0435 \u043a\u043b\u0438\u043a\u0438 \u0432 \u043d\u043e\u043b\u044c",Frame.builder().attackClicks(-9).useClicks(-1).build().attackClicks==0,"negative clicks");
        r.check("\u0437\u0430\u0436\u0438\u043c \u0441\u043b\u043e\u0442\u0430 \u0445\u043e\u0442\u0431\u0430\u0440\u0430",Frame.builder().hotbarSlot(99).build().hotbarSlot==8&&Frame.builder().hotbarSlot(-7).build().hotbarSlot==-1,"hotbar clamp");
        r.check("\u0446\u0435\u043d\u0442\u0440 \u043a\u043b\u0438\u043a\u0430 \u0431\u0435\u0437 \u0444\u043b\u0430\u0433\u0430 \u043d\u0435 \u043f\u0438\u0448\u0435\u0442\u0441\u044f",!Frame.builder().guiCenter(false,10,20).build().hasGuiCenter,"guiCenter flag");
    }
    /** Кадр после записи на диск и чтения обратно должен быть тем же кадром. */
    private static void serialization(Result r){
        Frame source=full(),copy=Frame.fromNBT(source.toNBT(),true);
        String diff=diff(source,copy);
        r.check("\u043f\u043e\u043b\u043d\u044b\u0439 \u043a\u0430\u0434\u0440 \u0447\u0435\u0440\u0435\u0437 NBT",diff==null,diff==null?"":diff);
        Frame empty=Frame.fromNBT(new NBTTagCompound(),false);
        r.check("\u043f\u0443\u0441\u0442\u043e\u0439 \u043a\u0430\u0434\u0440 \u0431\u0435\u0437\u043e\u043f\u0430\u0441\u0435\u043d",empty.clickSeq.length==0&&empty.guiKeys.length==0&&empty.tickIndex==0L&&empty.hotbarSlot==-1,"empty frame");
        Frame edge=Frame.fromNBT(Frame.builder().tickIndex(TICKS_HOUR).keyMask(0).build().toNBT(),true);
        r.check("\u043c\u0435\u0442\u043a\u0430 \u0432\u0440\u0435\u043c\u0435\u043d\u0438 72000 \u0438 \u043f\u0443\u0441\u0442\u0430\u044f \u043c\u0430\u0441\u043a\u0430",edge.tickIndex==TICKS_HOUR&&edge.hasKeyMask&&edge.keyMask==0,"tick="+edge.tickIndex+" mask="+edge.keyMask);
    }
    /** Сквозная проверка потока: ни одного потерянного, ни одного лишнего события, метки времени строго растут. */
    private static void eventStream(Result r){
        List<Frame> recorded=new ArrayList<Frame>(STREAM_FRAMES);
        long seed=20260903L;
        for(int i=0;i<STREAM_FRAMES;i++){seed=next(seed);recorded.add(synthetic(i,seed));}
        int lost=0,extra=0,mismatch=0,disorder=0,events=0;String first=null;long previous=-1L;
        for(int i=0;i<STREAM_FRAMES;i++){
            Frame a=recorded.get(i),b=Frame.fromNBT(a.toNBT(),true);
            int before=countEvents(a),after=countEvents(b);events+=before;
            if(after<before)lost++;else if(after>before)extra++;
            String d=diff(a,b);
            if(d!=null){mismatch++;if(first==null)first="frame "+i+": "+d;}
            if(b.tickIndex<=previous)disorder++;
            previous=b.tickIndex;
        }
        r.check(STREAM_FRAMES+" \u043a\u0430\u0434\u0440\u043e\u0432 / "+events+" \u0441\u043e\u0431\u044b\u0442\u0438\u0439: \u043f\u043e\u0442\u0435\u0440\u044c \u0438 \u043b\u0438\u0448\u043d\u0438\u0445 \u043d\u0435\u0442",
            lost==0&&extra==0&&mismatch==0,"lost="+lost+" extra="+extra+" mismatch="+mismatch+(first==null?"":" ("+first+")"));
        r.check("\u043c\u0435\u0442\u043a\u0438 \u0432\u0440\u0435\u043c\u0435\u043d\u0438 \u0441\u0442\u0440\u043e\u0433\u043e \u0440\u0430\u0441\u0442\u0443\u0442",disorder==0,"disorder="+disorder);
    }
    /** Смена GUI Scale и разрешения между записью и повтором не должна двигать цель клика в контейнере. */
    private static void guiScale(Result r){
        int[][] sizes={{427,240},{284,160},{213,120},{640,360},{569,320},{1024,576}};
        int recordedCX=-70,recordedCY=-30,expected=slotOf(recordedCX,recordedCY);
        boolean same=true;String why="";
        for(int i=0;i<sizes.length;i++){
            int w=sizes[i][0],h=sizes[i][1];
            int x=w/2+recordedCX,y=h/2+recordedCY;              // так точку клика считает повтор
            int guiLeft=(w-176)/2,guiTop=(h-166)/2;             // так ваниль центрирует контейнер
            int got=slotOf(x-guiLeft-88,y-guiTop-83);
            if(got!=expected){same=false;why=w+"x"+h+" \u2192 "+got+" \u0432\u043c\u0435\u0441\u0442\u043e "+expected;}
        }
        r.check("\u0441\u043b\u043e\u0442 \u043a\u043e\u043d\u0442\u0435\u0439\u043d\u0435\u0440\u0430 \u043e\u0434\u0438\u043d \u043d\u0430 \u0432\u0441\u0435\u0445 GUI Scale",same,why);
        boolean bounded=true;
        for(int i=0;i<sizes.length;i++){int w=sizes[i][0];int x=w/2+recordedCX;if(x<0||x>=w)bounded=false;}
        r.check("\u0442\u043e\u0447\u043a\u0430 \u043a\u043b\u0438\u043a\u0430 \u043d\u0435 \u0432\u044b\u0445\u043e\u0434\u0438\u0442 \u0437\u0430 \u044d\u043a\u0440\u0430\u043d",bounded,"out of screen");
    }
    /** Слот сетки контейнера по смещению от его центра: шаг 18 единиц интерфейса. */
    private static int slotOf(int dx,int dy){return floorDiv(dx,18)*100+floorDiv(dy,18);}
    private static int floorDiv(int a,int b){int q=a/b;if((a%b!=0)&&((a<0)!=(b<0)))q--;return q;}
    private static long next(long seed){return seed*6364136223846793005L+1442695040888963407L;}
    /** Кадр со всеми заполненными полями: если сериализация что-то теряет, тест это увидит. */
    private static Frame full(){
        return Frame.builder().x(12.5d).y(64.0625d).z(-8.75d).yaw(-179.5f).pitch(42.25f)
            .motionX(0.125d).motionY(-0.0625d).motionZ(0.375d).onGround(true).sneak(true).sprint(true)
            .leftClick(true).rightClick(true).attackClicks(3).useClicks(2).clickSeq(new int[]{0,1,0,1,0})
            .jump(true).moveForward(1f).moveStrafe(-1f).keyMask(Frame.K_FORWARD|Frame.K_SNEAK|Frame.K_ATTACK|Frame.K_USE|Frame.K_DROP)
            .hotbarSlot(5).dYaw(-12.5f).dPitch(3.25f)
            .guiClick(true).guiX(0.375f).guiY(0.625f).guiScreen("net.minecraft.client.gui.inventory.GuiChest")
            .guiCenter(true,-70,-30).guiShift(true).guiButton(2)
            .guiKeys(new int[]{1,42,3,2,-70,-30,0,17,119,0,0,0}).openScreen("net.minecraft.client.gui.inventory.GuiChest")
            .screenState(true).dropAll(true).worldReset(true).chatMessage("/mirror ping").tickIndex(71999L).elytra(true).build();
    }
    /** Синтетический кадр: клавиши, клики с порядком, действия окон и чат — детерминированно от seed. */
    private static Frame synthetic(int index,long seed){
        int r1=(int)((seed>>>17)&0xFF),r2=(int)((seed>>>25)&0xFF),r3=(int)((seed>>>33)&0xFF);
        int mask=0;
        if((r1&1)!=0)mask|=Frame.K_FORWARD;if((r1&2)!=0)mask|=Frame.K_BACK;if((r1&4)!=0)mask|=Frame.K_LEFT;
        if((r1&8)!=0)mask|=Frame.K_RIGHT;if((r1&16)!=0)mask|=Frame.K_JUMP;if((r1&32)!=0)mask|=Frame.K_SNEAK;
        if((r1&64)!=0)mask|=Frame.K_SPRINT;if((r1&128)!=0)mask|=Frame.K_ATTACK;
        int attack=r2%4,use=r3%3;
        int[] seq=new int[attack+use];int a=attack,u=use,n=0;boolean left=(r2&1)==0;
        while(a>0||u>0){
            if(left&&a>0){seq[n++]=0;a--;}
            else if(!left&&u>0){seq[n++]=1;u--;}
            else if(a>0){seq[n++]=0;a--;}
            else{seq[n++]=1;u--;}
            left=!left;
        }
        Frame.Builder b=Frame.builder()
            .x(index*0.03125d).y(64d+(index%32)*0.25d).z(-index*0.0625d)
            .yaw((index%720)*0.5f-180f).pitch((index%180)*0.5f-45f)
            .motionX((index%8)*0.03125d).motionY(-0.0625d).motionZ((index%5)*0.0625d)
            .onGround((index&1)==0).sneak((mask&Frame.K_SNEAK)!=0).sprint((mask&Frame.K_SPRINT)!=0)
            .jump((mask&Frame.K_JUMP)!=0).moveForward((mask&Frame.K_FORWARD)!=0?1f:0f).moveStrafe((mask&Frame.K_LEFT)!=0?1f:0f)
            .keyMask(mask).hotbarSlot(index%9).dYaw((r1%7)-3f).dPitch((r2%5)-2f)
            .leftClick(attack>0).rightClick(use>0).attackClicks(attack).useClicks(use).clickSeq(seq)
            .screenState(true).tickIndex(index);
        if(index%53==0)b.guiClick(true).guiScreen("net.minecraft.client.gui.inventory.GuiChest").guiX(0.375f).guiY(0.625f).guiCenter(true,-70,-30).guiButton(index%106==0?2:0).guiShift((index&2)==0);
        if(index%37==0)b.guiKeys(new int[]{0,17,119,0,0,0}).openScreen("net.minecraft.client.gui.inventory.GuiChest");
        if(index%200==0)b.chatMessage("/mirror ping "+index);
        if(index==2500)b.worldReset(true);
        if(index%400==0)b.dropAll(true);
        return b.build();
    }
    /** Сколько записанных событий несёт кадр: клики, действия окон и сообщение чата. */
    private static int countEvents(Frame f){return f.attackClicks+f.useClicks+f.clickSeq.length+f.guiKeys.length/6+(f.chatMessage==null||f.chatMessage.isEmpty()?0:1);}
    /** Первое расхождение двух кадров или null. */
    private static String diff(Frame a,Frame b){
        if(a.x!=b.x||a.y!=b.y||a.z!=b.z)return "position";
        if(a.yaw!=b.yaw||a.pitch!=b.pitch)return "rotation";
        if(a.motionX!=b.motionX||a.motionY!=b.motionY||a.motionZ!=b.motionZ)return "motion";
        if(a.onGround!=b.onGround||a.sneak!=b.sneak||a.sprint!=b.sprint||a.jump!=b.jump||a.elytra!=b.elytra)return "state";
        if(a.moveForward!=b.moveForward||a.moveStrafe!=b.moveStrafe)return "movement";
        if(a.hasKeyMask!=b.hasKeyMask||a.keyMask!=b.keyMask||a.hotbarSlot!=b.hotbarSlot)return "keys";
        if(a.dYaw!=b.dYaw||a.dPitch!=b.dPitch)return "look delta";
        if(a.leftClick!=b.leftClick||a.rightClick!=b.rightClick)return "click state";
        if(a.attackClicks!=b.attackClicks||a.useClicks!=b.useClicks)return "click count "+a.attackClicks+"/"+a.useClicks+" vs "+b.attackClicks+"/"+b.useClicks;
        if(!Arrays.equals(a.clickSeq,b.clickSeq))return "click order "+Arrays.toString(a.clickSeq)+" vs "+Arrays.toString(b.clickSeq);
        if(a.guiClick!=b.guiClick||a.guiButton!=b.guiButton||a.guiShift!=b.guiShift)return "gui click";
        if(a.guiX!=b.guiX||a.guiY!=b.guiY)return "gui point";
        if(a.hasGuiCenter!=b.hasGuiCenter||a.guiCenterX!=b.guiCenterX||a.guiCenterY!=b.guiCenterY)return "gui center";
        if(!same(a.guiScreen,b.guiScreen))return "gui screen";
        if(!same(a.openScreen,b.openScreen))return "open screen";
        if(!Arrays.equals(a.guiKeys,b.guiKeys))return "gui keys";
        if(a.dropAll!=b.dropAll||a.worldReset!=b.worldReset)return "flags";
        if(!same(a.chatMessage,b.chatMessage))return "chat";
        if(a.tickIndex!=b.tickIndex)return "tick index "+a.tickIndex+" vs "+b.tickIndex;
        return null;
    }
    private static boolean same(String a,String b){
        boolean ea=a==null||a.isEmpty(),eb=b==null||b.isEmpty();
        return ea&&eb?true:(ea!=eb?false:a.equals(b));
    }
}
