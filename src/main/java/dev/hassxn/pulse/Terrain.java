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

import java.util.*;

/** Pure deterministic math: no Bukkit world reads and no chunk recursion. */
final class Terrain {
    final Profile p; final long seed;
    Terrain(Profile p,long seed) { this.p=p;this.seed=seed; }
    static long hash(long seed,int x,int z) {
        long h=seed ^ (x*341873128712L) ^ (z*132897987541L);
        h=(h^(h>>>30))*0xbf58476d1ce4e5b9L; h=(h^(h>>>27))*0x94d049bb133111ebL; return h^(h>>>31);
    }
    static double unit(long h) { return (h>>>11)*0x1.0p-53; }
    double noise(double x,double z,long salt) {
        int ix=(int)Math.floor(x), iz=(int)Math.floor(z);
        double a=x-ix,b=z-iz; a=a*a*(3-2*a); b=b*b*(3-2*b);
        double n00=unit(hash(seed+salt,ix,iz)),n10=unit(hash(seed+salt,ix+1,iz));
        double n01=unit(hash(seed+salt,ix,iz+1)),n11=unit(hash(seed+salt,ix+1,iz+1));
        return ((n00*(1-a)+n10*a)*(1-b)+(n01*(1-a)+n11*a)*b)*2-1;
    }
    int height(int x,int z) {
        double n=.78*noise(x/p.scale,z/p.scale,17)+.22*noise(x/(p.scale*.5),z/(p.scale*.5),91);
        return p.height+(int)Math.round(n*p.amplitude*p.flattenWeight(x,z));
    }
    int caveOriginsInsideBorder(){
        if(!p.caves)return 0;
        int cell=Profile.bounded(p.yaml.getInt("caves.cell-size",96),64,192),count=0;
        for(int ax=Math.floorDiv(-p.size/2,cell);ax<=Math.floorDiv((p.size+1)/2-1,cell);ax++)
            for(int az=Math.floorDiv(-p.size/2,cell);az<=Math.floorDiv((p.size+1)/2-1,cell);az++){
                Random r=new Random(hash(seed+701,ax,az));
                if(r.nextDouble()>p.yaml.getDouble("caves.chance",.65))continue;
                if(p.inside(ax*cell+r.nextInt(cell),az*cell+r.nextInt(cell)))count++;
            }
        return count;
    }
    List<Sphere> caves(int cx,int cz) {
        List<Sphere> out=new ArrayList<Sphere>(); if(!p.caves)return out;
        int cell=Profile.bounded(p.yaml.getInt("caves.cell-size",96),64,192);
        int length=Profile.bounded(p.yaml.getInt("caves.length",90),24,160);
        int reach=(int)Math.ceil((length+8)/(double)cell)+1;
        int gx=Math.floorDiv(cx*16,cell),gz=Math.floorDiv(cz*16,cell);
        int min=Profile.bounded(p.yaml.getInt("caves.min-y",12),6,40);
        int max=Profile.bounded(p.yaml.getInt("caves.max-y",44),min,50);
        double radius=Math.max(1.5,Math.min(4,p.yaml.getDouble("caves.radius",2.6)));
        for(int ax=gx-reach;ax<=gx+reach;ax++)for(int az=gz-reach;az<=gz+reach;az++) {
            Random r=new Random(hash(seed+701,ax,az));
            if(r.nextDouble()>p.yaml.getDouble("caves.chance",.65))continue;
            double x=ax*cell+r.nextInt(cell),z=az*cell+r.nextInt(cell),y=min+r.nextInt(max-min+1),angle=r.nextDouble()*Math.PI*2;
            for(int step=0;step<length;step+=2) {
                angle+=(r.nextDouble()-.5)*.45; x+=Math.cos(angle)*2;z+=Math.sin(angle)*2;
                y=Math.max(min,Math.min(max,y+(r.nextDouble()-.5)*1.6));
                double rad=radius*(.85+.25*Math.sin(step*.13));
                if(x+rad>=cx*16 && x-rad<cx*16+16 && z+rad>=cz*16 && z-rad<cz*16+16)out.add(new Sphere(x,y,z,rad));
            }
        } return out;
    }
    static final class Sphere {
        final double x,y,z,r;
        Sphere(double x,double y,double z,double r){this.x=x;this.y=y;this.z=z;this.r=r;}
    }
}
