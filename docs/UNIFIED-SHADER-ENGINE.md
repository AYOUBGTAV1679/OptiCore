

## Alpha update — first render-thread OpenGL program owner

Added `GlShaderProgram`, a client-side OpenGL backend that compiles one vertex source and one fragment source, links them, returns the driver diagnostics on failure, and owns deletion of the linked program. Entry points assert Minecraft's render thread, source inputs have a 4 MiB defensive limit, and cleanup runs for partially completed compile/link attempts.

This is the first actual GPU compile/link capability in the codebase, but it is not yet connected to shader-pack selection or Minecraft's world-render pipeline. It does not create framebuffers, bind OptiFine uniforms/samplers, schedule render passes, or draw a shader pack. The caller must have a current OpenGL context and is responsible for restoring any GL state it changes. Runtime validation in a launched Forge client is still required before this backend can be considered production-ready.
