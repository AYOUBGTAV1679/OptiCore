package dev.opticore.cache;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes chunk coordinates whose boundary face geometry may be affected by
 * a block edit. Minecraft chunk coordinates use floor division by 16; bit
 * operations preserve that behavior for negative world coordinates.
 */
public final class ChunkBoundaryInvalidation {
    public record ChunkCoordinate(int x, int z) {}

    private ChunkBoundaryInvalidation() {}

    public static List<ChunkCoordinate> affectedByBlockEdit(int blockX, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        int localX = blockX & 15;
        int localZ = blockZ & 15;
        List<ChunkCoordinate> affected = new ArrayList<>(3);
        affected.add(new ChunkCoordinate(chunkX, chunkZ));
        if (localX == 0) affected.add(new ChunkCoordinate(chunkX - 1, chunkZ));
        if (localX == 15) affected.add(new ChunkCoordinate(chunkX + 1, chunkZ));
        if (localZ == 0) affected.add(new ChunkCoordinate(chunkX, chunkZ - 1));
        if (localZ == 15) affected.add(new ChunkCoordinate(chunkX, chunkZ + 1));
        return List.copyOf(affected);
    }
}
