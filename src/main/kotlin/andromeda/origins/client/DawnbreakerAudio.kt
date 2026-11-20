package andromeda.origins.client

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation

object DawnbreakerAudio {
    fun playBanishmentHeartbeat() {
        val client = Minecraft.getInstance()
        if (client.player == null || client.level == null) return
        val sound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("andromeda_origins:ability.dawnbreaker.banishment_heartbeat")) ?: return
        // Non-positional: listener/camera coordinates can still be settling after the 500-block snap.
        client.soundManager.play(SimpleSoundInstance.forUI(sound, .85f, .7f * andromeda.origins.compat.DawnbreakerPresentation.soundScale(andromeda.origins.compat.DawnbreakerPresentation.tier(client.player!!))))
    }
}
