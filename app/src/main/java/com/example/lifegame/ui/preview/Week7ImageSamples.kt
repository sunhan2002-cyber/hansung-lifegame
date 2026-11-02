package com.example.lifegame.ui.preview

data class ImageSamplePreview(val title: String, val viewpoint: String, val imageFile: String)

/** 검토 화면 전용 목록. 적용한 사건 그림은 앱과 같은 파일을 읽고 게임 상태는 변경하지 않는다. */
val Week7ImageSamples = listOf(
    ImageSamplePreview("유년기 친구", "평소 · 3인칭", "design_preview/week7/img_scene_child_friend_001.png"),
    ImageSamplePreview("청소년기 학업", "평소 · 3인칭", "design_preview/week7/img_scene_teen_study_001.png"),
    ImageSamplePreview("청소년기 아르바이트", "평소 · 3인칭", "design_preview/week7/img_scene_teen_part_time_001.png"),
    ImageSamplePreview("청년기 도전", "평소 · 3인칭", "design_preview/week7/img_scene_young_adult_adventure_001.png"),
    ImageSamplePreview("중년기 취미", "평소 · 3인칭", "design_preview/week7/img_scene_middle_hobby_001.png"),
    ImageSamplePreview("시험", "결정·돌발 · 1인칭", "design_preview/week7/img_pov_exam_paper_001.png"),
    ImageSamplePreview("몸살", "결정·돌발 · 1인칭", "design_preview/week7/img_pov_flu_morning_001.png"),
    ImageSamplePreview("회사 출입문", "결정·돌발 · 1인칭", "design_preview/week7/img_pov_layoff_door_001.png"),
    ImageSamplePreview("예술고 첫 학기", "수정된 사건 장면 · 1인칭", "week10/scenes/img_path_hap_09_001.png"),
    ImageSamplePreview("마지막 강의", "수정된 사건 장면 · 1인칭", "week10/scenes/img_path_int_10_001.png"),
)
