package andromeda.origins.config

import andromeda.origins.AndromedaOrigins
import andromeda.origins.compat.DawnbreakerPresentation
import andromeda.origins.compat.OriginFxStyle
import andromeda.origins.config.AndromedaConfigApplier
import andromeda.origins.config.AndromedaConfigRuntime
import andromeda.origins.config.AndromedaConfigState
import andromeda.origins.config.OriginConfigCatalog
import andromeda.origins.config.OriginConfigCatalog.OriginEntry
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Map as JavaMap
import java.util.Set as JavaSet
import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.annotations.RootConfig
import me.fzzyhmstrs.fzzy_config.annotations.Version
import me.fzzyhmstrs.fzzy_config.api.SaveType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.event.api.ServerUpdateContext
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum
import net.minecraft.resources.ResourceLocation


 /**
 * Fzzy Config-backed, server-authoritative gameplay configuration.
 *
 * Fzzy Config owns persistence, GUI generation, validation, permissions, Mod Menu integration,
 * and server/client synchronization. Andromeda only converts the validated settings into the
 * immutable runtime policy used by Origin filtering and Opoli power suppression.
 */
@RootConfig @Version(version = 1)
class AndromedaGameplayConfig : Config(ResourceLocation.fromNamespaceAndPath(AndromedaOrigins.MOD_ID, "gameplay")) {
    @Comment("Standard Andromeda Origins. Open an Origin to configure its availability, powers, diets, and weaknesses. Changes apply immediately; no restart is normally required.")
    var standardOrigins: StandardOriginsSection = StandardOriginsSection()
    @Comment("Shared presentation for every standard/Champion Origin. Cinematic adds smoother persistence, not larger particle clouds. Gameplay is unaffected.")
    var visuals = SharedVisualsSection()
    class SharedVisualsSection : ConfigSection() {
        var quality = ValidatedEnum(OriginFxStyle.Quality.STANDARD)
        var refinedContours = ValidatedBoolean(true)
        var reducedFlashes = ValidatedBoolean(true)
    }

    @Comment("Champion variants. Champion-only infrastructure such as cooldown acceleration is intentionally not exposed as a toggle.")
    var championOrigins: ChampionOriginsSection = ChampionOriginsSection()

    @Comment("Overgrown's built-in Origins. The master switch defaults OFF. Individual switches are remembered and take effect when the master switch is enabled.")
    var overgrownOrigins: OvergrownOriginsSection = OvergrownOriginsSection()

    @Comment("Prevent golden apples, enchanted golden apples, and golden carrots from resetting Incapacitated down counters or reviving players. Normal food effects, modded foods, and Origin revival abilities remain available.")
    var blockIncapacitatedFoodBypass = ValidatedBoolean(true)

    @Comment("Food balance and optional Let's Do compatibility. Settings apply immediately and are synchronized to clients.")
    var foodBalance = FoodBalanceSection()

    @Comment("Fabric server only: log bounded entity-unload mutation and nested-caller stacks for logout-crash investigation. Applies immediately; no startup argument or restart required. Diagnostic only, not a crash fix.")
    var logoutDiagnostics = ValidatedBoolean(false)

    @Comment("Optional Simply Swords and Simply More weapon implicit balance. Server-authoritative; changes apply to new hits immediately.")
    var weaponBalance = WeaponBalanceSection()

    override fun defaultPermLevel(): Int {
        return 2
    }

    override fun saveType(): SaveType {
        // A remote server's synced gameplay settings must not overwrite the player's local/single-player file.
        return SaveType.SEPARATE
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

    override fun onUpdateServer(context: ServerUpdateContext) {
        AndromedaConfigRuntime.set(toState())
        AndromedaConfigApplier.applyToServer(context.getServer())
    }

    fun toState(): AndromedaConfigState {
        val disabledOrigins: MutableSet<ResourceLocation> = LinkedHashSet<ResourceLocation>()
        val disabledStock: MutableSet<ResourceLocation> = LinkedHashSet<ResourceLocation>()
        val disabledByOrigin: MutableMap<ResourceLocation, MutableSet<ResourceLocation>> = LinkedHashMap<ResourceLocation, MutableSet<ResourceLocation>>()
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
        JavaSet.copyOf<ResourceLocation>(disabledStock),
        JavaSet.copyOf<ResourceLocation>(disabledOrigins),
        AndromedaGameplayConfig.Companion.immutablePowerMap(disabledByOrigin),
        blockIncapacitatedFoodBypass.get(),
        foodBalance.toState(),
        logoutDiagnostics.get(),
        weaponBalance.toState()
        )
    }

    private fun <S : OriginToggleSection>collectOriginEntry(
    disabledOrigins: MutableSet<ResourceLocation>,
    disabledByOrigin: MutableMap<ResourceLocation, MutableSet<ResourceLocation>>,
    entry: OriginEntry<S>
    ) {
        val section: S = entry.section.apply(this)
        if (!section.enabled.get()) disabledOrigins.add(entry.id)

        val disabled: MutableSet<ResourceLocation> = LinkedHashSet<ResourceLocation>()
        for (toggle in entry.toggles) {
            if (!toggle.getter.apply(section).get()) disabled.add(toggle.power)
        }
        if (!disabled.isEmpty()) disabledByOrigin.put(entry.id, JavaSet.copyOf<ResourceLocation>(disabled))
    }

    abstract class OriginToggleSection : ConfigSection() {
        @Comment("Controls whether this Origin can be selected. If disabled while selected, the player is returned to Origin selection.")
        var enabled: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }
    class ArachneSection : OriginToggleSection() {
        @Comment("Enable or disable Passive Features for Arachne. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Eightfold Swiftness for Arachne. Applies immediately and preserves live power resource/cooldown state.")
        var eightfoldSwiftness: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Weaver's Nest for Arachne. Applies immediately and preserves live power resource/cooldown state.")
        var weaversNest: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Carnivorous Diet for Arachne. Applies immediately and preserves live power resource/cooldown state.")
        var carnivorousDiet: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class FaerieSection : OriginToggleSection() {
        @Comment("Enable or disable Passive Features for Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Flutter for Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var flutter: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Fae Illusions for Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var faeIllusions: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Trickster for Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var trickster: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Iron Weakness for Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var ironWeakness: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class FenrkinSection : OriginToggleSection() {
        @Comment("Enable or disable Passive Features for Fenrkin. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable On the Hunt for Fenrkin. Applies immediately and preserves live power resource/cooldown state.")
        var onTheHunt: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Stamina Surge for Fenrkin. Applies immediately and preserves live power resource/cooldown state.")
        var staminaSurge: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Carnivorous Diet for Fenrkin. Applies immediately and preserves live power resource/cooldown state.")
        var carnivorousDiet: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class GorgonSection : OriginToggleSection() {
        @Comment("Enable or disable Passive Features for Gorgon. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Ophidian Gaze for Gorgon. Applies immediately and preserves live power resource/cooldown state.")
        var ophidianGaze: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Transference for Gorgon. Applies immediately and preserves live power resource/cooldown state.")
        var transference: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Iron Weakness for Gorgon. Applies immediately and preserves live power resource/cooldown state.")
        var ironWeakness: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Raw Diet for Gorgon. Applies immediately and preserves live power resource/cooldown state.")
        var rawDiet: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class HumanitySection : OriginToggleSection() {
        @Comment("Enable or disable Passive Features for The Human. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Indomitable for The Human. Applies immediately and preserves live power resource/cooldown state.")
        var indomitable: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Mortal Resolve for The Human. Applies immediately and preserves live power resource/cooldown state.")
        var mortalResolve: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class LichlingSection : OriginToggleSection() {
        @Comment("Enable or disable Passive Features for Lichling. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Death's Defiance for Lichling. Applies immediately and preserves live power resource/cooldown state.")
        var deathsDefiance: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Chimes of Necros for Lichling. Applies immediately and preserves live power resource/cooldown state.")
        var chimesOfNecros: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Iron Weakness for Lichling. Applies immediately and preserves live power resource/cooldown state.")
        var ironWeakness: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Raw Diet for Lichling. Applies immediately and preserves live power resource/cooldown state.")
        var rawDiet: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ManticoreSection : OriginToggleSection() {
        @Comment("Enable or disable Passive Features for Manticore. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Ravenous Lunge for Manticore. Applies immediately and preserves live power resource/cooldown state.")
        var ravenousLunge: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Beast of Blood for Manticore. Applies immediately and preserves live power resource/cooldown state.")
        var beastOfBlood: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Carnivorous Diet for Manticore. Applies immediately and preserves live power resource/cooldown state.")
        var carnivorousDiet: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class NereidSection : OriginToggleSection() {
        @Comment("Enable or disable Passive Features for Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Aquatic Traits for Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var aquaticTraits: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Gills for Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var gills: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Convalescing Aura for Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var convalescingAura: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Submersion for Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var submersion: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Pescatarian Diet for Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var pescatarianDiet: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class SatyrSection : OriginToggleSection() {
        @Comment("Enable or disable Passive Features for Satyr. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Rush, Swift Leap & Satyr's Landing for Satyr. Applies immediately and preserves live power resource/cooldown state.")
        var rushSwiftLeapLanding: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Vigil Perception for Satyr. Applies immediately and preserves live power resource/cooldown state.")
        var vigilPerception: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Vegetarian Diet for Satyr. Applies immediately and preserves live power resource/cooldown state.")
        var vegetarianDiet: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class SelkieSection : OriginToggleSection() {
        @Comment("Enable or disable Passive Features for Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Aquatic Traits for Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var aquaticTraits: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Coastal Phalanx & Surging Tides for Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var coastalPhalanx: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Sealskin Bastion for Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var sealskinBastion: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Pescatarian Diet for Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var pescatarianDiet: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class SirenSection : OriginToggleSection() {
        @Comment("Enable or disable Passive Features for Siren. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Aquatic Traits for Siren. Applies immediately and preserves live power resource/cooldown state.")
        var aquaticTraits: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Infatuation Tempo for Siren. Applies immediately and preserves live power resource/cooldown state.")
        var infatuationTempo: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Shrieking Wail for Siren. Applies immediately and preserves live power resource/cooldown state.")
        var shriekingWail: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Iron Weakness for Siren. Applies immediately and preserves live power resource/cooldown state.")
        var ironWeakness: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Carnivorous Diet for Siren. Applies immediately and preserves live power resource/cooldown state.")
        var carnivorousDiet: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class VeilbornSection : OriginToggleSection() {
        @Comment("Enable or disable Passive Features for Veilborn. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Shimmer for Veilborn. Applies immediately and preserves live power resource/cooldown state.")
        var shimmer: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Veil Step & Veil Transposition for Veilborn. Applies immediately and preserves live power resource/cooldown state.")
        var curtainStepTransposition: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Auroral Mirage for Veilborn. Applies immediately and preserves live power resource/cooldown state.")
        var auroralMirage: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class WyverianSection : OriginToggleSection() {
        @Comment("Enable or disable Passive Features for Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Ember Flames for Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var emberFlames: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Gusts of Freedom for Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var gustsOfFreedom: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Flight Hover for Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var flightHover: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Carnivorous Diet for Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var carnivorousDiet: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class StandardOriginsSection : ConfigSection() {
        @Comment("Arachne availability and gameplay powers.")
        var arachne: ArachneSection = ArachneSection()
        @Comment("Faerie availability and gameplay powers.")
        var faerie: FaerieSection = FaerieSection()
        @Comment("Fenrkin availability and gameplay powers.")
        var fenrkin: FenrkinSection = FenrkinSection()
        @Comment("Gorgon availability and gameplay powers.")
        var gorgon: GorgonSection = GorgonSection()
        @Comment("The Human availability and gameplay powers.")
        var humanity: HumanitySection = HumanitySection()
        @Comment("Lichling availability and gameplay powers.")
        var lichling: LichlingSection = LichlingSection()
        @Comment("Manticore availability and gameplay powers.")
        var manticore: ManticoreSection = ManticoreSection()
        @Comment("Nereid availability and gameplay powers.")
        var nereid: NereidSection = NereidSection()
        @Comment("Satyr availability and gameplay powers.")
        var satyr: SatyrSection = SatyrSection()
        @Comment("Selkie availability and gameplay powers.")
        var selkie: SelkieSection = SelkieSection()
        @Comment("Siren availability and gameplay powers.")
        var siren: SirenSection = SirenSection()
        @Comment("Veilborn availability and gameplay powers.")
        var veilborn: VeilbornSection = VeilbornSection()
        @Comment("Wyverian availability and gameplay powers.")
        var wyverian: WyverianSection = WyverianSection()
    }

    class ChampionArachneSection : OriginToggleSection() {
        @Comment("Enable or disable Champion Passive Features for Champion Arachne. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Eightfold Swiftness for Champion Arachne. Applies immediately and preserves live power resource/cooldown state.")
        var eightfoldSwiftness: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Weaver's Nest for Champion Arachne. Applies immediately and preserves live power resource/cooldown state.")
        var weaversNest: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionFaerieSection : OriginToggleSection() {
        @Comment("Enable or disable Champion Passive Features for Champion Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Flutter for Champion Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var flutter: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Fae Illusions for Champion Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var faeIllusions: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Trickster for Champion Faerie. Applies immediately and preserves live power resource/cooldown state.")
        var trickster: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionFenrkinSection : OriginToggleSection() {
        @Comment("Enable or disable Champion Passive Features for Champion Fenrkin. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable On the Hunt for Champion Fenrkin. Applies immediately and preserves live power resource/cooldown state.")
        var onTheHunt: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Stamina Surge for Champion Fenrkin. Applies immediately and preserves live power resource/cooldown state.")
        var staminaSurge: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionGorgonSection : OriginToggleSection() {
        @Comment("Enable or disable Champion Passive Features for Champion Gorgon. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Ophidian Gaze for Champion Gorgon. Applies immediately and preserves live power resource/cooldown state.")
        var ophidianGaze: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Transference for Champion Gorgon. Applies immediately and preserves live power resource/cooldown state.")
        var transference: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class DawnbreakerSection : OriginToggleSection() {
        var presentationPreset = ValidatedEnum(DawnbreakerPresentation.Preset.STANDARD)
        var animatedWings = AndromedaGameplayConfig.Companion.enabled()
        var afterimagesCompat = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Allow gold/black impact frames. Each client must also opt in under Dawnbreaker Visuals; never pauses gameplay.")
        var impactFrames = ValidatedBoolean(false)
        var stepImpactFrames = AndromedaGameplayConfig.Companion.enabled()
        var guardImpactFrames = AndromedaGameplayConfig.Companion.enabled()
        var bowImpactFrames = AndromedaGameplayConfig.Companion.enabled()
        var guardHud = AndromedaGameplayConfig.Companion.enabled()
        var successMessages = ValidatedBoolean(false)
        @Comment("Enable Measured Step: a cooldown-free blink up to 16 blocks to safe visible solid ground.")
        var measuredStep: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable Kneel: a three-second grounding command within 12 blocks, with no cooldown.")
        var kneel: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable Stillwater Guard: hold up to eight incoming vanilla arrows, snowballs or eggs for at most eight seconds; release to redirect.")
        var stillwaterGuard: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Animate Dawnbreaker stone appearance rising over two seconds. Disabling keeps immediate stone tint; Stone Appearance controls the entire stone visual.")
        var gradualPetrification: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        var seraphsDraw: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        var sovereignGaze: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        var transference: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        var presence: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        var begone: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        var radiantEffects: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        var stoneAppearance: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionHumanitySection : OriginToggleSection() {
        @Comment("Enable or disable Champion Passive Features for Champion Human. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Indomitable for Champion Human. Applies immediately and preserves live power resource/cooldown state.")
        var indomitable: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Mortal Triumph for Champion Human. Applies immediately and preserves live power resource/cooldown state.")
        var mortalTriumph: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionLichlingSection : OriginToggleSection() {
        @Comment("Enable or disable Champion Passive Features for Champion Lichling. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Death's Defiance for Champion Lichling. Applies immediately and preserves live power resource/cooldown state.")
        var deathsDefiance: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Chimes of Necros for Champion Lichling. Applies immediately and preserves live power resource/cooldown state.")
        var chimesOfNecros: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionManticoreSection : OriginToggleSection() {
        @Comment("Enable or disable Champion Passive Features for Champion Manticore. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Ravenous Lunge for Champion Manticore. Applies immediately and preserves live power resource/cooldown state.")
        var ravenousLunge: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Beast of Blood for Champion Manticore. Applies immediately and preserves live power resource/cooldown state.")
        var beastOfBlood: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionNereidSection : OriginToggleSection() {
        @Comment("Enable or disable Champion Passive Features for Champion Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Aquatic Traits for Champion Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var aquaticTraits: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Convalescing Aura for Champion Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var convalescingAura: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Submersion for Champion Nereid. Applies immediately and preserves live power resource/cooldown state.")
        var submersion: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionSatyrSection : OriginToggleSection() {
        @Comment("Enable or disable Champion Passive Features for Champion Satyr. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Rush, Swift Leap & Satyr's Landing for Champion Satyr. Applies immediately and preserves live power resource/cooldown state.")
        var rushSwiftLeapLanding: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Vigil Perception for Champion Satyr. Applies immediately and preserves live power resource/cooldown state.")
        var vigilPerception: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionSelkieSection : OriginToggleSection() {
        @Comment("Enable or disable Champion Passive Features for Champion Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Aquatic Traits for Champion Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var aquaticTraits: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Coastal Phalanx & Surging Tides for Champion Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var coastalPhalanx: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Sealskin Bastion for Champion Selkie. Applies immediately and preserves live power resource/cooldown state.")
        var sealskinBastion: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionSirenSection : OriginToggleSection() {
        @Comment("Enable or disable Champion Passive Features for Champion Siren. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Aquatic Traits for Champion Siren. Applies immediately and preserves live power resource/cooldown state.")
        var aquaticTraits: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Infatuation Tempo for Champion Siren. Applies immediately and preserves live power resource/cooldown state.")
        var infatuationTempo: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Shrieking Wail for Champion Siren. Applies immediately and preserves live power resource/cooldown state.")
        var shriekingWail: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionVeilbornSection : OriginToggleSection() {
        @Comment("Enable or disable Champion Passive Features for Champion Veilborn. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Shimmer for Champion Veilborn. Applies immediately and preserves live power resource/cooldown state.")
        var shimmer: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Veil Step & Veil Transposition for Champion Veilborn. Applies immediately and preserves live power resource/cooldown state.")
        var curtainStepTransposition: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Auroral Mirage for Champion Veilborn. Applies immediately and preserves live power resource/cooldown state.")
        var auroralMirage: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionWyverianSection : OriginToggleSection() {
        @Comment("Enable or disable Champion Passive Features for Champion Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var passiveFeatures: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Dragon's Breath for Champion Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var dragonsBreath: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Gusts of Freedom for Champion Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var gustsOfFreedom: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Enable or disable Flight Hover for Champion Wyverian. Applies immediately and preserves live power resource/cooldown state.")
        var flightHover: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }

    class ChampionOriginsSection : ConfigSection() {
        @Comment("Champion Arachne availability and gameplay powers.")
        var champion_arachne: ChampionArachneSection = ChampionArachneSection()
        @Comment("Champion Faerie availability and gameplay powers.")
        var champion_faerie: ChampionFaerieSection = ChampionFaerieSection()
        @Comment("Champion Fenrkin availability and gameplay powers.")
        var champion_fenrkin: ChampionFenrkinSection = ChampionFenrkinSection()
        @Comment("Champion Gorgon availability and gameplay powers.")
        var champion_gorgon: ChampionGorgonSection = ChampionGorgonSection()
        var dawnbreaker: DawnbreakerSection = DawnbreakerSection()
        @Comment("Champion Human availability and gameplay powers.")
        var champion_humanity: ChampionHumanitySection = ChampionHumanitySection()
        @Comment("Champion Lichling availability and gameplay powers.")
        var champion_lichling: ChampionLichlingSection = ChampionLichlingSection()
        @Comment("Champion Manticore availability and gameplay powers.")
        var champion_manticore: ChampionManticoreSection = ChampionManticoreSection()
        @Comment("Champion Nereid availability and gameplay powers.")
        var champion_nereid: ChampionNereidSection = ChampionNereidSection()
        @Comment("Champion Satyr availability and gameplay powers.")
        var champion_satyr: ChampionSatyrSection = ChampionSatyrSection()
        @Comment("Champion Selkie availability and gameplay powers.")
        var champion_selkie: ChampionSelkieSection = ChampionSelkieSection()
        @Comment("Champion Siren availability and gameplay powers.")
        var champion_siren: ChampionSirenSection = ChampionSirenSection()
        @Comment("Champion Veilborn availability and gameplay powers.")
        var champion_veilborn: ChampionVeilbornSection = ChampionVeilbornSection()
        @Comment("Champion Wyverian availability and gameplay powers.")
        var champion_wyverian: ChampionWyverianSection = ChampionWyverianSection()
    }

    class OvergrownOriginsSection : ConfigSection() {
        @Comment("Master switch for Overgrown's built-in Origin roster. OFF by default. Turn this on before individual stock Origin switches can make those Origins selectable.")
        var enableStockOrigins: ValidatedBoolean = ValidatedBoolean(false)

        @Comment("Allow the built-in Overgrown Arachnid Origin when the master switch is enabled.")
        var arachnid: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Allow the built-in Overgrown Avian Origin when the master switch is enabled.")
        var avian: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Allow the built-in Overgrown Blazeborn Origin when the master switch is enabled.")
        var blazeborn: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Allow the built-in Overgrown Buzzborne Origin when the master switch is enabled.")
        var buzzborne: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Allow the built-in Overgrown Elytrian Origin when the master switch is enabled.")
        var elytrian: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Allow the built-in Overgrown Enderian Origin when the master switch is enabled.")
        var enderian: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Allow the built-in Overgrown Feline Origin when the master switch is enabled.")
        var feline: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Allow the built-in Overgrown Human Origin when the master switch is enabled.")
        var human: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Allow the built-in Overgrown Merling Origin when the master switch is enabled.")
        var merling: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Allow the built-in Overgrown Phantom Origin when the master switch is enabled.")
        var phantom: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Allow the built-in Overgrown Shulk Origin when the master switch is enabled.")
        var shulk: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
        @Comment("Allow the built-in Overgrown Slimekin Origin when the master switch is enabled.")
        var slimekin: ValidatedBoolean = AndromedaGameplayConfig.Companion.enabled()
    }
    companion object {
        const val SCREEN_SCOPE: String = "andromeda_origins.gameplay"

        private fun immutablePowerMap(
        values: MutableMap<ResourceLocation, MutableSet<ResourceLocation>>): MutableMap<ResourceLocation, MutableSet<ResourceLocation>> {
            val copy: MutableMap<ResourceLocation, MutableSet<ResourceLocation>> = LinkedHashMap<ResourceLocation, MutableSet<ResourceLocation>>()
            values.forEach { (origin: ResourceLocation, powers: MutableSet<ResourceLocation>) -> copy.put(origin, JavaSet.copyOf<ResourceLocation>(powers))}
            return JavaMap.copyOf<ResourceLocation, MutableSet<ResourceLocation>>(copy)
        }

        private fun enabled(): ValidatedBoolean {
            return ValidatedBoolean(true)
        }

    }}
