package andromeda.origins.config

import andromeda.origins.AndromedaOrigins
import andromeda.origins.config.AndromedaGameplayConfig
import andromeda.origins.config.AndromedaGameplayConfig.ArachneSection
import andromeda.origins.config.AndromedaGameplayConfig.ChampionArachneSection
import andromeda.origins.config.AndromedaGameplayConfig.ChampionFaerieSection
import andromeda.origins.config.AndromedaGameplayConfig.ChampionFenrkinSection
import andromeda.origins.config.AndromedaGameplayConfig.ChampionGorgonSection
import andromeda.origins.config.AndromedaGameplayConfig.ChampionHumanitySection
import andromeda.origins.config.AndromedaGameplayConfig.ChampionLichlingSection
import andromeda.origins.config.AndromedaGameplayConfig.ChampionManticoreSection
import andromeda.origins.config.AndromedaGameplayConfig.ChampionNereidSection
import andromeda.origins.config.AndromedaGameplayConfig.ChampionSatyrSection
import andromeda.origins.config.AndromedaGameplayConfig.ChampionSelkieSection
import andromeda.origins.config.AndromedaGameplayConfig.ChampionSirenSection
import andromeda.origins.config.AndromedaGameplayConfig.ChampionVeilbornSection
import andromeda.origins.config.AndromedaGameplayConfig.ChampionWyverianSection
import andromeda.origins.config.AndromedaGameplayConfig.DawnbreakerSection
import andromeda.origins.config.AndromedaGameplayConfig.FaerieSection
import andromeda.origins.config.AndromedaGameplayConfig.FenrkinSection
import andromeda.origins.config.AndromedaGameplayConfig.GorgonSection
import andromeda.origins.config.AndromedaGameplayConfig.HumanitySection
import andromeda.origins.config.AndromedaGameplayConfig.LichlingSection
import andromeda.origins.config.AndromedaGameplayConfig.ManticoreSection
import andromeda.origins.config.AndromedaGameplayConfig.NereidSection
import andromeda.origins.config.AndromedaGameplayConfig.OriginToggleSection
import andromeda.origins.config.AndromedaGameplayConfig.OvergrownOriginsSection
import andromeda.origins.config.AndromedaGameplayConfig.SatyrSection
import andromeda.origins.config.AndromedaGameplayConfig.SelkieSection
import andromeda.origins.config.AndromedaGameplayConfig.SirenSection
import andromeda.origins.config.AndromedaGameplayConfig.VeilbornSection
import andromeda.origins.config.AndromedaGameplayConfig.WyverianSection
import java.util.LinkedHashMap
import java.util.List as JavaList
import java.util.Map as JavaMap
import java.util.Set as JavaSet
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import net.minecraft.resources.ResourceLocation


 /**
 * Authoritative catalog for Andromeda Origin IDs and their player-facing Fzzy controls.
 *
 *
 * Each entry binds an Origin ID directly to its existing config section and toggle fields. This
 * keeps config paths stable while eliminating the old duplicate allowlist in runtime policy and the
 * giant hand-written conversion table in [AndromedaGameplayConfig.toState].
 */
object OriginConfigCatalog {
    val STANDARD: MutableList<OriginEntry<*>> = JavaList.of<OriginEntry<*>>(
    OriginConfigCatalog.entry<ArachneSection>("arachne", false, Function { c: AndromedaGameplayConfig -> c.standardOrigins.arachne},
    OriginConfigCatalog.toggle<ArachneSection>("arachne/passives", Function { s: ArachneSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ArachneSection>("arachne/primary", Function { s: ArachneSection -> s.eightfoldSwiftness}),
    OriginConfigCatalog.toggle<ArachneSection>("arachne/secondary", Function { s: ArachneSection -> s.weaversNest}), OriginConfigCatalog.toggle<ArachneSection>("common/diet_carnivore", Function { s: ArachneSection -> s.carnivorousDiet})),
    OriginConfigCatalog.entry<FaerieSection>("faerie", false, Function { c: AndromedaGameplayConfig -> c.standardOrigins.faerie},
    OriginConfigCatalog.toggle<FaerieSection>("faerie/passives", Function { s: FaerieSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<FaerieSection>("faerie/primary", Function { s: FaerieSection -> s.flutter}),
    OriginConfigCatalog.toggle<FaerieSection>("faerie/secondary", Function { s: FaerieSection -> s.faeIllusions}), OriginConfigCatalog.toggle<FaerieSection>("faerie/prankster", Function { s: FaerieSection -> s.trickster}),
    OriginConfigCatalog.toggle<FaerieSection>("common/ironweakness", Function { s: FaerieSection -> s.ironWeakness})),
    OriginConfigCatalog.entry<FenrkinSection>("fenrkin", false, Function { c: AndromedaGameplayConfig -> c.standardOrigins.fenrkin},
    OriginConfigCatalog.toggle<FenrkinSection>("fenrkin/passives", Function { s: FenrkinSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<FenrkinSection>("fenrkin/primary", Function { s: FenrkinSection -> s.onTheHunt}),
    OriginConfigCatalog.toggle<FenrkinSection>("fenrkin/secondary", Function { s: FenrkinSection -> s.staminaSurge}), OriginConfigCatalog.toggle<FenrkinSection>("common/diet_carnivore", Function { s: FenrkinSection -> s.carnivorousDiet})),
    OriginConfigCatalog.entry<GorgonSection>("gorgon", false, Function { c: AndromedaGameplayConfig -> c.standardOrigins.gorgon},
    OriginConfigCatalog.toggle<GorgonSection>("gorgon/passives", Function { s: GorgonSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<GorgonSection>("gorgon/primary", Function { s: GorgonSection -> s.ophidianGaze}),
    OriginConfigCatalog.toggle<GorgonSection>("gorgon/secondary", Function { s: GorgonSection -> s.transference}), OriginConfigCatalog.toggle<GorgonSection>("common/ironweakness", Function { s: GorgonSection -> s.ironWeakness}),
    OriginConfigCatalog.toggle<GorgonSection>("common/diet_raw", Function { s: GorgonSection -> s.rawDiet})),
    OriginConfigCatalog.entry<HumanitySection>("humanity", false, Function { c: AndromedaGameplayConfig -> c.standardOrigins.humanity},
    OriginConfigCatalog.toggle<HumanitySection>("humanity/passives", Function { s: HumanitySection -> s.passiveFeatures}), OriginConfigCatalog.toggle<HumanitySection>("humanity/primary", Function { s: HumanitySection -> s.indomitable}),
    OriginConfigCatalog.toggle<HumanitySection>("humanity/secondary", Function { s: HumanitySection -> s.mortalResolve})),
    OriginConfigCatalog.entry<LichlingSection>("lichling", false, Function { c: AndromedaGameplayConfig -> c.standardOrigins.lichling},
    OriginConfigCatalog.toggle<LichlingSection>("lichling/passives", Function { s: LichlingSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<LichlingSection>("lichling/primary", Function { s: LichlingSection -> s.deathsDefiance}),
    OriginConfigCatalog.toggle<LichlingSection>("lichling/secondary", Function { s: LichlingSection -> s.chimesOfNecros}), OriginConfigCatalog.toggle<LichlingSection>("common/ironweakness", Function { s: LichlingSection -> s.ironWeakness}),
    OriginConfigCatalog.toggle<LichlingSection>("common/diet_raw", Function { s: LichlingSection -> s.rawDiet})),
    OriginConfigCatalog.entry<ManticoreSection>("manticore", false, Function { c: AndromedaGameplayConfig -> c.standardOrigins.manticore},
    OriginConfigCatalog.toggle<ManticoreSection>("manticore/passives", Function { s: ManticoreSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ManticoreSection>("manticore/primary", Function { s: ManticoreSection -> s.ravenousLunge}),
    OriginConfigCatalog.toggle<ManticoreSection>("manticore/secondary", Function { s: ManticoreSection -> s.beastOfBlood}), OriginConfigCatalog.toggle<ManticoreSection>("common/diet_carnivore", Function { s: ManticoreSection -> s.carnivorousDiet})),
    OriginConfigCatalog.entry<NereidSection>("nereid", false, Function { c: AndromedaGameplayConfig -> c.standardOrigins.nereid},
    OriginConfigCatalog.toggle<NereidSection>("nereid/passives", Function { s: NereidSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<NereidSection>("nereid/aquatic_traits", Function { s: NereidSection -> s.aquaticTraits}),
    OriginConfigCatalog.toggle<NereidSection>("nereid/gills", Function { s: NereidSection -> s.gills}), OriginConfigCatalog.toggle<NereidSection>("nereid/primary", Function { s: NereidSection -> s.convalescingAura}),
    OriginConfigCatalog.toggle<NereidSection>("nereid/secondary", Function { s: NereidSection -> s.submersion}), OriginConfigCatalog.toggle<NereidSection>("common/diet_pescatarian", Function { s: NereidSection -> s.pescatarianDiet})),
    OriginConfigCatalog.entry<SatyrSection>("satyr", false, Function { c: AndromedaGameplayConfig -> c.standardOrigins.satyr},
    OriginConfigCatalog.toggle<SatyrSection>("satyr/passives", Function { s: SatyrSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<SatyrSection>("satyr/primary", Function { s: SatyrSection -> s.rushSwiftLeapLanding}),
    OriginConfigCatalog.toggle<SatyrSection>("satyr/secondary", Function { s: SatyrSection -> s.vigilPerception}), OriginConfigCatalog.toggle<SatyrSection>("common/diet_vegetarian", Function { s: SatyrSection -> s.vegetarianDiet})),
    OriginConfigCatalog.entry<SelkieSection>("selkie", false, Function { c: AndromedaGameplayConfig -> c.standardOrigins.selkie},
    OriginConfigCatalog.toggle<SelkieSection>("selkie/passives", Function { s: SelkieSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<SelkieSection>("selkie/aquatic_traits", Function { s: SelkieSection -> s.aquaticTraits}),
    OriginConfigCatalog.toggle<SelkieSection>("selkie/primary", Function { s: SelkieSection -> s.coastalPhalanx}), OriginConfigCatalog.toggle<SelkieSection>("selkie/secondary", Function { s: SelkieSection -> s.sealskinBastion}),
    OriginConfigCatalog.toggle<SelkieSection>("common/diet_pescatarian", Function { s: SelkieSection -> s.pescatarianDiet})),
    OriginConfigCatalog.entry<SirenSection>("siren", false, Function { c: AndromedaGameplayConfig -> c.standardOrigins.siren},
    OriginConfigCatalog.toggle<SirenSection>("siren/passives", Function { s: SirenSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<SirenSection>("siren/aquatic_traits", Function { s: SirenSection -> s.aquaticTraits}),
    OriginConfigCatalog.toggle<SirenSection>("siren/primary", Function { s: SirenSection -> s.infatuationTempo}), OriginConfigCatalog.toggle<SirenSection>("siren/secondary", Function { s: SirenSection -> s.shriekingWail}),
    OriginConfigCatalog.toggle<SirenSection>("common/ironweakness", Function { s: SirenSection -> s.ironWeakness}), OriginConfigCatalog.toggle<SirenSection>("siren/diet_carnivore", Function { s: SirenSection -> s.carnivorousDiet})),
    OriginConfigCatalog.entry<VeilbornSection>("veilborn", false, Function { c: AndromedaGameplayConfig -> c.standardOrigins.veilborn},
    OriginConfigCatalog.toggle<VeilbornSection>("veilborn/passives", Function { s: VeilbornSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<VeilbornSection>("veilborn/shimmer", Function { s: VeilbornSection -> s.shimmer}),
    OriginConfigCatalog.toggle<VeilbornSection>("veilborn/primary", Function { s: VeilbornSection -> s.curtainStepTransposition}), OriginConfigCatalog.toggle<VeilbornSection>("veilborn/secondary", Function { s: VeilbornSection -> s.auroralMirage})),
    OriginConfigCatalog.entry<WyverianSection>("wyverian", false, Function { c: AndromedaGameplayConfig -> c.standardOrigins.wyverian},
    OriginConfigCatalog.toggle<WyverianSection>("wyverian/passives", Function { s: WyverianSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<WyverianSection>("wyverian/primary", Function { s: WyverianSection -> s.emberFlames}),
    OriginConfigCatalog.toggle<WyverianSection>("wyverian/secondary", Function { s: WyverianSection -> s.gustsOfFreedom}), OriginConfigCatalog.toggle<WyverianSection>("wyverian/sneakair", Function { s: WyverianSection -> s.flightHover}),
    OriginConfigCatalog.toggle<WyverianSection>("common/diet_carnivore", Function { s: WyverianSection -> s.carnivorousDiet}))
    )

    val CHAMPION: MutableList<OriginEntry<*>> = JavaList.of<OriginEntry<*>>(
    OriginConfigCatalog.entry<ChampionArachneSection>("champion_arachne", true, Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_arachne},
    OriginConfigCatalog.toggle<ChampionArachneSection>("champion/arachne_passives", Function { s: ChampionArachneSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionArachneSection>("arachne/primary", Function { s: ChampionArachneSection -> s.eightfoldSwiftness}),
    OriginConfigCatalog.toggle<ChampionArachneSection>("arachne/secondary", Function { s: ChampionArachneSection -> s.weaversNest})),
    OriginConfigCatalog.entry<ChampionFaerieSection>("champion_faerie", true, Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_faerie},
    OriginConfigCatalog.toggle<ChampionFaerieSection>("champion/faerie_passives", Function { s: ChampionFaerieSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionFaerieSection>("faerie/primary", Function { s: ChampionFaerieSection -> s.flutter}),
    OriginConfigCatalog.toggle<ChampionFaerieSection>("faerie/secondary", Function { s: ChampionFaerieSection -> s.faeIllusions}), OriginConfigCatalog.toggle<ChampionFaerieSection>("faerie/prankster", Function { s: ChampionFaerieSection -> s.trickster})),
    OriginConfigCatalog.entry<ChampionFenrkinSection>("champion_fenrkin", true, Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_fenrkin},
    OriginConfigCatalog.toggle<ChampionFenrkinSection>("champion/fenrkin_passives", Function { s: ChampionFenrkinSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionFenrkinSection>("champion/fenrkin_primary", Function { s: ChampionFenrkinSection -> s.onTheHunt}),
    OriginConfigCatalog.toggle<ChampionFenrkinSection>("champion/fenrkin_secondary", Function { s: ChampionFenrkinSection -> s.staminaSurge})),
    OriginConfigCatalog.entry<ChampionGorgonSection>("champion_gorgon", true, Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_gorgon},
    OriginConfigCatalog.toggle<ChampionGorgonSection>("champion/gorgon_passives", Function { s: ChampionGorgonSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionGorgonSection>("gorgon/primary", Function { s: ChampionGorgonSection -> s.ophidianGaze}),
    OriginConfigCatalog.toggle<ChampionGorgonSection>("gorgon/secondary", Function { s: ChampionGorgonSection -> s.transference})),
    OriginConfigCatalog.entry<DawnbreakerSection>("dawnbreaker", true, Function { c: AndromedaGameplayConfig -> c.championOrigins.dawnbreaker},
    OriginConfigCatalog.toggle<DawnbreakerSection>("champion/gorgon_passives", Function { s: DawnbreakerSection -> s.passiveFeatures}),
    OriginConfigCatalog.toggle<DawnbreakerSection>("dawnbreaker/measured_step", Function { s: DawnbreakerSection -> s.measuredStep}),
    OriginConfigCatalog.toggle<DawnbreakerSection>("dawnbreaker/kneel", Function { s: DawnbreakerSection -> s.kneel}),
    OriginConfigCatalog.toggle<DawnbreakerSection>("dawnbreaker/stillwater", Function { s: DawnbreakerSection -> s.stillwaterGuard}),
    OriginConfigCatalog.toggle<DawnbreakerSection>("dawnbreaker/gradual_petrification", Function { s: DawnbreakerSection -> s.gradualPetrification}),
    OriginConfigCatalog.toggle<DawnbreakerSection>("dawnbreaker/bow", Function { s: DawnbreakerSection -> s.seraphsDraw}),
    OriginConfigCatalog.toggle<DawnbreakerSection>("dawnbreaker/primary", Function { s: DawnbreakerSection -> s.sovereignGaze}),
    OriginConfigCatalog.toggle<DawnbreakerSection>("dawnbreaker/secondary", Function { s: DawnbreakerSection -> s.transference}),
    OriginConfigCatalog.toggle<DawnbreakerSection>("dawnbreaker/presence", Function { s: DawnbreakerSection -> s.presence}),
    OriginConfigCatalog.toggle<DawnbreakerSection>("dawnbreaker/begone", Function { s: DawnbreakerSection -> s.begone}),
    OriginConfigCatalog.toggle<DawnbreakerSection>("dawnbreaker/radiance", Function { s: DawnbreakerSection -> s.radiantEffects}),
    OriginConfigCatalog.toggle<DawnbreakerSection>("dawnbreaker/stone_appearance", Function { s: DawnbreakerSection -> s.stoneAppearance})),
    OriginConfigCatalog.entry<ChampionHumanitySection>("champion_humanity", true, Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_humanity},
    OriginConfigCatalog.toggle<ChampionHumanitySection>("champion/humanity_passives", Function { s: ChampionHumanitySection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionHumanitySection>("humanity/primary", Function { s: ChampionHumanitySection -> s.indomitable}),
    OriginConfigCatalog.toggle<ChampionHumanitySection>("champion/humanity_secondary", Function { s: ChampionHumanitySection -> s.mortalTriumph})),
    OriginConfigCatalog.entry<ChampionLichlingSection>("champion_lichling", true, Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_lichling},
    OriginConfigCatalog.toggle<ChampionLichlingSection>("champion/lichling_passives", Function { s: ChampionLichlingSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionLichlingSection>("lichling/primary", Function { s: ChampionLichlingSection -> s.deathsDefiance}),
    OriginConfigCatalog.toggle<ChampionLichlingSection>("lichling/secondary", Function { s: ChampionLichlingSection -> s.chimesOfNecros})),
    OriginConfigCatalog.entry<ChampionManticoreSection>("champion_manticore", true, Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_manticore},
    OriginConfigCatalog.toggle<ChampionManticoreSection>("champion/manticore_passives", Function { s: ChampionManticoreSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionManticoreSection>("manticore/primary", Function { s: ChampionManticoreSection -> s.ravenousLunge}),
    OriginConfigCatalog.toggle<ChampionManticoreSection>("manticore/secondary", Function { s: ChampionManticoreSection -> s.beastOfBlood})),
    OriginConfigCatalog.entry<ChampionNereidSection>("champion_nereid", true, Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_nereid},
    OriginConfigCatalog.toggle<ChampionNereidSection>("champion/nereid_passives", Function { s: ChampionNereidSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionNereidSection>("nereid/aquatic_traits", Function { s: ChampionNereidSection -> s.aquaticTraits}),
    OriginConfigCatalog.toggle<ChampionNereidSection>("nereid/primary", Function { s: ChampionNereidSection -> s.convalescingAura}), OriginConfigCatalog.toggle<ChampionNereidSection>("nereid/secondary", Function { s: ChampionNereidSection -> s.submersion})),
    OriginConfigCatalog.entry<ChampionSatyrSection>("champion_satyr", true, Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_satyr},
    OriginConfigCatalog.toggle<ChampionSatyrSection>("champion/satyr_passives", Function { s: ChampionSatyrSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionSatyrSection>("satyr/primary", Function { s: ChampionSatyrSection -> s.rushSwiftLeapLanding}),
    OriginConfigCatalog.toggle<ChampionSatyrSection>("satyr/secondary", Function { s: ChampionSatyrSection -> s.vigilPerception})),
    OriginConfigCatalog.entry<ChampionSelkieSection>("champion_selkie", true, Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_selkie},
    OriginConfigCatalog.toggle<ChampionSelkieSection>("champion/selkie_passives", Function { s: ChampionSelkieSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionSelkieSection>("selkie/aquatic_traits", Function { s: ChampionSelkieSection -> s.aquaticTraits}),
    OriginConfigCatalog.toggle<ChampionSelkieSection>("selkie/primary", Function { s: ChampionSelkieSection -> s.coastalPhalanx}), OriginConfigCatalog.toggle<ChampionSelkieSection>("selkie/secondary", Function { s: ChampionSelkieSection -> s.sealskinBastion})),
    OriginConfigCatalog.entry<ChampionSirenSection>("champion_siren", true, Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_siren},
    OriginConfigCatalog.toggle<ChampionSirenSection>("champion/siren_passives", Function { s: ChampionSirenSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionSirenSection>("siren/aquatic_traits", Function { s: ChampionSirenSection -> s.aquaticTraits}),
    OriginConfigCatalog.toggle<ChampionSirenSection>("siren/primary", Function { s: ChampionSirenSection -> s.infatuationTempo}), OriginConfigCatalog.toggle<ChampionSirenSection>("siren/secondary", Function { s: ChampionSirenSection -> s.shriekingWail})),
    OriginConfigCatalog.entry<ChampionVeilbornSection>("champion_veilborn", true, Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_veilborn},
    OriginConfigCatalog.toggle<ChampionVeilbornSection>("champion/veilborn_passives", Function { s: ChampionVeilbornSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionVeilbornSection>("veilborn/shimmer", Function { s: ChampionVeilbornSection -> s.shimmer}),
    OriginConfigCatalog.toggle<ChampionVeilbornSection>("veilborn/primary", Function { s: ChampionVeilbornSection -> s.curtainStepTransposition}), OriginConfigCatalog.toggle<ChampionVeilbornSection>("veilborn/secondary", Function { s: ChampionVeilbornSection -> s.auroralMirage})),
    OriginConfigCatalog.entry<ChampionWyverianSection>("champion_wyverian", true, Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_wyverian},
    OriginConfigCatalog.toggle<ChampionWyverianSection>("champion/wyverian_passives", Function { s: ChampionWyverianSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionWyverianSection>("champion/wyverian_primary", Function { s: ChampionWyverianSection -> s.dragonsBreath}),
    OriginConfigCatalog.toggle<ChampionWyverianSection>("wyverian/secondary", Function { s: ChampionWyverianSection -> s.gustsOfFreedom}), OriginConfigCatalog.toggle<ChampionWyverianSection>("wyverian/sneakair", Function { s: ChampionWyverianSection -> s.flightHover}))
    )

    val STOCK: MutableList<StockEntry> = JavaList.of<StockEntry>(
    OriginConfigCatalog.stock("arachnid", Function { s: OvergrownOriginsSection -> s.arachnid}), OriginConfigCatalog.stock("avian", Function { s: OvergrownOriginsSection -> s.avian}), OriginConfigCatalog.stock("blazeborn", Function { s: OvergrownOriginsSection -> s.blazeborn}),
    OriginConfigCatalog.stock("buzzborne", Function { s: OvergrownOriginsSection -> s.buzzborne}), OriginConfigCatalog.stock("elytrian", Function { s: OvergrownOriginsSection -> s.elytrian}), OriginConfigCatalog.stock("enderian", Function { s: OvergrownOriginsSection -> s.enderian}),
    OriginConfigCatalog.stock("feline", Function { s: OvergrownOriginsSection -> s.feline}), OriginConfigCatalog.stock("human", Function { s: OvergrownOriginsSection -> s.human}), OriginConfigCatalog.stock("merling", Function { s: OvergrownOriginsSection -> s.merling}),
    OriginConfigCatalog.stock("phantom", Function { s: OvergrownOriginsSection -> s.phantom}), OriginConfigCatalog.stock("shulk", Function { s: OvergrownOriginsSection -> s.shulk}), OriginConfigCatalog.stock("slimekin", Function { s: OvergrownOriginsSection -> s.slimekin})
    )

    val NON_CONFIGURABLE_INFRASTRUCTURE: MutableSet<ResourceLocation> = JavaSet.of<ResourceLocation>(
    OriginConfigCatalog.andromeda("common/figura"), OriginConfigCatalog.andromeda("common/champion"), OriginConfigCatalog.andromeda("common/champion_cooldowns"),
    OriginConfigCatalog.andromeda("humanity/figura_mortal_resolve")
    )

    val ALL: MutableList<OriginEntry<*>> = Stream.concat<OriginEntry<*>>(OriginConfigCatalog.STANDARD.stream(), OriginConfigCatalog.CHAMPION.stream()).toList()
    val BY_ID: MutableMap<ResourceLocation, OriginEntry<*>>
    val ANDROMEDA_IDS: MutableSet<ResourceLocation>
    val CONFIGURABLE_POWERS: MutableMap<ResourceLocation, MutableSet<ResourceLocation>>
    val STOCK_IDS: MutableSet<ResourceLocation> = OriginConfigCatalog.STOCK.stream().map<ResourceLocation>(StockEntry::id)
    .collect(Collectors.toUnmodifiableSet())

    init {
        val entries: MutableMap<ResourceLocation, OriginEntry<*>> = LinkedHashMap<ResourceLocation, OriginEntry<*>>()
        for (entry in OriginConfigCatalog.ALL) {
            check(entries.put(entry.id, entry) == null) {"Duplicate Andromeda Origin in config catalog: " + entry.id}
        }
        BY_ID = JavaMap.copyOf<ResourceLocation, OriginEntry<*>>(entries)
        ANDROMEDA_IDS = JavaSet.copyOf<ResourceLocation>(entries.keys)
        CONFIGURABLE_POWERS = OriginConfigCatalog.ALL.stream().collect(Collectors.toUnmodifiableMap({ entry -> entry.id }, Function { obj: OriginEntry<*> -> obj.configurablePowers()}))
    }

    fun standardIds(): MutableList<ResourceLocation> {
        return OriginConfigCatalog.STANDARD.stream().map<ResourceLocation>({ entry -> entry.id }).toList()
    }

    fun championIds(): MutableList<ResourceLocation> {
        return OriginConfigCatalog.CHAMPION.stream().map<ResourceLocation>({ entry -> entry.id }).toList()
    }

    fun stockIds(): MutableList<ResourceLocation> {
        return OriginConfigCatalog.STOCK.stream().map<ResourceLocation>(StockEntry::id).toList()
    }

    fun isAndromedaOrigin(id: ResourceLocation?): Boolean {
        return OriginConfigCatalog.ANDROMEDA_IDS.contains(id)
    }

    fun isStockOrigin(id: ResourceLocation?): Boolean {
        return OriginConfigCatalog.STOCK_IDS.contains(id)
    }

    fun configurablePowers(originId: ResourceLocation?): MutableSet<ResourceLocation> {
        return CONFIGURABLE_POWERS[originId] ?: mutableSetOf()
    }

    @SafeVarargs private fun <S : OriginToggleSection>entry(
    path: String,
    champion: Boolean,
    section: Function<AndromedaGameplayConfig, S>,
    vararg toggles: PowerToggle<S>
    ): OriginEntry<S> {
        return OriginEntry<S>(OriginConfigCatalog.andromeda(path), champion, section, JavaList.of<PowerToggle<S>>(*toggles))
    }

    private fun <S : OriginToggleSection>toggle(
    power: String,
    getter: Function<S, ValidatedBoolean>
    ): PowerToggle<S> {
        return PowerToggle<S>(OriginConfigCatalog.andromeda(power), getter)
    }

    private fun stock(path: String,
    getter: Function<OvergrownOriginsSection, ValidatedBoolean>): StockEntry {
        return StockEntry(OriginConfigCatalog.id("origins", path), getter)
    }

    fun andromeda(path: String): ResourceLocation {
        return OriginConfigCatalog.id(AndromedaOrigins.MOD_ID, path)
    }

    private fun id(namespace: String, path: String): ResourceLocation {
        return ResourceLocation.fromNamespaceAndPath(namespace, path)
    }

    data class PowerToggle<S : OriginToggleSection> (val power: ResourceLocation,
    val getter: Function<S, ValidatedBoolean>
    ) {
    }


    data class OriginEntry<S : OriginToggleSection> (val id: ResourceLocation,
    val champion: Boolean,
    val section: Function<AndromedaGameplayConfig, S>,
    val toggles: MutableList<PowerToggle<S>>
    ) {
        fun configurablePowers(): MutableSet<ResourceLocation> {
            return toggles.stream().map<ResourceLocation>({ toggle -> toggle.power }).collect(Collectors.toUnmodifiableSet())
        }
    }


    data class StockEntry (val id: ResourceLocation,
    val getter: Function<OvergrownOriginsSection, ValidatedBoolean>
    ) {
    }

}
