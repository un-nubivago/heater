# Changelog

## [Unreleased]

## [4.2] - 2026-01-11

### Added

+ Added fuel whitelisting and casting functionalities to the `Thermostat`.

### Changed

+ Updated `Burning` dependency to `3.0`.
+ Fixed the burning propagation logic.

## [4.1] - 2025-12-26

Backward release for Minecraft 1.20.1, no corresponding tag.

### Changed

+ Updated `Burning` dependency to `2.0` to use the new `BurningPropagator` interface and logic.

## [4.0.7] - 2025-10-05

### Changed

+ Update target Minecraft version to 1.21.9.

## [4.0.6] - 2025-08-18

### Changed

+ Update target Minecraft version to 1.21.6, with up to 1.21.8 compatibility.
+ Update `burning` library to 1.1+1.21.6 version.

## [4.0.5] - 2025-08-16

### Changed

+ Update target Minecraft version to 1.21.5.

## [4.0.4] - 2025-08-16

### Changed

+ Update target Minecraft version to 1.21.4.
+ Update `burning` library to 1.0+1.21.4 version.

## [4.0.3] - 2025-06-23

### Changed

+ Update `burning` library to 1.0+1.21.2 version.

### Fixed

+ Fix occasional game-crashing NullPointerException when breaking a heat receiving block.

## [4.0.2] - 2025-06-21

### Changed

+ Update target Minecraft version to 1.21.2, with 1.21.3 compatibility.
+ Update `burning` library version to 1.0.1+1.21.

## [4.0.1] - 2025-06-15

### Changed

+ Update `burning` library version to 1.0+1.21.

## [4.0] - 2024-10-13

### Added

+ Now using `burning` for transferring ~~heat~~ burning fuel between furnace-like blocks.

### Changed

+ Update target Minecraft version to 1.21, with 1.21.1 compatibility.

### Removed

+ Remove all the logic regarding tags.

## [3.3] - 2024-09-07

### Fixed

+ Fix interoperability with Variant Furnaces and hopefully with other 3rd party mods, too ([#5](https://github.com/un-nubivago/heater/issues/5)).

## [3.2] - 2024-08-03

### Changed

+ Rename `heater:adapters/heat_sink` dynamic registry to `heater:adapters/furnace`.

+ Update connection and propagation logic through tags.

### Fixed

+ Optimize heat propagation through caching.

+ Optimize Heater container logic through Fabric Transfer API for Items.

## [3.1] - 2024-04-15

### Added

+ Add `heater:adapters/heat_sink` dynamic registry for better compatibility with third-party mods.

## [3.0] - 2024-03-17

### Changed

+ Update target Minecraft version to 1.20.3, with 1.20.4 compatibility.

## [2.0] - 2024-02-26

### Added

+ Add the new Thermostat block, fully oxidiziable and waxable.

### Changed

+ Drop mod's patch version because unnecessary.

+ Update target Minecraft version to 1.20.2 and remap mod.

## [1.2.0] - 2024-02-16

### Added

+ Add compatibility with mods that add furnaces whose entities don't extend `AbstractFurnaceBlockEntity`.

### Fixed

+ Fix Heaters dropping their content when they get oxidized, restored, waxed, or unwaxed.

## [1.1.1] - 2024-02-09

### Fixed

+ Fix oxidation, restoration, waxing, and unwaxing processes for every introduced block. They now work correctly, and you can obtain every block in Survival.

## [1.1.0] - 2024-01-31

### Added

+ Add new textures for `Unaffected`, `Exposed`, `Weathered`, and `Oxidized` variants for the `Heater` and the `Heat Pipe` blocks ([#2](https://github.com/un-nubivago/heater/pull/2)) (StarOcean)

+ Add `Exposed`, `Weathered`, and `Oxidized` variants of the `Heater` and the `Heat Pipe` blocks and items. Only accessible through creative mode.

+ Add craftable `Waxed` variants of the aforementioned blocks and items.

## [1.0.0] - 2024-01-03

Initial release.

[4.2]: https://github.com/un-nubivago/heater/releases/tag/4.2
[4.1]: https://github.com/un-nubivago/heater/releases
[4.0.7]: https://github.com/un-nubivago/heater/releases/tag/4.0.7
[4.0.6]: https://github.com/un-nubivago/heater/releases/tag/4.0.6
[4.0.5]: https://github.com/un-nubivago/heater/releases/tag/4.0.5
[4.0.4]: https://github.com/un-nubivago/heater/releases/tag/4.0.4
[4.0.3]: https://github.com/un-nubivago/heater/releases/tag/4.0.3
[4.0.2]: https://github.com/un-nubivago/heater/releases/tag/4.0.2
[4.0.1]: https://github.com/un-nubivago/heater/releases/tag/4.0.1
[4.0]: https://github.com/un-nubivago/heater/releases/tag/4.0
[3.3]: https://github.com/un-nubivago/heater/releases/tag/3.3
[3.2]: https://github.com/un-nubivago/heater/releases/tag/3.2
[3.1]: https://github.com/un-nubivago/heater/releases/tag/3.1
[3.0]: https://github.com/un-nubivago/heater/releases/tag/3.0
[2.0]: https://github.com/un-nubivago/heater/releases/tag/2.0
[1.2.0]: https://github.com/un-nubivago/heater/releases/tag/1.2.0
[1.1.1]: https://github.com/un-nubivago/heater/releases/tag/1.1.1
[1.1.0]: https://github.com/un-nubivago/heater/releases/tag/1.1.0
[1.0.0]: https://github.com/un-nubivago/heater/releases/tag/1.0.0
