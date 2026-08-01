package gg.earu.coh

import gg.earu.coh.platform.Platform
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Coh {
    const val MOD_ID = "coh"
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    lateinit var platform: Platform
        private set

    fun init(platform: Platform) {
        this.platform = platform
        LOGGER.info("COH {} initialized", platform.modVersion)
    }
}
