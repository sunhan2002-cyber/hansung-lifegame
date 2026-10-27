package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.BodyType
import com.example.lifegame.domain.model.Stats

/** 운동능력 우선, 이후 저건강/저운동, 나머지 보통 체형으로 판정한다. */
fun bodyTypeFor(stats: Stats): BodyType = when {
    stats.fitness >= 70 -> BodyType.MUSCULAR
    stats.health < 35 || stats.fitness < 30 -> BodyType.SLIM
    else -> BodyType.STANDARD
}
