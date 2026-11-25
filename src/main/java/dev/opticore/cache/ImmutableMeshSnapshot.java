package dev.opticore.cache;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Immutable, Minecraft-independent CPU mesh payload.
 *
 * <p>The snapshot owns defensive copies of all byte data and contains no
 * references to BufferBuilder, RenderedBuffer, VertexBuffer, world objects,
 * or other renderer-owned resources. It is suitable for bounded CPU caching;
 * a renderer integration must still define and validate the vertex layout,
 * index interpretation, and upload lifecycle before using it for drawing.</p>
 */
public final class ImmutableMeshSnapshot implements CacheEntry {
    private static final int MAX_LAYOUT_LENGTH = 256;

    private final String vertexLayout;
    private final int vertexStride;
    private final int drawMode;
    private final int sectionX;
    private final int sectionY;
    private final int sectionZ;
    private final int indexElementBytes;
    private final byte[] vertices;
    private final byte[] indices;
    private final long sizeBytes;

    public ImmutableMeshSnapshot(
            String vertexLayout,
            int vertexStride,
            int drawMode,
            int sectionX,
            int sectionY,
            int sectionZ,
            int indexElementBytes,
            byte[] vertices,
            byte[] indices) {
        this.vertexLayout = validateLayout(vertexLayout);
        if (vertexStride < 1) {
            throw new IllegalArgumentException("vertexStride must be positive");
        }
        if (drawMode < 0) {
            throw new IllegalArgumentException("drawMode must not be negative");
        }
        if (indexElementBytes != 0 && indexElementBytes != 2 && indexElementBytes != 4) {
            throw new IllegalArgumentException("indexElementBytes must be 0, 2, or 4");
        }
        this.vertices = Objects.requireNonNull(vertices, "vertices").clone();
        this.indices = Objects.requireNonNull(indices, "indices").clone();
        if (this.vertices.length % vertexStride != 0) {
            throw new IllegalArgumentException("vertex payload is not aligned to vertexStride");
        }
        if (this.indices.length == 0 && indexElementBytes != 0) {
            throw new IllegalArgumentException("empty index payload must use indexElementBytes=0");
        }
        if (this.indices.length > 0 && indexElementBytes == 0) {
            throw new IllegalArgumentException("non-empty index payload requires an element width");
        }
        if (indexElementBytes != 0 && this.indices.length % indexElementBytes != 0) {
            throw new IllegalArgumentException("index payload is not aligned to its element width");
        }
        this.vertexStride = vertexStride;
        this.drawMode = drawMode;
        this.sectionX = sectionX;
        this.sectionY = sectionY;
        this.sectionZ = sectionZ;
        this.indexElementBytes = indexElementBytes;
        this.sizeBytes = Math.addExact((long) this.vertices.length, this.indices.length);
    }

    private static String validateLayout(String value) {
        Objects.requireNonNull(value, "vertexLayout");
        if (value.isBlank() || value.length() > MAX_LAYOUT_LENGTH) {
            throw new IllegalArgumentException("vertexLayout must contain 1..256 characters");
        }
        return value;
    }

    public String vertexLayout() { return vertexLayout; }
    public int vertexStride() { return vertexStride; }
    public int drawMode() { return drawMode; }
    public int sectionX() { return sectionX; }
    public int sectionY() { return sectionY; }
    public int sectionZ() { return sectionZ; }
    public int vertexCount() { return vertices.length / vertexStride; }
    public int indexCount() { return indexElementBytes == 0 ? 0 : indices.length / indexElementBytes; }
    public int indexElementBytes() { return indexElementBytes; }
    public long sizeBytes() { return sizeBytes; }

    /** A fresh read-only view; callers cannot mutate the cached payload. */
    public ByteBuffer verticesView() {
        return ByteBuffer.wrap(vertices).asReadOnlyBuffer();
    }

    /** A fresh read-only view; callers cannot mutate the cached payload. */
    public ByteBuffer indicesView() {
        return ByteBuffer.wrap(indices).asReadOnlyBuffer();
    }

    public byte[] copyVertexBytes() { return vertices.clone(); }
    public byte[] copyIndexBytes() { return indices.clone(); }
}
