package com.mirror.recorder.gui;
import com.mirror.recorder.MirrorRecorder;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.FontRenderer;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
public final class UiTheme{
    private UiTheme(){}
    public static final int
        PANEL=0xB40E1526,PANEL_HEAD=0xC4172038,BORDER=0xFF283449,BORDER_HI=0xFF3A4A66,
        CARD=0xAA1A2438,CARD_HOVER=0xAA24304A,CARD_SEL=0xAA22375C,CARD_SELB=0xFF6FA8FF,
        ACCENT=0xFF7AA2FF,GREEN=0xFF56D6A0,BLUE=0xFF5FB2FF,PURPLE=0xFFBB94F5,RED=0xFFF07B76,
        AMBER=0xFFF3BE63,GRAY=0xFF6A7690,TEXT=0xFFF2F5FB,TEXT_DIM=0xFFAEB9CC,TEXT_MUTE=0xFF6E7A90,
        DEEP=0xAA0B1120;
    public static final int GLASS_BACKDROP=0x8C05080F,FUNCTION_LIST_GLASS=0xB4141C2B,GLASS_TINT_TOP=0x32263C68,GLASS_TINT_BOTTOM=0x160A1020,HUD_GLASS=0xB4121A28;
    public static final long HOVER_MS=150L,SCROLL_MS=170L,TIP_DELAY_MS=260L;
    public static final long TIP_FADE_MS=120L;
    /** Мягкое затухание к концу движения: используется для подсказок. */
    public static float easeOut(float t){if(t<=0f)return 0f;if(t>=1f)return 1f;float u=1f-t;return 1f-u*u*u;}
    private static final int CORNER=14;
    /** Мягкий пульс 0..1 для индикаторов активного состояния. */
    public static float pulse(long periodMs){
        if(periodMs<=0L)return 1f;
        double phase=(System.currentTimeMillis()%periodMs)/(double)periodMs;
        return (float)(0.5-0.5*Math.cos(phase*Math.PI*2.0));
    }
    private static Object tipKey=null;private static long tipAt=0L,tipLast=0L;
    /** Подсказка появляется не сразу, чтобы не мелькать при проводке курсором. */
    public static boolean tipReady(Object key){
        if(key==null)return false;long now=System.currentTimeMillis();
        if(tipKey==null||!tipKey.equals(key)||now-tipLast>250L){tipKey=key;tipAt=now;}
        tipLast=now;return now-tipAt>=TIP_DELAY_MS;
    }
    /** Насколько подсказка проявилась: 0..1. Вызывать только после tipReady. */
    public static float tipFade(){
        long e=System.currentTimeMillis()-tipAt-TIP_DELAY_MS;
        if(e<=0L)return 0f;if(e>=TIP_FADE_MS)return 1f;return easeOut(e/(float)TIP_FADE_MS);
    }
    /** Ограничение отрисовки прямоугольником: содержимое не выходит за список. */
    public static void beginClip(net.minecraft.client.Minecraft mc,int l,int t,int r,int b){
        net.minecraft.client.gui.ScaledResolution sr=new net.minecraft.client.gui.ScaledResolution(mc);int f=sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(l*f,(sr.getScaledHeight()-b)*f,Math.max(0,r-l)*f,Math.max(0,b-t)*f);
    }
    public static void endClip(){GL11.glDisable(GL11.GL_SCISSOR_TEST);}
    /** Smootherstep: мягче обычного smoothstep на входе и выходе, без рывков. */
    public static float ease(float t){if(t<=0f)return 0f;if(t>=1f)return 1f;return t*t*t*(t*(t*6f-15f)+10f);}
    public static float approach(float value,float target,float delta,float speed){float step=Math.min(1f,Math.max(0f,delta)*speed);return value+(target-value)*step;}
    public static float transition(float from,float to,long started,long now,long duration){
        if(duration<=0L)return to;long elapsed=now-started;if(elapsed<=0L)return from;if(elapsed>=duration)return to;
        return from+(to-from)*ease(elapsed/(float)duration);
    }
    public static int alpha(int color,float a){int v=(int)(((color>>>24)&255)*Math.max(0f,Math.min(1f,a)));return (v<<24)|(color&0x00FFFFFF);}
    public static int lerp(int a,int b,float t){if(t<=0f)return a;if(t>=1f)return b;int aa=(a>>>24)&255,ar=(a>>16)&255,ag=(a>>8)&255,ab=a&255,ba=(b>>>24)&255,br=(b>>16)&255,bg=(b>>8)&255,bb=b&255;return ((int)(aa+(ba-aa)*t)<<24)|((int)(ar+(br-ar)*t)<<16)|((int)(ag+(bg-ag)*t)<<8)|(int)(ab+(bb-ab)*t);}
    public static int darken(int c,float f){int a=(c>>>24)&255,r=(int)(((c>>16)&255)*f),g=(int)(((c>>8)&255)*f),b=(int)((c&255)*f);return(a<<24)|(r<<16)|(g<<8)|b;}
    private static int tint(int c,int rgb,float amount){return lerp(c,(c&0xFF000000)|(rgb&0x00FFFFFF),amount);}
    private static float rad(float w,float h){return Math.max(1.5f,Math.min(6f,Math.min(w,h)*0.5f));}
    private static void glColor(int c){GL11.glColor4f(((c>>16)&255)/255f,((c>>8)&255)/255f,(c&255)/255f,((c>>>24)&255)/255f);}
    /** Состояние GL меняем только через GlStateManager: иначе с OptiFine / Fast Render цвета и текстуры ломаются. */
    private static void beginDraw(){
        GlStateManager.disableTexture2D();GlStateManager.disableAlpha();GlStateManager.disableCull();GlStateManager.disableDepth();
        GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(770,771,1,0);GlStateManager.shadeModel(GL11.GL_SMOOTH);
    }
    private static void endDraw(){
        GlStateManager.shadeModel(GL11.GL_FLAT);GlStateManager.disableBlend();GlStateManager.enableDepth();
        GlStateManager.enableCull();GlStateManager.enableAlpha();GlStateManager.enableTexture2D();resetColor();
    }
    /** Сброс цвета и кэша GlStateManager: иначе следующий текст или иконка окрашиваются нашим цветом. */
    private static void resetColor(){GlStateManager.color(1f,1f,1f,0.996f);GlStateManager.color(1f,1f,1f,1f);}
    private static void corner(float cx,float cy,float r,float from,float to,int n,float[] xs,float[] ys,int[] i){
        for(int s=0;s<=n;s++){double a=Math.toRadians(from+(to-from)*s/n);xs[i[0]]=cx+(float)Math.cos(a)*r;ys[i[0]]=cy+(float)Math.sin(a)*r;i[0]++;}
    }
    private static int outline(float l,float t,float r,float b,float radius,float[] xs,float[] ys){
        float w=r-l,h=b-t;if(w<=0f||h<=0f)return 0;
        float rad=Math.min(radius,Math.min(w,h)*0.5f);int n=Math.max(8,CORNER);int[] i={0};
        corner(l+rad,t+rad,rad,180,270,n,xs,ys,i);corner(r-rad,t+rad,rad,270,360,n,xs,ys,i);
        corner(r-rad,b-rad,rad,0,90,n,xs,ys,i);corner(l+rad,b-rad,rad,90,180,n,xs,ys,i);
        return i[0];
    }
    private static void fillRound(float l,float t,float r,float b,float radius,int top,int bottom){
        float w=r-l,h=b-t;if(w<=0f||h<=0f)return;
        float[] xs=new float[80],ys=new float[80];int n=outline(l,t,r,b,radius,xs,ys);if(n<3)return;
        float cx=(l+r)*0.5f,cy=(t+b)*0.5f;
        beginDraw();GL11.glBegin(GL11.GL_TRIANGLE_FAN);glColor(lerp(top,bottom,0.5f));GL11.glVertex2f(cx,cy);
        for(int i=0;i<n;i++){glColor(lerp(top,bottom,h<=1f?0f:Math.max(0f,Math.min(1f,(ys[i]-t)/h))));GL11.glVertex2f(xs[i],ys[i]);}
        glColor(lerp(top,bottom,h<=1f?0f:Math.max(0f,Math.min(1f,(ys[0]-t)/h))));GL11.glVertex2f(xs[0],ys[0]);
        GL11.glEnd();endDraw();
    }
    private static void strokeRound(float l,float t,float r,float b,float radius,int color,float thickness){
        float w=r-l,h=b-t;if(w<=1.5f||h<=1.5f)return;
        float[] ox=new float[80],oy=new float[80],ix=new float[80],iy=new float[80];
        int n=outline(l,t,r,b,radius,ox,oy);if(n<3)return;
        outline(l+thickness,t+thickness,r-thickness,b-thickness,Math.max(0.5f,radius-thickness),ix,iy);
        beginDraw();glColor(color);GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for(int i=0;i<n;i++){GL11.glVertex2f(ox[i],oy[i]);GL11.glVertex2f(ix[i],iy[i]);}
        GL11.glVertex2f(ox[0],oy[0]);GL11.glVertex2f(ix[0],iy[0]);GL11.glEnd();endDraw();
    }
    public static void round(int l,int t,int r,int b,int c){fillRound(l,t,r,b,rad(r-l,b-t),c,c);}
    public static void roundGradient(int l,int t,int r,int b,int top,int bottom){fillRound(l,t,r,b,rad(r-l,b-t),top,bottom);}
    public static void border(int l,int t,int r,int b,int c){strokeRound(l,t,r,b,rad(r-l,b-t),c,1f);}
    /** Мягкое свечение вокруг элемента: два затухающих контура вместо дорогого блюра. */
    private static void glow(int l,int t,int r,int b,float rr,int color,float a){
        float k=Math.max(0f,Math.min(1f,a));if(k<=0.01f)return;
        strokeRound(l-1,t-1,r+1,b+1,rr+1f,alpha(color,0.55f*k),1f);
        strokeRound(l-2,t-2,r+2,b+2,rr+2f,alpha(color,0.24f*k),1f);
    }
    public static void shadow(int l,int t,int r,int b){shadow(l,t,r,b,1f);}
    /** a 0..1 — тень гаснет вместе с элементом, иначе она «выстреливает» раньше него. */
    public static void shadow(int l,int t,int r,int b,float a){
        float k=Math.max(0f,Math.min(1f,a));if(k<=0.03f)return;
        fillRound(l+4,t+7,r+4,b+7,rad(r-l,b-t)+2f,alpha(0x0E000000,k),alpha(0x0E000000,k));
        fillRound(l+3,t+5,r+3,b+5,rad(r-l,b-t)+1f,alpha(0x16000000,k),alpha(0x16000000,k));
        fillRound(l+2,t+3,r+2,b+3,rad(r-l,b-t)+0.5f,alpha(0x1E000000,k),alpha(0x1E000000,k));
        fillRound(l+1,t+2,r+1,b+2,rad(r-l,b-t),alpha(0x26000000,k),alpha(0x26000000,k));
    }
    public static void window(int l,int t,int r,int b,int headH){
        shadow(l,t,r,b);
        fillRound(l,t,r,b,6f,tint(PANEL,0xFFFFFF,0.05f),PANEL);
        if(headH>8)fillRound(l+1,t+1,r-1,t+headH,5f,tint(PANEL_HEAD,0xFFFFFF,0.08f),PANEL_HEAD);
        strokeRound(l,t,r,b,6f,BORDER_HI,1f);
    }
    public static void card(int l,int t,int r,int b,int fill,int brdr){
        fillRound(l,t,r,b,rad(r-l,b-t),tint(fill,0xFFFFFF,0.07f),tint(fill,0x000000,0.05f));
        strokeRound(l,t,r,b,rad(r-l,b-t),brdr,1f);
    }
    public static void buttonBg(int l,int t,int r,int b,int accent,float hover,boolean enabled,boolean pressed,boolean primary){buttonBg(l,t,r,b,accent,hover,enabled,pressed,primary,1f);}
    /** fade 0..1 — плавное появление кнопки. Геометрия не меняется, попадание мышью остаётся точным. */
    public static void buttonBg(int l,int t,int r,int b,int accent,float hover,boolean enabled,boolean pressed,boolean primary,float fade){
        float fa=Math.max(0f,Math.min(1f,fade));if(fa<=0.03f)return;
        float h=ease(Math.max(0f,Math.min(1f,hover)));int top,bottom,brdr;float rr=rad(r-l,b-t);
        if(!enabled){top=0xA0161D2C;bottom=0xA0111825;brdr=0xFF242E40;}
        else if(primary){
            int base=lerp(darken(accent,0.44f),darken(accent,0.60f),h);if(pressed)base=darken(accent,0.36f);
            top=alpha(lerp(base,accent,0.24f),0.96f);bottom=alpha(base,0.96f);
            brdr=lerp(darken(accent,0.70f),accent,0.34f+0.66f*h);
        }else{
            int base=lerp(CARD,CARD_HOVER,h);if(pressed)base=darken(base,0.86f);
            top=lerp(base,alpha(0xFFFFFFFF,0.12f),0.05f+0.10f*h);bottom=base;
            brdr=lerp(BORDER,accent,0.14f+0.60f*h);
        }
        if(enabled&&h>0.02f)glow(l,t,r,b,rr,accent,(primary?0.17f:0.10f)*h*fa);
        fillRound(l,t,r,b,rr,alpha(top,fa),alpha(bottom,fa));
        if(enabled&&!pressed&&b-t>11)round(l+2,t+1,r-2,t+2,alpha(0xFFFFFFFF,(primary?0.15f:0.08f)*fa));
        if(enabled&&h>0.02f&&b-t>9){int bar=alpha(accent,(0.22f+0.58f*h)*fa);fillRound(l+2,t+3,l+4,b-3,1f,bar,bar);}
        strokeRound(l,t,r,b,rr,alpha(brdr,fa),1f);
    }
    public static int pillAt(FontRenderer fr,String text,int x,int y,int fill,int brdr,int tc){
        int w=fr.getStringWidth(text)+10;fillRound(x,y,x+w,y+12,6f,tint(fill,0xFFFFFF,0.08f),fill);
        strokeRound(x,y,x+w,y+12,6f,brdr,1f);fr.drawString(text,x+5,y+2,tc);return w;
    }
    public static void sectionLabel(FontRenderer fr,String text,int x,int y,int w){drawFitted(fr,text,x,y,Math.max(16,w),TEXT_MUTE);}
    /** Заголовок группы: точка акцента и жирный текст вместо лишних линий. */
    public static void sectionHead(FontRenderer fr,String text,int x,int y,int w,int accent){
        dot(x,y+1,alpha(accent,0.85f));
        drawFitted(fr,"\u00a7l"+text,x+11,y,Math.max(16,w-11),lerp(TEXT_MUTE,TEXT_DIM,0.55f));
    }
    public static void inset(int l,int t,int r,int b){card(l,t,r,b,DEEP,BORDER);}
    public static void dot(int x,int y,int color){
        beginDraw();glColor(darken(color,0.55f));circle(x+3f,y+3f,3.1f,22,true);glColor(color);circle(x+3f,y+3f,2.1f,22,true);endDraw();
    }
    public static void progress(int l,int t,int r,int b,float value,int color){
        float rr=rad(r-l,b-t);fillRound(l,t,r,b,rr,DEEP,DEEP);
        int w=(int)((r-l)*Math.max(0f,Math.min(1f,value)));
        if(w>1)fillRound(l,t,l+w,b,rr,tint(color,0xFFFFFF,0.10f),color);
        strokeRound(l,t,r,b,rr,BORDER,1f);
    }
    public static void switchAt(int x,int y,int w,int h,float progress,boolean enabled){
        float p=ease(progress),rr=h*0.5f;
        int track=enabled?lerp(0xC41A2231,0xC4245F4C,p):0xC4161C28;
        int edge=enabled?lerp(0xFF4A566C,GREEN,p):0xFF2E3746;
        fillRound(x,y,x+w,y+h,rr,track,track);
        if(enabled&&p>0.02f)fillRound(x+1,y+1,x+1+Math.max(2,Math.round((w-2)*p)),y+h-1,Math.max(1.5f,rr-1f),alpha(GREEN,0.26f*p),alpha(GREEN,0.16f*p));
        if(enabled&&p>0.02f)glow(x,y,x+w,y+h,rr,GREEN,0.22f*p);
        strokeRound(x,y,x+w,y+h,rr,edge,1f);
        int pad=2,d=h-pad*2,kx=x+pad+Math.round((w-pad*2-d)*p);
        fillRound(kx+1,y+pad+1,kx+d+1,y+pad+d+1,d*0.5f,0x33000000,0x33000000);
        fillRound(kx,y+pad,kx+d,y+pad+d,d*0.5f,enabled?lerp(0xFFE6EBF3,0xFFFFFFFF,p):0xFF7E899E,enabled?lerp(0xFFC9D3E2,0xFFEDF2FA,p):0xFF6B7688);
    }
    public static void vgrad(int l,int t,int r,int b,int top,int bottom){
        int h=b-t;if(h<=0||r<=l)return;
        beginDraw();GL11.glBegin(GL11.GL_QUADS);
        glColor(top);GL11.glVertex2f(l,t);GL11.glVertex2f(r,t);glColor(bottom);GL11.glVertex2f(r,b);GL11.glVertex2f(l,b);
        GL11.glEnd();endDraw();
    }
    public static void header(FontRenderer fr,int l,int t,int r,int headH,String title,String subtitle,int accent){header(fr,l,t,r,headH,title,subtitle,accent,0);}
    public static void header(FontRenderer fr,int l,int t,int r,int headH,String title,String subtitle,int accent,int reserveRight){
        boolean two=subtitle!=null&&headH>=26;int room=Math.max(24,r-l-30-Math.max(24,reserveRight)),fh=fr.FONT_HEIGHT;
        int titleY=two?t+Math.max(2,(headH-(fh*2+2))/2):t+Math.max(0,(headH-fh)/2);
        drawFitted(fr,"§l"+title,l+15,titleY,room,TEXT);
        if(two)drawFitted(fr,subtitle,l+15,titleY+fh+2,room,TEXT_MUTE);
    }
    public static void rowCard(int l,int t,int r,int b,boolean hovered,boolean selected,int accent){rowCard(l,t,r,b,hovered?1f:0f,selected,accent,1f);}
    /** hover 0..1 — плавное появление подсветки строки. */
    public static void rowCard(int l,int t,int r,int b,float hover,boolean selected,int accent){rowCard(l,t,r,b,hover,selected,accent,1f);}
    /** fade 0..1 — ступенчатое появление строки при открытии списка. */
    public static void rowCard(int l,int t,int r,int b,float hover,boolean selected,int accent,float fade){
        float fa=Math.max(0f,Math.min(1f,fade));if(fa<=0.03f)return;
        float h=ease(Math.max(0f,Math.min(1f,hover)));float rr=rad(r-l,b-t);
        int fill=selected?CARD_SEL:lerp(CARD,CARD_HOVER,h);
        if(selected)glow(l,t,r,b,rr,CARD_SELB,0.20f*fa);
        else if(h>0.02f)glow(l,t,r,b,rr,accent,0.10f*h*fa);
        fillRound(l,t,r,b,rr,alpha(tint(fill,0xFFFFFF,0.09f),fa),alpha(tint(fill,0x000000,0.07f),fa));
        if(b-t>9){
            float bar=(selected?0.92f:0.55f*h)*fa;
            if(bar>0.01f){int bc=selected?CARD_SELB:accent;fillRound(l+2,t+3,l+4,b-3,1f,alpha(bc,bar),alpha(bc,bar));}
        }
        strokeRound(l,t,r,b,rr,alpha(selected?CARD_SELB:lerp(BORDER,lerp(BORDER,accent,0.55f),h),fa),1f);
    }
    /** Slot cards have one border only: no inner bar that looks like a second scrollbar. */
    public static void slotCard(int l,int t,int r,int b,boolean hovered,boolean selected,int accent){slotCard(l,t,r,b,hovered?1f:0f,selected,accent);}
    public static void slotCard(int l,int t,int r,int b,float hover,boolean selected,int accent){
        float h=ease(Math.max(0f,Math.min(1f,hover)));float rr=rad(r-l,b-t);
        int fill=selected?CARD_SEL:lerp(CARD,CARD_HOVER,h);
        if(selected)glow(l,t,r,b,rr,CARD_SELB,0.20f);else if(h>0.02f)glow(l,t,r,b,rr,accent,0.10f*h);
        fillRound(l,t,r,b,rr,tint(fill,0xFFFFFF,0.09f),tint(fill,0x000000,0.07f));
        strokeRound(l,t,r,b,rr,selected?CARD_SELB:lerp(BORDER,lerp(BORDER,accent,0.55f),h),1f);
    }
    public static int chip(FontRenderer fr,String text,int x,int y,int color){
        int w=fr.getStringWidth(text)+8;fillRound(x,y,x+w,y+11,5.5f,alpha(color,0.16f),alpha(color,0.16f));
        strokeRound(x,y,x+w,y+11,5.5f,alpha(color,0.5f),1f);fr.drawString(text,x+4,y+(11-fr.FONT_HEIGHT)/2,color);return w;
    }
    public static void badge(FontRenderer fr,String glyph,int x,int y,int size,int color,boolean active){
        int fill=alpha(color,active?0.30f:0.13f),text=active?lerp(color,0xFFFFFFFF,0.32f):alpha(color,0.76f);float rr=rad(size,size);
        fillRound(x,y,x+size,y+size,rr,tint(fill,0xFFFFFF,0.12f),fill);
        strokeRound(x,y,x+size,y+size,rr,alpha(color,active?0.92f:0.42f),1f);
        if(isNumber(glyph)){drawNumber(fr,glyph,x,y,x+size,y+size,text,false);return;}
        drawVectorIcon(glyph,x+size/2f,y+size/2f,Math.max(0.78f,Math.min(1.15f,size/14f)),text);
    }
    private static boolean isNumber(String s){if(s==null||s.length()==0||s.length()>3)return false;for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c<'0'||c>'9')return false;}return true;}
    private static void drawNumber(FontRenderer fr,String value,int l,int t,int r,int b,int color,boolean rightAligned){
        int raw=Math.max(1,fr.getStringWidth(value));
        int x=rightAligned?r-4-raw:l+((r-l-raw)/2);int y=t+Math.max(0,(b-t-fr.FONT_HEIGHT)/2);
        fr.drawString(value,x,y,color);
    }
    public static void slotNumber(FontRenderer fr,String value,int l,int t,int r,int b,int color,boolean selected){
        int fill=selected?alpha(color,0.20f):0x28101826;float rr=Math.min(5f,Math.min(r-l,b-t)*0.5f);
        fillRound(l,t,r,b,rr,tint(fill,0xFFFFFF,0.08f),fill);strokeRound(l,t,r,b,rr,alpha(color,selected?0.70f:0.28f),1f);
        drawNumber(fr,value,l,t,r,b,selected?TEXT:TEXT_DIM,false);
    }
    public static final int TABLE_HEAD=13,TABLE_ROW=11;
    public static int tableValueX(int l,int r){int w=Math.max(60,r-l);int vx=l+Math.max(56,Math.min(92,w*36/100));return Math.max(l+48,Math.min(vx,r-72));}
    public static int tableHeight(int rows){return TABLE_HEAD+Math.max(0,rows)*TABLE_ROW;}
    public static int tableRowsFit(int avail){return Math.max(0,(avail-TABLE_HEAD)/TABLE_ROW);}
    /** Плоская таблица. bottom — жёсткий предел: ниже рамки ничего не рисуется. */
    public static int infoTable(FontRenderer fr,String[] labels,String[] values,int l,int t,int r){
        return infoTable(fr,labels,values,l,t,r,t+4096);
    }
    public static int infoTable(FontRenderer fr,String[] labels,String[] values,int l,int t,int r,int bottom){
        int n=labels==null?0:labels.length;if(fr==null||n==0||r-l<70||t+TABLE_HEAD>bottom)return 0;
        int vx=tableValueX(l,r),labelW=Math.max(24,vx-l-8),valueW=Math.max(24,r-vx-2);
        drawFitted(fr,Lang.s("Параметр","Parameter","Параметр","Parameter","Parametr"),l+2,t,labelW,0xFF6F8AA8);
        drawFitted(fr,Lang.s("Значение","Value","Значення","Wert","Wartość"),vx,t,valueW,0xFF6F8AA8);
        int shown=0;
        for(int i=0;i<n;i++){
            int y=t+TABLE_HEAD+i*TABLE_ROW;
            if(y+TABLE_ROW-2>bottom)break;
            drawFitted(fr,labels[i]==null?"":labels[i],l+2,y,labelW,TEXT_DIM);
            drawFitted(fr,values==null||i>=values.length||values[i]==null?"":values[i],vx,y,valueW,TEXT);
            shown++;
        }
        return tableHeight(shown);
    }
    public static void tableHeader(FontRenderer fr,String left,String right,int l,int r,int y){
        int vx=tableValueX(l,r);
        drawFitted(fr,left,l+2,y,Math.max(24,vx-l-8),0xFF6F8AA8);drawFitted(fr,right,vx,y,Math.max(24,r-vx-2),0xFF6F8AA8);
    }
    public static void tableRow(FontRenderer fr,String label,String value,int l,int r,int y){tableRow(fr,label,value,l,r,y,false);}
    public static void tableRow(FontRenderer fr,String label,String value,int l,int r,int y,boolean alt){
        int vx=tableValueX(l,r);
        drawFitted(fr,label==null?"":label,l+2,y,Math.max(24,vx-l-8),TEXT_DIM);
        drawFitted(fr,value==null?"":value,vx,y,Math.max(24,r-vx-2),TEXT);
    }
    public static int drawWrapped(FontRenderer fr,String text,int x,int y,int maxW,int maxLines,int color){
        if(text==null||maxW<=0||maxLines<=0)return 0;
        List<String> lines=fr.listFormattedStringToWidth(text,maxW);int n=Math.min(maxLines,lines.size()),yy=y;
        for(int i=0;i<n;i++){String line=lines.get(i);if(i==n-1&&lines.size()>n){while(line.length()>1&&fr.getStringWidth(line+"...")>maxW)line=line.substring(0,line.length()-1);line=line+"...";}fr.drawString(line,x,yy,color);yy+=10;}return n*10;
    }
    public static boolean isControlSymbol(String s){return "+".equals(s)||"-".equals(s)||"−".equals(s)||"<".equals(s)||">".equals(s)||"‹".equals(s)||"›".equals(s)||"×".equals(s);}
    public static void controlSymbol(String glyph,float x,float y,float scale,int color){drawVectorIcon(glyph,x,y,scale,color);}
    private static void drawVectorIcon(String g,float x,float y,float s,int c){
        if(g==null)g="";beginDraw();glColor(c);GL11.glEnable(GL11.GL_LINE_SMOOTH);GL11.glHint(GL11.GL_LINE_SMOOTH_HINT,GL11.GL_NICEST);GL11.glLineWidth(1.35f);
        if(g.matches("[0-9]+")){}
        else if(g.equals("\u25cf")){circle(x,y,2.7f*s,24,true);}
        else if(g.equals("\u25b6")){fillPoly(x-2.1f*s,y-3.1f*s,x+2.8f*s,y,x-2.1f*s,y+3.1f*s);}
        else if(g.equals(">")||g.equals("›")||g.equals("\u203a")){poly(false,x-1.8f*s,y-2.8f*s,x+2.0f*s,y,x-1.8f*s,y+2.8f*s);}
        else if(g.equals("<")||g.equals("‹")||g.equals("\u2039")){poly(false,x+1.8f*s,y-2.8f*s,x-2.0f*s,y,x+1.8f*s,y+2.8f*s);}
        else if(g.equals("\u270e")||g.equals("edit")){line(x-2.4f*s,y+2.3f*s,x+2.3f*s,y-2.4f*s,x+2.3f*s,y-2.4f*s,x+3.0f*s,y-1.7f*s,x+3.0f*s,y-1.7f*s,x-1.7f*s,y+3.0f*s,x-2.4f*s,y+2.3f*s,x-1.7f*s,y+3.0f*s);}
        else if(g.equals("copy")||g.equals("\u2398")){poly(true,x-2.8f*s,y-1.6f*s,x+1.2f*s,y-1.6f*s,x+1.2f*s,y+2.8f*s,x-2.8f*s,y+2.8f*s);poly(true,x-1.2f*s,y-2.8f*s,x+2.8f*s,y-2.8f*s,x+2.8f*s,y+1.6f*s,x+1.2f*s,y+1.6f*s);}
        else if(g.equals("\u21bb")||g.equals("~")){arc(x,y,2.8f*s,35,315,20);line(x+2.0f*s,y-2.1f*s,x+3.3f*s,y-2.2f*s,x+2.8f*s,y-0.8f*s);}
        else if(g.equals("\u25a0")){fillPoly(x-2.4f*s,y-2.4f*s,x+2.4f*s,y-2.4f*s,x+2.4f*s,y+2.4f*s,x-2.4f*s,y+2.4f*s);}
        else if(g.equals("\u00d7")||g.equals("×")){line(x-2.4f*s,y-2.4f*s,x+2.4f*s,y+2.4f*s,x+2.4f*s,y-2.4f*s,x-2.4f*s,y+2.4f*s);}
        else if(g.equals("+")||g.equals("−")||g.equals("-")){line(x-2.7f*s,y,x+2.7f*s,y);if(g.equals("+"))line(x,y-2.7f*s,x,y+2.7f*s);}
        else if(g.equals("\u2191")||g.equals("\u2193")){float d=g.equals("\u2191")?-1f:1f;line(x,y-3f*s,x,y+3f*s,x-2.2f*s,y+d*1.1f*s,x,y+d*3f*s,x+2.2f*s,y+d*1.1f*s,x,y+d*3f*s);}
        else if(g.equals("\u2699")){circle(x,y,1.55f*s,18,false);line(x,y-3.4f*s,x,y-2.2f*s,x,y+2.2f*s,x,y+3.4f*s,x-3.4f*s,y,x-2.2f*s,y,x+2.2f*s,y,x+3.4f*s,y);}
        else if(g.equals("\u2709")){fillPoly(x-3.2f*s,y-2.2f*s,x+3.2f*s,y-2.2f*s,x+3.2f*s,y+2.4f*s,x-3.2f*s,y+2.4f*s);line(x-3f*s,y-1.8f*s,x,y+0.5f*s,x,y+0.5f*s,x+3f*s,y-1.8f*s);}
        else if(g.equals("\u2691")){line(x-2.4f*s,y-3.3f*s,x-2.4f*s,y+3.3f*s);fillPoly(x-2.2f*s,y-3f*s,x+2.7f*s,y-2f*s,x-2.2f*s,y-0.7f*s);}
        else if(g.equals("\u2248")||g.equals("\u2500")){poly(false,x-3.2f*s,y-1.4f*s,x-1f*s,y-2f*s,x+1f*s,y-0.8f*s,x+3.2f*s,y-1.4f*s);poly(false,x-3.2f*s,y+1.4f*s,x-1f*s,y+0.8f*s,x+1f*s,y+2f*s,x+3.2f*s,y+1.4f*s);}
        else if(g.equals("\u221e")){arc(x-1.7f*s,y,1.8f*s,35,325,18);arc(x+1.7f*s,y,1.8f*s,215,505,18);}
        else if(g.equals("\u25cb")){circle(x,y,2.7f*s,24,false);}
        else if(g.equals("\u00bb")){poly(false,x-3f*s,y-2.7f*s,x-0.4f*s,y,x-3f*s,y+2.7f*s);poly(false,x+0.2f*s,y-2.7f*s,x+2.8f*s,y,x+0.2f*s,y+2.7f*s);}
        else if(g.equals("\u2726")||g.equals("*")){line(x,y-3.3f*s,x,y+3.3f*s,x-3.3f*s,y,x+3.3f*s,y,x-2.2f*s,y-2.2f*s,x+2.2f*s,y+2.2f*s,x+2.2f*s,y-2.2f*s,x-2.2f*s,y+2.2f*s);}
        else if(g.equals("\u2694")){line(x-2.7f*s,y-2.7f*s,x+2.7f*s,y+2.7f*s,x+2.7f*s,y-2.7f*s,x-2.7f*s,y+2.7f*s,x-3.1f*s,y+1.8f*s,x-1.8f*s,y+3.1f*s,x+3.1f*s,y+1.8f*s,x+1.8f*s,y+3.1f*s);}
        else if(g.equals("\u23f1")){circle(x,y+0.3f*s,2.7f*s,22,false);line(x,y-3.8f*s,x,y-2.8f*s,x,y+0.3f*s,x+1.6f*s,y+1.2f*s);}
        else if(g.equals("?")){poly(false,x-2.1f*s,y-1.8f*s,x-1.4f*s,y-2.8f*s,x+1.2f*s,y-2.8f*s,x+2.1f*s,y-1.7f*s,x+2.1f*s,y-0.7f*s,x,y+0.9f*s,x,y+1.7f*s);line(x,y+3f*s,x,y+3.15f*s);}
        else if(g.equals("\u266b")){circle(x-1.0f*s,y+1.9f*s,1.7f*s,16,true);line(x+0.7f*s,y+1.9f*s,x+0.7f*s,y-3.0f*s);poly(false,x+0.7f*s,y-3.0f*s,x+2.0f*s,y-2.6f*s,x+3.1f*s,y-1.4f*s);}
        else if(g.equals("T")){line(x-2.8f*s,y-2.7f*s,x+2.8f*s,y-2.7f*s);line(x,y-2.7f*s,x,y+3.1f*s);}
        else{fillPoly(x,y-3f*s,x+3f*s,y,x,y+3f*s,x-3f*s,y);}
        GL11.glLineWidth(1f);GL11.glDisable(GL11.GL_LINE_SMOOTH);endDraw();
    }
    private static void line(float...p){GL11.glBegin(GL11.GL_LINES);for(int i=0;i+1<p.length;i+=2)GL11.glVertex2f(p[i],p[i+1]);GL11.glEnd();}
    private static void poly(boolean loop,float...p){GL11.glBegin(loop?GL11.GL_LINE_LOOP:GL11.GL_LINE_STRIP);for(int i=0;i+1<p.length;i+=2)GL11.glVertex2f(p[i],p[i+1]);GL11.glEnd();}
    private static void fillPoly(float...p){GL11.glBegin(GL11.GL_TRIANGLE_FAN);for(int i=0;i+1<p.length;i+=2)GL11.glVertex2f(p[i],p[i+1]);GL11.glEnd();}
    private static void circle(float x,float y,float r,int n,boolean fill){GL11.glBegin(fill?GL11.GL_TRIANGLE_FAN:GL11.GL_LINE_LOOP);if(fill)GL11.glVertex2f(x,y);for(int i=0;i<=(fill?n:n-1);i++){double a=Math.PI*2.0*i/n;GL11.glVertex2f(x+(float)Math.cos(a)*r,y+(float)Math.sin(a)*r);}GL11.glEnd();}
    private static void arc(float x,float y,float r,float from,float to,int n){GL11.glBegin(GL11.GL_LINE_STRIP);for(int i=0;i<=n;i++){double a=Math.toRadians(from+(to-from)*i/n);GL11.glVertex2f(x+(float)Math.cos(a)*r,y+(float)Math.sin(a)*r);}GL11.glEnd();}
    /** Bitmap font of 1.12.2 cannot be scaled: GlStateManager.scale makes glyphs overlap and look doubled. Fit by ellipsis instead. */
    public static String ellipsize(FontRenderer fr,String text,int maxW){
        if(text==null)return "";if(maxW<=0)return "";if(fr.getStringWidth(text)<=maxW)return text;
        String dots="...";int dw=fr.getStringWidth(dots);if(dw>=maxW)return "";
        String s=text;while(s.length()>0&&fr.getStringWidth(s)+dw>maxW)s=s.substring(0,s.length()-1);return s+dots;
    }
    public static float fitScale(FontRenderer fr,String text,int maxW){return 1f;}
    public static int fittedWidth(FontRenderer fr,String text,int maxW){return Math.min(maxW,fr.getStringWidth(ellipsize(fr,text,maxW)));}
    public static void drawFitted(FontRenderer fr,String text,float x,float y,int maxW,int color){drawFitted0(fr,text,x,y,maxW,color,false);}
    public static void drawFittedShadow(FontRenderer fr,String text,float x,float y,int maxW,int color){drawFitted0(fr,text,x,y,maxW,color,true);}
    private static void drawFitted0(FontRenderer fr,String text,float x,float y,int maxW,int color,boolean shadow){
        if(text==null||maxW<=0)return;String shown=ellipsize(fr,text,maxW);int ix=Math.round(x),iy=Math.round(y);
        if(shadow)fr.drawStringWithShadow(shown,ix,iy,color);else fr.drawString(shown,ix,iy,color);resetColor();
    }
    public static void drawCenteredFitted(FontRenderer fr,String text,float centerX,float y,int maxW,int color,boolean shadow){int fw=fittedWidth(fr,text,maxW);drawFitted0(fr,text,centerX-fw/2f,y,maxW,color,shadow);}
    public static void tooltip(FontRenderer fr,String[] lines,int mx,int my,int screenW,int screenH){tooltip(fr,lines,mx,my,screenW,screenH,tipFade());}
    /** fade 0..1 — подсказка проявляется плавно и слегка подъезжает вверх. */
    public static void tooltip(FontRenderer fr,String[] lines,int mx,int my,int screenW,int screenH,float fade){
        float fa=Math.max(0f,Math.min(1f,fade));if(fa<=0.03f)return;
        if(lines==null||lines.length==0||!tipsEnabled())return;
        int maxText=Math.max(70,Math.min(280,screenW-46));
        List<String> shown=new ArrayList<String>();
        for(String line:lines){
            if(line==null){shown.add("");continue;}
            List<String> part=fr.listFormattedStringToWidth(line,maxText);
            if(part.isEmpty())shown.add("");else shown.addAll(part);
        }
        if(shown.isEmpty())return;
        int textW=0;for(String line:shown)textW=Math.max(textW,fr.getStringWidth(line));
        textW=Math.max(24,Math.min(textW,maxText));
        int pad=6,boxW=textW+pad*2,boxH=shown.size()*10+pad*2-2;
        int bx=mx+12,by=my+10;
        if(bx+boxW>screenW-4)bx=mx-8-boxW;
        if(bx<4)bx=Math.max(4,(screenW-boxW)/2);
        if(by+boxH>screenH-4)by=screenH-4-boxH;
        if(by<4)by=4;
        by+=Math.round((1f-fa)*4f);
        shadow(bx,by,bx+boxW,by+boxH,fa);
        card(bx,by,bx+boxW,by+boxH,alpha(PANEL,fa),alpha(BORDER_HI,fa));
        for(int i=0;i<shown.size();i++)drawFitted(fr,shown.get(i),bx+pad,by+pad+i*10,textW,alpha(i==0?TEXT:TEXT_DIM,fa));
    }
    public static void buttonTooltip(FontRenderer fr,List<GuiButton> buttons,int mx,int my,int screenW,int screenH){if(!tipsEnabled())return;for(GuiButton g:buttons)if(g instanceof StyledButton){StyledButton b=(StyledButton)g;if(b.visible&&b.tooltip!=null&&mx>=b.x&&my>=b.y&&mx<b.x+b.width&&my<b.y+b.height){if(tipReady(b))tooltip(fr,b.tooltip,mx,my,screenW,screenH);return;}}}
    private static boolean tipsEnabled(){try{MirrorRecorder m=MirrorRecorder.getInstance();return m==null||m.getConfig()==null||m.getConfig().isShowTooltips();}catch(Throwable ignored){return true;}}
    public static void scrollBar(int x,int top,int bottom,int visible,int total,int offset,int color){scrollBar(x,top,bottom,visible,total,offset,color,0f);}
    /** Версия с дробным смещением: ползунок едет вместе со списком. */
    public static void scrollBar(int x,int top,int bottom,int visible,int total,float offset,int color,float hover){
        if(total<=visible||bottom-top<8)return;
        float t=ease(hover);int w=4+Math.round(2f*t);
        fillRound(x,top,x+w,bottom,w*0.5f,0x30101826,0x30101826);
        int track=bottom-top,h=Math.min(track,Math.max(18,track*visible/total));
        float max=Math.max(1f,total-visible),pos=top+(track-h)*Math.max(0f,Math.min(max,offset))/max;
        int knob=lerp(alpha(color,0.62f),lerp(color,0xFFFFFFFF,0.14f),t);
        fillRound(x,pos,x+w,pos+h,w*0.5f,knob,knob);
    }
    public static void scrollBar(int x,int top,int bottom,int visible,int total,int offset,int color,float hover){
        if(total<=visible||bottom-top<8)return;
        float t=ease(hover);int w=4+Math.round(2f*t);
        fillRound(x,top,x+w,bottom,w*0.5f,0x30101826,0x30101826);
        int track=bottom-top,h=Math.min(track,Math.max(18,track*visible/total)),max=Math.max(1,total-visible);
        int pos=top+(track-h)*Math.max(0,Math.min(max,offset))/max;
        int knob=lerp(alpha(color,0.62f),lerp(color,0xFFFFFFFF,0.14f),t);
        fillRound(x,pos,x+w,pos+h,w*0.5f,knob,knob);
    }
}
