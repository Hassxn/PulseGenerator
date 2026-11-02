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

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** Immutable generation profile, stored separately for each world. */
public final class Profile {
    final YamlConfiguration yaml;
    final int size, height, spawnSize, transition, roadWidth, roadTransition, eventSize, eventTransition;
    final double amplitude, scale;
    final boolean roads, events, caves, trees;
    final Material road;
    final List<int[]> centers = new ArrayList<int[]>();
    final List<Ore> ores = new ArrayList<Ore>();

    Profile(YamlConfiguration y, int worldSize) {
        if(worldSize<512||worldSize>30000)throw new IllegalArgumentException("Tamanio fuera de rango: 512-30000");
        yaml = y; size = worldSize;
        height = bounded(y.getInt("terrain.base-height", 68), 48, 100);
        amplitude = Math.max(0, Math.min(18, y.getDouble("terrain.hill-amplitude", 9)));
        scale = Math.max(96, y.getDouble("terrain.scale", 180));
        spawnSize = bounded(y.getInt("spawn.size", 160), 150, size - 32);
        transition = bounded(y.getInt("spawn.transition", 48), 16, 256);
        roads = y.getBoolean("roads.enabled", true);
        roadWidth = bounded(y.getInt("roads.width", 12), 4, 64);
        roadTransition = bounded(y.getInt("roads.transition", 16), 4, 128);
        Material m = Material.matchMaterial(y.getString("roads.material", "GRAVEL"));
        if (m == null || !m.isSolid() || m == Material.AIR) throw new IllegalArgumentException("Material de roads invalido");
        road = m;
        events = y.getBoolean("events.enabled", true);
        eventSize = bounded(y.getInt("events.size", 64), 16, 256);
        eventTransition = bounded(y.getInt("events.transition", 24), 8, 128);
        for (String s : y.getStringList("events.centers")) {
            String[] xy = s.split(",");
            if (xy.length != 2) throw new IllegalArgumentException("Centro de evento invalido: " + s);
            int x = Integer.parseInt(xy[0].trim()), z = Integer.parseInt(xy[1].trim());
            if (Math.abs((long)x) + eventSize / 2 + eventTransition < size / 2 && Math.abs((long)z) + eventSize / 2 + eventTransition < size / 2)
                centers.add(new int[]{x,z});
        }
        caves = y.getBoolean("caves.enabled", true); trees = y.getBoolean("trees.enabled", true);
        ConfigurationSection sec = y.getConfigurationSection("ores");
        double multiplier = Math.max(0, Math.min(8, y.getDouble("profiles." + y.getString("profile", "hcf"), 1.65)));
        if (sec != null) for (String key : sec.getKeys(false)) {
            Material type = Material.matchMaterial(key);
            if (type == null || !key.endsWith("_ORE")) throw new IllegalArgumentException("Mineral invalido: " + key);
            ConfigurationSection o = sec.getConfigurationSection(key);
            if (o == null) throw new IllegalArgumentException("Configuracion invalida: " + key);
            int low = bounded(o.getInt("min-y", 5), 2, 120), high = bounded(o.getInt("max-y", 32), low, 120);
            ores.add(new Ore(type, low, high, Math.min(200, (int)Math.round(Math.max(0,o.getDouble("attempts", 1))*multiplier)),
                bounded(o.getInt("vein-size", 6), 1, 32), Math.max(0, Math.min(1,o.getDouble("chance",1)))));
        }
    }
    static int bounded(int v, int min, int max) { return Math.max(min, Math.min(max,v)); }
    boolean spawn(int x, int z) { return x >= -spawnSize/2 && x < (spawnSize+1)/2 && z >= -spawnSize/2 && z < (spawnSize+1)/2; }
    boolean road(int x, int z) { return roads && (Math.abs(x+.5) <= roadWidth/2.0 || Math.abs(z+.5) <= roadWidth/2.0); }
    boolean event(int x, int z) {
        if (events) for(int[] c: centers) if(Math.abs(x-c[0]+.5)<=eventSize/2.0 && Math.abs(z-c[1]+.5)<=eventSize/2.0) return true;
        return false;
    }
    boolean clear(int x, int z) { return spawn(x,z) || road(x,z) || event(x,z); }
    boolean inside(int x, int z) { return x>=-size/2 && x<(size+1)/2 && z>=-size/2 && z<(size+1)/2; }
    double flattenWeight(int x, int z) {
        double d = Math.max(Math.abs(x+.5),Math.abs(z+.5))-spawnSize/2.0;
        double w = fade(d, transition);
        if(roads) w = Math.min(w,fade(Math.min(Math.abs(x+.5),Math.abs(z+.5))-roadWidth/2.0,roadTransition));
        if(events) for(int[] c: centers) w = Math.min(w,fade(Math.max(Math.abs(x-c[0]+.5),Math.abs(z-c[1]+.5))-eventSize/2.0,eventTransition));
        return w;
    }
    private static double fade(double d, double range) { double t = Math.max(0,Math.min(1,d/range)); return t*t*(3-2*t); }
    void save(File f) throws IOException { yaml.set("world-size",size); yaml.save(f); }
    static final class Ore {
        final Material type; final int min,max,attempts,vein; final double chance;
        Ore(Material t,int a,int b,int n,int v,double c) { type=t; min=a;max=b;attempts=n;vein=v;chance=c; }
    }
}
