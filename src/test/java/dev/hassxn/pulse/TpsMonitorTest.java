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

import org.junit.Test;
import static org.junit.Assert.*;

public class TpsMonitorTest {
    public static class NativeServer { public double[] recentTps={20,20,20}; }
    public static class Spigot { final NativeServer handle=new NativeServer(); public NativeServer getServer(){return handle;} }
    public static class Paper { public double[] getTPS(){return new double[]{19.8,20,20};} }
    public static class BrokenApi extends Spigot { public double[] getTPS(){throw new IllegalStateException("unavailable");} }

    @Test public void slowSchedulerCannotOverrideNativeServerTps(){
        Spigot s=new Spigot();TpsMonitor m=new TpsMonitor(s,0);
        for(int i=1;i<=300;i++)assertEquals(20,m.tick(i*66000000L),0);
        assertTrue(m.isNative());s.handle.recentTps[0]=12;assertEquals(12,m.current(),0);
        s.handle.recentTps[0]=20;assertEquals(20,m.current(),0);
    }
    @Test public void supportsPublicForkApi(){TpsMonitor m=new TpsMonitor(new Paper(),0);assertEquals(19.8,m.current(),0);assertTrue(m.isNative());}
    @Test public void brokenPublicApiFallsBackToNativeField(){TpsMonitor m=new TpsMonitor(new BrokenApi(),0);assertEquals(20,m.current(),0);assertTrue(m.isNative());}
    @Test public void fallbackMeasuresWindowsAndRecovers(){
        TpsMonitor m=new TpsMonitor(new Object(),0);
        for(int i=1;i<=75;i++)m.tick(i*100000000L);
        assertEquals(10,m.current(),0);assertFalse(m.isNative());
        for(int i=1;i<=150;i++)m.tick(7500000000L+i*50000000L);
        assertEquals(20,m.current(),0);
    }
}
