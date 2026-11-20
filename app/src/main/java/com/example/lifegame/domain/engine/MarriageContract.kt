package com.example.lifegame.domain.engine

/** 결혼 콘텐츠와 엔진이 공유하는 상대·단계 플래그 계약. */
object MarriageContract {
    const val MARRIED = "married"
    const val DATING = "dating"
    const val ENGAGED = "engaged"
    const val FAMILIES_MET = "families_met"
    const val PARTNER_PREFIX = "partner_"

    fun partnerIds(flags: Set<String>): Set<String> = flags
        .filterTo(linkedSetOf()) { it.startsWith(PARTNER_PREFIX) && it.length > PARTNER_PREFIX.length }
        .mapTo(linkedSetOf()) { it.removePrefix(PARTNER_PREFIX) }

    fun canFinalize(flags: Set<String>): Boolean =
        MARRIED !in flags &&
            DATING in flags && ENGAGED in flags && FAMILIES_MET in flags &&
            partnerIds(flags).size == 1

    /** 실제 본식 결과에서만 혼인을 확정하며 같은 인생에 보상·기록을 재적용하지 않는다. */
    fun requireFirstWedding(eventId: String, beforeFlags: Set<String>, outcomeAdds: Set<String>) {
        if (MARRIED !in outcomeAdds || !eventId.startsWith("love_wedding_")) return
        require(canFinalize(beforeFlags)) {
            "wedding requires one partner, dating, engagement, both-family meeting, and no prior marriage"
        }
    }
}
