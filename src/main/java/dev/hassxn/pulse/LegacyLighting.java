package dev.hassxn.pulse;

import org.bukkit.Chunk;
import java.lang.reflect.*;

/** Spigot 1.8.8 defers this lighting pass unless explicitly requested. */
final class LegacyLighting {
    private Method handle,lit,relight,done;
    private Field mustSave;
    void complete(Chunk chunk)throws ReflectiveOperationException{
        if(handle==null){
            handle=chunk.getClass().getMethod("getHandle");
            Class<?> type=handle.getReturnType();
            if(!type.getName().startsWith("net.minecraft.server.v1_8_R3."))throw new IllegalStateException("La pregeneracion completa requiere Spigot 1.8.8 (v1_8_R3).");
            lit=type.getMethod("u");relight=type.getMethod("n");done=type.getMethod("isDone");mustSave=type.getField("mustSave");
        }
        Object nativeChunk=handle.invoke(chunk);
        if(!Boolean.TRUE.equals(done.invoke(nativeChunk)))throw new IllegalStateException("Poblacion pendiente en "+chunk.getX()+","+chunk.getZ());
        if(!Boolean.TRUE.equals(lit.invoke(nativeChunk)))relight.invoke(nativeChunk);
        if(!Boolean.TRUE.equals(lit.invoke(nativeChunk)))throw new IllegalStateException("Iluminacion pendiente en "+chunk.getX()+","+chunk.getZ());
        mustSave.setBoolean(nativeChunk,true);
    }
}
