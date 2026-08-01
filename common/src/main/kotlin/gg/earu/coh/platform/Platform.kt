package gg.earu.coh.platform

import java.nio.file.Path

/** Loader-specific services, implemented once per mod loader. */
interface Platform {
    /** Root directory for all COH config: `config/coh/`. */
    val configDir: Path

    /** True when running on a client (integrated or remote server connection). */
    val isClient: Boolean

    /** Mod version, for logs/UI. */
    val modVersion: String
}
