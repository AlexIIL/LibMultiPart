# LibMultiPart

This is a library mod for the [Fabric](https://fabricmc.net/) API, based around [Minecraft](https://minecraft.net), that adds support for multiple "parts" (such as pipes, facades, wires, etc) in a single block.

## Maven

You can depend on this by adding this to your build.gradle:

```
repositories {
    maven {
        name = "BuildCraft"
        url = "https://mod-buildcraft.com/maven"
    }
}

dependencies {
    modCompile "alexiil.mc.lib:libmultipart-all:0.2.0"
}
```

There are two modules availible: "libmultipart-all" and "libmultipart-base". The base module is *just* libmultipart, while libmultipart-all includes libmultipart-base and it's dependencies: libblockattributes-core and libnetworkstack-base.

## Getting Started

You can either look at the [wiki](https://github.com/AlexIIL/LibMultiPart/wiki) for a brief overview, or look at [SimplePipes](https://github.com/AlexIIL/SimplePipes) source code for the tank or facades for examples on how to register and use multiparts. (To get help you can either open an issue here, or ping AlexIIL on the fabric or CottonMC discord servers).
