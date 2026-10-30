package dev.hassxn.pulse;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.*;
import java.util.*;

@SuppressWarnings("deprecation")
public final class HcfGenerator extends ChunkGenerator {
    final Profile p;
    HcfGenerator(Profile p){this.p=p;}
    @Override public byte[][] generateBlockSections(World world,Random random,int cx,int cz,BiomeGrid biomes){
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)biomes.setBiome(x,z,Biome.PLAINS);
        return generate(world.getSeed(),cx,cz);
    }
    byte[][] generate(long seed,int cx,int cz) {
        byte[][] b=new byte[16][];
        Terrain t=new Terrain(p,seed);
        for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
            int wx=cx*16+x,wz=cz*16+z,h=t.height(wx,wz);
            for(int y=0;y<=h;y++)set(b,x,y,z,y==0?Material.BEDROCK:y<h-3?Material.STONE:y<h?Material.DIRT:p.road(wx,wz)&&!p.spawn(wx,wz)?p.road:Material.GRASS);
        }
        for(Terrain.Sphere s:t.caves(cx,cz)) {
            for(int x=Math.max(0,(int)Math.floor(s.x-s.r)-cx*16);x<=Math.min(15,(int)Math.ceil(s.x+s.r)-cx*16);x++)
                for(int z=Math.max(0,(int)Math.floor(s.z-s.r)-cz*16);z<=Math.min(15,(int)Math.ceil(s.z+s.r)-cz*16);z++) {
                    int wx=cx*16+x,wz=cz*16+z;if(p.spawn(wx,wz))continue;
                    double dx=wx+.5-s.x,dz=wz+.5-s.z;
                    for(int y=Math.max(3,(int)(s.y-s.r));y<=Math.min(t.height(wx,wz)-7,(int)(s.y+s.r));y++) {
                        double dy=(y+.5-s.y)*1.15;
                        if(dx*dx+dy*dy+dz*dz<s.r*s.r)set(b,x,y,z,Material.AIR);
                    }
                }
        }
        Random r=new Random(Terrain.hash(seed+311,cx,cz));
        for(Profile.Ore o:p.ores)for(int n=0;n<o.attempts;n++) {
            if(r.nextDouble()>o.chance)continue;
            vein(b,r,o,cx,cz);
        }
        // Additional veins around natural cave walls, without filling the cave itself.
        double boost=Math.max(1,Math.min(4,p.yaml.getDouble("caves.ore-boost",1.65)));
        if(p.caves && boost>1)for(Profile.Ore o:p.ores)for(int n=0;n<(int)Math.round(o.attempts*(boost-1));n++) {
            if(r.nextDouble()>o.chance)continue;
            int x=r.nextInt(16),z=r.nextInt(16),y=o.min+r.nextInt(o.max-o.min+1);
            if(nearAir(b,x,y,z))veinAt(b,r,o,cx,cz,x,y,z);
        }
        ponds(b,t,cx,cz);
        if(p.trees)trees(b,t,cx,cz);
        return b;
    }
    private void vein(byte[][] b,Random r,Profile.Ore o,int cx,int cz){
        veinAt(b,r,o,cx,cz,r.nextInt(16),o.min+r.nextInt(o.max-o.min+1),r.nextInt(16));
    }
    private void veinAt(byte[][] b,Random r,Profile.Ore o,int cx,int cz,int x,int y,int z){
        for(int k=0;k<o.vein;k++) {
            if(!p.spawn(cx*16+x,cz*16+z) && get(b,x,y,z)==Material.STONE.getId())set(b,x,y,z,o.type);
            switch(r.nextInt(6)){case 0:x++;break;case 1:x--;break;case 2:z++;break;case 3:z--;break;case 4:y++;break;default:y--;}
            x=Profile.bounded(x,0,15);z=Profile.bounded(z,0,15);y=Profile.bounded(y,o.min,o.max);
        }
    }
    private boolean nearAir(byte[][] b,int x,int y,int z){
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int dy=-2;dy<=2;dy++)
            if(x+dx>=0 && x+dx<16 && z+dz>=0 && z+dz<16 && get(b,x+dx,y+dy,z+dz)==0)return true;
        return false;
    }
    private void ponds(byte[][] b,Terrain t,int cx,int cz){
        int cell=40;
        for(int ax=Math.floorDiv(cx*16-5,cell);ax<=Math.floorDiv(cx*16+20,cell);ax++)
            for(int az=Math.floorDiv(cz*16-5,cell);az<=Math.floorDiv(cz*16+20,cell);az++) {
                Random r=new Random(Terrain.hash(t.seed+819,ax,az));
                if(r.nextDouble()>p.yaml.getDouble("resources.ponds-chance",.025))continue;
                int px=ax*cell+8+r.nextInt(24),pz=az*cell+8+r.nextInt(24),h=t.height(px,pz);
                boolean valid=true;
                for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)if(p.clear(px+dx,pz+dz)||t.height(px+dx,pz+dz)!=h)valid=false;
                if(!valid)continue;
                for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++) {
                    int x=px+dx-cx*16,z=pz+dz-cz*16,d=dx*dx+dz*dz;
                    if(d<=5){set(b,x,h-1,z,Material.SAND);set(b,x,h,z,Material.STATIONARY_WATER);}
                    else if(d<=10)set(b,x,h,z,Material.SAND);
                }
                if(p.yaml.getBoolean("resources.sugar-cane",true)) {
                    int x=px+3-cx*16,z=pz-cz*16;
                    set(b,x,h+1,z,Material.SUGAR_CANE_BLOCK);set(b,x,h+2,z,Material.SUGAR_CANE_BLOCK);
                }
            }
    }
    private void trees(byte[][] b,Terrain t,int cx,int cz){
        int cell=Profile.bounded(p.yaml.getInt("trees.spacing",13),10,48);
        for(int ax=Math.floorDiv(cx*16-4,cell);ax<=Math.floorDiv(cx*16+19,cell);ax++)
            for(int az=Math.floorDiv(cz*16-4,cell);az<=Math.floorDiv(cz*16+19,cell);az++) {
                Random r=new Random(Terrain.hash(t.seed+911,ax,az));
                if(r.nextDouble()>p.yaml.getDouble("trees.chance",.42))continue;
                int tx=ax*cell+3+r.nextInt(cell-6),tz=az*cell+3+r.nextInt(cell-6),h=t.height(tx,tz);
                boolean valid=true;
                for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)if(p.clear(tx+dx,tz+dz))valid=false;
                if(!valid || pondAt(t,tx,tz))continue;
                int oak=Math.max(0,p.yaml.getInt("trees.oak-weight",65)), birch=Math.max(0,p.yaml.getInt("trees.birch-weight",25)),dry=Math.max(0,p.yaml.getInt("trees.dry-weight",10));
                int kind=r.nextInt(Math.max(1,oak+birch+dry));boolean isBirch=kind>=oak&&kind<oak+birch,isDry=kind>=oak+birch;
                int trunk=isBirch?6:isDry?4:5,radius=isBirch?1:2;
                // Legacy block sections carry IDs only: birch metadata is applied by a populator.
                for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)for(int dy=-2;dy<=1;dy++) {
                    if(Math.abs(dx)+Math.abs(dz)>radius+1 || (isDry&&Math.abs(dx)+Math.abs(dz)>1))continue;
                    int x=tx+dx-cx*16,z=tz+dz-cz*16,y=h+trunk+dy;
                    if(get(b,x,y,z)==0)set(b,x,y,z,Material.LEAVES);
                }
                for(int y=h+1;y<=h+trunk;y++)set(b,tx-cx*16,y,tz-cz*16,Material.LOG);
            }
    }
    private boolean pondAt(Terrain t,int x,int z){
        // Same deterministic test as ponds(), avoiding trees next to a candidate pond.
        for(int ax=Math.floorDiv(x-7,40);ax<=Math.floorDiv(x+7,40);ax++)for(int az=Math.floorDiv(z-7,40);az<=Math.floorDiv(z+7,40);az++){
            Random r=new Random(Terrain.hash(t.seed+819,ax,az));
            if(r.nextDouble()>p.yaml.getDouble("resources.ponds-chance",.025))continue;
            int px=ax*40+8+r.nextInt(24),pz=az*40+8+r.nextInt(24);
            if(Math.abs(x-px)<=6&&Math.abs(z-pz)<=6)return true;
        }return false;
    }
    static int get(byte[][] b,int x,int y,int z){if(x<0||x>15||z<0||z>15||y<0||y>=256)return -1;byte[] s=b[y>>4];return s==null?0:s[((y&15)<<8)|(z<<4)|x]&255;}
    static void set(byte[][] b,int x,int y,int z,Material m){if(x<0||x>15||z<0||z>15||y<0||y>=256)return;int s=y>>4;if(b[s]==null)b[s]=new byte[4096];b[s][((y&15)<<8)|(z<<4)|x]=(byte)m.getId();}
    @Override public Location getFixedSpawnLocation(World w,Random r){return new Location(w,.5,p.height+1,.5);}
    @Override public boolean canSpawn(World w,int x,int z){return p.spawn(x,z);}
    @Override public List<BlockPopulator> getDefaultPopulators(World w){return Collections.<BlockPopulator>singletonList(new NaturePopulator());}
    private final class NaturePopulator extends BlockPopulator {
        @Override public void populate(World w,Random ignored,Chunk c){
            Terrain t=new Terrain(p,w.getSeed());
            // Set birch species and enable decay checks using data available in 1.8.
            int cell=Profile.bounded(p.yaml.getInt("trees.spacing",13),10,48);
            if(p.trees)for(int ax=Math.floorDiv(c.getX()*16-4,cell);ax<=Math.floorDiv(c.getX()*16+19,cell);ax++)for(int az=Math.floorDiv(c.getZ()*16-4,cell);az<=Math.floorDiv(c.getZ()*16+19,cell);az++){
                Random r=new Random(Terrain.hash(w.getSeed()+911,ax,az));if(r.nextDouble()>p.yaml.getDouble("trees.chance",.42))continue;
                int tx=ax*cell+3+r.nextInt(cell-6),tz=az*cell+3+r.nextInt(cell-6);
                int oak=Math.max(0,p.yaml.getInt("trees.oak-weight",65)),birch=Math.max(0,p.yaml.getInt("trees.birch-weight",25)),dry=Math.max(0,p.yaml.getInt("trees.dry-weight",10));
                int kind=r.nextInt(Math.max(1,oak+birch+dry));byte data=(byte)(kind>=oak&&kind<oak+birch?2:0);
                for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
                    int lx=tx+dx-c.getX()*16,lz=tz+dz-c.getZ()*16;if(lx<0||lx>15||lz<0||lz>15)continue;
                    for(int y=t.height(tx,tz)+1;y<=t.height(tx,tz)+7;y++){
                        org.bukkit.block.Block block=c.getBlock(lx,y,lz);
                        if(block.getType()==Material.LOG||block.getType()==Material.LEAVES)block.setData(data,false);
                    }
                }
            }
            Random r=new Random(Terrain.hash(w.getSeed()+222,c.getX(),c.getZ()));
            if(r.nextDouble()<p.yaml.getDouble("resources.animals-chance",.025)){
                int x=c.getX()*16+8,z=c.getZ()*16+8,y=t.height(x,z)+1;
                if(!p.clear(x,z)&&p.inside(x,z)&&c.getBlock(8,y-1,8).getType()==Material.GRASS&&c.getBlock(8,y,8).getType()==Material.AIR){
                    EntityType[] types={EntityType.COW,EntityType.SHEEP,EntityType.PIG,EntityType.CHICKEN};EntityType type=types[r.nextInt(types.length)];
                    for(int i=0;i<2;i++)w.spawnEntity(new Location(w,x+.5,y,z+.5),type);
                }
            }
        }
    }
}
