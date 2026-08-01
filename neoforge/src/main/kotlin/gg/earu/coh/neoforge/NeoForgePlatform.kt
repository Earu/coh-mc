package gg.earu.coh.neoforge

import gg.earu.coh.platform.Platform
import java.nio.file.Path

class NeoForgePlatform(
    override val configDir: Path,
    override val isClient: Boolean,
    override val modVersion: String,
) : Platform
