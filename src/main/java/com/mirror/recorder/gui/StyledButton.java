package com.mirror.recorder.gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.input.Mouse;
/**
 * Кнопка единого стиля: акцент, тумблер, значок слева и вторая строка с коротким описанием.
 * Описание видно сразу, без наведения — подсказка остаётся полной версией.
 */
public class StyledButton extends GuiButton{
    public int accent=UiTheme.ACCENT;public boolean primary=false,toggle=false,on=false;public String[] tooltip=null;
    public String subtitle=null,icon=null,value=null;public boolean compact=false,flat=false;
    private float hover=0f,press=0f,knob=-1f;
    private float hoverFrom=0f,pressFrom=0f,knobFrom=0f;private long hoverAt=0L,pressAt=0L,knobAt=0L;
    private boolean hoverTarget=false,pressTarget=false,knobTarget=false;
    public StyledButton(int id,int x,int y,int w,int h,String label){super(id,x,y,w,h,label);}
    public StyledButton accent(int c){accent=c;return this;}
    public StyledButton primary(){primary=true;return this;}
    public StyledButton asToggle(boolean state){toggle=true;on=state;knob=state?1f:0f;return this;}
    public StyledButton tip(String...t){tooltip=t;return this;}
    public StyledButton sub(String text){subtitle=text;return this;}
    public StyledButton icon(String glyph){icon=glyph;return this;}
    public StyledButton value(String text){value=text;return this;}
    public StyledButton compact(){compact=true;return this;}
    /** Без собственного фона и рамки: часть единого блока. */
    public StyledButton flat(){flat=true;return this;}
    public void setOn(boolean state){on=state;}
    public boolean isHover(int mx,int my){return visible&&mx>=x&&my>=y&&mx<x+width&&my<y+height;}
    @Override public void drawButton(Minecraft mc,int mx,int my,float pt){
        if(!visible)return;
        hovered=mx>=x&&my>=y&&mx<x+width&&my<y+height;
        long now=System.currentTimeMillis();boolean hTarget=hovered&&enabled,pTarget=hTarget&&Mouse.isButtonDown(0);
        if(knob<0f){knob=on?1f:0f;knobFrom=knob;knobTarget=on;knobAt=now;}
        if(hTarget!=hoverTarget){hoverFrom=hover;hoverTarget=hTarget;hoverAt=now;}
        if(pTarget!=pressTarget){pressFrom=press;pressTarget=pTarget;pressAt=now;}
        if(on!=knobTarget){knobFrom=knob;knobTarget=on;knobAt=now;}
        hover=UiTheme.transition(hoverFrom,hoverTarget?1f:0f,hoverAt,now,UiTheme.HOVER_MS);
        press=UiTheme.transition(pressFrom,pressTarget?1f:0f,pressAt,now,90L);
        knob=UiTheme.transition(knobFrom,knobTarget?1f:0f,knobAt,now,170L);
        boolean pressed=press>0.45f;
        float h=hover;
        int acc=toggle?UiTheme.lerp(UiTheme.GRAY,UiTheme.GREEN,UiTheme.ease(knob)):accent;
        if(flat){
            int col=!enabled?UiTheme.alpha(UiTheme.TEXT_MUTE,0.7f):UiTheme.lerp(UiTheme.TEXT_DIM,UiTheme.TEXT,h);
            if(enabled&&h>0.01f)UiTheme.round(x+1,y+1,x+width-1,y+height-1,UiTheme.alpha(UiTheme.ACCENT,0.18f*h));
            if(UiTheme.isControlSymbol(displayString))UiTheme.controlSymbol(displayString,x+width/2f,y+height/2f+(pressed?1:0),Math.max(0.85f,Math.min(1.2f,Math.min(width,height)/14f)),col);
            else drawCenteredString(mc.fontRenderer,displayString,x+width/2,y+(height-mc.fontRenderer.FONT_HEIGHT)/2+(pressed?1:0),col);
            return;
        }
        UiTheme.buttonBg(x,y,x+width,y+height,acc,hover,enabled,pressed,primary&&!toggle);
        int shift=pressed?1:0;
        if(icon==null&&UiTheme.isControlSymbol(displayString)){UiTheme.controlSymbol(displayString,x+width/2f,y+height/2f+shift,Math.max(0.85f,Math.min(1.2f,Math.min(width,height)/14f)),enabled?UiTheme.lerp(UiTheme.TEXT_DIM,UiTheme.TEXT,h):UiTheme.TEXT_MUTE);return;}
        int left=x+8;
        if(icon!=null&&width>=44&&height>=16){
            int size=13,pad=6;
            UiTheme.badge(mc.fontRenderer,icon,x+pad,y+(height-size)/2+shift,size,acc,enabled&&(on||!toggle));
            left=x+pad+size+5;
        }
        int right=x+width-7;
        if(toggle){
            int sw=Math.min(22,Math.max(20,width/9)),sh=Math.max(9,Math.min(11,height-8)),sx=x+width-sw-6,sy=y+(height-sh)/2+shift;
            UiTheme.switchAt(sx,sy,sw,sh,knob,enabled);
            right=sx-6;
        }else if(value!=null){
            String val=value;int maxVal=Math.max(16,width/2-8),vw=UiTheme.fittedWidth(mc.fontRenderer,val,maxVal);
            UiTheme.drawFitted(mc.fontRenderer,val,x+width-9-vw,y+(height-mc.fontRenderer.FONT_HEIGHT)/2+shift,maxVal,enabled?UiTheme.lerp(UiTheme.TEXT_DIM,UiTheme.TEXT,h):UiTheme.TEXT_MUTE);
            right=x+width-13-vw;
        }
        int base=enabled?UiTheme.TEXT_DIM:UiTheme.TEXT_MUTE,live=enabled?UiTheme.TEXT:UiTheme.TEXT_MUTE;
        int textColor=primary&&!toggle&&enabled?0xFFFFFFFF:UiTheme.lerp(base,live,h);
        int room=Math.max(8,right-left);
        boolean twoLine=subtitle!=null&&height>=26&&room>=30;
        if(!toggle&&icon==null&&value==null&&!twoLine&&!compact){
            UiTheme.drawCenteredFitted(mc.fontRenderer,displayString,x+width/2f,y+(height-mc.fontRenderer.FONT_HEIGHT)/2+shift,Math.max(8,width-10),textColor,false);
            return;
        }
        String label=displayString;
        if(twoLine){
            int blockY=y+Math.max(0,(height-(mc.fontRenderer.FONT_HEIGHT*2+1))/2)+shift;
            UiTheme.drawFitted(mc.fontRenderer,label,left,blockY,room,textColor);
            UiTheme.drawFitted(mc.fontRenderer,subtitle,left,blockY+mc.fontRenderer.FONT_HEIGHT+1,room,enabled?UiTheme.TEXT_MUTE:UiTheme.alpha(UiTheme.TEXT_MUTE,0.6f));
        }else{
            UiTheme.drawFitted(mc.fontRenderer,label,left,y+(height-mc.fontRenderer.FONT_HEIGHT)/2+shift,room,textColor);
        }
    }
}
