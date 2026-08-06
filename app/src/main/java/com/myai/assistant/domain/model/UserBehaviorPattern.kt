package com.myai.assistant.domain.model

/**
 * 用户行为模式模型 - 用于学习用户习惯
 */
data class UserBehaviorPattern(
    val id: Long = 0,
    val patternType: PatternType,
    val description: String,
    val frequency: Float, // 0.0 to 1.0
    val lastObserved: Long,
    val observationCount: Int = 1,
    val confidence: Float = 0.5f, // 0.0 to 1.0
    val relatedData: Map<String, String> = emptyMap()
) {
    enum class PatternType {
        SLEEP_SCHEDULE,
        MEAL_TIME,
        WORK_ROUTINE,
        COMMUNICATION_STYLE,
        PREFERENCE_CHANGE,
        LOCATION_PATTERN,
        APP_USAGE,
        MOOD_CYCLE
    }
}
