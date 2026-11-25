package dev.opticore.cache;

import java.nio.ReadOnlyBufferException;
import java.util.Arrays;

public final class ImmutableMeshSnapshotSelfTest {
    private ImmutableMeshSnapshotSelfTest() {}

    public static void main(String[] args) {
        byte[] vertices = {1, 2, 3, 4, 5, 6, 7, 8};
        byte[] indices = {0, 0, 1, 0};
        var mesh = new ImmutableMeshSnapshot(
                "position_uv_color", 4, 4, -2, 3, 7, 2, vertices, indices);

        vertices[0] = 99;
        indices[0] = 99;
        check(mesh.copyVertexBytes()[0] == 1, "constructor copies vertex bytes");
        check(mesh.copyIndexBytes()[0] == 0, "constructor copies index bytes");
        check(mesh.vertexCount() == 2, "vertex count derives from stride");
        check(mesh.indexCount() == 2, "index count derives from element width");
        check(mesh.sizeBytes() == 12, "retained size includes vertex and index payloads");
        check(mesh.sectionX() == -2 && mesh.sectionY() == 3 && mesh.sectionZ() == 7,
                "section coordinates are retained");
        check(mesh.vertexLayout().equals("position_uv_color") && mesh.drawMode() == 4,
                "layout and draw mode are retained");

        byte[] copy = mesh.copyVertexBytes();
        copy[0] = 88;
        check(mesh.copyVertexBytes()[0] == 1, "copy accessor cannot mutate snapshot");

        boolean readOnly = false;
        try {
            mesh.verticesView().put(0, (byte) 42);
        } catch (ReadOnlyBufferException expected) {
            readOnly = true;
        }
        check(readOnly, "buffer view is read-only");

        expectFailure(() -> new ImmutableMeshSnapshot("x", 0, 4, 0, 0, 0, 0,
                new byte[4], new byte[0]), "zero stride rejected");
        expectFailure(() -> new ImmutableMeshSnapshot("x", 3, 4, 0, 0, 0, 0,
                new byte[4], new byte[0]), "misaligned vertex payload rejected");
        expectFailure(() -> new ImmutableMeshSnapshot("x", 4, 4, 0, 0, 0, 2,
                new byte[4], new byte[0]), "orphan index width rejected");
        expectFailure(() -> new ImmutableMeshSnapshot("x", 4, 4, 0, 0, 0, 0,
                new byte[4], new byte[2]), "missing index width rejected");
        expectFailure(() -> new ImmutableMeshSnapshot("x", 4, 4, 0, 0, 0, 2,
                new byte[4], new byte[3]), "misaligned index payload rejected");
        expectFailure(() -> new ImmutableMeshSnapshot(" ", 4, 4, 0, 0, 0, 0,
                new byte[4], new byte[0]), "blank layout rejected");
        expectFailure(() -> new ImmutableMeshSnapshot("x", 4, -1, 0, 0, 0, 0,
                new byte[4], new byte[0]), "negative draw mode rejected");

        System.out.println("ImmutableMeshSnapshotSelfTest: 14 checks passed");
    }

    private static void expectFailure(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label);
        } catch (IllegalArgumentException expected) {
            // Expected validation failure.
        }
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
