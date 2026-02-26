package com.volcagrids.midi

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper

class MidiDeviceScanner(private val context: Context) {
    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
    
    fun findVolcaAndConnect(onConnected: (MidiDevice) -> Unit) {
        val infos = midiManager.devices
        for (info in infos) {
            // Logic to identify Volca Drum by name or product ID
            if (info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)?.contains("volca", ignoreCase = true) == true) {
                midiManager.openDevice(info, { device ->
                    onConnected(device)
                }, Handler(Looper.getMainLooper()))
                return
            }
        }
    }
}
