package dev.hassxn.pulse;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

public class GenerationTest {
    private Profile profile()throws Exception{
        YamlConfiguration y=new YamlConfiguration();y.load(new File("src/main/resources/config.yml"));
        YamlConfiguration ores=new YamlConfiguration();ores.load(new File("src/main/resources/ores.yml"));
        for(String key:ores.getKeys(false))y.set(key,ores.get(key));
        YamlConfiguration trees=new YamlConfiguration();trees.load(new File("src/main/resources/trees.yml"));y.set("trees",trees);
        YamlConfiguration mines=new YamlConfiguration();mines.load(new File("src/main/resources/mines.yml"));y.set("caves",mines.get("caves"));
        return new Profile(y,3000);
    }
    @Test public void spawnIsFlatClearAndSolid()throws Exception{
        Profile p=profile();HcfGenerator g=new HcfGenerator(p);
        for(int cx=-5;cx<5;cx++)for(int cz=-5;cz<5;cz++){
            byte[][] b=g.generate(12345,cx,cz);
            for(int x=0;x<16;x++)for(int z=0;z<16;z++){
                assertEquals(Material.GRASS.getId(),HcfGenerator.get(b,x,p.height,z));
                for(int y=1;y<p.height-3;y++)assertEquals(Material.STONE.getId(),HcfGenerator.get(b,x,y,z));
                for(int y=p.height+1;y<85;y++)assertEquals(0,HcfGenerator.get(b,x,y,z));
            }
        }
    }
    @Test public void deterministicRegardlessOfChunkOrder()throws Exception{
        HcfGenerator g=new HcfGenerator(profile());byte[][] a=g.generate(123,51,-37);
        g.generate(123,52,-37);g.generate(123,50,-38);
        assertTrue(Arrays.deepEquals(a,g.generate(123,51,-37)));
        assertFalse(Arrays.deepEquals(a,g.generate(124,51,-37)));
    }
    @Test public void terrainHasNoCliffsAndRoadsAreClear()throws Exception{
        Profile p=profile();Terrain t=new Terrain(p,91);HcfGenerator g=new HcfGenerator(p);
        for(int x=-1500;x<1500;x+=3)for(int z=-1500;z<1500;z+=7){
            assertTrue(Math.abs(t.height(x,z)-t.height(x+1,z))<=1);
            assertTrue(Math.abs(t.height(x,z)-t.height(x,z+1))<=1);
        }
        byte[][] b=g.generate(91,0,40);
        for(int x=0;x<6;x++)for(int z=0;z<16;z++){
            assertEquals(Material.GRAVEL.getId(),HcfGenerator.get(b,x,p.height,z));
            for(int y=p.height+1;y<85;y++)assertEquals(0,HcfGenerator.get(b,x,y,z));
        }
    }
    @Test public void naturalCavesAndEnrichedOresExist()throws Exception{
        Profile p=profile();HcfGenerator rich=new HcfGenerator(p);
        YamlConfiguration low=new YamlConfiguration();low.loadFromString(p.yaml.saveToString());low.set("profile","vanilla");
        HcfGenerator vanilla=new HcfGenerator(new Profile(low,3000));
        int cavities=0,richOres=0,baseOres=0;
        for(int cx=25;cx<35;cx++)for(int cz=25;cz<35;cz++){
            byte[][] a=rich.generate(321,cx,cz),b=vanilla.generate(321,cx,cz);
            for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=5;y<48;y++){
                int id=HcfGenerator.get(a,x,y,z);if(id==0)cavities++;
                if(isOre(id))richOres++;if(isOre(HcfGenerator.get(b,x,y,z)))baseOres++;
            }
        }
        assertTrue("Natural caves",cavities>100);assertTrue("HCF > vanilla",richOres>baseOres*1.2);
    }
    private boolean isOre(int id){return id==14||id==15||id==16||id==21||id==56||id==73||id==129;}
    @Test public void boundsAndEventPads()throws Exception{
        Profile p=profile();assertTrue(p.inside(-1500,-1500));assertTrue(p.inside(1499,1499));assertFalse(p.inside(1500,0));
        Terrain t=new Terrain(p,45);for(int[] c:p.centers)for(int x=-32;x<32;x++)for(int z=-32;z<32;z++)assertEquals(p.height,t.height(c[0]+x,c[1]+z));
    }
}
