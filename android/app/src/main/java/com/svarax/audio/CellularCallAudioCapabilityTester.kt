package com.svarax.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlin.math.sqrt

/**
 * CellularCallAudioCapabilityTester:
 * Performs legitimate, unprivileged capability testing of available Android AudioSources
 * and telephony routing devices during an active cellular call.
 *
 * Explicit constraints:
 * - Does NOT attempt to bypass Android security.
 * - Does NOT use undocumented APIs.
 * - Does NOT request or claim privileged permissions (such as CAPTURE_AUDIO_OUTPUT).
 * - Does NOT automatically enable speakerphone.
 * - Distinguishes LOCAL MICROPHONE, REMOTE CALLER AUDIO, BOTH, and UNKNOWN.
 * - Strictly answers whether the physical device exposes remote cellular caller audio.
 */
class CellularCallAudioCapabilityTester(private val context: Context) {

    companion object {
        private const val TAG = "SvaraX_CallCapability"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val TEST_DURATION_PER_SOURCE_MS = 2500L // 2.5s per source
    }

    enum class TestResultStatus {
        USABLE,
        SILENCE,
        PERMISSION_DENIED,
        UNSUPPORTED,
        PLATFORM_LIMITED
    }

    enum class LikelySignalClassification {
        LOCAL_MICROPHONE,
        REMOTE_CALLER_AUDIO,
        BOTH,
        UNKNOWN
    }

    data class SourceReport(
        val sourceName: String,
        val audioSourceConstant: Int?,
        val initSuccess: Boolean,
        val exceptionMessage: String?,
        val samplesRead: Long,
        val nonZeroSamples: Long,
        val rms: Double,
        val signalState: String, // SILENCE or SIGNAL_PRESENT
        val audioDevice: String,
        val audioMode: String,
        val likelySignal: LikelySignalClassification,
        val resultStatus: TestResultStatus,
        val limitationSummary: String
    )

    data class TelephonyDeviceInfo(
        val audioMode: String,
        val isSpeakerphoneOn: Boolean,
        val hasTelephonyInput: Boolean,
        val hasTelephonyOutput: Boolean,
        val allInputDevices: List<String>,
        val allOutputDevices: List<String>,
        val canThirdPartyBindRouting: Boolean,
        val summaryNote: String
    )

    data class FullDiagnosticReport(
        val isCellularCallActive: Boolean,
        val audioMode: String,
        val isSpeakerphoneOn: Boolean,
        val sourceReports: List<SourceReport>,
        val telephonyDeviceInfo: TelephonyDeviceInfo,
        val canExposeRemoteCallerAudio: String, // "NO", "YES", or "DEVICE/OEM DEPENDENT"
        val explicitArchitectureStatement: String
    )

    @SuppressLint("MissingPermission")
    fun runDiagnostic(
        onProgress: (stepName: String, progressPercent: Int) -> Unit
    ): FullDiagnosticReport {
        Log.i(TAG, "Starting Cellular Call Audio Capability Diagnostic...")
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val currentAudioMode = when (audioManager.mode) {
            AudioManager.MODE_IN_CALL -> "MODE_IN_CALL"
            AudioManager.MODE_IN_COMMUNICATION -> "MODE_IN_COMMUNICATION"
            AudioManager.MODE_RINGTONE -> "MODE_RINGTONE"
            AudioManager.MODE_NORMAL -> "MODE_NORMAL"
            else -> "OTHER (${audioManager.mode})"
        }
        val isSpeakerOn = audioManager.isSpeakerphoneOn
        val isCallActive = (audioManager.mode == AudioManager.MODE_IN_CALL || audioManager.mode == AudioManager.MODE_IN_COMMUNICATION)

        Log.i(TAG, "Active environment: mode=$currentAudioMode, speakerphoneOn=$isSpeakerOn, isCallActive=$isCallActive")

        val reports = mutableListOf<SourceReport>()

        // 1. Test MediaRecorder.AudioSource.MIC
        onProgress("Testing 1/5: AudioSource.MIC", 10)
        reports.add(
            testAudioSource(
                sourceName = "MIC",
                sourceId = MediaRecorder.AudioSource.MIC,
                audioManager = audioManager,
                inherentClassification = LikelySignalClassification.LOCAL_MICROPHONE,
                limitation = "Captures local Phone A microphone & ambient acoustic sound only. Remote caller earpiece audio is physically and logically isolated by Android OS."
            )
        )

        // 2. Test MediaRecorder.AudioSource.VOICE_COMMUNICATION
        onProgress("Testing 2/5: AudioSource.VOICE_COMMUNICATION", 30)
        reports.add(
            testAudioSource(
                sourceName = "VOICE_COMMUNICATION",
                sourceId = MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                audioManager = audioManager,
                inherentClassification = LikelySignalClassification.LOCAL_MICROPHONE,
                limitation = "Applies Acoustic Echo Cancellation (AEC) and Noise Suppression to local mic for VoIP. Does not mix or tap cellular downlink."
            )
        )

        // 3. Test MediaRecorder.AudioSource.VOICE_UPLINK
        onProgress("Testing 3/5: AudioSource.VOICE_UPLINK", 50)
        reports.add(
            testAudioSource(
                sourceName = "VOICE_UPLINK",
                sourceId = MediaRecorder.AudioSource.VOICE_UPLINK,
                audioManager = audioManager,
                inherentClassification = LikelySignalClassification.LOCAL_MICROPHONE,
                limitation = "Transmitted cellular uplink stream. Restricted or unsupported for third-party apps on standard Android firmware."
            )
        )

        // 4. Test MediaRecorder.AudioSource.VOICE_DOWNLINK
        onProgress("Testing 4/5: AudioSource.VOICE_DOWNLINK", 70)
        reports.add(
            testAudioSource(
                sourceName = "VOICE_DOWNLINK",
                sourceId = MediaRecorder.AudioSource.VOICE_DOWNLINK,
                audioManager = audioManager,
                inherentClassification = LikelySignalClassification.UNKNOWN,
                limitation = "Direct remote caller cellular audio stream. Strictly guarded by Android OS security; requires system-privileged CAPTURE_AUDIO_OUTPUT permission."
            )
        )

        // 5. Query TYPE_TELEPHONY and routing device information
        onProgress("Testing 5/5: TYPE_TELEPHONY Device Routing", 90)
        val telephonyInfo = queryTelephonyRouting(audioManager)

        // Add pseudo-source report for TYPE_TELEPHONY
        reports.add(
            SourceReport(
                sourceName = "TYPE_TELEPHONY Routing",
                audioSourceConstant = null,
                initSuccess = telephonyInfo.hasTelephonyInput || telephonyInfo.hasTelephonyOutput,
                exceptionMessage = if (!telephonyInfo.canThirdPartyBindRouting) "Routing binding restricted to system telephony stack" else null,
                samplesRead = 0,
                nonZeroSamples = 0,
                rms = 0.0,
                signalState = "SILENCE",
                audioDevice = if (telephonyInfo.hasTelephonyInput) "AudioDeviceInfo.TYPE_TELEPHONY (Present)" else "TYPE_TELEPHONY (Absent / Not Exposed)",
                audioMode = currentAudioMode,
                likelySignal = LikelySignalClassification.UNKNOWN,
                resultStatus = TestResultStatus.PLATFORM_LIMITED,
                limitationSummary = telephonyInfo.summaryNote
            )
        )

        onProgress("Diagnostic Complete", 100)

        // Determine if remote caller audio can be exposed
        val downlinkReport = reports.firstOrNull { it.sourceName == "VOICE_DOWNLINK" }
        val canExpose: String
        val statement: String

        if (downlinkReport != null && downlinkReport.initSuccess && downlinkReport.nonZeroSamples > 0 && downlinkReport.rms > 100.0) {
            // Note: Even if RMS is non-zero on MIC, we DO NOT claim remote caller audio!
            // Only if VOICE_DOWNLINK genuinely captures audio would it be exposed.
            canExpose = "DEVICE/OEM DEPENDENT"
            statement = "This device OEM has non-standard audio HAL behavior that permitted AudioSource.VOICE_DOWNLINK initialization without standard CAPTURE_AUDIO_OUTPUT rejection."
        } else {
            canExpose = "NO"
            statement = "The current Svara_X APK cannot implement silent-earpiece cellular call transcription using ordinary third-party Android APIs. Android OS security architecture restricts CAPTURE_AUDIO_OUTPUT to system-signed firmware dialers, and standard AudioSource.MIC only captures local microphone audio when speakerphone is disabled."
        }

        Log.i(TAG, "Final Determination: canExposeRemoteCallerAudio=$canExpose")

        return FullDiagnosticReport(
            isCellularCallActive = isCallActive,
            audioMode = currentAudioMode,
            isSpeakerphoneOn = isSpeakerOn,
            sourceReports = reports,
            telephonyDeviceInfo = telephonyInfo,
            canExposeRemoteCallerAudio = canExpose,
            explicitArchitectureStatement = statement
        )
    }

    @SuppressLint("MissingPermission")
    private fun testAudioSource(
        sourceName: String,
        sourceId: Int,
        audioManager: AudioManager,
        inherentClassification: LikelySignalClassification,
        limitation: String
    ): SourceReport {
        Log.d(TAG, "Testing AudioSource: $sourceName ($sourceId)...")

        val currentAudioMode = when (audioManager.mode) {
            AudioManager.MODE_IN_CALL -> "MODE_IN_CALL"
            AudioManager.MODE_IN_COMMUNICATION -> "MODE_IN_COMMUNICATION"
            AudioManager.MODE_RINGTONE -> "MODE_RINGTONE"
            AudioManager.MODE_NORMAL -> "MODE_NORMAL"
            else -> "OTHER (${audioManager.mode})"
        }

        var audioRecord: AudioRecord? = null
        var initSuccess = false
        var exceptionMessage: String? = null
        var totalSamplesRead = 0L
        var totalNonZeroSamples = 0L
        var sumSquares = 0.0
        var audioDeviceName = "Default Audio HAL"
        var resultStatus = TestResultStatus.UNSUPPORTED

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                exceptionMessage = "getMinBufferSize failed (ERROR_BAD_VALUE or unsupported source)"
                resultStatus = TestResultStatus.UNSUPPORTED
            } else {
                val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)
                audioRecord = AudioRecord(
                    sourceId,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                    initSuccess = true

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val routed = audioRecord.routedDevice
                        if (routed != null) {
                            audioDeviceName = "${routed.productName} (Type: ${getDeviceTypeName(routed.type)})"
                        }
                    }

                    // Start recording and sample for duration
                    audioRecord.startRecording()
                    if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        val shortBuffer = ShortArray(bufferSize / 2)
                        val endTime = System.currentTimeMillis() + TEST_DURATION_PER_SOURCE_MS

                        while (System.currentTimeMillis() < endTime) {
                            val read = audioRecord.read(shortBuffer, 0, shortBuffer.size)
                            if (read > 0) {
                                totalSamplesRead += read
                                for (i in 0 until read) {
                                    val s = shortBuffer[i]
                                    if (s != 0.toShort()) {
                                        totalNonZeroSamples++
                                    }
                                    sumSquares += s.toDouble() * s.toDouble()
                                }
                            } else if (read < 0) {
                                Log.w(TAG, "AudioRecord.read returned error code: $read")
                                break
                            }
                        }

                        try {
                            audioRecord.stop()
                        } catch (ignored: Exception) {}
                    } else {
                        exceptionMessage = "Failed to transition to RECORDSTATE_RECORDING"
                        resultStatus = TestResultStatus.PLATFORM_LIMITED
                    }
                } else {
                    initSuccess = false
                    exceptionMessage = "AudioRecord state != STATE_INITIALIZED (AudioSource rejected by AudioFlinger/HAL)"
                    resultStatus = if (sourceId == MediaRecorder.AudioSource.VOICE_DOWNLINK || sourceId == MediaRecorder.AudioSource.VOICE_UPLINK) {
                        TestResultStatus.PERMISSION_DENIED
                    } else {
                        TestResultStatus.UNSUPPORTED
                    }
                }
            }
        } catch (se: SecurityException) {
            initSuccess = false
            exceptionMessage = "SecurityException: ${se.message ?: "Permission denied by OS"}"
            resultStatus = TestResultStatus.PERMISSION_DENIED
            Log.e(TAG, "SecurityException while probing $sourceName", se)
        } catch (e: Exception) {
            initSuccess = false
            exceptionMessage = "${e.javaClass.simpleName}: ${e.message ?: "Initialization exception"}"
            resultStatus = TestResultStatus.PLATFORM_LIMITED
            Log.e(TAG, "Exception while probing $sourceName", e)
        } finally {
            try {
                audioRecord?.stop()
            } catch (ignored: Exception) {}
            try {
                audioRecord?.release()
            } catch (ignored: Exception) {}
        }

        val rms = if (totalSamplesRead > 0) sqrt(sumSquares / totalSamplesRead) else 0.0
        val signalState = if (totalNonZeroSamples > 0 && rms > 25.0) "SIGNAL_PRESENT" else "SILENCE"

        if (initSuccess) {
            resultStatus = if (signalState == "SIGNAL_PRESENT") {
                TestResultStatus.USABLE
            } else {
                TestResultStatus.SILENCE
            }
        }

        // Signal classification rule:
        // NEVER classify MIC as REMOTE_CALLER_AUDIO.
        val likelySignal = when (sourceId) {
            MediaRecorder.AudioSource.MIC, MediaRecorder.AudioSource.VOICE_COMMUNICATION -> {
                if (signalState == "SIGNAL_PRESENT") LikelySignalClassification.LOCAL_MICROPHONE else LikelySignalClassification.UNKNOWN
            }
            MediaRecorder.AudioSource.VOICE_UPLINK -> {
                if (signalState == "SIGNAL_PRESENT") LikelySignalClassification.LOCAL_MICROPHONE else LikelySignalClassification.UNKNOWN
            }
            MediaRecorder.AudioSource.VOICE_DOWNLINK -> {
                if (signalState == "SIGNAL_PRESENT") LikelySignalClassification.REMOTE_CALLER_AUDIO else LikelySignalClassification.UNKNOWN
            }
            else -> LikelySignalClassification.UNKNOWN
        }

        Log.i(TAG, "Source: $sourceName | Init: $initSuccess | Samples: $totalSamplesRead | NonZero: $totalNonZeroSamples | RMS: %.2f | Status: $resultStatus".format(rms))

        return SourceReport(
            sourceName = sourceName,
            audioSourceConstant = sourceId,
            initSuccess = initSuccess,
            exceptionMessage = exceptionMessage,
            samplesRead = totalSamplesRead,
            nonZeroSamples = totalNonZeroSamples,
            rms = rms,
            signalState = signalState,
            audioDevice = audioDeviceName,
            audioMode = currentAudioMode,
            likelySignal = likelySignal,
            resultStatus = resultStatus,
            limitationSummary = limitation
        )
    }

    private fun queryTelephonyRouting(audioManager: AudioManager): TelephonyDeviceInfo {
        val currentAudioMode = when (audioManager.mode) {
            AudioManager.MODE_IN_CALL -> "MODE_IN_CALL"
            AudioManager.MODE_IN_COMMUNICATION -> "MODE_IN_COMMUNICATION"
            AudioManager.MODE_RINGTONE -> "MODE_RINGTONE"
            AudioManager.MODE_NORMAL -> "MODE_NORMAL"
            else -> "OTHER (${audioManager.mode})"
        }
        val isSpeakerOn = audioManager.isSpeakerphoneOn

        var hasTelephonyIn = false
        var hasTelephonyOut = false
        val inputList = mutableListOf<String>()
        val outputList = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                for (dev in inputDevices) {
                    val desc = "${dev.productName} (type ${getDeviceTypeName(dev.type)})"
                    inputList.add(desc)
                    if (dev.type == AudioDeviceInfo.TYPE_TELEPHONY) {
                        hasTelephonyIn = true
                    }
                }

                val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                for (dev in outputDevices) {
                    val desc = "${dev.productName} (type ${getDeviceTypeName(dev.type)})"
                    outputList.add(desc)
                    if (dev.type == AudioDeviceInfo.TYPE_TELEPHONY) {
                        hasTelephonyOut = true
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error enumerating AudioDeviceInfo devices", e)
            }
        }

        val note = if (hasTelephonyIn || hasTelephonyOut) {
            "Hardware modem exposes TYPE_TELEPHONY endpoints to Android AudioService, but third-party applications are barred by SELinux and permissions from binding audio routes directly to the telephony modem bus."
        } else {
            "TYPE_TELEPHONY routing descriptor is not exposed to third-party applications in this OEM's public audio device catalog."
        }

        return TelephonyDeviceInfo(
            audioMode = currentAudioMode,
            isSpeakerphoneOn = isSpeakerOn,
            hasTelephonyInput = hasTelephonyIn,
            hasTelephonyOutput = hasTelephonyOut,
            allInputDevices = inputList,
            allOutputDevices = outputList,
            canThirdPartyBindRouting = false,
            summaryNote = note
        )
    }

    private fun getDeviceTypeName(type: Int): String {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "TYPE_BUILTIN_EARPIECE"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "TYPE_BUILTIN_SPEAKER"
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "TYPE_BUILTIN_MIC"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "TYPE_BLUETOOTH_SCO"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "TYPE_BLUETOOTH_A2DP"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "TYPE_WIRED_HEADSET"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "TYPE_WIRED_HEADPHONES"
            AudioDeviceInfo.TYPE_TELEPHONY -> "TYPE_TELEPHONY"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "TYPE_USB_DEVICE"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "TYPE_USB_HEADSET"
            else -> "TYPE_CODE_$type"
        }
    }
}
