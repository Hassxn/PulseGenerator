package dev.hassxn.pulse;

import java.lang.reflect.*;

/** Prefer the server's TPS counter; scheduler cadence is not a server TPS counter. */
final class TpsMonitor {
    private final Object server;
    private final Method apiTps;
    private final Object handle;
    private final Field recentTps;
    private long windowStart;
    private int ticks;
    private double fallback=20;
    private boolean nativeReading;

    TpsMonitor(Object server,long now){
        this.server=server;windowStart=now;
        Method api=null;Object nativeServer=null;Field field=null;
        try{api=server.getClass().getMethod("getTPS");}catch(ReflectiveOperationException ignored){}
        try{
            nativeServer=server.getClass().getMethod("getServer").invoke(server);
            field=nativeServer.getClass().getField("recentTps");
        }catch(ReflectiveOperationException | NullPointerException ignored){}
        apiTps=api;handle=nativeServer;recentTps=field;
    }
    double tick(long now){
        ticks++;
        long elapsed=now-windowStart;
        if(elapsed>=5000000000L){
            fallback=Math.min(20,ticks*1000000000.0/elapsed);
            windowStart=now;ticks=0;
        }
        return current();
    }
    double current(){
        double value=Double.NaN;
        if(apiTps!=null)try{value=first(apiTps.invoke(server));}catch(ReflectiveOperationException | RuntimeException ignored){}
        if(Double.isNaN(value)&&recentTps!=null)try{value=first(recentTps.get(handle));}catch(ReflectiveOperationException | RuntimeException ignored){}
        nativeReading=!Double.isNaN(value);
        return nativeReading?value:fallback;
    }
    boolean isNative(){return nativeReading;}
    private static double first(Object value){
        if(!(value instanceof double[])||((double[])value).length==0)return Double.NaN;
        double tps=((double[])value)[0];
        return Double.isNaN(tps)||Double.isInfinite(tps)||tps<0?Double.NaN:Math.min(20,tps);
    }
}
