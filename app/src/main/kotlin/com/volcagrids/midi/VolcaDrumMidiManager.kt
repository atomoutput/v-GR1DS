package com.volcagrids.midi

import android.media.midi.MidiDevice
import android.media.midi.MidiInputPort
import android.media.midi.MidiReceiver

class VolcaDrumMidiManager {
    private var inputPort: MidiInputPort? = null
    private val throttler = MIDIThrottler(maxCCsPerSecond = 100)
    var triggerNotes = intArrayOf(60, 60, 60, 60, 60, 60)

    companion object {
        const val NOTE_NUMBER = 60 // C3
        const val DEFAULT_VELOCITY = 80
        const val ACCENT_VELOCITY = 127

        // CC Mapping
        const val CC_PAN = 10
        const val CC_LEVEL_1 = 17
        const val CC_LEVEL_2 = 18
        const val CC_PITCH_12 = 28
        const val CC_RESONATOR_DECAY = 117
        const val CC_RESONATOR_BODY = 118
        const val CC_RESONATOR_TUNE = 119

        // Throttling: max CCs per second to prevent buffer overflow
        const val MAX_CC_PER_SECOND = 100
    }

    fun setInputPort(port: MidiInputPort) {
        this.inputPort = port
    }

    /**
     * Send trigger immediately (not throttled - triggers are time-critical)
     */
    fun sendTrigger(channel: Int, isAccent: Boolean) {
        val velocity = if (isAccent) ACCENT_VELOCITY else DEFAULT_VELOCITY
        val buffer = byteArrayOf(
            (0x90 or (channel and 0x0F)).toByte(), // Note On
            triggerNotes[channel % 6].toByte(), // Generic Configurable Note
            velocity.toByte()
        )
        inputPort?.send(buffer, 0, buffer.size)
    }

    /**
     * Send CC through throttler to prevent MIDI buffer overflow.
     * Message is queued and will be sent on next processQueue() call.
     */
    fun queueCC(channel: Int, cc: Int, value: Int): Boolean {
        return throttler.queueCC(channel, cc, value)
    }

    /**
     * Send CC immediately (bypass throttler - use sparingly!)
     * Only use for critical parameter changes that must happen now.
     */
    fun sendCCImmediate(channel: Int, cc: Int, value: Int) {
        val buffer = byteArrayOf(
            (0xB0 or (channel and 0x0F)).toByte(), // Control Change
            cc.toByte(),
            (value and 0x7F).toByte()
        )
        inputPort?.send(buffer, 0, buffer.size)
    }

    /**
     * Process throttled CC queue and send pending messages.
     * Call this regularly from your sequencer thread (every 10-20ms).
     */
    fun processCCQueue() {
        val messages = throttler.processQueue()
        messages.forEach { msg ->
            sendCCImmediate(msg.channel, msg.cc, msg.value)
        }
    }

    /**
     * Clear all queued CC messages
     */
    fun clearCCQueue() {
        throttler.clear()
    }

    /**
     * Get current CC queue depth for diagnostics
     */
    fun getCCQueueSize(): Int = throttler.getQueueSize()

    fun setLayerBalance(channel: Int, balance: Int) {
        // balance 0-127. 0 = Layer 1 fully, 127 = Layer 2 fully.
        queueCC(channel, CC_LEVEL_1, 127 - balance)
        queueCC(channel, CC_LEVEL_2, balance)
    }

    fun setResonator(decay: Int, body: Int) {
        // Volca Drum waveguide is global usually, or per-part.
        // Assuming global for simplicity as a performance macro.
        // Queue these to avoid flooding the buffer
        for (ch in 0..5) {
            queueCC(ch, CC_RESONATOR_DECAY, decay)
            queueCC(ch, CC_RESONATOR_BODY, body)
        }
    }
}
