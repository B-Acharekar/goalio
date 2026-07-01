package com.goalio.scores

object GoalioAppVisibility {
    @Volatile
    var isForeground: Boolean = false
        private set

    fun markForeground() {
        isForeground = true
    }

    fun markBackground() {
        isForeground = false
    }
}
