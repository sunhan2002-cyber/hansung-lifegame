package com.example.lifegame.domain.engine

/** 결혼 콘텐츠와 엔진이 공유하는 상대·단계 플래그 계약. */
object MarriageContract {
    const val MARRIED = "married"
    const val DATING = "dating"
    const val ENGAGED = "engaged"
    const val FAMILIES_MET = "families_met"
    const val PARTNER_PREFIX = "partner_"
    const val PARTNER_ID_PREFIX = "partner_id_"
    const val SPOUSE_ID_PREFIX = "spouse_id_"
    const val WEDDING_READY = "wedding_ready"
    const val PREPARATION_EVENT = "love_wedding_prep_01"

    private val weddingEventIds = setOf(
        "love_wedding_first_love",
        "love_wedding_music",
        "love_wedding_book",
        "love_wedding_01",
    )

    fun isCeremony(eventId: String): Boolean = eventId in weddingEventIds

    data class PartnerDefinition(
        val id: String,
        val routeFlag: String,
        val persistentFlag: String,
        val scopedFlag: String? = null,
        val origins: Map<String, Set<String>>,
    )

    /** 신기훈 PR #83의 연애·결혼 계약을 엔진의 안정 ID로 옮긴 목록. */
    val partners = listOf(
        partner("festival", "first_love", "met_festival", "dating__adult_077", "teen_021" to setOf("A", "C"), "adult_077" to setOf("A", "C")),
        partner("bus", "first_love", "met_bus", "dating__path_soc_03", "path_soc_03" to setOf("A")),
        partner("music_band", "music", "met_music_band", "dating__path_hap_05", "path_hap_05" to setOf("A")),
        partner("music_campfire", "music", "met_music_campfire", "dating__adult_075", "adult_075" to setOf("A", "B")),
        partner("book_club", "book", "met_book_club", "dating__adult_074", "adult_074" to setOf("C")),
        partner("book_blind", "book", "met_book_blind", "dating__path_soc_11", "path_soc_11" to setOf("C")),
        partner("ideal_street", "ideal", "met_ideal_street", null, "adult_005" to setOf("A", "C"), "adult_006" to setOf("A")),
        partner("ideal_bar", "ideal", "met_ideal_bar", null, "adult_008" to setOf("A", "B")),
        partner("app", "app", "met_app", "dating__adult_074", "adult_074" to setOf("A")),
        partner("closing", "closing", "met_closing", "dating__adult_076", "adult_076" to setOf("A", "C")),
        partner("gym", "gym", "met_gym", "dating__adult_078", "adult_078" to setOf("A", "B")),
        partner("blind_date", "blind_date", "met_blind_date", "dating__adult_073", "adult_073" to setOf("A", "C")),
        partner("letter", "letter", "letter_friend", "dating__love_letter_01", "love_letter_01" to setOf("A", "B", "C")),
    )

    fun partnerIds(flags: Set<String>): Set<String> = flags
        .filterTo(linkedSetOf()) {
            it.startsWith(PARTNER_PREFIX) && !it.startsWith(PARTNER_ID_PREFIX) && it.length > PARTNER_PREFIX.length
        }
        .mapTo(linkedSetOf()) { it.removePrefix(PARTNER_PREFIX) }

    fun currentPartnerId(flags: Set<String>): String? = flags
        .singleOrNull { it.startsWith(PARTNER_ID_PREFIX) }
        ?.removePrefix(PARTNER_ID_PREFIX)

    fun spouseId(flags: Set<String>): String? = flags
        .firstOrNull { it.startsWith(SPOUSE_ID_PREFIX) }
        ?.removePrefix(SPOUSE_ID_PREFIX)

    fun canPrepare(flags: Set<String>): Boolean =
        MARRIED !in flags && DATING in flags && ENGAGED in flags && FAMILIES_MET in flags &&
            partnerIds(flags).size == 1 && resolvedPartner(flags) != null

    fun canFinalize(eventId: String, flags: Set<String>): Boolean {
        if (!canPrepare(flags) || WEDDING_READY !in flags) return false
        val definition = resolvedPartner(flags) ?: return false
        val routeMatches = when (eventId) {
            "love_wedding_first_love" -> definition.routeFlag == "partner_first_love"
            "love_wedding_music" -> definition.routeFlag == "partner_music"
            "love_wedding_book" -> definition.routeFlag == "partner_book"
            "love_wedding_01" -> definition.routeFlag !in setOf("partner_first_love", "partner_music", "partner_book")
            else -> false
        }
        return routeMatches
    }

    /** 실제 본식 결과에서만 혼인을 확정하며 같은 인생에 보상·기록을 재적용하지 않는다. */
    fun requireFirstWedding(eventId: String, beforeFlags: Set<String>, outcomeAdds: Set<String>) {
        if (MARRIED !in outcomeAdds || !eventId.startsWith("love_wedding_")) return
        require(canFinalize(eventId, beforeFlags)) {
            "wedding requires an approved ceremony, one partner id, dating, engagement, both-family meeting, preparation, and no prior marriage"
        }
    }

    /** 선택 결과마다 현재 상대 ID를 고정하고, 결별 시 해제하며, 본식에서 배우자 ID로 승격한다. */
    fun synchronizeIdentity(eventId: String, choiceId: String, flags: Set<String>): Set<String> {
        val withoutIdentity = flags.filterNotTo(linkedSetOf()) {
            it.startsWith(PARTNER_ID_PREFIX) || it.startsWith(SPOUSE_ID_PREFIX)
        }
        val previousPartner = currentPartnerId(flags)?.let { id -> partners.firstOrNull { it.id == id } }
        val selected = partners.firstOrNull { choiceId in it.origins[eventId].orEmpty() && it.matches(flags) }
            ?: previousPartner?.takeIf { it.matches(flags) }
            ?: inferUniquePartner(flags)
        if (DATING !in flags || partnerIds(flags).isEmpty()) return withoutIdentity
        val partner = selected ?: return withoutIdentity
        val withPartner = withoutIdentity + "$PARTNER_ID_PREFIX${partner.id}"
        return if (MARRIED in flags) withPartner + "$SPOUSE_ID_PREFIX${partner.id}" else withPartner
    }

    private fun inferUniquePartner(flags: Set<String>): PartnerDefinition? =
        partners.filter { it.matches(flags) }.singleOrNull()

    private fun resolvedPartner(flags: Set<String>): PartnerDefinition? {
        val idFlags = flags.filter { it.startsWith(PARTNER_ID_PREFIX) }
        if (idFlags.isEmpty()) return inferUniquePartner(flags)
        val id = idFlags.singleOrNull()?.removePrefix(PARTNER_ID_PREFIX) ?: return null
        return partners.firstOrNull { it.id == id && it.matches(flags) }
    }

    private fun PartnerDefinition.matches(flags: Set<String>): Boolean =
        routeFlag in flags && persistentFlag in flags && (scopedFlag == null || scopedFlag in flags)

    private fun partner(
        id: String,
        route: String,
        persistent: String,
        scoped: String?,
        vararg origins: Pair<String, Set<String>>,
    ) = PartnerDefinition(id, "$PARTNER_PREFIX$route", persistent, scoped, mapOf(*origins))
}
