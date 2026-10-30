package dev.hassxn.pulse;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.ItemStack;
import java.util.concurrent.ThreadLocalRandom;

final class Protection implements Listener {
    private final PulseGenerator plugin;
    Protection(PulseGenerator plugin){this.plugin=plugin;}
    private Profile profile(Block b){return plugin.profiles.get(b.getWorld().getName());}
    private boolean protectedBlock(Block b){Profile p=profile(b);return p!=null&&p.yaml.getBoolean("spawn.protect",true)&&p.spawn(b.getX(),b.getZ());}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void breakBlock(BlockBreakEvent e){if(protectedBlock(e.getBlock())&&!e.getPlayer().hasPermission("pulsegenerator.admin"))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void place(BlockPlaceEvent e){if(protectedBlock(e.getBlock())&&!e.getPlayer().hasPermission("pulsegenerator.admin"))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void bucket(PlayerBucketEmptyEvent e){if(protectedBlock(e.getBlockClicked().getRelative(e.getBlockFace()))&&!e.getPlayer().hasPermission("pulsegenerator.admin"))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void bucket(PlayerBucketFillEvent e){if(protectedBlock(e.getBlockClicked())&&!e.getPlayer().hasPermission("pulsegenerator.admin"))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void explosion(EntityExplodeEvent e){e.blockList().removeIf(this::protectedBlock);}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void explosion(BlockExplodeEvent e){e.blockList().removeIf(this::protectedBlock);}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void flow(BlockFromToEvent e){if(protectedBlock(e.getToBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void fire(BlockBurnEvent e){if(protectedBlock(e.getBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void ignite(BlockIgniteEvent e){if(protectedBlock(e.getBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void entity(EntityChangeBlockEvent e){if(protectedBlock(e.getBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void extend(BlockPistonExtendEvent e){for(Block b:e.getBlocks())if(protectedBlock(b)||protectedBlock(b.getRelative(e.getDirection()))){e.setCancelled(true);break;}}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void retract(BlockPistonRetractEvent e){for(Block b:e.getBlocks())if(protectedBlock(b)||protectedBlock(b.getRelative(e.getDirection()))){e.setCancelled(true);break;}}
    @SuppressWarnings("deprecation")
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void decay(LeavesDecayEvent e){
        Profile p=profile(e.getBlock());
        if(p!=null && (e.getBlock().getData()&3)==0 && ThreadLocalRandom.current().nextDouble()<p.yaml.getDouble("trees.apple-chance",.01))
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(),new ItemStack(Material.APPLE));
    }
}
