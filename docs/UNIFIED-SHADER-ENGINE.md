

## Alpha update — first render-thread OpenGL program owner

Added `GlShaderProgram`, a client-side OpenGL backend that compiles one vertex source and one fragment source, links them, returns the driver diagnostics on failure, and owns deletion of the linked program. Entry points assert Minecraft's render thread, source inputs have a 4 MiB defensive limit, and cleanup runs for partially completed compile/link attempts.

This is the first actual GPU compile/link capability in the codebase, but it is not yet connected to shader-pack selection or Minecraft's world-render pipeline. It does not create framebuffers, bind OptiFine uniforms/samplers, schedule render passes, or draw a shader pack. The caller must have a current OpenGL context and is responsible for restoring any GL state it changes. Runtime validation in a launched Forge client is still required before this backend can be considered production-ready.


## Alpha update — bounded program cache and reload invalidation

Added a render-thread-only LRU cache for linked GL programs with a hard limit of 32 entries. Cache hits require exact vertex and fragment source matches. Replacement programs are compiled before old entries are released, so a failed replacement does not discard the last valid cached program. Evicted and explicitly cleared entries release their GL handles.

The client resource-reload listener now schedules cache cleanup through Minecraft's render-call queue, so GL deletion is deferred until the render thread has the context. This establishes bounded ownership and reload invalidation for compiled programs. It is still not a pack activation system or render graph: no pack is automatically compiled, no world pass binds these programs, and resize/context-loss handling beyond resource reload remains to be implemented and tested.


## Alpha update — selected-pack compile bridge

Added an explicit runtime bridge from the saved shader-pack selection to the source descriptor, GL compiler, and bounded program cache. A caller can request a program by its relative program ID; the bridge resolves the selected folder/ZIP, runs bounded include expansion and declaration preflight, then asks the GL backend to compile and link the two stages. Failures are returned as diagnostics and do not produce a program handle.

This is now a real selected-pack-to-GLSL-compile path when explicitly called on the render thread with a current graphics context. It still does not enumerate and compile every program automatically, activate the pack globally, bind uniforms or textures, draw terrain/entities, or execute shadow/composite/final passes. No in-game shader-pack rendering is claimed.


## Alpha update — bounded off-screen render target foundation

Added a render-thread-owned off-screen target abstraction for the next render-graph stage. Each target owns an RGBA8 color texture, a depth renderbuffer, and a framebuffer. Allocation is transactional: incomplete targets are discarded, prior framebuffer/texture/renderbuffer bindings are restored, and an existing target is retained if a resize allocation fails.

Targets enforce dimension and pixel-count limits, support explicit clear and scoped bind/restore operations, and release their GL objects on close. This is resource infrastructure only: no world-render stage uses the target yet, no shader uniforms or scene textures are populated, and no shadow/composite/final pass is active. Runtime validation with a live Forge graphics context remains necessary.


## Alpha update — bounded render-target manager and reload lifecycle

Added a render-thread-only LRU manager for named off-screen targets. It caps the cache at four targets and 16,777,216 aggregate pixels. Resize requests are budget-checked and use the target's transactional replacement path; new allocations happen before eviction so an allocation failure does not discard existing targets. Explicit release and full clear close their OpenGL resources.

The resource-reload listener now clears both compiled shader programs and managed off-screen targets on Minecraft's render-call queue. Off-screen clear operations also restore the prior global GL clear-color value.

This remains resource management, not a render graph or world integration. No terrain/entity geometry is rendered into these targets, and no shader pack pass is activated. A live Forge client test is still required to validate driver behavior and lifecycle under resize/reload.


## Alpha update — declarative render graph model

Added a Minecraft-independent immutable render-graph description. Each pass declares its input resources, output resources, explicit pass dependencies, and depth behavior. Validation checks missing producers and inputs, missing dependencies, duplicate writers, invalid pass declarations, and dependency cycles. A stable topological schedule is returned only when validation succeeds; resource producer/consumer relationships automatically create ordering edges.

The graph model is intentionally declarative and performs no OpenGL calls. It is not yet connected to Forge world-render events, Minecraft's world renderer, shader uniforms, or framebuffer execution. It does not draw terrain, entities, shadows, reflections, or post-processing. A later integration step must define concrete pass executors, external Minecraft render resources, GL-state restoration, and a safe vanilla fallback before enabling world-render changes.


## Alpha update — fail-safe render graph executor

Added a backend-agnostic executor for validated render graphs. It invokes caller-registered pass handlers in dependency order, refuses to run an invalid graph, reports missing handlers, and stops immediately on the first handler exception while preserving an accurate list of completed passes. This provides deterministic scheduling and failure reporting for a future renderer backend.

The executor does not itself bind framebuffers, compile or bind programs, manipulate Minecraft render buffers, or restore OpenGL state. Any future OpenGL pass handler must scope and restore its own GL state and provide a vanilla fallback at the integration boundary. No Forge world-render event is hooked yet; the executor is currently infrastructure covered by Minecraft-independent tests.
