import React, { useState } from 'react';
import {
  ChevronLeft,
  ChevronDown,
  ChevronUp,
  Radio,
  Mic,
  Volume2,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Play,
  Square,
  Copy,
  Check,
  Terminal,
  Info
} from 'lucide-react';
import { CellularCallAudioCapabilityDiagnostic } from './CellularCallAudioCapabilityDiagnostic';
import { AudioMetrics, AudioTestResult } from '../types';

interface DiagnosticsViewProps {
  onBack: () => void;
  // Audio Record Test Props
  isAudioRecording: boolean;
  audioElapsedSeconds: number;
  audioTargetSeconds: number;
  audioMetrics: AudioMetrics;
  audioTestResult: AudioTestResult | null;
  selectedDurationOption: number | 'custom';
  customDurationInput: string;
  onSelectDurationOption: (opt: number | 'custom') => void;
  onChangeCustomDuration: (val: string) => void;
  onStartAudioRecording: () => void;
  onStopAudioRecording: () => void;
  // Speech Recognition Test Props
  isSpeechTesting: boolean;
  speechStatus: string;
  partialSpeechTranscript: string;
  finalSpeechTranscript: string;
  onStartSpeechTesting: () => void;
  onStopSpeechTesting: () => void;
}

export function DiagnosticsView({
  onBack,
  isAudioRecording,
  audioElapsedSeconds,
  audioTargetSeconds,
  audioMetrics,
  audioTestResult,
  selectedDurationOption,
  customDurationInput,
  onSelectDurationOption,
  onChangeCustomDuration,
  onStartAudioRecording,
  onStopAudioRecording,
  isSpeechTesting,
  speechStatus,
  partialSpeechTranscript,
  finalSpeechTranscript,
  onStartSpeechTesting,
  onStopSpeechTesting
}: DiagnosticsViewProps) {
  const [showTechnicalDetails, setShowTechnicalDetails] = useState(false);
  const [copiedLogcat, setCopiedLogcat] = useState(false);

  const copyLogcatCommand = () => {
    navigator.clipboard.writeText('adb logcat | grep -E "Svara|AudioRecord|FraudDetector|RiskEngine"');
    setCopiedLogcat(true);
    setTimeout(() => setCopiedLogcat(false), 2000);
  };

  return (
    <div id="view_diagnostics" className="space-y-4">
      {/* HEADER WITH BACK BUTTON */}
      <div className="flex items-center justify-between px-1 pt-1">
        <button
          onClick={onBack}
          className="text-xs font-semibold text-[#4F7CFF] hover:text-[#3d68e6] flex items-center gap-1 transition-colors"
        >
          <ChevronLeft className="w-4 h-4" />
          <span>Back to Settings</span>
        </button>
        <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700">
          ADVANCED
        </span>
      </div>

      <div>
        <h2 className="text-xl font-bold text-slate-100 tracking-tight">Audio Diagnostics</h2>
        <p className="text-xs text-slate-400 mt-0.5">Physical device capabilities and hardware audio probe</p>
      </div>

      {/* 1. AUDIO CAPABILITIES (CLEAN CONSUMER OVERVIEW) */}
      <div className="p-4 rounded-2xl bg-[#151E2E] border border-slate-800 space-y-3 shadow-sm">
        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-300">
          Audio capabilities
        </h3>

        <div className="space-y-2 text-xs">
          {/* Microphone */}
          <div className="p-3 rounded-xl bg-[#1C2638] border border-slate-800/80 flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <Mic className="w-4 h-4 text-emerald-400" />
              <div>
                <p className="font-semibold text-slate-200">Microphone</p>
                <p className="text-[11px] text-slate-400">16kHz 16-bit PCM AudioRecord</p>
              </div>
            </div>
            <span className="text-[11px] font-bold text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded-md">
              Available
            </span>
          </div>

          {/* Speech Recognition */}
          <div className="p-3 rounded-xl bg-[#1C2638] border border-slate-800/80 flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <Radio className="w-4 h-4 text-emerald-400" />
              <div>
                <p className="font-semibold text-slate-200">Speech recognition</p>
                <p className="text-[11px] text-slate-400">Android SpeechRecognizer engine</p>
              </div>
            </div>
            <span className="text-[11px] font-bold text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded-md">
              Available
            </span>
          </div>

          {/* Call Detection */}
          <div className="p-3 rounded-xl bg-[#1C2638] border border-slate-800/80 flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              <div>
                <p className="font-semibold text-slate-200">Call detection</p>
                <p className="text-[11px] text-slate-400">Telecom call screening service</p>
              </div>
            </div>
            <span className="text-[11px] font-bold text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded-md">
              Available
            </span>
          </div>

          {/* Remote Caller Audio */}
          <div className="p-3 rounded-xl bg-[#1C2638] border border-slate-800/80 flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <Volume2 className="w-4 h-4 text-amber-400" />
              <div>
                <p className="font-semibold text-slate-200">Remote caller audio</p>
                <p className="text-[11px] text-slate-400">Android cellular isolation architecture</p>
              </div>
            </div>
            <span className="text-[11px] font-bold text-amber-400 bg-amber-500/10 border border-amber-500/20 px-2 py-0.5 rounded-md">
              Unavailable on this device
            </span>
          </div>
        </div>

        <p className="text-[11px] text-slate-400 leading-relaxed pt-1">
          Android does not expose the direct remote cellular audio stream to third-party applications on standard production firmware. Svara_X analyzes audio received through the microphone.
        </p>
      </div>

      {/* 2. INTERACTIVE HARDWARE PROBES */}
      <div className="p-4 rounded-2xl bg-[#151E2E] border border-slate-800 space-y-4 shadow-sm">
        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-300">
          Acoustic &amp; Speech Hardware Verification
        </h3>

        {/* PROBE A: AUDIO RECORD TEST */}
        <div className="p-3.5 rounded-xl bg-[#1C2638] border border-slate-800/80 space-y-3">
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-xs font-bold text-slate-200">Microphone AudioRecord Test</h4>
              <p className="text-[11px] text-slate-400">Verifies physical microphone PCM sampling &amp; RMS</p>
            </div>
            <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
              isAudioRecording ? 'bg-emerald-500/20 text-emerald-400' : 'bg-slate-800 text-slate-400'
            }`}>
              {isAudioRecording ? 'RECORDING' : 'IDLE'}
            </span>
          </div>

          {/* Duration Selector */}
          <div className="grid grid-cols-5 gap-1 pt-1">
            {[5, 10, 30, 60, 'custom' as const].map((opt) => (
              <button
                key={opt.toString()}
                type="button"
                disabled={isAudioRecording}
                onClick={() => onSelectDurationOption(opt)}
                className={`py-1 text-[11px] font-semibold rounded-lg border text-center transition-colors ${
                  selectedDurationOption === opt
                    ? 'bg-[#4F7CFF] text-white border-[#4F7CFF]'
                    : 'bg-slate-900 text-slate-400 border-slate-800 hover:text-slate-200'
                }`}
              >
                {typeof opt === 'number' ? `${opt}s` : 'Custom'}
              </button>
            ))}
          </div>

          {selectedDurationOption === 'custom' && (
            <div className="flex items-center gap-2 pt-1 text-xs">
              <span className="text-slate-400">Duration (sec):</span>
              <input
                type="number"
                min={1}
                max={300}
                value={customDurationInput}
                onChange={(e) => onChangeCustomDuration(e.target.value)}
                className="w-20 px-2 py-1 bg-slate-900 border border-slate-700 rounded text-white text-xs font-mono"
              />
            </div>
          )}

          {/* Real-time Metrics when active */}
          {isAudioRecording && (
            <div className="p-3 rounded-xl bg-slate-950 border border-emerald-500/40 space-y-2">
              <div className="flex items-center justify-between text-xs">
                <span className="text-emerald-400 font-bold">Elapsed: {audioElapsedSeconds}s / {audioTargetSeconds}s</span>
                <span className="text-slate-400 font-mono text-[11px]">RMS: {audioMetrics.currentRms}</span>
              </div>
              <div className="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                <div
                  className="bg-emerald-400 h-full transition-all duration-100"
                  style={{ width: `${Math.min(100, (audioElapsedSeconds / audioTargetSeconds) * 100)}%` }}
                />
              </div>
            </div>
          )}

          {audioTestResult && (
            <div className="p-2.5 rounded-xl bg-emerald-950/30 border border-emerald-800/40 text-[11px] text-emerald-300">
              ✓ Test complete: {audioTestResult.duration.toFixed(1)}s recorded, {audioTestResult.samples.toLocaleString()} samples, Peak RMS: {audioTestResult.peakRms.toFixed(1)}.
            </div>
          )}

          <div>
            {!isAudioRecording ? (
              <button
                id="btn_start_audio_record"
                onClick={onStartAudioRecording}
                className="w-full py-2 rounded-xl bg-[#4F7CFF] hover:bg-[#3d68e6] text-white font-semibold text-xs transition-colors flex items-center justify-center gap-1.5"
              >
                <Play className="w-3.5 h-3.5 fill-current" />
                <span>Start Audio Record Test</span>
              </button>
            ) : (
              <button
                id="btn_stop_audio_record"
                onClick={onStopAudioRecording}
                className="w-full py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-semibold text-xs transition-colors flex items-center justify-center gap-1.5"
              >
                <Square className="w-3.5 h-3.5 fill-current" />
                <span>Stop Recording</span>
              </button>
            )}
          </div>
        </div>

        {/* PROBE B: SPEECH RECOGNITION TEST */}
        <div className="p-3.5 rounded-xl bg-[#1C2638] border border-slate-800/80 space-y-3">
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-xs font-bold text-slate-200">Speech Recognition Test</h4>
              <p className="text-[11px] text-slate-400">Verifies Android on-device speech recognizer callbacks</p>
            </div>
            <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-900 text-slate-300">
              {speechStatus}
            </span>
          </div>

          <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 text-[11px] font-mono text-slate-300 space-y-1">
            <p className="text-slate-500 text-[10px]">Partial: {partialSpeechTranscript || '—'}</p>
            <p className="text-white font-sans text-xs">"{finalSpeechTranscript}"</p>
          </div>

          <div>
            {!isSpeechTesting ? (
              <button
                id="btn_start_speech_test"
                onClick={onStartSpeechTesting}
                className="w-full py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-xs transition-colors border border-slate-700 flex items-center justify-center gap-1.5"
              >
                <Mic className="w-3.5 h-3.5 text-[#4F7CFF]" />
                <span>Test Speech Recognition</span>
              </button>
            ) : (
              <button
                id="btn_stop_speech_test"
                onClick={onStopSpeechTesting}
                className="w-full py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-xs transition-colors border border-slate-700"
              >
                Complete Speech Test
              </button>
            )}
          </div>
        </div>
      </div>

      {/* 3. EXPANDABLE TECHNICAL DETAILS (PRESERVING FULL ARCHITECTURAL DIAGNOSTIC) */}
      <div className="rounded-2xl border border-slate-800 bg-[#151E2E] overflow-hidden">
        <button
          onClick={() => setShowTechnicalDetails(!showTechnicalDetails)}
          className="w-full p-4 flex items-center justify-between text-left hover:bg-[#1C2638]/60 transition-colors"
        >
          <div className="space-y-0.5">
            <h3 className="text-xs font-bold text-slate-200">Technical details</h3>
            <p className="text-[11px] text-slate-400">
              Cellular AudioSource matrix, permissions, RMS samples, and logcat
            </p>
          </div>
          <div className="text-slate-500 pl-2">
            {showTechnicalDetails ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </div>
        </button>

        {showTechnicalDetails && (
          <div className="p-4 pt-2 border-t border-slate-800 space-y-4">
            {/* LOGCAT FILTER REFERENCE */}
            <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between gap-2">
              <div className="min-w-0 font-mono text-[11px]">
                <span className="text-slate-500 block text-[9px] uppercase font-bold">ADB Logcat Filter:</span>
                <code className="text-[#4F7CFF] truncate block">adb logcat | grep -E "Svara|AudioRecord|FraudDetector"</code>
              </div>
              <button
                onClick={copyLogcatCommand}
                className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs shrink-0 flex items-center gap-1 border border-slate-700"
              >
                {copiedLogcat ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copiedLogcat ? 'Copied' : 'Copy'}</span>
              </button>
            </div>

            {/* EMBEDDED CELLULAR CAPABILITY DIAGNOSTIC */}
            <CellularCallAudioCapabilityDiagnostic />
          </div>
        )}
      </div>
    </div>
  );
}
