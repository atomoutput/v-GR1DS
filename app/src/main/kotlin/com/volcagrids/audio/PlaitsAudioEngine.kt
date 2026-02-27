package com.volcagrids.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import com.volcagrids.plaits.PlaitsTrack
import kotlin.concurrent.thread

/**
 * Plaits Audio Engine
 * Renders all Plaits tracks and outputs to AudioTrack
 * Runs on a dedicated high-priority thread for low-latency audio
 */
class PlaitsAudioEngine {
    companion object {
        const val SAMPLE_RATE = 48000
        const val BLOCK_SIZE = 24
        const val BUFFER_SIZE_IN_BLOCKS = 64
        const val CHANNELS = 2  // Stereo
    }

    // Audio tracks (up to 7: 6 for Grids parts + 1 for Poly mode)
    private val tracks = mutableListOf<PlaitsTrack>()

    // Audio output
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    // Render thread
    private var renderThread: Thread? = null
    @Volatile private var isRunning = false

    // Mix buffer
    private val mixBuffer = ShortArray(BLOCK_SIZE * CHANNELS)
    private val tempBuffer = ShortArray(BLOCK_SIZE * CHANNELS)

    /**
     * Initialize audio engine
     */
    fun init() {
        // Initialize AudioTrack
        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setLegacyStreamType(android.media.AudioManager.STREAM_MUSIC)
            .build()

        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val bufferSize = (minBufferSize * BUFFER_SIZE_IN_BLOCKS).coerceAtLeast(
            BLOCK_SIZE * CHANNELS * 4 * BUFFER_SIZE_IN_BLOCKS
        )

        audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSize,
            AudioTrack.MODE_STREAM,
            Process.THREAD_PRIORITY_AUDIO
        )

        audioTrack?.play()
    }

    /**
     * Add a Plaits track
     */
    fun addTrack(track: PlaitsTrack) {
        tracks.add(track)
    }

    /**
     * Remove a Plaits track
     */
    fun removeTrack(track: PlaitsTrack) {
        tracks.remove(track)
    }

    /**
     * Get track by index
     */
    fun getTrack(index: Int): PlaitsTrack? {
        return tracks.getOrNull(index)
    }

    /**
     * Get all tracks
     */
    fun getAllTracks(): List<PlaitsTrack> = tracks.toList()

    /**
     * Start audio rendering
     */
    fun start() {
        if (isRunning) return

        isRunning = true
        renderThread = thread(
            name = "PlaitsAudioRender",
            priority = Thread.MAX_PRIORITY
        ) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            renderLoop()
        }
    }

    /**
     * Stop audio rendering
     */
    fun stop() {
        isRunning = false
        renderThread?.join(1000)
        renderThread = null
    }

    /**
     * Release audio resources
     */
    fun release() {
        stop()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    /**
     * Main render loop
     */
    private fun renderLoop() {
        while (isRunning) {
            try {
                // Render all tracks and mix
                renderBlock()

                // Write to AudioTrack
                audioTrack?.write(mixBuffer, 0, mixBuffer.size)
            } catch (e: Exception) {
                // Handle buffer underrun or other errors
                android.util.Log.e("PlaitsAudio", "Render error: ${e.message}")
            }
        }
    }

    /**
     * Render one block of audio
     */
    private fun renderBlock() {
        // Clear mix buffer
        for (i in mixBuffer.indices) {
            mixBuffer[i] = 0
        }

        // Render and mix all enabled tracks
        for (track in tracks) {
            if (track.enabled && !track.muted) {
                track.render(tempBuffer, BLOCK_SIZE)

                // Mix into output buffer with soft clipping
                for (i in 0 until BLOCK_SIZE * CHANNELS) {
                    val mixed = mixBuffer[i].toInt() + tempBuffer[i].toInt()
                    mixBuffer[i] = softClip(mixed)
                }
            }
        }
    }

    /**
     * Soft clipping to prevent digital distortion
     */
    private fun softClip(sample: Int): Short {
        val normalized = sample / 65534.0  // Normalize to -1.0 to 1.0 range
        val clipped = when {
            normalized < -1.0 -> -1.0
            normalized > 1.0 -> 1.0
            else -> {
                // Soft knee - use Double for calculation then convert
                val x = normalized * 1.5
                val result = if (x < -1.0) -1.0
                else if (x > 1.0) 1.0
                else x * (27.0 + x * x) / (27.0 + 9.0 * x * x)
                result
            }
        }
        return (clipped * 32767).toInt().coerceIn(-32768, 32767).toShort()
    }

    /**
     * Set master volume
     */
    fun setMasterVolume(volume: Float) {
        audioTrack?.setVolume(volume.coerceIn(0.0f, 1.0f))
    }

    /**
     * Check if engine is running
     */
    fun isRunning(): Boolean = isRunning

    /**
     * Get number of active tracks
     */
    fun getTrackCount(): Int = tracks.size
}
