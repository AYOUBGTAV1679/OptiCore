# Alpha 09 HUD correction

Fixed a UI logic issue: the chunk diagnostics overlay was nested under the FPS counter preference. Turning off the FPS counter therefore also hid diagnostics even when diagnostics were enabled.

The HUD now evaluates the two preferences independently and draws either or both. Dispatcher sampling remains read-only and opt-in.

Validation: the standalone cache suite passed 25/25 tests. Those tests cover the Minecraft-independent cache only; they do not compile Forge client classes. A full Forge build and in-game verification remain pending.
