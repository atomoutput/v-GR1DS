package com.volcagrids.storage

import com.volcagrids.ui.components.SceneData

/**
 * Scene Storage - Simple JSON-like persistence for scenes
 * Uses basic string formatting instead of external serialization library
 */

data class SceneJSON(
    val index: Int,
    val name: String,
    val engineAX: Int,
    val engineAY: Int,
    val engineBX: Int,
    val engineBY: Int,
    val densitiesA: List<Int>,
    val densitiesB: List<Int>,
    val randomnessA: Int,
    val randomnessB: Int,
    val timestamp: Long = System.currentTimeMillis()
)

object SceneStorage {
    
    fun sceneToJSON(scene: SceneData): SceneJSON {
        return SceneJSON(
            index = scene.index,
            name = scene.name,
            engineAX = scene.engineAX,
            engineAY = scene.engineAY,
            engineBX = scene.engineBX,
            engineBY = scene.engineBY,
            densitiesA = scene.densitiesA,
            densitiesB = scene.densitiesB,
            randomnessA = scene.randomnessA,
            randomnessB = scene.randomnessB
        )
    }
    
    fun jSONToScene(jsonScene: SceneJSON): SceneData {
        return SceneData(
            index = jsonScene.index,
            name = jsonScene.name,
            engineAX = jsonScene.engineAX,
            engineAY = jsonScene.engineAY,
            engineBX = jsonScene.engineBX,
            engineBY = jsonScene.engineBY,
            densitiesA = jsonScene.densitiesA,
            densitiesB = jsonScene.densitiesB,
            randomnessA = jsonScene.randomnessA,
            randomnessB = jsonScene.randomnessB,
            isSaved = true
        )
    }
    
    /**
     * Simple JSON serialization without external library
     * Format: {index:0,name:"Scene",engineAX:127,...}
     */
    fun serializeScene(scene: SceneData): String {
        val json = sceneToJSON(scene)
        return buildString {
            append("{")
            append("\"index\":${json.index},")
            append("\"name\":\"${json.name}\",")
            append("\"engineAX\":${json.engineAX},")
            append("\"engineAY\":${json.engineAY},")
            append("\"engineBX\":${json.engineBX},")
            append("\"engineBY\":${json.engineBY},")
            append("\"densitiesA\":[${json.densitiesA.joinToString()}],")
            append("\"densitiesB\":[${json.densitiesB.joinToString()}],")
            append("\"randomnessA\":${json.randomnessA},")
            append("\"randomnessB\":${json.randomnessB},")
            append("\"timestamp\":${json.timestamp}")
            append("}")
        }
    }
    
    /**
     * Simple JSON deserialization
     * Parses the basic format created by serializeScene
     */
    fun deserializeScene(jsonString: String): SceneData? {
        return try {
            // Very basic parsing - extract values between delimiters
            fun extractInt(key: String): Int {
                val regex = "\"$key\":(\\d+)".toRegex()
                return regex.find(jsonString)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            }
            
            fun extractString(key: String): String {
                val regex = "\"$key\":\"([^\"]*)\"".toRegex()
                return regex.find(jsonString)?.groupValues?.get(1) ?: ""
            }
            
            fun extractIntList(key: String): List<Int> {
                val regex = "\"$key\":\\[(.*?)\\]".toRegex()
                return regex.find(jsonString)?.groupValues?.get(1)
                    ?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()
            }
            
            SceneData(
                index = extractInt("index"),
                name = extractString("name"),
                engineAX = extractInt("engineAX"),
                engineAY = extractInt("engineAY"),
                engineBX = extractInt("engineBX"),
                engineBY = extractInt("engineBY"),
                densitiesA = extractIntList("densitiesA"),
                densitiesB = extractIntList("densitiesB"),
                randomnessA = extractInt("randomnessA"),
                randomnessB = extractInt("randomnessB"),
                isSaved = true
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun serializeScenes(scenes: List<SceneData>): String {
        return scenes.joinToString(prefix = "[", postfix = "]", separator = ",") { scene ->
            serializeScene(scene)
        }
    }
    
    fun deserializeScenes(jsonString: String): List<SceneData> {
        return try {
            // Remove outer brackets and split by },{
            val inner = jsonString.trim('[', ']')
            if (inner.isEmpty()) return emptyList()
            
            val sceneStrings = inner.split("},{")
            sceneStrings.mapIndexed { index, sceneStr ->
                var fixedStr = sceneStr
                if (index == 0) fixedStr = fixedStr.trimStart('{')
                if (index == sceneStrings.size - 1) fixedStr = fixedStr.trimEnd('}')
                if (index > 0) fixedStr = "{$fixedStr}"
                if (index < sceneStrings.size - 1) fixedStr = "$fixedStr}"
                
                deserializeScene(fixedStr)
            }.filterNotNull()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
