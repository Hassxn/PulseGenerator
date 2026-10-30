package dev.hassxn.pulse;

import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.*;
import java.nio.file.*;
import java.util.*;

final class Pregenerator implements Runnable {
    private final PulseGenerator plugin;
    private final LegacyLighting lighting=new LegacyLighting();
    private final Map<String,Job> jobs=new LinkedHashMap<String,Job>();
    private long last=System.nanoTime(),checkpoint=System.currentTimeMillis();
    private double tps=20;
    Pregenerator(PulseGenerator plugin){this.plugin=plugin;}
    void restore(World w,Profile p) throws IOException {
        File file=file(w.getName()); if(!file.exists())return;
        YamlConfiguration y=YamlConfiguration.loadConfiguration(file);
        Job j=new Job(w,p,y.getInt("halo-chunks",2));
        j.cursor=Math.max(0,Math.min(j.total,y.getLong("cursor")));
        j.millis=y.getLong("elapsed-ms");j.paused=y.getBoolean("paused")||!plugin.getConfig().getBoolean("pregeneration.auto-resume",true);
        jobs.put(w.getName(),j);
    }
    void start(World w,Profile p) throws IOException {
        if(jobs.containsKey(w.getName()))throw new IllegalArgumentException("Ya existe una tarea; usa resume o status.");
        Job j=new Job(w,p,plugin.getConfig().getInt("pregeneration.halo-chunks",2));jobs.put(w.getName(),j);save(j);
    }
    void pause(String name,boolean paused)throws IOException{Job j=job(name);if(j.cursor==j.total)throw new IllegalArgumentException("Pregeneracion ya completada.");j.paused=paused;save(j);}
    String status(String name){Job j=job(name);return String.format(Locale.ROOT,"%s: %.2f%% (%d/%d chunks), %s, TPS estimados %.1f",name,100.0*j.cursor/j.total,j.cursor,j.total,j.cursor==j.total?"completado":j.paused?"pausado":"activo",tps);}
    private Job job(String name){Job j=jobs.get(name);if(j==null)throw new IllegalArgumentException("Sin tarea para ese mundo.");return j;}
    private File file(String name){return new File(plugin.getDataFolder(),"jobs/"+name+".yml");}
    @Override public void run(){
        long now=System.nanoTime();double tick=(now-last)/1e6;last=now;tps=.95*tps+.05*Math.min(20,1000/Math.max(1,tick));
        long end=now+Profile.bounded(plugin.getConfig().getInt("pregeneration.budget-ms",20),1,50)*1000000L;
        int remaining=Profile.bounded(plugin.getConfig().getInt("pregeneration.chunks-per-tick",1),1,16);
        if(tps>=plugin.getConfig().getDouble("pregeneration.minimum-tps",17))for(Job j:jobs.values()){
            if(j.paused||j.cursor==j.total)continue;
            long begin=System.currentTimeMillis();
            try{
                while(remaining>0&&System.nanoTime()<end&&j.cursor<j.total){
                    int x=j.min+(int)(j.cursor%j.width),z=j.min+(int)(j.cursor/j.width);
                    // 1.8 lighting checks a 17-block radius; edge columns need 5x5 chunks.
                    for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)j.w.getChunkAt(x+dx,z+dz);
                    lighting.complete(j.w.getChunkAt(x,z));
                    // Remove only chunks far from players. Safe unload respects spawn/in-use checks.
                    for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
                        int ux=x+dx,uz=z+dz;
                        if(j.w.isChunkLoaded(ux,uz)&&!nearPlayer(j.w,ux,uz))j.w.unloadChunk(ux,uz,true,true);
                    }
                    j.cursor++;remaining--;
                }
                j.millis+=System.currentTimeMillis()-begin;
                if(j.cursor==j.total){save(j);report(j);plugin.getLogger().info("Pregeneracion completada: "+j.w.getName());}
            }catch(Exception e){j.paused=true;plugin.getLogger().severe("Pregeneracion pausada: "+e.getMessage());try{save(j);}catch(IOException ex){plugin.getLogger().severe(ex.toString());}}
        }
        if(System.currentTimeMillis()-checkpoint>=Math.max(5,plugin.getConfig().getInt("pregeneration.checkpoint-seconds",15))*1000L){
            checkpoint=System.currentTimeMillis();saveAll();
        }
    }
    private boolean nearPlayer(World w,int x,int z){for(Player p:w.getPlayers())if(Math.abs((p.getLocation().getBlockX()>>4)-x)<=12&&Math.abs((p.getLocation().getBlockZ()>>4)-z)<=12)return true;return false;}
    void saveAll(){for(Job j:jobs.values())if(j.cursor<j.total||!file(j.w.getName()).exists())try{save(j);}catch(IOException e){j.paused=true;plugin.getLogger().severe("No se pudo guardar tarea: "+e);}}
    private void save(Job j)throws IOException{
        j.w.save(); // Disk world state precedes cursor: replay is safe after interruption.
        YamlConfiguration y=new YamlConfiguration();y.set("cursor",j.cursor);y.set("total",j.total);y.set("halo-chunks",j.halo);y.set("paused",j.paused);y.set("elapsed-ms",j.millis);
        atomic(y,file(j.w.getName()));
    }
    static void atomic(YamlConfiguration y,File file)throws IOException{
        Files.createDirectories(file.toPath().getParent());File tmp=new File(file.getPath()+".tmp");y.save(tmp);
        try{Files.move(tmp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException e){Files.move(tmp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}
    }
    private void report(Job j)throws IOException{
        YamlConfiguration y=new YamlConfiguration();y.set("plugin","Pulse Generator");y.set("author","Hassxn");y.set("world",j.w.getName());y.set("seed",j.w.getSeed());y.set("size",j.p.size);y.set("spawn-size",j.p.spawnSize);y.set("chunks-completed-with-halo",j.total);y.set("generation-work-ms",j.millis);y.set("ore-profile",j.p.yaml.getString("profile"));
        y.set("caves","Cuevas naturales procedurales; sin minas prison ni regeneracion artificial.");
        y.set("cave-systems-originating-inside-border",new Terrain(j.p,j.w.getSeed()).caveOriginsInsideBorder());
        List<String> centers=new ArrayList<String>();if(j.p.events)for(int[] c:j.p.centers)centers.add(c[0]+","+c[1]);y.set("event-centers",centers);
        y.set("completed-at",new Date().toString());atomic(y,new File(plugin.getDataFolder(),"reports/"+j.w.getName()+".yml"));
    }
    private static final class Job{
        final World w;final Profile p;final int min,width,halo;final long total;long cursor,millis;boolean paused;
        Job(World w,Profile p,int halo){this.w=w;this.p=p;this.halo=Profile.bounded(halo,1,4);min=Math.floorDiv(-p.size/2,16)-this.halo;int max=Math.floorDiv((p.size+1)/2-1,16)+this.halo;width=max-min+1;total=(long)width*width;}
    }
}
