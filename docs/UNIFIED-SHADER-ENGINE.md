

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


## Alpha update — scoped shader program binding and executor preflight

GlShaderProgram.bindScoped() now captures GL_CURRENT_PROGRAM, binds the custom program, and restores the previously active program when its Binding is closed. This avoids assuming that program zero was active before an OptiCore pass and gives future pass handlers a safer state boundary.

The render-graph executor now resolves every required handler before invoking the first pass. A missing handler later in the schedule therefore fails before any earlier pass can partially draw the frame. Tests cover this preflight behavior as well as the existing stop-on-exception path.

These changes improve the safety primitives but do not yet connect the graph to a Forge render event or draw the Minecraft scene. bindScoped() restores only the active program; any future handler that changes textures, framebuffers, viewport, blend/depth state, uniforms, or buffers must restore those states separately.


## Alpha update — typed GLSL uniform uploads

GlShaderProgram now caches uniform locations per linked program and exposes bounded typed setters for integer/sampler indices, float, vec2, vec3, vec4, and column-major mat4 values. Uploads require the owning program to be currently bound, and missing/optimized-out uniforms are treated as no-ops as OpenGL specifies. The matrix API validates that at least 16 floats remain in the supplied buffer.

This supplies the basic parameter-upload layer needed by future shader passes. It does not yet map OptiFine's standard uniforms (camera matrices, time, fog, lightmap, world time, etc.) or bind texture inputs; those require a dedicated Minecraft/OptiFine compatibility layer and runtime testing.


## Alpha update — framebuffer isolation and complete clears

Off-screen target allocation and scoped binding now preserve draw and read framebuffer bindings independently instead of collapsing both through the combined framebuffer binding. This matters when another renderer temporarily uses distinct read/draw targets.

The target clear operation now temporarily disables scissor testing and enables color/depth write masks so a prior GUI or renderer state cannot leave part of the off-screen target uncleared. It restores the previous clear color/depth values, color/depth masks, scissor enable state, framebuffer bindings, and viewport even when the clear scope exits exceptionally.

This hardens the off-screen resource layer only. It does not yet schedule a render pass or composite the target onto Minecraft's main framebuffer.


Off-screen target resize and close now reject attempts made while that target is bound for reading or drawing. This prevents deleting an active framebuffer from silently replacing the caller's binding with framebuffer zero and invalidating the surrounding scope.


## Alpha update — scene uniform capture adapter

Added an immutable scene snapshot model and a Forge 1.20.1 render-stage observer. When the client config option captureSceneUniforms is enabled, OptiCore samples the pose-stack model-view matrix, projection matrix, camera world position, partial tick, render tick, viewport size, dimension ID, game time, and day time at RenderLevelStageEvent.Stage.AFTER_LEVEL. The latest snapshot is exposed through a read-only accessor for the next uniform-binding layer.

Capture is disabled by default. The observer does not bind programs, alter render state, allocate GPU resources, or draw anything; sampling occurs after the level render completes. The snapshot validates finite values and defensively copies matrices. Standalone regression tests cover immutability, matrix validation, partial-tick bounds, time conversion, and day-cycle normalization.

Fog color/density, lightmap values, inverse matrices, and per-pack OptiFine uniform aliases are intentionally not synthesized yet. They require separate API and semantic validation before being exposed as compatibility uniforms. No world shader rendering is enabled by this observer.


## Alpha update — common scene uniform binder

Added an explicit render-thread uniform binder for an already-bound OptiCore GL program. It uploads the captured model-view and projection matrices plus the commonly used OptiFine-style names `gbufferModelView`, `gbufferProjection`, `cameraPosition`, `viewWidth`, `viewHeight`, `frameTimeCounter`, `worldTime`, and `worldDay`. Uniforms absent from a particular program remain harmless no-ops through the existing location lookup behavior.

This is deliberately a small compatibility starter set, not a claim of complete OptiFine uniform parity. It does not synthesize inverse matrices, fog, rain/thunder, eye brightness, lightmap, entity/block context, shadow parameters, or texture samplers. The binder is an explicit API and is not called automatically by the world renderer; callers must bind the program first and invoke it on the render thread.


## Alpha update — weather, fog colour, and camera light capture

Added a separate immutable environment snapshot for sky/fog colour, rain strength, thunder strength, sky darkening, and block/sky light levels sampled at the camera's block position. The Forge observer captures it alongside the matrices and time data, only when the existing opt-in capture setting is enabled.

The common uniform binder now has an overload that accepts this environment sample and uploads `fogColor`, `rainStrength`, `wetness`, `thunderStrength`, `skyDarken`, `blockLightLevel`, and `skyLightLevel` when the corresponding uniforms exist. The original two-argument method remains available and skips environment values. These names and values are a practical starter mapping, not full OptiFine semantic parity: fog distance, eye brightness, weather transitions, biome-specific fog rules, entity/block light contexts, and shadow uniforms still need dedicated work and validation.


## Alpha update — weather, fog colour, and camera light capture

Added a separate immutable environment snapshot for sky/fog colour, rain strength, thunder strength, sky darkening, and block/sky light levels sampled at the camera's block position. The Forge observer captures it alongside the matrices and time data, only when the existing opt-in capture setting is enabled.

The common uniform binder now has an overload that accepts this environment sample and uploads `fogColor`, `rainStrength`, `wetness`, `thunderStrength`, `skyDarken`, `blockLightLevel`, and `skyLightLevel` when the corresponding uniforms exist. The original two-argument method remains available and skips environment values. These names and values are a practical starter mapping, not full OptiFine semantic parity: fog distance, eye brightness, weather transitions, biome-specific fog rules, entity/block light contexts, and shadow uniforms still need dedicated work and validation.


## Accuracy note — captured sky colour versus fog parameters

The current Forge observer samples `ClientLevel.getSkyColor(camera, partialTick)`. This is a sky-colour sample used as an environment input; it is not Minecraft's resolved fog colour or fog density. The corresponding uniform is therefore named `skyColor`, not `fogColor`. True fog colour/distance and eye-medium parameters remain unimplemented until a stable, correctly timed fog capture point is added. This avoids feeding a misleading sky-colour value into shader packs as if it were OptiFine fog data.


## Alpha update — inverse scene matrices

Added a dependency-free, column-major 4x4 inversion utility using pivoted Gauss-Jordan elimination in double precision. Singular, malformed, non-finite, or out-of-float-range matrices are rejected instead of uploading invalid values. Scene snapshots expose inverse model-view and projection matrices, and the uniform binder uploads `gbufferModelViewInverse` and `gbufferProjectionInverse` alongside the original matrices.

Standalone tests cover identity, translation, multiplication by the computed inverse, malformed input, and singular matrices. This supplies matrix data only; normal matrices, shadow-camera matrices, and the full OptiFine alias set remain separate work.


## Alpha update — normal matrix capture and upload

Added a dependency-free inverse-transpose 3x3 normal-matrix calculation derived from the linear part of the captured model-view matrix. Translation is ignored, while singular, malformed, non-finite, or out-of-range transforms are rejected. The scene snapshot exposes the derived matrix and the GL program wrapper now supports typed column-major mat3 uploads; the common binder sends it as `normalMatrix`.

Standalone regression checks cover identity, non-uniform scale, translation independence, shear, and invalid/singular inputs. This provides the matrix required for correct normal-vector transformation, but it does not yet add lighting calculations, material handling, shadow maps, or world-render integration. The normal-matrix upload is still part of the explicit binder call and does not activate shader packs automatically.


## Alpha update — shadow camera matrix foundation

Added standalone, graphics-API-independent shadow camera helpers:
- right-handed `lookAt` view matrix construction;
- OpenGL orthographic projection matrix construction;
- column-major projection/view matrix multiplication.

Inputs are validated for non-finite values, degenerate view directions, parallel up vectors, and invalid projection bounds. Regression tests cover expected camera transforms, multiplication, and invalid inputs, and are included in the Java 17 cache-test script.

This is only the mathematical foundation. No light direction is selected, no shadow framebuffer or depth texture is rendered, no terrain/entity casters are submitted, and no shadow sampling is wired into the world pipeline. The next implementation step is a bounded shadow-pass resource contract with explicit GL-state capture/restore and a vanilla fallback before any live world-render integration.


## Shadow depth target resource

`ShadowDepthTarget` now allocates a depth-only OpenGL texture framebuffer with nearest filtering, clamp-to-edge wrapping, comparison sampling disabled, and explicit framebuffer completeness validation. It enforces dimension/pixel limits, restores prior draw/read framebuffer and texture bindings after allocation, provides scoped framebuffer/viewport binding, and clears depth while restoring the depth-write, scissor, and clear-depth state. Closing a target while it is bound is rejected.

This is resource infrastructure only: there is no light-space scene capture, caster submission, shadow filtering, or shader sampling yet. The target is not registered in the live world-render pipeline.
