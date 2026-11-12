package com.example.lifegame.ui

import com.example.lifegame.ui.screens.EndingStep
import com.example.lifegame.ui.screens.previousEndingStep
import org.junit.Assert.assertEquals
import org.junit.Test

/** 결말 흐름의 뒤로가기 목적지. 지난 인생 기록에서 "주마등 다시 보기" 뒤 뒤로가기를 누르면 앱이 꺼지던 문제(2026-11-12). */
class EndingStepBackTest {

    @Test
    fun aFinishedLifeStepsBackOneStageAtATime() {
        assertEquals(EndingStep.FUTURE, previousEndingStep(EndingStep.CERTIFICATE, EndingStep.RECAP))
        assertEquals(EndingStep.RECAP, previousEndingStep(EndingStep.FUTURE, EndingStep.RECAP))
    }

    @Test
    fun aReopenedRecordReturnsToTheCertificateFromTheReplay() {
        // 지난 기록은 인생기록증에서 열린다. 다시 보기로 들어간 주마등 · 미래 모습은 인생기록증으로 돌아간다.
        assertEquals(EndingStep.CERTIFICATE, previousEndingStep(EndingStep.RECAP, EndingStep.CERTIFICATE))
        assertEquals(EndingStep.CERTIFICATE, previousEndingStep(EndingStep.FUTURE, EndingStep.CERTIFICATE))
    }

    @Test
    fun theFirstStageNeverLooksBeforeTheStart() {
        // 예전 계산은 여기서 entries[-1]을 읽었다.
        for (initial in EndingStep.entries) {
            assertEquals(initial, previousEndingStep(EndingStep.RECAP, initial))
        }
    }
}
