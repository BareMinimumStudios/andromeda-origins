package andromeda.origins.mixin

import andromeda.origins.compat.EntityUnloadDiagnostics
import net.minecraft.world.level.entity.PersistentEntitySectionManager
import net.minecraft.world.level.entity.EntityAccess
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.Redirect
import it.unimi.dsi.fastutil.longs.LongSet
import java.util.function.LongPredicate
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(PersistentEntitySectionManager::class)
abstract class EntityUnloadDiagnosticsMixin {
    @Unique @Volatile private var `andromeda$ownerThread`: Thread? = null
    @Unique @Volatile private var `andromeda$unloadDepth`: Int = 0
    @Unique private var `andromeda$lastChunk`: Long? = null
    @Unique private var `andromeda$lastEntity`: String? = null

    @Inject(method = ["<init>"], at = [At("RETURN")])
    private fun `andromeda$rememberOwner`(ci: CallbackInfo) {
        // Capture even while disabled so enabling the live config covers existing worlds.
        `andromeda$ownerThread` = Thread.currentThread()
    }

    @Redirect(method = ["processUnloads"], at = At(value = "INVOKE",
        target = "Lit/unimi/dsi/fastutil/longs/LongSet;removeIf(Ljava/util/function/LongPredicate;)Z"))
    private fun `andromeda$observeUnloading`(queue: LongSet, predicate: LongPredicate): Boolean {
        if (!EntityUnloadDiagnostics.enabled) return queue.removeIf(predicate)
        val before = queue.size
        EntityUnloadDiagnostics.entry(this, `andromeda$ownerThread`, `andromeda$unloadDepth`,
            before, `andromeda$lastChunk`, `andromeda$lastEntity`)
        val previousChunk = `andromeda$lastChunk`
        val previousEntity = `andromeda$lastEntity`
        `andromeda$lastChunk` = null
        `andromeda$lastEntity` = null
        `andromeda$unloadDepth`++
        try {
            return queue.removeIf(LongPredicate { chunk ->
                `andromeda$lastChunk` = chunk
                `andromeda$lastEntity` = null
                predicate.test(chunk)
            })
        } catch (failure: Throwable) {
            EntityUnloadDiagnostics.failure(this, `andromeda$ownerThread`, `andromeda$unloadDepth`,
                `andromeda$lastChunk`, `andromeda$lastEntity`, before, queue.size, failure)
            throw failure
        } finally {
            `andromeda$unloadDepth`--
            `andromeda$lastChunk` = previousChunk
            `andromeda$lastEntity` = previousEntity
        }
    }

    @Inject(method = ["unloadEntity"], at = [At("HEAD")])
    private fun `andromeda$rememberEntity`(entity: EntityAccess, ci: CallbackInfo) {
        if (EntityUnloadDiagnostics.enabled && `andromeda$unloadDepth` > 0)
            `andromeda$lastEntity` = "${entity.javaClass.name}/${entity.uuid}"
    }

    @Redirect(method = ["updateChunkStatus(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/entity/Visibility;)V"],
        at = At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;add(J)Z"))
    private fun `andromeda$traceAdd`(queue: LongSet, chunk: Long): Boolean {
        val changed = queue.add(chunk)
        if (EntityUnloadDiagnostics.enabled) EntityUnloadDiagnostics.mutation(this, `andromeda$ownerThread`,
            `andromeda$unloadDepth`, chunk, "add", changed, queue.size)
        return changed
    }

    @Redirect(method = ["updateChunkStatus(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/entity/Visibility;)V"],
        at = At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;remove(J)Z"))
    private fun `andromeda$traceRemove`(queue: LongSet, chunk: Long): Boolean {
        val changed = queue.remove(chunk)
        if (EntityUnloadDiagnostics.enabled) EntityUnloadDiagnostics.mutation(this, `andromeda$ownerThread`,
            `andromeda$unloadDepth`, chunk, "remove", changed, queue.size)
        return changed
    }
}
