package com.mirror.recorder.gui;
import com.mirror.recorder.config.RecorderConfig;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/**
 * Живое превью вкладки «Визуализация»: маршрут и метка с тем же пульсом, что в мире.
 */
public final class VisualPreview{
    private VisualPreview(){}

    public static void draw(FontRenderer fr,RecorderConfig c,int l,int t,int r,int b,int focus){
        if(fr==null||c==null||r-l<40||b-t<36)return;
        UiTheme.card(l,t,r,b,UiTheme.DEEP,UiTheme.BORDER_HI);
        int innerL=l+4,innerT=t+4,innerR=r-4,innerB=b-16;
        if(innerB-innerT<20||innerR-innerL<20)return;
        UiTheme.roundGradient(innerL,innerT,innerR,innerB,0xFF152036,0xFF0B1220);
        int ground=innerT+(innerB-innerT)*62/100;

        boolean pathOn=c.isPathLineEnabled();
        int markerMode=c.getStartMarkerMode();
        int[] xs=new int[7],ys=new int[7];
        buildPath(innerL,innerT,innerR,innerB,ground,c,xs,ys);

        if(pathOn)drawPath(c,xs,ys,focus==0);
        if(markerMode>0)drawMarker(c,xs[0],ys[0],innerT,innerB,focus==1);
        else if(focus==1)drawOffMark(xs[0],ys[0]);

        if(markerMode>0&&c.isMarkerLabel()){
            String lab=Lang.s("Начало записи","Start","Початок запису","Start","Start");
            int lw=Math.min(innerR-innerL-8,fr.getStringWidth(lab));
            UiTheme.drawFitted(fr,lab,xs[0]-lw/2,Math.max(innerT+2,ys[0]-18),lw+2,focus==1?UiTheme.TEXT:UiTheme.TEXT_DIM);
        }

        String cap=caption(c);
        UiTheme.drawFitted(fr,cap,l+8,b-13,Math.max(20,r-l-16),UiTheme.TEXT_MUTE);
    }

    public static int hit(int mx,int my,int l,int t,int r,int b){
        if(mx<l||mx>=r||my<t||my>=b)return -1;
        return mx<l+(r-l)*38/100?1:0;
    }

    private static void buildPath(int l,int t,int r,int b,int ground,RecorderConfig c,int[] xs,int[] ys){
        int w=r-l,h=b-t;
        float lift=Math.min(0.45f,c.getPathLineHeight()/1.5f);
        int base=ground-Math.max(6,(int)(h*0.10f+lift*h*0.35f));
        float shown=Math.max(0.35f,Math.min(1f,c.getPathLineLength()/300f));
        int n=xs.length;
        for(int i=0;i<n;i++){
            float u=i/(float)(n-1)*shown;
            xs[i]=l+8+(int)((w-18)*u);
            float wave=(float)Math.sin(u*Math.PI*1.15)*h*0.10f;
            ys[i]=base-(int)wave-(int)(u*h*0.08f);
            if(ys[i]<t+8)ys[i]=t+8;
            if(ys[i]>b-8)ys[i]=b-8;
        }
    }

    private static void drawPath(RecorderConfig c,int[] xs,int[] ys,boolean focus){
        int style=c.getPathLineStyle();
        int rgb=(c.getPathLineR()<<16)|(c.getPathLineG()<<8)|c.getPathLineB();
        int base=0xFF000000|rgb;
        float a=Math.max(0.25f,c.getPathLineAlpha()/255f);
        int n=xs.length;
        float travel=wrap01(System.currentTimeMillis()/1800f);
        float breath=0.84f+0.16f*UiTheme.pulse(1600L);
        if(style!=2){
            begin();
            GL11.glLineWidth(Math.max(4.2f,Math.min(10f,c.getPathLineWidth()+3.2f)));
            pathStrip(xs,ys,base,a*0.18f*breath*(focus?1f:0.75f),c.isPathLineFade(),travel,0.10f);
            GL11.glLineWidth(Math.max(2.0f,Math.min(8f,c.getPathLineWidth()*(focus?1.15f:1f))));
            pathStrip(xs,ys,base,a*breath*(focus?1f:0.78f),c.isPathLineFade(),travel,0.38f);
            end();
        }
        if(style!=0){
            int step=Math.max(1,Math.min(3,c.getPathPointStep()/12+1));
            int s=Math.max(2,Math.min(7,Math.round(c.getPathPointSize()*22f)));
            begin();
            GL11.glLineWidth(focus?2f:1.3f);
            for(int i=0;i<n;i+=step){
                float fade=c.isPathLineFade()?1f-0.7f*i/(float)Math.max(1,n-1):1f;
                float u=i/(float)Math.max(1,n-1),d=Math.abs(u-travel);if(d>0.5f)d=1f-d;
                float hit=Math.max(0f,1f-d*7.5f);
                color(UiTheme.alpha(base,Math.min(1f,a*fade*(focus?1f:0.85f)+hit*0.28f)));
                cross(xs[i],ys[i],s);
            }
            end();
        }
    }

    private static void pathStrip(int[] xs,int[] ys,int base,float a,boolean fade,float travel,float boost){
        int n=xs.length;
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for(int i=0;i<n;i++){
            float f=fade?1f-0.75f*i/(float)Math.max(1,n-1):1f;
            float u=i/(float)Math.max(1,n-1),d=Math.abs(u-travel);if(d>0.5f)d=1f-d;
            float hit=Math.max(0f,1f-d*7.5f);
            color(UiTheme.alpha(base,Math.min(1f,a*f+hit*boost)));
            GL11.glVertex2f(xs[i]+0.5f,ys[i]+0.5f);
        }
        GL11.glEnd();
    }

    private static void drawMarker(RecorderConfig c,int x,int y,int top,int bottom,boolean focus){
        int rgb=(c.getMarkerR()<<16)|(c.getMarkerG()<<8)|c.getMarkerB();
        float pulse=c.isMarkerPulse()?0.72f+0.28f*UiTheme.pulse(1400L):1f;
        int col=UiTheme.alpha(0xFF000000|rgb,Math.max(0.28f,c.getMarkerAlpha()/255f)*pulse*(focus?1f:0.86f));
        int mode=c.getStartMarkerMode();
        begin();color(UiTheme.alpha(col,focus?0.28f:0.16f));circle(x,y,focus?13:10,24,true);end();
        if(mode==1){
            begin();color(col);circle(x,y,focus?4.2f:3.2f,16,true);end();
            if(c.isMarkerPulse())drawExpanding(x,y,6f,col);
            return;
        }
        float rad=Math.max(5f,Math.min(18f,c.getMarkerRadius()*9f));
        begin();
        GL11.glLineWidth(focus?2.4f:1.6f);color(col);circle(x,y,rad,28,false);
        if(mode==3){
            float h=Math.max(10f,Math.min(bottom-top-8,c.getMarkerBeaconHeight()*1.1f));
            GL11.glLineWidth(4.2f);color(UiTheme.alpha(col,0.22f));
            GL11.glBegin(GL11.GL_LINES);GL11.glVertex2f(x+0.5f,y);GL11.glVertex2f(x+0.5f,y-h);GL11.glEnd();
            GL11.glLineWidth(focus?2.2f:1.4f);color(col);
            GL11.glBegin(GL11.GL_LINES);GL11.glVertex2f(x+0.5f,y);GL11.glVertex2f(x+0.5f,y-h);GL11.glEnd();
        }
        end();
        if(c.isMarkerPulse())drawExpanding(x,y,rad,col);
    }

    private static void drawExpanding(int x,int y,float rad,int col){
        long now=System.currentTimeMillis();
        begin();
        for(int k=0;k<2;k++){
            float phase=wrap01(now/1400f+k*0.5f);
            float er=rad*(0.92f+0.70f*phase);
            color(UiTheme.alpha(col,(1f-phase)*0.38f));
            GL11.glLineWidth(2.0f-k*0.5f);
            circle(x,y,er,28,false);
        }
        end();
    }

    private static void drawOffMark(int x,int y){
        begin();GL11.glLineWidth(1.2f);color(0x66AEB9CC);circle(x,y,6,16,false);end();
    }


    private static String caption(RecorderConfig c){
        boolean path=c.isPathLineEnabled();boolean mark=c.getStartMarkerMode()>0;
        if(path&&mark)return Lang.s("В мире: маршрут и метка старта","In the world: route and start mark","У світі: маршрут і мітка старту","In der Welt: Route und Startmarkierung","W świecie: trasa i znacznik startu");
        if(path)return Lang.s("В мире: только маршрут","In the world: route only","У світі: тільки маршрут","In der Welt: nur Route","W świecie: tylko trasa");
        if(mark)return Lang.s("В мире: только метка старта","In the world: start mark only","У світі: тільки мітка старту","In der Welt: nur Startmarkierung","W świecie: tylko znacznik startu");
        return Lang.s("Сейчас в мире ничего не рисуется","Nothing is drawn in the world now","Зараз у світі нічого не малюється","In der Welt wird gerade nichts gezeichnet","Teraz w świecie nic nie jest rysowane");
    }

    private static float wrap01(float v){return v-(float)Math.floor(v);}
    private static void begin(){
        GlStateManager.disableTexture2D();GlStateManager.disableAlpha();GlStateManager.disableCull();GlStateManager.disableDepth();
        GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(770,771,1,0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);GL11.glHint(GL11.GL_LINE_SMOOTH_HINT,GL11.GL_NICEST);
    }
    private static void end(){
        GL11.glLineWidth(1f);GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableDepth();GlStateManager.enableCull();GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();GlStateManager.disableBlend();GlStateManager.color(1f,1f,1f,1f);
    }
    private static void color(int c){GL11.glColor4f(((c>>16)&255)/255f,((c>>8)&255)/255f,(c&255)/255f,((c>>>24)&255)/255f);}
    private static void circle(float x,float y,float rad,int n,boolean fill){
        GL11.glBegin(fill?GL11.GL_TRIANGLE_FAN:GL11.GL_LINE_LOOP);
        if(fill)GL11.glVertex2f(x,y);
        for(int i=0;i<=n;i++){double a=Math.PI*2.0*i/n;GL11.glVertex2f(x+(float)Math.cos(a)*rad,y+(float)Math.sin(a)*rad);}
        GL11.glEnd();
    }
    private static void cross(int x,int y,int s){
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x-s,y);GL11.glVertex2f(x+s,y);
        GL11.glVertex2f(x,y-s);GL11.glVertex2f(x,y+s);
        GL11.glEnd();
    }
}
