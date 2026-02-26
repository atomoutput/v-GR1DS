package com.volcagrids.engine

enum class LogicMode {
    NONE,       // Standard Grids trigger
    NOT_KICK,   // Trigger only if Kick is NOT hitting
    NOT_SNARE,  // Trigger only if Snare is NOT hitting
    AND_HH,     // Trigger only if Hi-Hat IS hitting
    XOR_PREV    // Trigger only if the previous part did NOT hit
}

class LogicEngine {
    fun applyLogic(
        currentState: Int, 
        partIndex: Int, 
        mode: LogicMode
    ): Boolean {
        val isTriggered = (currentState and (1 shl partIndex)) != 0
        if (!isTriggered) return false
        
        return when (mode) {
            LogicMode.NONE -> true
            LogicMode.NOT_KICK -> (currentState and 0x01) == 0
            LogicMode.NOT_SNARE -> (currentState and 0x02) == 0
            LogicMode.AND_HH -> (currentState and 0x04) != 0
            LogicMode.XOR_PREV -> {
                val prevIndex = if (partIndex == 0) 2 else partIndex - 1
                (currentState and (1 shl prevIndex)) == 0
            }
        }
    }
}
