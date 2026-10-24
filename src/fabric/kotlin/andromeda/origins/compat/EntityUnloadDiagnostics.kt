package andromeda.origins.compat

import net.minecraft.world.level.ChunkPos
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

/** Diagnostic only: retain Minecraft's original queue and propagate its failures. */
object EntityUnloadDiagnostics {
    val enabled: Boolean = java.lang.Boolean.getBoolean("andromeda.entityUnloadDiagnostics")
    private val logger = LoggerFactory.getLogger("Andromeda Origins/Entity Unload Trace")
    private val reports = AtomicInteger()
    private val entries = AtomicInteger()
    private const val LIMIT = 32

    init {
        if (enabled) logger.info("ENTITY_UNLOAD_TRACE enabled: observing queue mutations and nested/off-thread unload entry; maximum {} reports per trace kind; no unloading repair", LIMIT)
    }

    fun entry(manager: Any, owner: Thread?, depth: Int, size: Int, outerChunk: Long?, outerEntity: String?) {
        val current = Thread.currentThread()
        if (owner == null || (current === owner && depth == 0)) return
        val number = entries.incrementAndGet()
        if (number > LIMIT) return
        logger.warn("ENTITY_UNLOAD_ENTRY #{} manager={} callerThread={} expectedThread={} outerDepth={} queueSize={} outerChunk={} outerEntity={}; iterator removal can mutate the queue without a status update",
            number, System.identityHashCode(manager), current.name, owner.name, depth, size,
            outerChunk?.let(::ChunkPos), outerEntity, Throwable("Nested/off-thread entity unload caller"))
        if (number == LIMIT) logger.warn("Entity unload entry trace reached its {}-report limit for this JVM", LIMIT)
    }

    fun mutation(manager: Any, owner: Thread?, depth: Int, chunk: Long, operation: String, changed: Boolean, size: Int) {
        val current = Thread.currentThread()
        if (!changed || owner == null || (current === owner && depth == 0)) return
        val number = reports.incrementAndGet()
        if (number > LIMIT) return
        logger.warn("ENTITY_UNLOAD_TRACE #{} manager={} callerThread={} expectedThread={} duringUnload={} operation={} chunk={} queueSize={}; actual queue membership changed",
            number, System.identityHashCode(manager), current.name, owner.name, depth > 0, operation, ChunkPos(chunk), size,
            Throwable("Entity unloading queue mutation caller"))
        if (number == LIMIT) logger.warn("Entity unload mutation trace reached its {}-report limit for this JVM; failure reports remain enabled", LIMIT)
    }

    fun failure(manager: Any, owner: Thread?, depth: Int, chunk: Long?, entity: String?, before: Int, after: Int, failure: Throwable) {
        logger.error("ENTITY_UNLOAD_FAILURE manager={} callerThread={} expectedThread={} unloadDepth={} lastPredicateChunk={} lastUnloadingEntity={} queueBefore={} queueAfter={}; original exception is rethrown",
            System.identityHashCode(manager), Thread.currentThread().name, owner?.name, depth, chunk?.let(::ChunkPos), entity, before, after, failure)
    }
}
