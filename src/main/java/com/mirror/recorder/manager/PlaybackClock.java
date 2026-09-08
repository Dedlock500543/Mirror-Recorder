package com.mirror.recorder.manager;
/** Своя временная шкала повтора, полностью независимая от FPS и от времени кадра.
 *
 *  Шаг делает игровой тик, а не отрисовка, поэтому 20/30/60/144 FPS, лаги и просадки на скорость
 *  повтора не влияют вообще. Остаток хранится целым числом тысячных кадра, поэтому ошибка округления
 *  не накапливается ни за 10 минут, ни за час: за N тиков проходит ровно N*speed кадров.
 *  Класс намеренно не знает ни про Minecraft, ни про конфиг — его проверяет автотест (/mirror selftest). */
public final class PlaybackClock{
    /** Одна тысячная кадра — единица аккумулятора. */
    public static final int UNIT=1000,MIN_SPEED=250,MAX_SPEED=4000;
    private int remainder=0;
    /** Скорость в тысячных: округление один раз здесь, дальше только целые числа. */
    public static int toMilli(double speed){long v=Math.round(speed*UNIT);if(v<MIN_SPEED)v=MIN_SPEED;if(v>MAX_SPEED)v=MAX_SPEED;return (int)v;}
    public void reset(){remainder=0;}
    public int getRemainder(){return remainder;}
    /** Сколько кадров пройти в этом тике. Дробная часть переносится точно, без float. */
    public int advance(int speedMilli){
        if(speedMilli<MIN_SPEED)speedMilli=MIN_SPEED;
        if(speedMilli>MAX_SPEED)speedMilli=MAX_SPEED;
        remainder+=speedMilli;int steps=remainder/UNIT;remainder-=steps*UNIT;return steps;
    }
    /** Непройденные шаги возвращаются в аккумулятор: конец записи не съедает дробную часть шкалы. */
    public void giveBack(int steps){if(steps>0)remainder+=steps*UNIT;}
}
