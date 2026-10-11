package andromeda.origins.config

import andromeda.origins.AndromedaOrigins
import andromeda.origins.config.AndromedaGameplayConfig
import java.util.LinkedHashMap
import andromeda.origins.config.AndromedaGameplayConfig.OvergrownOriginsSection
import andromeda.origins.config.AndromedaGameplayConfig.OriginToggleSection
import andromeda.origins.config.AndromedaGameplayConfig.ArachneSection
import andromeda.origins.config.AndromedaGameplayConfig.FaerieSection
import andromeda.origins.config.AndromedaGameplayConfig.FenrkinSection
import andromeda.origins.config.AndromedaGameplayConfig.GorgonSection
import andromeda.origins.config.AndromedaGameplayConfig.HumanitySection
import andromeda.origins.config.AndromedaGameplayConfig.LichlingSection
import andromeda.origins.config.AndromedaGameplayConfig.ManticoreSection
import andromeda.origins.config.AndromedaGameplayConfig.NereidSection
import andromeda.origins.config.AndromedaGameplayConfig.SatyrSection
import andromeda.origins.config.AndromedaGameplayConfig.SelkieSection
import andromeda.origins.config.AndromedaGameplayConfig.SirenSection
import andromeda.origins.config.AndromedaGameplayConfig.VeilbornSection
import andromeda.origins.config.AndromedaGameplayConfig.WyverianSection
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


 /**
 * Authoritative catalog for Andromeda Origin IDs and their player-facing Fzzy controls.
 *
 *
 * Each entry binds an Origin ID directly to its existing config section and toggle fields. This
 * keeps config paths stable while eliminating the old duplicate allowlist in runtime policy and the
 * giant hand-written conversion table in [AndromedaGameplayConfig.toState].
 */
object OriginConfigCatalog {
    val STANDARD: kotlin.collections.MutableList<OriginEntry<*>> = java.util.List.of<OriginEntry<*>>(
    OriginConfigCatalog.entry<ArachneSection>("arachne", false, java.util.function.Function { c: AndromedaGameplayConfig -> c.standardOrigins.arachne},
    OriginConfigCatalog.toggle<ArachneSection>("arachne/passives", java.util.function.Function { s: ArachneSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ArachneSection>("arachne/primary", java.util.function.Function { s: ArachneSection -> s.eightfoldSwiftness}),
    OriginConfigCatalog.toggle<ArachneSection>("arachne/secondary", java.util.function.Function { s: ArachneSection -> s.weaversNest}), OriginConfigCatalog.toggle<ArachneSection>("common/diet_carnivore", java.util.function.Function { s: ArachneSection -> s.carnivorousDiet})),
    OriginConfigCatalog.entry<FaerieSection>("faerie", false, java.util.function.Function { c: AndromedaGameplayConfig -> c.standardOrigins.faerie},
    OriginConfigCatalog.toggle<FaerieSection>("faerie/passives", java.util.function.Function { s: FaerieSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<FaerieSection>("faerie/primary", java.util.function.Function { s: FaerieSection -> s.flutter}),
    OriginConfigCatalog.toggle<FaerieSection>("faerie/secondary", java.util.function.Function { s: FaerieSection -> s.faeIllusions}), OriginConfigCatalog.toggle<FaerieSection>("faerie/prankster", java.util.function.Function { s: FaerieSection -> s.trickster}),
    OriginConfigCatalog.toggle<FaerieSection>("common/ironweakness", java.util.function.Function { s: FaerieSection -> s.ironWeakness})),
    OriginConfigCatalog.entry<FenrkinSection>("fenrkin", false, java.util.function.Function { c: AndromedaGameplayConfig -> c.standardOrigins.fenrkin},
    OriginConfigCatalog.toggle<FenrkinSection>("fenrkin/passives", java.util.function.Function { s: FenrkinSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<FenrkinSection>("fenrkin/primary", java.util.function.Function { s: FenrkinSection -> s.onTheHunt}),
    OriginConfigCatalog.toggle<FenrkinSection>("fenrkin/secondary", java.util.function.Function { s: FenrkinSection -> s.staminaSurge}), OriginConfigCatalog.toggle<FenrkinSection>("common/diet_carnivore", java.util.function.Function { s: FenrkinSection -> s.carnivorousDiet})),
    OriginConfigCatalog.entry<GorgonSection>("gorgon", false, java.util.function.Function { c: AndromedaGameplayConfig -> c.standardOrigins.gorgon},
    OriginConfigCatalog.toggle<GorgonSection>("gorgon/passives", java.util.function.Function { s: GorgonSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<GorgonSection>("gorgon/primary", java.util.function.Function { s: GorgonSection -> s.ophidianGaze}),
    OriginConfigCatalog.toggle<GorgonSection>("gorgon/secondary", java.util.function.Function { s: GorgonSection -> s.transference}), OriginConfigCatalog.toggle<GorgonSection>("common/ironweakness", java.util.function.Function { s: GorgonSection -> s.ironWeakness}),
    OriginConfigCatalog.toggle<GorgonSection>("common/diet_raw", java.util.function.Function { s: GorgonSection -> s.rawDiet})),
    OriginConfigCatalog.entry<HumanitySection>("humanity", false, java.util.function.Function { c: AndromedaGameplayConfig -> c.standardOrigins.humanity},
    OriginConfigCatalog.toggle<HumanitySection>("humanity/passives", java.util.function.Function { s: HumanitySection -> s.passiveFeatures}), OriginConfigCatalog.toggle<HumanitySection>("humanity/primary", java.util.function.Function { s: HumanitySection -> s.indomitable}),
    OriginConfigCatalog.toggle<HumanitySection>("humanity/secondary", java.util.function.Function { s: HumanitySection -> s.mortalResolve})),
    OriginConfigCatalog.entry<LichlingSection>("lichling", false, java.util.function.Function { c: AndromedaGameplayConfig -> c.standardOrigins.lichling},
    OriginConfigCatalog.toggle<LichlingSection>("lichling/passives", java.util.function.Function { s: LichlingSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<LichlingSection>("lichling/primary", java.util.function.Function { s: LichlingSection -> s.deathsDefiance}),
    OriginConfigCatalog.toggle<LichlingSection>("lichling/secondary", java.util.function.Function { s: LichlingSection -> s.chimesOfNecros}), OriginConfigCatalog.toggle<LichlingSection>("common/ironweakness", java.util.function.Function { s: LichlingSection -> s.ironWeakness}),
    OriginConfigCatalog.toggle<LichlingSection>("common/diet_raw", java.util.function.Function { s: LichlingSection -> s.rawDiet})),
    OriginConfigCatalog.entry<ManticoreSection>("manticore", false, java.util.function.Function { c: AndromedaGameplayConfig -> c.standardOrigins.manticore},
    OriginConfigCatalog.toggle<ManticoreSection>("manticore/passives", java.util.function.Function { s: ManticoreSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ManticoreSection>("manticore/primary", java.util.function.Function { s: ManticoreSection -> s.ravenousLunge}),
    OriginConfigCatalog.toggle<ManticoreSection>("manticore/secondary", java.util.function.Function { s: ManticoreSection -> s.beastOfBlood}), OriginConfigCatalog.toggle<ManticoreSection>("common/diet_carnivore", java.util.function.Function { s: ManticoreSection -> s.carnivorousDiet})),
    OriginConfigCatalog.entry<NereidSection>("nereid", false, java.util.function.Function { c: AndromedaGameplayConfig -> c.standardOrigins.nereid},
    OriginConfigCatalog.toggle<NereidSection>("nereid/passives", java.util.function.Function { s: NereidSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<NereidSection>("nereid/aquatic_traits", java.util.function.Function { s: NereidSection -> s.aquaticTraits}),
    OriginConfigCatalog.toggle<NereidSection>("nereid/gills", java.util.function.Function { s: NereidSection -> s.gills}), OriginConfigCatalog.toggle<NereidSection>("nereid/primary", java.util.function.Function { s: NereidSection -> s.convalescingAura}),
    OriginConfigCatalog.toggle<NereidSection>("nereid/secondary", java.util.function.Function { s: NereidSection -> s.submersion}), OriginConfigCatalog.toggle<NereidSection>("common/diet_pescatarian", java.util.function.Function { s: NereidSection -> s.pescatarianDiet})),
    OriginConfigCatalog.entry<SatyrSection>("satyr", false, java.util.function.Function { c: AndromedaGameplayConfig -> c.standardOrigins.satyr},
    OriginConfigCatalog.toggle<SatyrSection>("satyr/passives", java.util.function.Function { s: SatyrSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<SatyrSection>("satyr/primary", java.util.function.Function { s: SatyrSection -> s.rushSwiftLeapLanding}),
    OriginConfigCatalog.toggle<SatyrSection>("satyr/secondary", java.util.function.Function { s: SatyrSection -> s.vigilPerception}), OriginConfigCatalog.toggle<SatyrSection>("common/diet_vegetarian", java.util.function.Function { s: SatyrSection -> s.vegetarianDiet})),
    OriginConfigCatalog.entry<SelkieSection>("selkie", false, java.util.function.Function { c: AndromedaGameplayConfig -> c.standardOrigins.selkie},
    OriginConfigCatalog.toggle<SelkieSection>("selkie/passives", java.util.function.Function { s: SelkieSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<SelkieSection>("selkie/aquatic_traits", java.util.function.Function { s: SelkieSection -> s.aquaticTraits}),
    OriginConfigCatalog.toggle<SelkieSection>("selkie/primary", java.util.function.Function { s: SelkieSection -> s.coastalPhalanx}), OriginConfigCatalog.toggle<SelkieSection>("selkie/secondary", java.util.function.Function { s: SelkieSection -> s.sealskinBastion}),
    OriginConfigCatalog.toggle<SelkieSection>("common/diet_pescatarian", java.util.function.Function { s: SelkieSection -> s.pescatarianDiet})),
    OriginConfigCatalog.entry<SirenSection>("siren", false, java.util.function.Function { c: AndromedaGameplayConfig -> c.standardOrigins.siren},
    OriginConfigCatalog.toggle<SirenSection>("siren/passives", java.util.function.Function { s: SirenSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<SirenSection>("siren/aquatic_traits", java.util.function.Function { s: SirenSection -> s.aquaticTraits}),
    OriginConfigCatalog.toggle<SirenSection>("siren/primary", java.util.function.Function { s: SirenSection -> s.infatuationTempo}), OriginConfigCatalog.toggle<SirenSection>("siren/secondary", java.util.function.Function { s: SirenSection -> s.shriekingWail}),
    OriginConfigCatalog.toggle<SirenSection>("common/ironweakness", java.util.function.Function { s: SirenSection -> s.ironWeakness}), OriginConfigCatalog.toggle<SirenSection>("siren/diet_carnivore", java.util.function.Function { s: SirenSection -> s.carnivorousDiet})),
    OriginConfigCatalog.entry<VeilbornSection>("veilborn", false, java.util.function.Function { c: AndromedaGameplayConfig -> c.standardOrigins.veilborn},
    OriginConfigCatalog.toggle<VeilbornSection>("veilborn/passives", java.util.function.Function { s: VeilbornSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<VeilbornSection>("veilborn/shimmer", java.util.function.Function { s: VeilbornSection -> s.shimmer}),
    OriginConfigCatalog.toggle<VeilbornSection>("veilborn/primary", java.util.function.Function { s: VeilbornSection -> s.curtainStepTransposition}), OriginConfigCatalog.toggle<VeilbornSection>("veilborn/secondary", java.util.function.Function { s: VeilbornSection -> s.auroralMirage})),
    OriginConfigCatalog.entry<WyverianSection>("wyverian", false, java.util.function.Function { c: AndromedaGameplayConfig -> c.standardOrigins.wyverian},
    OriginConfigCatalog.toggle<WyverianSection>("wyverian/passives", java.util.function.Function { s: WyverianSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<WyverianSection>("wyverian/primary", java.util.function.Function { s: WyverianSection -> s.emberFlames}),
    OriginConfigCatalog.toggle<WyverianSection>("wyverian/secondary", java.util.function.Function { s: WyverianSection -> s.gustsOfFreedom}), OriginConfigCatalog.toggle<WyverianSection>("wyverian/sneakair", java.util.function.Function { s: WyverianSection -> s.flightHover}),
    OriginConfigCatalog.toggle<WyverianSection>("common/diet_carnivore", java.util.function.Function { s: WyverianSection -> s.carnivorousDiet}))
    )

    val CHAMPION: kotlin.collections.MutableList<OriginEntry<*>> = java.util.List.of<OriginEntry<*>>(
    OriginConfigCatalog.entry<ChampionArachneSection>("champion_arachne", true, java.util.function.Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_arachne},
    OriginConfigCatalog.toggle<ChampionArachneSection>("champion/arachne_passives", java.util.function.Function { s: ChampionArachneSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionArachneSection>("arachne/primary", java.util.function.Function { s: ChampionArachneSection -> s.eightfoldSwiftness}),
    OriginConfigCatalog.toggle<ChampionArachneSection>("arachne/secondary", java.util.function.Function { s: ChampionArachneSection -> s.weaversNest})),
    OriginConfigCatalog.entry<ChampionFaerieSection>("champion_faerie", true, java.util.function.Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_faerie},
    OriginConfigCatalog.toggle<ChampionFaerieSection>("champion/faerie_passives", java.util.function.Function { s: ChampionFaerieSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionFaerieSection>("faerie/primary", java.util.function.Function { s: ChampionFaerieSection -> s.flutter}),
    OriginConfigCatalog.toggle<ChampionFaerieSection>("faerie/secondary", java.util.function.Function { s: ChampionFaerieSection -> s.faeIllusions}), OriginConfigCatalog.toggle<ChampionFaerieSection>("faerie/prankster", java.util.function.Function { s: ChampionFaerieSection -> s.trickster})),
    OriginConfigCatalog.entry<ChampionFenrkinSection>("champion_fenrkin", true, java.util.function.Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_fenrkin},
    OriginConfigCatalog.toggle<ChampionFenrkinSection>("champion/fenrkin_passives", java.util.function.Function { s: ChampionFenrkinSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionFenrkinSection>("fenrkin/primary", java.util.function.Function { s: ChampionFenrkinSection -> s.onTheHunt}),
    OriginConfigCatalog.toggle<ChampionFenrkinSection>("fenrkin/secondary", java.util.function.Function { s: ChampionFenrkinSection -> s.staminaSurge})),
    OriginConfigCatalog.entry<ChampionGorgonSection>("champion_gorgon", true, java.util.function.Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_gorgon},
    OriginConfigCatalog.toggle<ChampionGorgonSection>("champion/gorgon_passives", java.util.function.Function { s: ChampionGorgonSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionGorgonSection>("gorgon/primary", java.util.function.Function { s: ChampionGorgonSection -> s.ophidianGaze}),
    OriginConfigCatalog.toggle<ChampionGorgonSection>("gorgon/secondary", java.util.function.Function { s: ChampionGorgonSection -> s.transference})),
    OriginConfigCatalog.entry<ChampionHumanitySection>("champion_humanity", true, java.util.function.Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_humanity},
    OriginConfigCatalog.toggle<ChampionHumanitySection>("champion/humanity_passives", java.util.function.Function { s: ChampionHumanitySection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionHumanitySection>("humanity/primary", java.util.function.Function { s: ChampionHumanitySection -> s.indomitable}),
    OriginConfigCatalog.toggle<ChampionHumanitySection>("champion/humanity_secondary", java.util.function.Function { s: ChampionHumanitySection -> s.mortalTriumph})),
    OriginConfigCatalog.entry<ChampionLichlingSection>("champion_lichling", true, java.util.function.Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_lichling},
    OriginConfigCatalog.toggle<ChampionLichlingSection>("champion/lichling_passives", java.util.function.Function { s: ChampionLichlingSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionLichlingSection>("lichling/primary", java.util.function.Function { s: ChampionLichlingSection -> s.deathsDefiance}),
    OriginConfigCatalog.toggle<ChampionLichlingSection>("lichling/secondary", java.util.function.Function { s: ChampionLichlingSection -> s.chimesOfNecros})),
    OriginConfigCatalog.entry<ChampionManticoreSection>("champion_manticore", true, java.util.function.Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_manticore},
    OriginConfigCatalog.toggle<ChampionManticoreSection>("champion/manticore_passives", java.util.function.Function { s: ChampionManticoreSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionManticoreSection>("manticore/primary", java.util.function.Function { s: ChampionManticoreSection -> s.ravenousLunge}),
    OriginConfigCatalog.toggle<ChampionManticoreSection>("manticore/secondary", java.util.function.Function { s: ChampionManticoreSection -> s.beastOfBlood})),
    OriginConfigCatalog.entry<ChampionNereidSection>("champion_nereid", true, java.util.function.Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_nereid},
    OriginConfigCatalog.toggle<ChampionNereidSection>("champion/nereid_passives", java.util.function.Function { s: ChampionNereidSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionNereidSection>("nereid/aquatic_traits", java.util.function.Function { s: ChampionNereidSection -> s.aquaticTraits}),
    OriginConfigCatalog.toggle<ChampionNereidSection>("nereid/primary", java.util.function.Function { s: ChampionNereidSection -> s.convalescingAura}), OriginConfigCatalog.toggle<ChampionNereidSection>("nereid/secondary", java.util.function.Function { s: ChampionNereidSection -> s.submersion})),
    OriginConfigCatalog.entry<ChampionSatyrSection>("champion_satyr", true, java.util.function.Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_satyr},
    OriginConfigCatalog.toggle<ChampionSatyrSection>("champion/satyr_passives", java.util.function.Function { s: ChampionSatyrSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionSatyrSection>("satyr/primary", java.util.function.Function { s: ChampionSatyrSection -> s.rushSwiftLeapLanding}),
    OriginConfigCatalog.toggle<ChampionSatyrSection>("satyr/secondary", java.util.function.Function { s: ChampionSatyrSection -> s.vigilPerception})),
    OriginConfigCatalog.entry<ChampionSelkieSection>("champion_selkie", true, java.util.function.Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_selkie},
    OriginConfigCatalog.toggle<ChampionSelkieSection>("champion/selkie_passives", java.util.function.Function { s: ChampionSelkieSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionSelkieSection>("selkie/aquatic_traits", java.util.function.Function { s: ChampionSelkieSection -> s.aquaticTraits}),
    OriginConfigCatalog.toggle<ChampionSelkieSection>("selkie/primary", java.util.function.Function { s: ChampionSelkieSection -> s.coastalPhalanx}), OriginConfigCatalog.toggle<ChampionSelkieSection>("selkie/secondary", java.util.function.Function { s: ChampionSelkieSection -> s.sealskinBastion})),
    OriginConfigCatalog.entry<ChampionSirenSection>("champion_siren", true, java.util.function.Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_siren},
    OriginConfigCatalog.toggle<ChampionSirenSection>("champion/siren_passives", java.util.function.Function { s: ChampionSirenSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionSirenSection>("siren/aquatic_traits", java.util.function.Function { s: ChampionSirenSection -> s.aquaticTraits}),
    OriginConfigCatalog.toggle<ChampionSirenSection>("siren/primary", java.util.function.Function { s: ChampionSirenSection -> s.infatuationTempo}), OriginConfigCatalog.toggle<ChampionSirenSection>("siren/secondary", java.util.function.Function { s: ChampionSirenSection -> s.shriekingWail})),
    OriginConfigCatalog.entry<ChampionVeilbornSection>("champion_veilborn", true, java.util.function.Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_veilborn},
    OriginConfigCatalog.toggle<ChampionVeilbornSection>("champion/veilborn_passives", java.util.function.Function { s: ChampionVeilbornSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionVeilbornSection>("veilborn/shimmer", java.util.function.Function { s: ChampionVeilbornSection -> s.shimmer}),
    OriginConfigCatalog.toggle<ChampionVeilbornSection>("veilborn/primary", java.util.function.Function { s: ChampionVeilbornSection -> s.curtainStepTransposition}), OriginConfigCatalog.toggle<ChampionVeilbornSection>("veilborn/secondary", java.util.function.Function { s: ChampionVeilbornSection -> s.auroralMirage})),
    OriginConfigCatalog.entry<ChampionWyverianSection>("champion_wyverian", true, java.util.function.Function { c: AndromedaGameplayConfig -> c.championOrigins.champion_wyverian},
    OriginConfigCatalog.toggle<ChampionWyverianSection>("champion/wyverian_passives", java.util.function.Function { s: ChampionWyverianSection -> s.passiveFeatures}), OriginConfigCatalog.toggle<ChampionWyverianSection>("champion/wyverian_primary", java.util.function.Function { s: ChampionWyverianSection -> s.dragonsBreath}),
    OriginConfigCatalog.toggle<ChampionWyverianSection>("wyverian/secondary", java.util.function.Function { s: ChampionWyverianSection -> s.gustsOfFreedom}), OriginConfigCatalog.toggle<ChampionWyverianSection>("wyverian/sneakair", java.util.function.Function { s: ChampionWyverianSection -> s.flightHover}))
    )

    val STOCK: kotlin.collections.MutableList<StockEntry> = java.util.List.of<StockEntry>(
    OriginConfigCatalog.stock("arachnid", java.util.function.Function { s: OvergrownOriginsSection -> s.arachnid}), OriginConfigCatalog.stock("avian", java.util.function.Function { s: OvergrownOriginsSection -> s.avian}), OriginConfigCatalog.stock("blazeborn", java.util.function.Function { s: OvergrownOriginsSection -> s.blazeborn}),
    OriginConfigCatalog.stock("buzzborne", java.util.function.Function { s: OvergrownOriginsSection -> s.buzzborne}), OriginConfigCatalog.stock("elytrian", java.util.function.Function { s: OvergrownOriginsSection -> s.elytrian}), OriginConfigCatalog.stock("enderian", java.util.function.Function { s: OvergrownOriginsSection -> s.enderian}),
    OriginConfigCatalog.stock("feline", java.util.function.Function { s: OvergrownOriginsSection -> s.feline}), OriginConfigCatalog.stock("human", java.util.function.Function { s: OvergrownOriginsSection -> s.human}), OriginConfigCatalog.stock("merling", java.util.function.Function { s: OvergrownOriginsSection -> s.merling}),
    OriginConfigCatalog.stock("phantom", java.util.function.Function { s: OvergrownOriginsSection -> s.phantom}), OriginConfigCatalog.stock("shulk", java.util.function.Function { s: OvergrownOriginsSection -> s.shulk}), OriginConfigCatalog.stock("slimekin", java.util.function.Function { s: OvergrownOriginsSection -> s.slimekin})
    )

    val NON_CONFIGURABLE_INFRASTRUCTURE: kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation> = java.util.Set.of<net.minecraft.resources.ResourceLocation>(
    OriginConfigCatalog.andromeda("common/figura"), OriginConfigCatalog.andromeda("common/champion"), OriginConfigCatalog.andromeda("common/champion_cooldowns"),
    OriginConfigCatalog.andromeda("humanity/figura_mortal_resolve")
    )

    val ALL: kotlin.collections.MutableList<OriginEntry<*>> = java.util.stream.Stream.concat<OriginEntry<*>>(OriginConfigCatalog.STANDARD.stream(), OriginConfigCatalog.CHAMPION.stream()).toList()
    val BY_ID: kotlin.collections.MutableMap<net.minecraft.resources.ResourceLocation, OriginEntry<*>>
    val ANDROMEDA_IDS: kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>
    val CONFIGURABLE_POWERS: kotlin.collections.MutableMap<net.minecraft.resources.ResourceLocation, kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation>>
    val STOCK_IDS: kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation> = OriginConfigCatalog.STOCK.stream().map<net.minecraft.resources.ResourceLocation>(StockEntry::id)
    .collect(java.util.stream.Collectors.toUnmodifiableSet())

    init {
        val entries: kotlin.collections.MutableMap<net.minecraft.resources.ResourceLocation, OriginEntry<*>> = LinkedHashMap<net.minecraft.resources.ResourceLocation, OriginEntry<*>>()
        for (entry in OriginConfigCatalog.ALL) {
            kotlin.check(entries.put(entry.id, entry) == null) {"Duplicate Andromeda Origin in config catalog: " + entry.id}
        }
        BY_ID = java.util.Map.copyOf<net.minecraft.resources.ResourceLocation, OriginEntry<*>>(entries)
        ANDROMEDA_IDS = java.util.Set.copyOf<net.minecraft.resources.ResourceLocation>(entries.keys)
        CONFIGURABLE_POWERS = OriginConfigCatalog.ALL.stream().collect(java.util.stream.Collectors.toUnmodifiableMap({ entry -> entry.id }, java.util.function.Function { obj: OriginEntry<*> -> obj.configurablePowers()}))
    }

    fun standardIds(): kotlin.collections.MutableList<net.minecraft.resources.ResourceLocation> {
        return OriginConfigCatalog.STANDARD.stream().map<net.minecraft.resources.ResourceLocation>({ entry -> entry.id }).toList()
    }

    fun championIds(): kotlin.collections.MutableList<net.minecraft.resources.ResourceLocation> {
        return OriginConfigCatalog.CHAMPION.stream().map<net.minecraft.resources.ResourceLocation>({ entry -> entry.id }).toList()
    }

    fun stockIds(): kotlin.collections.MutableList<net.minecraft.resources.ResourceLocation> {
        return OriginConfigCatalog.STOCK.stream().map<net.minecraft.resources.ResourceLocation>(StockEntry::id).toList()
    }

    fun isAndromedaOrigin(id: net.minecraft.resources.ResourceLocation?): kotlin.Boolean {
        return OriginConfigCatalog.ANDROMEDA_IDS.contains(id)
    }

    fun isStockOrigin(id: net.minecraft.resources.ResourceLocation?): kotlin.Boolean {
        return OriginConfigCatalog.STOCK_IDS.contains(id)
    }

    fun configurablePowers(originId: net.minecraft.resources.ResourceLocation?): kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation> {
        return OriginConfigCatalog.CONFIGURABLE_POWERS.getOrDefault(originId, kotlin.collections.mutableSetOf<net.minecraft.resources.ResourceLocation>())
    }

    @java.lang.SafeVarargs private fun <S : OriginToggleSection>entry(
    path: kotlin.String,
    champion: kotlin.Boolean,
    section: java.util.function.Function<AndromedaGameplayConfig, S>,
    vararg toggles: PowerToggle<S>
    ): OriginEntry<S> {
        return OriginEntry<S>(OriginConfigCatalog.andromeda(path), champion, section, java.util.List.of<PowerToggle<S>>(*toggles))
    }

    private fun <S : OriginToggleSection>toggle(
    power: kotlin.String,
    getter: java.util.function.Function<S, me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean>
    ): PowerToggle<S> {
        return PowerToggle<S>(OriginConfigCatalog.andromeda(power), getter)
    }

    private fun stock(path: kotlin.String,
    getter: java.util.function.Function<OvergrownOriginsSection, me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean>): StockEntry {
        return StockEntry(OriginConfigCatalog.id("origins", path), getter)
    }

    fun andromeda(path: kotlin.String): net.minecraft.resources.ResourceLocation {
        return OriginConfigCatalog.id(AndromedaOrigins.MOD_ID, path)
    }

    private fun id(namespace: kotlin.String, path: kotlin.String): net.minecraft.resources.ResourceLocation {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, path)
    }

    data class PowerToggle<S : OriginToggleSection> (val power: net.minecraft.resources.ResourceLocation,
    val getter: java.util.function.Function<S, me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean>
    ) {
    }


    data class OriginEntry<S : OriginToggleSection> (val id: net.minecraft.resources.ResourceLocation,
    val champion: kotlin.Boolean,
    val section: java.util.function.Function<AndromedaGameplayConfig, S>,
    val toggles: kotlin.collections.MutableList<PowerToggle<S>>
    ) {
        fun configurablePowers(): kotlin.collections.MutableSet<net.minecraft.resources.ResourceLocation> {
            return toggles.stream().map<net.minecraft.resources.ResourceLocation>({ toggle -> toggle.power }).collect(java.util.stream.Collectors.toUnmodifiableSet())
        }
    }


    data class StockEntry (val id: net.minecraft.resources.ResourceLocation,
    val getter: java.util.function.Function<OvergrownOriginsSection, me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean>
    ) {
    }

}
