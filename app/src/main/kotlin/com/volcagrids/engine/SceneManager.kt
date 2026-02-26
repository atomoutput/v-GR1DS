package com.volcagrids.engine

data class Scene(
    val engineAX: Int,
    val engineAY: Int,
    val engineBX: Int,
    val engineBY: Int,
    val densitiesA: IntArray,
    val densitiesB: IntArray,
    val randomnessA: Int,
    val randomnessB: Int
)

class SceneManager {
    private val scenes = mutableMapOf<Int, Scene>()
    
    fun saveScene(index: Int, scene: Scene) {
        scenes[index] = scene
    }
    
    fun getScene(index: Int): Scene? = scenes[index]
}
