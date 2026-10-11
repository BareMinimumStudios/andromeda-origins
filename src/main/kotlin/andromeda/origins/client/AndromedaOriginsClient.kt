package andromeda.origins.client

import andromeda.origins.client.VersionHandshakeClient
import andromeda.origins.client.config.AndromedaFzzyConfigClient
import andromeda.origins.client.VeilStepClient
import andromeda.origins.client.VeilStepVisuals


object AndromedaOriginsClient {
    fun initialize() {
        VersionHandshakeClient.register()
        AndromedaFzzyConfigClient.register()
        VeilStepClient.register()
        VeilStepVisuals.register()
            // Origin Toggle is data-driven at data/andromeda_origins/keybinds/toggle.json.
        // Opoli registers it as key.andromeda_origins.toggle and preserves saved rebinds.
    }
}
