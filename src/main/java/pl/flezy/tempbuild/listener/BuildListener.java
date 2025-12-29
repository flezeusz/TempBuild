package pl.flezy.tempbuild.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Bisected;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.flezy.tempbuild.manager.BuildManager;

import java.util.Iterator;

public class BuildListener implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (BuildManager.hasBypass(player, location)) return;

        if (!BuildManager.canTempBuild(player, location)) {
            BlockState replaced = event.getBlockReplacedState();
            if (replaced.getType().isCollidable()) {
                event.setCancelled(true);
                return;
            }
            BuildManager.addPlayerBlock(location);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (BuildManager.hasBypass(player, location)) return;

        if (!BuildManager.canBreak(player, location)){
            event.setCancelled(true);
        } else if (BuildManager.placedBlocks.containsKey(location)) {
            Block block = event.getBlock();
            if (block.getBlockData() instanceof Bisected bisected) {
                if (bisected.getHalf() == Bisected.Half.TOP) {
                    Location bottomLocation = location.clone().add(0, -1, 0);
                    BuildManager.placedBlocks.remove(bottomLocation);
                }
            }
            BuildManager.placedBlocks.remove(location);
        }
    }

    @EventHandler
    public void onBlockExplodeEvent(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> BuildManager.isRegion(block.getLocation()));
    }

    @EventHandler
    public void onEntityExplodeEvent(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> BuildManager.isRegion(block.getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block != null && BuildManager.placedBlocks.containsKey(block.getLocation())) {
            BuildManager.updateBlockData(block.getLocation());

            Location topLocation = block.getLocation().clone().add(0, 1, 0);
            if (BuildManager.placedBlocks.containsKey(topLocation)) {
                BuildManager.updateBlockData(topLocation);
            }

            Location bottomLocation = block.getLocation().clone().add(0, -1, 0);
            if (BuildManager.placedBlocks.containsKey(bottomLocation)) {
                BuildManager.updateBlockData(bottomLocation);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (BuildManager.placedBlocks.containsKey(event.getToBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (BuildManager.placedBlocks.containsKey(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Location location = event.getBlock().getLocation();
        if (BuildManager.placedBlocks.containsKey(location)) {
            BuildManager.updateBlockData(location);
        }
    }
}
