package com.volcagrids.engine

class MorphEngine {
    fun interpolate(sceneA: Scene, sceneB: Scene, factor: Float): Scene {
        val f = factor.coerceIn(0f, 1f)
        val invF = 1f - f

        return Scene(
            engineAX = (sceneA.engineAX * invF + sceneB.engineAX * f).toInt(),
            engineAY = (sceneA.engineAY * invF + sceneB.engineAY * f).toInt(),
            engineBX = (sceneA.engineBX * invF + sceneB.engineBX * f).toInt(),
            engineBY = (sceneA.engineBY * invF + sceneB.engineBY * f).toInt(),
            densitiesA = IntArray(3) { i ->
                (sceneA.densitiesA[i] * invF + sceneB.densitiesA[i] * f).toInt()
            },
            densitiesB = IntArray(3) { i ->
                (sceneA.densitiesB[i] * invF + sceneB.densitiesB[i] * f).toInt()
            },
            randomnessA = (sceneA.randomnessA * invF + sceneB.randomnessA * f).toInt(),
            randomnessB = (sceneA.randomnessB * invF + sceneB.randomnessB * f).toInt()
        )
    }
}
