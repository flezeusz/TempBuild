package pl.flezy.tempbuild.manager;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.flezy.tempbuild.TempBuild;

import java.util.*;

public class BuildManager {
    private static final Random RANDOM = new Random();
    public static final Map<Location, BlockData> placedBlocks = new HashMap<>();


    public static boolean hasBypass(Player player, Location location) {
        World world = location.getWorld();
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        return WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, BukkitAdapter.adapt(world));
    }

    public static boolean canTempBuild(Player player, Location location) {
        if (hasBypass(player, location)) {
            return true;
        }

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        StateFlag tempBuildFlag = TempBuild.getInstance().TEMP_BUILD_FLAG;

        RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();

        StateFlag.State state = regionContainer.createQuery().queryState(BukkitAdapter.adapt(location), localPlayer, tempBuildFlag);
        return state != StateFlag.State.ALLOW;
    }

    public static boolean canBreak(Player player, Location location) {
        if (placedBlocks.containsKey(location)) {
            return true;
        }

        return canTempBuild(player, location);
    }


    public static void updateBlockData(Location location) {
        if (placedBlocks.containsKey(location)) {
            placedBlocks.put(location, location.getBlock().getBlockData());
        }
    }

    public static void addPlayerBlock(Location location) {
        Block block = location.getBlock();
        BlockData blockData = block.getBlockData();
        placedBlocks.put(location, blockData);

        // Dodaj górną połowę drzwi/innych multi-block struktur
        if (blockData instanceof Bisected bisected) {
            if (bisected.getHalf() == Bisected.Half.BOTTOM) {
                Location topLocation = location.clone().add(0, 1, 0);
                Block topBlock = topLocation.getBlock();
                if (topBlock.getBlockData() instanceof Bisected) {
                    placedBlocks.put(topLocation, topBlock.getBlockData());
                }
            }
        }

        int entityId = RANDOM.nextInt(Integer.MAX_VALUE);
        int decayTime = TempBuild.getInstance().config.blockDecayTime * 20;
        if (decayTime<=0) return;
        new BukkitRunnable(){
            int ticks = 0;
            @Override
            public void run() {
                BlockData currentData = placedBlocks.get(location);
                if (currentData == null) {
                    Bukkit.getOnlinePlayers().forEach(online ->
                            online.sendBlockDamage(location, 0f, entityId));
                    cancel();
                    return;
                }

                Block block = location.getBlock();
                Material currentMaterial = block.getType();
                Material expectedMaterial = currentData.getMaterial();

                if (currentMaterial != expectedMaterial){
                    Bukkit.getOnlinePlayers().forEach(online ->
                            online.sendBlockDamage(location, 0f, entityId));
                    placedBlocks.remove(location);
                    cancel();
                    return;
                }

                if (ticks >= decayTime){
                    if (TempBuild.getInstance().config.dropBlocks) {
                        block.breakNaturally();
                    }
                    else {
                        block.setType(Material.AIR);
                    }

                    if (currentData instanceof Bisected bisected) {
                        if (bisected.getHalf() == Bisected.Half.BOTTOM) {
                            Location topLocation = location.clone().add(0, 1, 0);
                            placedBlocks.remove(topLocation);
                        }
                    }

                    placedBlocks.remove(location);
                    cancel();
                    return;
                }

                if (ticks > 0){
                    Bukkit.getOnlinePlayers().forEach(online ->
                            online.sendBlockDamage(location, (float) ticks /decayTime, entityId));
                }
                ticks++;
            }
        }.runTaskTimer(TempBuild.getInstance(),0, 1);
    }
}
