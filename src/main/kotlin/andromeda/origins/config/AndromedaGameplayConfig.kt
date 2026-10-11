package andromeda.origins.config

import andromeda.origins.AndromedaOrigins
import andromeda.origins.config.OriginConfigCatalog
import andromeda.origins.config.OriginConfigCatalog.OriginEntry
import andromeda.origins.config.AndromedaConfigApplier
import andromeda.origins.config.AndromedaConfigRuntime
import andromeda.origins.config.AndromedaConfigState
import java.util.LinkedHashMap
import java.util.LinkedHashSet


 /**
 * Fzzy Config-backed, server-authoritative gameplay configuration.
 *
 * Fzzy Config owns persistence, GUI generation, validation, permissions, Mod Menu integration,
 * and server/client synchronization. Andromeda only converts the validated settings into the
 * immutable runtime policy used by Origin filtering and Opoli power suppression.
 */
@me.fzzyhmstrs.fzzy_config.annotations.RootConfig @me.fzzyhmstrs.fzzy_config.annotations.Version(version = 1)
class AndromedaGameplayConfig : me.fzzyhmstrs.fzzy_config.config.Config(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.MOD_ID, "gameplay")) {
    @me.fzzyhmstrs.fzzy_config.annotations.Comment("Standard Andromeda Origins. Open an Origin to configure its availability, powers, diets, and weaknesses. Changes apply immediately; no restart is normally required.")
    var standardOrigins: StandardOriginsSection = StandardOriginsSection()

    @me.fzzyhmstrs.fzzy_config.annotations.Comment("Champion variants. Champion-only infrastructure such as cooldown acceleration is intentionally not exposed as a toggle.")
    var championOrigins: ChampionOriginsSection = ChampionOriginsSection()

    @me.fzzyhmstrs.fzzy_config.annotations.Comment("Overgrown's built-in Origins. The master switch defaults OFF. Individual switches are remembered and take effect when the master switch is enabled.")
    var overgrownOrigins: OvergrownOriginsSection = OvergrownOriginsSection()

    override fun defaultPermLevel(): kotlin.Int {
        return 2
    }

    override fun saveType(): me.fzzyhmstrs.fzzy_config.api.SaveType {
        // A remote server's synced gameplay settings must not overwrite the player's local/single-player file.
        return me.fzzyhmstrs.fzzy_config.api.SaveType.SEPARATE
    }

    override fun onSyncServer() {
        AndromedaConfigRuntime.set(toState())
    }

    override fun onSyncClient() {
        AndromedaConfigRuntime.set(toState())
    }

    override fun onUpdateClient() {
        AndromedaConfigRuntime.set(toState())
    }

    override fun onUpdateServer(context: me.fzzyhmstrs.fzzy_config.event.api.ServerUpdateContext) {
        AndromedaConfigRuntime.set(toState())
        AndromedaConfigApplier.applyToServer(context.getServer())
    }

    fun toState(): AndromedaConfigState {
        val disabledOrigins: kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation> = LinkedHashSet<net.minecraft.resources.ResourceLocation>()
        val disabledStock: kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation> = LinkedHashSet<net.minecraft.resources.ResourceLocation>()
        val disabledByOrigin: kotlin.collections.MutableMap<net.minecraft.resources.ResourceLocation, kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>> = LinkedHashMap<net.minecraft.resources.ResourceLocation, kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>>()
        for (entry in OriginConfigCatalog.STANDARD) {
            collectOriginEntry(disabledOrigins, disabledByOrigin, entry)
        }
        for (entry in OriginConfigCatalog.CHAMPION) {
            collectOriginEntry(disabledOrigins, disabledByOrigin, entry)
        }


        for (entry in OriginConfigCatalog.STOCK) {
            if (!entry.getter.apply(overgrownOrigins).get()) disabledStock.add(entry.id)
        }

        return AndromedaConfigState(
        overgrownOrigins.enableStockOrigins.get(),
        java.util.Set.copyOf<net.minecraft.resources.ResourceLocation>(disabledStock),
        java.util.Set.copyOf<net.minecraft.resources.ResourceLocation>(disabledOrigins),
        AndromedaGameplayConfig.Companion.immutablePowerMap(disabledByOrigin)
        )
    }

    private fun <S : OriginToggleSection>collectOriginEntry(
    disabledOrigins: kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>,
    disabledByOrigin: kotlin.collections.MutableMap<net.minecraft.resources.ResourceLocation, kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>>,
    entry: OriginEntry<S>
    ) {
        val section: S = entry.section.apply(this)
        if (!section.enabled.get()) disabledOrigins.add(entry.id)

        val disabled: kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation> = LinkedHashSet<net.minecraft.resources.ResourceLocation>()
        for (toggle in entry.toggles) {
            if (!toggle.getter.apply(section).get()) disabled.add(toggle.power)
        }
        if (!disabled.isEmpty()) disabledByOrigin.put(entry.id, java.util.Set.copyOf<net.minecraft.resources.ResourceLocation>(disabled))
    }

    abstract class OriginToggleSection : me.fzzyhmstrs.fzzy_config.config.ConfigSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Controls whether this Origin can be selected. If disabled while selected, the player is returned to Origin selection.")
        var enabled: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }
    class ArachneSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Passive Features for Arachne. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Eightfold Swiftness for Arachne. Applies immediately and preserves live power resource/cooldown state.")
        var eightfoldSwiftness: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Weaver's Nest for Arachne. Applies immediately and preserves live power resource/cooldown state.")
        var weaversNest: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Carnivorous Diet for Arachne. Applies immediately and preserves live power resource/cooldown state.")
        var carnivorousDiet: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class FaerieSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Passive Features for Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Flutter for Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var flutter: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Fae Illusions for Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var faeIllusions: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Trickster for Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var trickster: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Iron Weakness for Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var ironWeakness: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class FenrkinSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Passive Features for Fenrkin. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable On the Hunt for Fenrkin. Applies immediately and preserves live power resource/cooldown state.")
        var onTheHunt: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Stamina Surge for Fenrkin. Applies immediately and preserves live power resource/cooldown state.")
        var staminaSurge: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Carnivorous Diet for Fenrkin. Applies immediately and preserves live power resource/cooldown state.")
        var carnivorousDiet: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class GorgonSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Passive Features for Gorgon. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Ophidian Gaze for Gorgon. Applies immediately and preserves live power resource/cooldown state.")
        var ophidianGaze: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Transference for Gorgon. Applies immediately and preserves live power resource/cooldown state.")
        var transference: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Iron Weakness for Gorgon. Applies immediately and preserves live power resource/cooldown state.")
        var ironWeakness: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Raw Diet for Gorgon. Applies immediately and preserves live power resource/cooldown state.")
        var rawDiet: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class HumanitySection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Passive Features for The Human. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Indomitable for The Human. Applies immediately and preserves live power resource/cooldown state.")
        var indomitable: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Mortal Resolve for The Human. Applies immediately and preserves live power resource/cooldown state.")
        var mortalResolve: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class LichlingSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Passive Features for Lichling. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Death's Defiance for Lichling. Applies immediately and preserves live power resource/cooldown state.")
        var deathsDefiance: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Chimes of Necros for Lichling. Applies immediately and preserves live power resource/cooldown state.")
        var chimesOfNecros: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Iron Weakness for Lichling. Applies immediately and preserves live power resource/cooldown state.")
        var ironWeakness: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Raw Diet for Lichling. Applies immediately and preserves live power resource/cooldown state.")
        var rawDiet: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ManticoreSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Passive Features for Manticore. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Ravenous Lunge for Manticore. Applies immediately and preserves live power resource/cooldown state.")
        var ravenousLunge: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Beast of Blood for Manticore. Applies immediately and preserves live power resource/cooldown state.")
        var beastOfBlood: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Carnivorous Diet for Manticore. Applies immediately and preserves live power resource/cooldown state.")
        var carnivorousDiet: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class NereidSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Passive Features for Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Aquatic Traits for Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var aquaticTraits: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Gills for Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var gills: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Convalescing Aura for Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var convalescingAura: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Submersion for Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var submersion: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Pescatarian Diet for Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var pescatarianDiet: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class SatyrSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Passive Features for Satyr. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Rush, Swift Leap & Satyr's Landing for Satyr. Applies immediately and preserves live power resource/cooldown state.")
        var rushSwiftLeapLanding: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Vigil Perception for Satyr. Applies immediately and preserves live power resource/cooldown state.")
        var vigilPerception: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Vegetarian Diet for Satyr. Applies immediately and preserves live power resource/cooldown state.")
        var vegetarianDiet: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class SelkieSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Passive Features for Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Aquatic Traits for Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var aquaticTraits: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Coastal Phalanx & Surging Tides for Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var coastalPhalanx: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Sealskin Bastion for Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var sealskinBastion: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Pescatarian Diet for Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var pescatarianDiet: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class SirenSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Passive Features for Siren. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Aquatic Traits for Siren. Applies immediately and preserves live power resource/cooldown state.")
        var aquaticTraits: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Infatuation Tempo for Siren. Applies immediately and preserves live power resource/cooldown state.")
        var infatuationTempo: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Shrieking Wail for Siren. Applies immediately and preserves live power resource/cooldown state.")
        var shriekingWail: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Iron Weakness for Siren. Applies immediately and preserves live power resource/cooldown state.")
        var ironWeakness: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Carnivorous Diet for Siren. Applies immediately and preserves live power resource/cooldown state.")
        var carnivorousDiet: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class VeilbornSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Passive Features for Veilborn. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Shimmer for Veilborn. Applies immediately and preserves live power resource/cooldown state.")
        var shimmer: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Veil Step & Veil Transposition for Veilborn. Applies immediately and preserves live power resource/cooldown state.")
        var curtainStepTransposition: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Auroral Mirage for Veilborn. Applies immediately and preserves live power resource/cooldown state.")
        var auroralMirage: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class WyverianSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Passive Features for Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Ember Flames for Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var emberFlames: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Gusts of Freedom for Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var gustsOfFreedom: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Flight Hover for Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var flightHover: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Carnivorous Diet for Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var carnivorousDiet: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class StandardOriginsSection : me.fzzyhmstrs.fzzy_config.config.ConfigSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Arachne availability and gameplay powers.")
        var arachne: ArachneSection = ArachneSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Faerie availability and gameplay powers.")
        var faerie: FaerieSection = FaerieSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Fenrkin availability and gameplay powers.")
        var fenrkin: FenrkinSection = FenrkinSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Gorgon availability and gameplay powers.")
        var gorgon: GorgonSection = GorgonSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("The Human availability and gameplay powers.")
        var humanity: HumanitySection = HumanitySection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Lichling availability and gameplay powers.")
        var lichling: LichlingSection = LichlingSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Manticore availability and gameplay powers.")
        var manticore: ManticoreSection = ManticoreSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Nereid availability and gameplay powers.")
        var nereid: NereidSection = NereidSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Satyr availability and gameplay powers.")
        var satyr: SatyrSection = SatyrSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Selkie availability and gameplay powers.")
        var selkie: SelkieSection = SelkieSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Siren availability and gameplay powers.")
        var siren: SirenSection = SirenSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Veilborn availability and gameplay powers.")
        var veilborn: VeilbornSection = VeilbornSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Wyverian availability and gameplay powers.")
        var wyverian: WyverianSection = WyverianSection()
    }

    class ChampionArachneSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Champion Passive Features for Champion Arachne. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Eightfold Swiftness for Champion Arachne. Applies immediately and preserves live power resource/cooldown state.")
        var eightfoldSwiftness: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Weaver's Nest for Champion Arachne. Applies immediately and preserves live power resource/cooldown state.")
        var weaversNest: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionFaerieSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Champion Passive Features for Champion Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Flutter for Champion Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var flutter: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Fae Illusions for Champion Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var faeIllusions: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Trickster for Champion Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var trickster: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionFenrkinSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Champion Passive Features for Champion Fenrkin. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable On the Hunt for Champion Fenrkin. Applies immediately and preserves live power resource/cooldown state.")
        var onTheHunt: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Stamina Surge for Champion Fenrkin. Applies immediately and preserves live power resource/cooldown state.")
        var staminaSurge: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionGorgonSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Champion Passive Features for Champion Gorgon. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Ophidian Gaze for Champion Gorgon. Applies immediately and preserves live power resource/cooldown state.")
        var ophidianGaze: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Transference for Champion Gorgon. Applies immediately and preserves live power resource/cooldown state.")
        var transference: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionHumanitySection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Champion Passive Features for Champion Human. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Indomitable for Champion Human. Applies immediately and preserves live power resource/cooldown state.")
        var indomitable: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Mortal Triumph for Champion Human. Applies immediately and preserves live power resource/cooldown state.")
        var mortalTriumph: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionLichlingSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Champion Passive Features for Champion Lichling. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Death's Defiance for Champion Lichling. Applies immediately and preserves live power resource/cooldown state.")
        var deathsDefiance: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Chimes of Necros for Champion Lichling. Applies immediately and preserves live power resource/cooldown state.")
        var chimesOfNecros: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionManticoreSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Champion Passive Features for Champion Manticore. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Ravenous Lunge for Champion Manticore. Applies immediately and preserves live power resource/cooldown state.")
        var ravenousLunge: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Beast of Blood for Champion Manticore. Applies immediately and preserves live power resource/cooldown state.")
        var beastOfBlood: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionNereidSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Champion Passive Features for Champion Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Aquatic Traits for Champion Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var aquaticTraits: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Convalescing Aura for Champion Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var convalescingAura: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Submersion for Champion Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var submersion: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionSatyrSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Champion Passive Features for Champion Satyr. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Rush, Swift Leap & Satyr's Landing for Champion Satyr. Applies immediately and preserves live power resource/cooldown state.")
        var rushSwiftLeapLanding: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Vigil Perception for Champion Satyr. Applies immediately and preserves live power resource/cooldown state.")
        var vigilPerception: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionSelkieSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Champion Passive Features for Champion Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Aquatic Traits for Champion Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var aquaticTraits: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Coastal Phalanx & Surging Tides for Champion Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var coastalPhalanx: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Sealskin Bastion for Champion Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var sealskinBastion: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionSirenSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Champion Passive Features for Champion Siren. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Aquatic Traits for Champion Siren. Applies immediately and preserves live power resource/cooldown state.")
        var aquaticTraits: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Infatuation Tempo for Champion Siren. Applies immediately and preserves live power resource/cooldown state.")
        var infatuationTempo: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Shrieking Wail for Champion Siren. Applies immediately and preserves live power resource/cooldown state.")
        var shriekingWail: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionVeilbornSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Champion Passive Features for Champion Veilborn. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Shimmer for Champion Veilborn. Applies immediately and preserves live power resource/cooldown state.")
        var shimmer: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Veil Step & Veil Transposition for Champion Veilborn. Applies immediately and preserves live power resource/cooldown state.")
        var curtainStepTransposition: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Auroral Mirage for Champion Veilborn. Applies immediately and preserves live power resource/cooldown state.")
        var auroralMirage: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionWyverianSection : OriginToggleSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Champion Passive Features for Champion Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Dragon's Breath for Champion Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var dragonsBreath: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Gusts of Freedom for Champion Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var gustsOfFreedom: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Enable or disable Flight Hover for Champion Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var flightHover: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionOriginsSection : me.fzzyhmstrs.fzzy_config.config.ConfigSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Champion Arachne availability and gameplay powers.")
        var champion_arachne: ChampionArachneSection = ChampionArachneSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Champion Faerie availability and gameplay powers.")
        var champion_faerie: ChampionFaerieSection = ChampionFaerieSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Champion Fenrkin availability and gameplay powers.")
        var champion_fenrkin: ChampionFenrkinSection = ChampionFenrkinSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Champion Gorgon availability and gameplay powers.")
        var champion_gorgon: ChampionGorgonSection = ChampionGorgonSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Champion Human availability and gameplay powers.")
        var champion_humanity: ChampionHumanitySection = ChampionHumanitySection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Champion Lichling availability and gameplay powers.")
        var champion_lichling: ChampionLichlingSection = ChampionLichlingSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Champion Manticore availability and gameplay powers.")
        var champion_manticore: ChampionManticoreSection = ChampionManticoreSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Champion Nereid availability and gameplay powers.")
        var champion_nereid: ChampionNereidSection = ChampionNereidSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Champion Satyr availability and gameplay powers.")
        var champion_satyr: ChampionSatyrSection = ChampionSatyrSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Champion Selkie availability and gameplay powers.")
        var champion_selkie: ChampionSelkieSection = ChampionSelkieSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Champion Siren availability and gameplay powers.")
        var champion_siren: ChampionSirenSection = ChampionSirenSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Champion Veilborn availability and gameplay powers.")
        var champion_veilborn: ChampionVeilbornSection = ChampionVeilbornSection()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Champion Wyverian availability and gameplay powers.")
        var champion_wyverian: ChampionWyverianSection = ChampionWyverianSection()
    }

    class OvergrownOriginsSection : me.fzzyhmstrs.fzzy_config.config.ConfigSection() {
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Master switch for Overgrown's built-in Origin roster. OFF by default. Turn this on before individual stock Origin switches can make those Origins selectable.")
        var enableStockOrigins: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean(false)

        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Allow the built-in Overgrown Arachnid Origin when the master switch is enabled.")
        var arachnid: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Allow the built-in Overgrown Avian Origin when the master switch is enabled.")
        var avian: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Allow the built-in Overgrown Blazeborn Origin when the master switch is enabled.")
        var blazeborn: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Allow the built-in Overgrown Buzzborne Origin when the master switch is enabled.")
        var buzzborne: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Allow the built-in Overgrown Elytrian Origin when the master switch is enabled.")
        var elytrian: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Allow the built-in Overgrown Enderian Origin when the master switch is enabled.")
        var enderian: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Allow the built-in Overgrown Feline Origin when the master switch is enabled.")
        var feline: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Allow the built-in Overgrown Human Origin when the master switch is enabled.")
        var human: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Allow the built-in Overgrown Merling Origin when the master switch is enabled.")
        var merling: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Allow the built-in Overgrown Phantom Origin when the master switch is enabled.")
        var phantom: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Allow the built-in Overgrown Shulk Origin when the master switch is enabled.")
        var shulk: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @me.fzzyhmstrs.fzzy_config.annotations.Comment("Allow the built-in Overgrown Slimekin Origin when the master switch is enabled.")
        var slimekin: me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }
    companion object {
        const val SCREEN_SCOPE: kotlin.String = "andromeda_origins.gameplay"

        private fun immutablePowerMap(
        values: kotlin.collections.MutableMap<net.minecraft.resources.ResourceLocation, kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>>): kotlin.collections.MutableMap<net.minecraft.resources.ResourceLocation, kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>> {
            val copy: kotlin.collections.MutableMap<net.minecraft.resources.ResourceLocation, kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>> = LinkedHashMap<net.minecraft.resources.ResourceLocation, kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>>()
            values.forEach { (origin: net.minecraft.resources.ResourceLocation, powers: kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>) -> copy.put(origin, java.util.Set.copyOf<net.minecraft.resources.ResourceLocation>(powers))}
            return java.util.Map.copyOf<net.minecraft.resources.ResourceLocation, kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>>(copy)
        }

        private fun enabled(): me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean {
            return me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean(true)
        }

    }}
