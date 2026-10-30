package dev.hassxn.pulse;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.util.*;

public final class PulseGenerator extends JavaPlugin implements CommandExecutor,TabCompleter {
    final Map<String,Profile> profiles=new LinkedHashMap<String,Profile>();
    Pregenerator pregen;
    @Override public void onEnable(){
        saveDefaultConfig();for(String s:new String[]{"ores.yml","trees.yml","mines.yml"})if(!new File(getDataFolder(),s).exists())saveResource(s,false);
        pregen=new Pregenerator(this);
        File dir=new File(getDataFolder(),"worlds");File[] files=dir.listFiles((d,n)->n.endsWith(".yml"));
        if(files!=null)for(File f:files)try{
            YamlConfiguration y=new YamlConfiguration();y.load(f);
            String name=f.getName().substring(0,f.getName().length()-4);validateName(name);
            Profile p=new Profile(y,y.getInt("world-size"));profiles.put(name,p);
        }catch(Exception e){getLogger().severe("No se pudo restaurar "+f.getName()+": "+e);getServer().getPluginManager().disablePlugin(this);return;}
        getCommand("pulse").setExecutor(this);getCommand("pulse").setTabCompleter(this);
        getServer().getPluginManager().registerEvents(new Protection(this),this);
        // Spigot 1.8 requires the primary world before additional worlds can load.
        // Keep STARTUP to provide getDefaultWorldGenerator for an optional primary HCF world.
        getServer().getScheduler().runTask(this,()->{
            for(Map.Entry<String,Profile> entry:profiles.entrySet())try{
                String name=entry.getKey();Profile p=entry.getValue();World existing=Bukkit.getWorld(name);
                if(existing!=null&&!(existing.getGenerator() instanceof HcfGenerator))throw new IllegalStateException("Mundo cargado con otro generador: "+name);
                World w=existing!=null?existing:new WorldCreator(name).seed(p.yaml.getLong("seed")).generator(new HcfGenerator(p)).generateStructures(false).createWorld();
                setup(w,p);pregen.restore(w,p);
            }catch(Exception e){getLogger().log(java.util.logging.Level.SEVERE,"No se pudo cargar mundo Pulse",e);getServer().getPluginManager().disablePlugin(this);return;}
        });
        getServer().getScheduler().runTaskTimer(this,pregen,1L,1L);
        getLogger().info("Pulse Generator 1.0.0 - Gratis - Desarrollado por Hassxn");
    }
    @Override public void onDisable(){if(pregen!=null)pregen.saveAll();}
    @Override public ChunkGenerator getDefaultWorldGenerator(String name,String id){
        File f=profileFile(name);if(!f.exists())throw new IllegalArgumentException("Crea el mundo con /pulse create antes de usarlo como mundo principal.");
        YamlConfiguration y=YamlConfiguration.loadConfiguration(f);return new HcfGenerator(new Profile(y,y.getInt("world-size")));
    }
    File profileFile(String name){validateName(name);return new File(getDataFolder(),"worlds/"+name+".yml");}
    static void validateName(String name){if(!name.matches("[A-Za-z0-9_-]{1,48}"))throw new IllegalArgumentException("Nombre: 1-48 letras, numeros, _ o -.");}
    private Profile fresh(int size)throws Exception{
        YamlConfiguration y=new YamlConfiguration();y.loadFromString(getConfig().saveToString());
        for(String file:new String[]{"ores.yml","trees.yml","mines.yml"}){
            YamlConfiguration part=new YamlConfiguration();part.load(new File(getDataFolder(),file));
            if(file.equals("trees.yml"))y.set("trees",part);else for(String key:part.getKeys(false))y.set(key,part.get(key));
        }return new Profile(y,size);
    }
    private void setup(World w,Profile p){
        if(w==null)throw new IllegalStateException("No se pudo cargar el mundo");
        w.getWorldBorder().setCenter(0,0);w.getWorldBorder().setSize(p.size);w.getWorldBorder().setWarningDistance(8);
        w.setSpawnLocation(0,p.height+1,0);w.setKeepSpawnInMemory(false);
    }
    private World managed(String name){World w=Bukkit.getWorld(name);if(w==null||!profiles.containsKey(name))throw new IllegalArgumentException("Mundo no administrado por Pulse.");return w;}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!sender.hasPermission("pulse.admin")){sender.sendMessage(ChatColor.RED+"Sin permiso.");return true;}
        try{
            if(args.length==0||args[0].equalsIgnoreCase("help")){help(sender);return true;}
            String sub=args[0].toLowerCase(Locale.ROOT);
            if(sub.equals("reload")){reloadConfig();sender.sendMessage(ChatColor.GREEN+"Configuracion recargada. El terreno de mundos existentes mantiene su perfil.");return true;}
            if(sub.equals("create")){
                if(args.length!=3&&args.length!=5)throw new IllegalArgumentException("/pulse create <mundo> <lado> [--seed <numero>]");
                String name=args[1];validateName(name);int size=Integer.parseInt(args[2]);
                if(size<512||size>30000)throw new IllegalArgumentException("El lado debe estar entre 512 y 30000 bloques.");
                File folder=new File(Bukkit.getWorldContainer(),name);
                if(folder.exists()||Bukkit.getWorld(name)!=null||profileFile(name).exists())throw new IllegalArgumentException("Ese mundo ya existe. No se sobrescribe.");
                if(args.length==5&&!args[3].equalsIgnoreCase("--seed"))throw new IllegalArgumentException("Usa --seed <numero>.");
                long seed=args.length==5?Long.parseLong(args[4]):new Random().nextLong();Profile p=fresh(size);
                p.yaml.set("seed",seed);p.save(profileFile(name));profiles.put(name,p);
                World w=new WorldCreator(name).seed(seed).generator(new HcfGenerator(p)).generateStructures(false).createWorld();setup(w,p);
                pregen.start(w,p);sender.sendMessage(ChatColor.GREEN+"Mundo "+name+" creado: "+size+" x "+size+", seed "+seed+". Pregeneracion iniciada.");return true;
            }
            if(args.length!=2)throw new IllegalArgumentException("/pulse "+sub+" <mundo>");
            World w=managed(args[1]);
            if(sub.equals("status"))sender.sendMessage(ChatColor.AQUA+pregen.status(w.getName()));
            else if(sub.equals("pregen")){pregen.start(w,profiles.get(w.getName()));sender.sendMessage(ChatColor.GREEN+"Pregeneracion iniciada.");}
            else if(sub.equals("pause")||sub.equals("resume")){pregen.pause(w.getName(),sub.equals("pause"));sender.sendMessage(ChatColor.GREEN+"Tarea "+(sub.equals("pause")?"pausada.":"reanudada."));}
            else if(sub.equals("tp")){if(!(sender instanceof Player))throw new IllegalArgumentException("Solo jugadores.");((Player)sender).teleport(w.getSpawnLocation());}
            else help(sender);
        }catch(Exception e){sender.sendMessage(ChatColor.RED+"Pulse: "+e.getMessage());getLogger().fine(e.toString());}
        return true;
    }
    private void help(CommandSender s){s.sendMessage(ChatColor.AQUA+"Pulse Generator | Gratis | Hassxn");s.sendMessage("/pulse create <mundo> <lado> [--seed <numero>]");s.sendMessage("/pulse <pregen|pause|resume|status|tp> <mundo>");s.sendMessage("/pulse reload");}
    @Override public List<String> onTabComplete(CommandSender s,Command c,String a,String[] args){
        if(!s.hasPermission("pulse.admin"))return Collections.emptyList();
        List<String> source=args.length==1?Arrays.asList("create","pregen","pause","resume","status","tp","reload","help"):args.length==2?new ArrayList<String>(profiles.keySet()):Collections.<String>emptyList();
        List<String> out=new ArrayList<String>();for(String v:source)if(v.startsWith(args[args.length-1].toLowerCase(Locale.ROOT)))out.add(v);return out;
    }
}
