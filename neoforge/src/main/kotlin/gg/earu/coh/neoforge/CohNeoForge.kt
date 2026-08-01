package gg.earu.coh.neoforge

import gg.earu.coh.Coh
import gg.earu.coh.server.CohServer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.fml.loading.FMLPaths

@Mod(Coh.MOD_ID)
class CohNeoForge {
    init {
        Coh.init(
            NeoForgePlatform(
                configDir = FMLPaths.CONFIGDIR.get().resolve(Coh.MOD_ID),
                isClient = FMLEnvironment.dist.isClient,
                modVersion = ModList.get().getModContainerById(Coh.MOD_ID)
                    .map { it.modInfo.version.toString() }.orElse("dev"),
            )
        )
        CohServer.init(Coh.platform)

        Payloads.register()
        MinecraftForge.EVENT_BUS.register(ServerEvents)
        if (FMLEnvironment.dist.isClient) {
            ClientEvents.wire()
            MinecraftForge.EVENT_BUS.register(ClientEvents)
        }
    }
}
