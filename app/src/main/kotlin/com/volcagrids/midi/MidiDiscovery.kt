package com.volcagrids.midi

import android.content.Context
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper

class MidiDiscovery(context: Context) {
    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
    
    fun getAvailableDevices(): List<MidiDeviceInfo> {
        return midiManager.devices.toList()
    }

    fun openDevice(info: MidiDeviceInfo, onOpened: (android.media.midi.MidiDevice) -> Unit) {
        midiManager.openDevice(info, { device ->
            onOpened(device)
        }, Handler(Looper.getMainLooper()))
    }
}
