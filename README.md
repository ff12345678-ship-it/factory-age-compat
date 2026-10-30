# Factory Age Compat

Source of **Factory Age Compatibility** (`factory_age_compat`), the small helper mod shipped with the
[Factory Age](https://www.curseforge.com/minecraft/modpacks/factory-age) modpack (Minecraft 1.21.1, NeoForge).
It patches interactions between mods in the pack. Released on CurseForge as the project *Factory Age Compat*.

## What it does

- **Flux Dust**: makes the Flux Networks obsidian-and-redstone conversion work on top of a compressed obsidian base (for void islands), with a JEI page.
- **Runelic**: preloads its configuration with Minecraft property adapters so world loading does not fail.
- **JEI recipe visibility**: shows crafting recipes that JEI hides (e.g. Quark chests, LaserIO card clearing) and writes an audit table of all recipes.
- **Pattern Encoding Terminal**: takes a blank pattern from the ME network when the terminal slot is empty.
- **Mekanical Create factory**: cancels vanilla crafting-table recipes in the factory simulation.
- **Natural Charged Snowball**, **Firmament Starship Beacon**: two KubeJS items whose behaviour is implemented here.
- **Load-time filters**: skips recipe-unlock advancements and unused Farmer's Market recipes; lowers the ProjectE debug log level.
- **Entity Accelerator** (ExtendedAE Plus 1.6.2): pays from the FE stored in Applied Flux cells first, then falls back to AE power. Config: `entityTickerPreferFlux`.

Everything that depends on an optional mod is guarded (`@Pseudo` mixins, caught errors), so it does not crash if that mod is replaced.

## Layout

- `src/` Java sources (package `factoryage.compat`, mixins in `factoryage.compat.mixin`).
- `resources/` `neoforge.mods.toml` and the mixin config.

## Building

This is not a Gradle project. It is compiled with plain `javac` (JDK 21) against the mod jars of the Factory Age
instance (NeoForge 21.1.x, AE2 19.x, JEI, KubeJS, Mekanism, Applied Flux, ExtendedAE Plus, Flux Networks, Runelic, Prickle),
then the compiled classes and `resources/` are packed into one jar. The mixin list in `resources/factory_age_compat.mixins.json`
must match the files in `src/factoryage/compat/mixin/`.

## License

MIT, see `LICENSE`.

---

# 工厂时代兼容模组

这是随整合包 [Factory Age（工厂时代）](https://www.curseforge.com/minecraft/modpacks/factory-age) 发布的小型兼容模组的源码，
用来修补整合包里各模组之间的问题，在 CurseForge 上以 *Factory Age Compat* 项目发布。功能见上面的英文列表。
不是 Gradle 工程，用 JDK 21 的 javac 针对实例里的模组 jar 编译，再和 `resources/` 一起打成 jar。
