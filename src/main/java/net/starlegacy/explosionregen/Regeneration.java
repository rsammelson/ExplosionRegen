package net.starlegacy.explosionregen;

import net.starlegacy.explosionregen.data.ExplodedBlockData;
import net.starlegacy.explosionregen.data.ExplodedEntityData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public class Regeneration {
    public static void pulse(ExplosionRegenPlugin plugin) {
        regenerateBlocks(plugin, false);
        regenerateEntities(plugin, false);
    }

    public static int regenerateBlocks(ExplosionRegenPlugin plugin, boolean instant) {
        long millisecondDelay = (long) (plugin.getSettings().getRegenDelay() * 60L * 1_000L);
        long maxNanos = (long) (plugin.getSettings().getPlacementIntensity() * 1_000_000L);

        long start = System.nanoTime();

        int regenerated = 0;

        for (World world : Bukkit.getWorlds()) {
            List<ExplodedBlockData> blocks = plugin.getWorldData().getBlocks(world);

            for (Iterator<ExplodedBlockData> iterator = blocks.iterator(); iterator.hasNext(); ) {
                ExplodedBlockData data = iterator.next();
                if (!instant) {
                    if (System.nanoTime() - start > maxNanos) { // i.e. taking too long
                        return regenerated;
                    }

                    if (System.currentTimeMillis() - data.getExplodedTime() < millisecondDelay) {
                        continue;
                    }
                }

                iterator.remove();
                regenerateBlock(world, data);

                regenerated++;
            }
        }

        return regenerated;
    }

    private static void regenerateBlock(World world, ExplodedBlockData data) {
        Block block = world.getBlockAt(data.getX(), data.getY(), data.getZ());
        BlockData blockData = data.getBlockData();
        block.setBlockData(blockData, false);

        @Nullable byte[] tileData = data.getTileData();
        if (tileData != null) {
            NMSUtils.setTileEntity(block, tileData);
        }
    }

    public static int regenerateEntities(ExplosionRegenPlugin plugin, boolean instant) {
        long millisecondDelay = (long) (plugin.getSettings().getRegenDelay() * 60L * 1_000L);
        int regenerated = 0;

        for (World world : Bukkit.getWorlds()) {
            List<ExplodedEntityData> entities = plugin.getWorldData().getEntities(world);
            for (Iterator<ExplodedEntityData> iterator = entities.iterator(); iterator.hasNext(); ) {
                ExplodedEntityData data = iterator.next();
                if (!instant && System.currentTimeMillis() - data.getExplodedTime() < millisecondDelay) {
                    continue;
                }
                iterator.remove();
                regenerateEntity(world, data);
                regenerated++;
            }
        }

        return regenerated;
    }

    private static void regenerateEntity(World world, ExplodedEntityData data) {
        Location location = new Location(world, data.getX(), data.getY(), data.getZ(), data.getPitch(), data.getYaw());
        Entity entity = world.spawnEntity(location, data.getEntityType());

        @Nullable byte[] nmsData = data.getNmsData();
        if (nmsData != null) {
            NMSUtils.restoreEntityData(entity, nmsData);
        }

        if (entity instanceof LivingEntity) {
            ((LivingEntity) entity).setHealth(((LivingEntity) entity).getHealth());
        }
    }
}