package com.volcagrids.engine

import kotlin.math.abs

class PhysicsEngine(
    var x: Float = 127f,
    var y: Float = 127f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var friction: Float = 0.98f,
    var elastic: Float = 0.9f,
    var enabled: Boolean = false
) {
    fun update() {
        x += vx
        y += vy
        vx *= friction
        vy *= friction
        
        // Bounce X
        if (x < 0) {
            x = 0f
            vx = -vx * elastic
        } else if (x > 255) {
            x = 255f
            vx = -vx * elastic
        }
        
        // Bounce Y
        if (y < 0) {
            y = 0f
            vy = -vy * elastic
        } else if (y > 255) {
            y = 255f
            vy = -vy * elastic
        }
    }
    
    fun applyImpulse(ivx: Float, ivy: Float) {
        vx += ivx
        vy += ivy
    }
}

class ModulationSource(
    val generator: PatternGenerator,
    var targetCC: Int,
    var channel: Int,
    var amount: Float = 1.0f
) {
    private var lastValue = -1

    fun update(): Int? {
        // Get raw level before thresholding from PatternGenerator
        // Note: I'll need to expose rawLevel in PatternGenerator
        return null 
    }
}
