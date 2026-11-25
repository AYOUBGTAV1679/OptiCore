package dev.opticore.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.opticore.cache.ImmutableMeshSnapshot;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Copies a vanilla RenderedBuffer into an independent CPU snapshot.
 *
 * <p>This method must be called while the RenderedBuffer is still valid and
 * before vanilla releases it. It never releases, stores, or returns the
 * RenderedBuffer, DrawState, or any GPU resource. Sequential-index buffers
 * are deliberately skipped because ImmutableMeshSnapshot currently models
 * only explicit index payloads.</p>
 */
public final class RenderedBufferSnapshotCopier {
    private RenderedBufferSnapshotCopier() {}

    public static ImmutableMeshSnapshot copy(
            BufferBuilder.RenderedBuffer rendered,
            int sectionX,
            int sectionY,
            int sectionZ) {
        Objects.requireNonNull(rendered, "rendered");
        if (rendered.isEmpty()) return null;

        BufferBuilder.DrawState state = rendered.drawState();
        if (state == null || state.sequentialIndex() || state.indexOnly()) return null;

        VertexFormat format = state.format();
        if (format == null || format.getVertexSize() <= 0) return null;

        byte[] vertexBytes = copyRemaining(rendered.vertexBuffer());
        byte[] indexBytes = state.indexCount() == 0
                ? new byte[0]
                : copyRemaining(rendered.indexBuffer());

        int stride = format.getVertexSize();
        if (vertexBytes.length != state.vertexBufferSize()
                || vertexBytes.length != (long) state.vertexCount() * stride
                || vertexBytes.length % stride != 0) {
            return null;
        }

        int indexBytesPerElement = state.indexCount() == 0
                ? 0 : state.indexType().bytes;
        if (indexBytesPerElement != 0
                && indexBytes.length != (long) state.indexCount() * indexBytesPerElement) {
            return null;
        }

        return new ImmutableMeshSnapshot(
                format.toString(),
                stride,
                state.mode().asGLMode,
                sectionX,
                sectionY,
                sectionZ,
                indexBytesPerElement,
                vertexBytes,
                indexBytes);
    }

    private static byte[] copyRemaining(ByteBuffer source) {
        Objects.requireNonNull(source, "source");
        ByteBuffer view = source.asReadOnlyBuffer();
        byte[] copy = new byte[view.remaining()];
        view.get(copy);
        return copy;
    }
}
