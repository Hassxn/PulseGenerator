/*
 * PulseGenerator - HCF world generator / Generador de mundos HCF
 * Copyright (C) 2026 Hassxn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * EN: This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 3,
 * as published by the Free Software Foundation.
 * This program is distributed WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the LICENSE file for the full terms.
 *
 * ES: Este programa es software libre: puede redistribuirlo y/o modificarlo
 * bajo los terminos de la Licencia Publica General de GNU, version 3,
 * publicada por la Free Software Foundation.
 * Se distribuye SIN NINGUNA GARANTIA; ni siquiera la garantia implicita de
 * COMERCIABILIDAD o IDONEIDAD PARA UN PROPOSITO PARTICULAR.
 * Consulte LICENSE para los terminos completos y LICENCIA.md para la
 * explicacion en espanol.
 */

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
