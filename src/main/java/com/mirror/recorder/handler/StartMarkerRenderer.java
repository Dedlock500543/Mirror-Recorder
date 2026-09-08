package com.mirror.recorder.handler;
import com.mirror.recorder.config.RecorderConfig;import com.mirror.recorder.manager.RecorderManager;import net.minecraft.client.Minecraft;import net.minecraft.client.entity.EntityPlayerSP;import net.minecraft.client.gui.*;import net.minecraft.client.renderer.*;import net.minecraft.client.renderer.vertex.DefaultVertexFormats;import net.minecraftforge.client.event.RenderWorldLastEvent;import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;import net.minecraftforge.fml.relauncher.*;import org.lwjgl.opengl.GL11;
@SideOnly(Side.CLIENT) public class StartMarkerRenderer{
    private static final double[] COS16=new double[16],SIN16=new double[16],COS48=new double[48],SIN48=new double[48];
    static{for(int i=0;i<16;i++){double an=Math.PI*2*i/16d;COS16[i]=Math.cos(an);SIN16[i]=Math.sin(an);}for(int i=0;i<48;i++){double an=Math.PI*2*i/48d;COS48[i]=Math.cos(an);SIN48[i]=Math.sin(an);}}
    private String labelCacheText=null,labelCacheKeyLabel=null;private long labelCacheKeyDist=Long.MIN_VALUE;
    private final Minecraft mc=Minecraft.getMinecraft();private final RecorderManager manager;private final RecorderConfig config;private final BaritoneReturnController returner;public StartMarkerRenderer(RecorderManager m,RecorderConfig c,BaritoneReturnController r){manager=m;config=c;returner=r;}
    @SubscribeEvent public void onRenderWorldLast(RenderWorldLastEvent e){int mode=config.getStartMarkerMode();boolean recovery=returner!=null&&returner.isRecovering();boolean recording=manager.isRecording()&&manager.hasRecordingStart();if(!recovery&&(mode==0||!recording))return;EntityPlayerSP p=mc.player;if(p==null)return;if(recovery&&mode==0)mode=3;double x,y,z;String label;if(recovery){x=returner.getTargetX();y=returner.getTargetY();z=returner.getTargetZ();label=(com.mirror.recorder.gui.Lang.s("Начальная точка","Start point","Початкова точка","Startpunkt","Punkt startu"));}else{x=manager.getRecordingStartX();y=manager.getRecordingStartY();z=manager.getRecordingStartZ();label=(com.mirror.recorder.gui.Lang.s("Начало записи","Recording start","Початок запису","Aufnahmebeginn","Początek nagrania"));}net.minecraft.client.renderer.entity.RenderManager rm=mc.getRenderManager();double cx=rm.viewerPosX,cy=rm.viewerPosY,cz=rm.viewerPosZ;boolean pulseOn=config.isMarkerPulse();long now=System.currentTimeMillis();float breath=pulseOn?0.72f+0.28f*wave(now,1400L):1f;float r=config.getMarkerR()/255f,g=config.getMarkerG()/255f,b=config.getMarkerB()/255f,a=(config.getMarkerAlpha()<=0?0f:Math.max(0.18f,config.getMarkerAlpha()/255f))*breath;
        GlStateManager.pushMatrix();GlStateManager.disableTexture2D();GlStateManager.enableBlend();GlStateManager.disableLighting();GlStateManager.enableDepth();if(config.isMarkerThroughWalls())GlStateManager.disableDepth();GlStateManager.depthMask(false);
        double rad=config.getMarkerRadius();
        if(mode==1)drawPoint(x,y,z,cx,cy,cz,r,g,b,a,pulseOn,now);else{drawHalo(x,y,z,cx,cy,cz,rad*1.35d,r,g,b,a*0.38f);drawRing(x,y,z,cx,cy,cz,mode==2?rad:rad*1.12,r,g,b,a,Math.max(2.2f,config.getPathLineWidth()+0.6f));if(pulseOn){drawPulseRings(x,y,z,cx,cy,cz,rad,r,g,b,a,now);}if(mode==3)drawBeacon(x,y,z,cx,cy,cz,config.getMarkerBeaconHeight(),r,g,b,a);}
        GL11.glLineWidth(1f);GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,GlStateManager.SourceFactor.ONE,GlStateManager.DestFactor.ZERO);GlStateManager.depthMask(true);GlStateManager.enableDepth();GlStateManager.disableLighting();GlStateManager.enableAlpha();GlStateManager.enableCull();GlStateManager.disableBlend();GlStateManager.enableTexture2D();GlStateManager.color(1,1,1,1);GlStateManager.popMatrix();double dx=p.posX-x,dy=p.posY-y,dz=p.posZ-z,dist=Math.sqrt(dx*dx+dy*dy+dz*dz);if(config.isMarkerLabel()&&(config.isMarkerLabelUnlimited()||dist<=config.getMarkerLabelDistance()))drawLabel(x,y,z,cx,cy,cz,dist,label);
    }
    private void drawPoint(double x,double y,double z,double cx,double cy,double cz,float r,float g,float b,float a,boolean pulseOn,long now){
        additive();drawHalo(x,y,z,cx,cy,cz,0.42d,r,g,b,a*0.45f);normalBlend();
        Tessellator t=Tessellator.getInstance();BufferBuilder q=t.getBuffer();GL11.glLineWidth(2.4f);
        q.begin(GL11.GL_LINE_LOOP,DefaultVertexFormats.POSITION_COLOR);for(int i=0;i<16;i++)q.pos(x+COS16[i]*0.18-cx,y+0.06-cy,z+SIN16[i]*0.18-cz).color(r,g,b,a).endVertex();t.draw();
        q.begin(GL11.GL_LINES,DefaultVertexFormats.POSITION_COLOR);q.pos(x-cx,y+0.05-cy,z-cz).color(r,g,b,a).endVertex();q.pos(x-cx,y+0.55-cy,z-cz).color(r,g,b,a*0.35f).endVertex();t.draw();
        if(pulseOn)drawPulseRings(x,y,z,cx,cy,cz,0.22d,r,g,b,a,now);
    }
    private void drawHalo(double x,double y,double z,double cx,double cy,double cz,double radius,float r,float g,float b,float a){
        additive();Tessellator t=Tessellator.getInstance();BufferBuilder q=t.getBuffer();q.begin(GL11.GL_TRIANGLE_FAN,DefaultVertexFormats.POSITION_COLOR);
        q.pos(x-cx,y+0.035-cy,z-cz).color(r,g,b,a).endVertex();
        for(int i=0;i<=48;i++){int k=i==48?0:i;q.pos(x+COS48[k]*radius-cx,y+0.035-cy,z+SIN48[k]*radius-cz).color(r,g,b,0f).endVertex();}t.draw();normalBlend();
    }
    private void drawRing(double x,double y,double z,double cx,double cy,double cz,double radius,float r,float g,float b,float a,float width){
        Tessellator t=Tessellator.getInstance();BufferBuilder q=t.getBuffer();
        additive();GL11.glLineWidth(width+3.2f);ring(q,t,x,y,z,cx,cy,cz,radius,r,g,b,a*0.22f);
        normalBlend();GL11.glLineWidth(width);ring(q,t,x,y,z,cx,cy,cz,radius,r,g,b,a);
    }
    private void drawPulseRings(double x,double y,double z,double cx,double cy,double cz,double radius,float r,float g,float b,float a,long now){
        Tessellator t=Tessellator.getInstance();BufferBuilder q=t.getBuffer();additive();
        for(int k=0;k<2;k++){float phase=wrap01(now/1400f+k*0.5f);double er=radius*(0.92d+0.70d*phase);float ea=a*(1f-phase)*0.42f;GL11.glLineWidth(2.4f-k*0.6f);ring(q,t,x,y,z,cx,cy,cz,er,r,g,b,ea);}
        normalBlend();
    }
    private void ring(BufferBuilder q,Tessellator t,double x,double y,double z,double cx,double cy,double cz,double radius,float r,float g,float b,float a){
        q.begin(GL11.GL_LINE_LOOP,DefaultVertexFormats.POSITION_COLOR);for(int i=0;i<48;i++)q.pos(x+COS48[i]*radius-cx,y+0.06-cy,z+SIN48[i]*radius-cz).color(r,g,b,a).endVertex();t.draw();
    }
    private void drawBeacon(double x,double y,double z,double cx,double cy,double cz,double height,float r,float g,float b,float a){
        Tessellator t=Tessellator.getInstance();BufferBuilder q=t.getBuffer();
        additive();GL11.glLineWidth(6.5f);q.begin(GL11.GL_LINES,DefaultVertexFormats.POSITION_COLOR);q.pos(x-cx,y+0.06-cy,z-cz).color(r,g,b,a*0.18f).endVertex();q.pos(x-cx,y+height-cy,z-cz).color(r,g,b,0f).endVertex();t.draw();
        normalBlend();GL11.glLineWidth(2.1f);q.begin(GL11.GL_LINES,DefaultVertexFormats.POSITION_COLOR);q.pos(x-cx,y+0.06-cy,z-cz).color(r,g,b,a).endVertex();q.pos(x-cx,y+height-cy,z-cz).color(r,g,b,a*0.12f).endVertex();t.draw();
    }
    private void drawLabel(double x,double y,double z,double cx,double cy,double cz,double dist,String label){FontRenderer fr=mc.fontRenderer;if(fr==null)return;long distKey=Math.round(dist*10d);String text=labelCacheText;if(text==null||distKey!=labelCacheKeyDist||!label.equals(labelCacheKeyLabel)){text=label+"  \u00b7  "+String.format(java.util.Locale.ROOT,"%.1f",dist)+" m";labelCacheText=text;labelCacheKeyDist=distKey;labelCacheKeyLabel=label;}GlStateManager.pushMatrix();GlStateManager.translate(x-cx,y+1.45-cy,z-cz);GlStateManager.rotate(-mc.getRenderManager().playerViewY,0,1,0);GlStateManager.rotate(mc.getRenderManager().playerViewX,1,0,0);float s=0.025f;GlStateManager.scale(-s,-s,s);GlStateManager.disableLighting();GlStateManager.enableBlend();GlStateManager.enableDepth();GlStateManager.depthMask(false);int w=fr.getStringWidth(text),lx=-w/2;Gui.drawRect(lx-4,-4,lx+w+4,13,0x90080B12);fr.drawStringWithShadow(text,lx,0,0xFFFFFFFF);GlStateManager.depthMask(true);GlStateManager.enableDepth();GlStateManager.disableBlend();GlStateManager.disableLighting();GlStateManager.popMatrix();}
    private static float wave(long now,long period){return (float)(0.5-0.5*Math.cos((now%period)/(double)period*Math.PI*2.0));}
    private static float wrap01(float v){return v-(float)Math.floor(v);}
    private static void additive(){GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,GlStateManager.DestFactor.ONE,GlStateManager.SourceFactor.ONE,GlStateManager.DestFactor.ZERO);}
    private static void normalBlend(){GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,GlStateManager.SourceFactor.ONE,GlStateManager.DestFactor.ZERO);}
}
