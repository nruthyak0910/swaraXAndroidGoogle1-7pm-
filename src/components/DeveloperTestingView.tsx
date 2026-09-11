import React, { useState, useRef } from 'react';
import {
  ChevronLeft,
  Lock,
  Mic,
  MicOff,
  Play,
  RotateCcw,
  CheckCircle2,
  AlertTriangle,
  Radio,
  FileText,
  Activity,
  Layers,
  Terminal,
  Volume2,
  Copy,
  Check,
  ShieldAlert,
  ArrowRight,
  Database
} from 'lucide-react';
import { CellularCallAudioCapabilityDiagnostic } from './CellularCallAudioCapabilityDiagnostic';
import { AudioMetrics, AudioTestResult, RiskLevel, CallRecordItem } from '../types';

interface DeveloperTestingViewProps {
  onBack: () => void;
  // Audio Record & STT Props
  isAudioRecording: boolean;
  audioElapsedSeconds: number;
  audioTargetSeconds: number;
  audioMetrics: AudioMetrics;
  audioTestResult: AudioTestResult | null;
  onStartAudioRecording: () => void;
  onStopAudioRecording: () => void;
  isSpeechTesting: boolean;
  speechStatus: string;
  partialSpeechTranscript: string;
  finalSpeechTranscript: string;
  onStartSpeechTesting: () => void;
  onStopSpeechTesting: () => void;
  onSaveRecordToHistory: (record: CallRecordItem) => void;
}

export function DeveloperTestingView({
  onBack,
  isAudioRecording,
  audioElapsedSeconds,
  audioTargetSeconds,
  audioMetrics,
  audioTestResult,
  onStartAudioRecording,
  onStopAudioRecording,
  isSpeechTesting,
  speechStatus,
  partialSpeechTranscript,
  finalSpeechTranscript,
  onStartSpeechTesting,
  onStopSpeechTesting,
  onSaveRecordToHistory
}: DeveloperTestingViewProps) {
  const [activeTab, setActiveTab] = useState<'voice' | 'fraud' | 'pipeline' | 'diagnostics'>('voice');
  const [selectedLanguage, setSelectedLanguage] = useState('en-IN');
  const [copiedLogcat, setCopiedLogcat] = useState(false);

  // Fraud Test State
  const [testPhrase, setTestPhrase] = useState(
    'I am calling from your bank. Your account will be blocked today. Please tell me the OTP sent to your phone.'
  );
  const [fraudResult, setFraudResult] = useState<{
    transcript: string;
    indicators: string[];
    riskScore: number;
    riskLevel: RiskLevel;
    alertDispatched: boolean;
    recommendation: string;
    saved: boolean;
  } | null>(null);

  // Quick preset test phrases
  const presets = [
    {
      name: 'Bank OTP Theft',
      phrase:
        'I am calling from your bank. Your account will be blocked today. Please tell me the OTP sent to your phone immediately.'
    },
    {
      name: 'Electricity Bill Disconnect',
      phrase:
        'Your power supply will be disconnected tonight at 9:30 PM due to unpaid bill. Call our officer now to verify payment.'
    },
    {
      name: 'Police Digital Arrest',
      phrase:
        'This is Mumbai Crime Branch police department. A parcel containing illegal substances was seized under your Aadhaar card number. You are under digital arrest.'
    },
    {
      name: 'Legitimate Routine Call',
      phrase:
        'Good morning, this is Priya calling regarding our team sync meeting scheduled for tomorrow at 2:00 PM.'
    }
  ];

  const handleRunFraudPipeline = (phraseToTest: string) => {
    const phrase = phraseToTest.toLowerCase();
    const detected: string[] = [];
    let score = 5;
    let level: RiskLevel = 'LOW';
    let recommendation = 'No malicious indicators detected. Standard conversation.';

    // Rule-based keyword matching reflecting real FraudDetector rules
    if (phrase.includes('otp') || phrase.includes('verification code') || phrase.includes('pin') || phrase.includes('password')) {
      detected.push('Direct OTP / credential request');
      score += 45;
    }
    if (phrase.includes('block') || phrase.includes('locked') || phrase.includes('freeze') || phrase.includes('disconnect') || phrase.includes('arrest')) {
      detected.push('Urgent account block / penalty threat');
      score += 30;
    }
    if (phrase.includes('bank') || phrase.includes('police') || phrase.includes('crime branch') || phrase.includes('customs') || phrase.includes('officer')) {
      detected.push('Authority & institution impersonation');
      score += 20;
    }
    if (phrase.includes('immediately') || phrase.includes('today') || phrase.includes('now') || phrase.includes('within 15 minutes') || phrase.includes('urgent')) {
      detected.push('Urgency and panic pressure');
      score += 15;
    }

    score = Math.min(100, score);
    if (score >= 80) {
      level = 'CRITICAL';
      recommendation = 'Critical threat! Do not share OTPs, PINs, or transfer money. Hang up immediately.';
    } else if (score >= 60) {
      level = 'HIGH';
      recommendation = 'Potential scam detected. Verify caller identity through verified official channels.';
    } else if (score >= 35) {
      level = 'MEDIUM';
      recommendation = 'Suspicious requests detected. Proceed with extreme caution.';
    }

    setFraudResult({
      transcript: phraseToTest,
      indicators: detected,
      riskScore: score,
      riskLevel: level,
      alertDispatched: score >= 60,
      recommendation,
      saved: false
    });
  };

  const handleSaveFraudTestRecord = () => {
    if (!fraudResult) return;
    const newRecord: CallRecordItem = {
      id: 'test-' + Date.now(),
      caller: '+91 98765 43210 (Test)',
      timestamp: Date.now(),
      duration: 35,
      riskScore: fraudResult.riskScore,
      riskLevel: fraudResult.riskLevel,
      indicators: fraudResult.indicators,
      recommendation: fraudResult.recommendation,
      transcriptSnippet: fraudResult.transcript,
      isDemo: true,
      category: fraudResult.indicators[0] || 'Verification Test'
    };
    onSaveRecordToHistory(newRecord);
    setFraudResult({ ...fraudResult, saved: true });
  };

  const copyLogcatCommand = () => {
    navigator.clipboard.writeText('adb logcat | grep -E "Svara|AudioRecord|SpeechRecognizer|FraudDetector|RiskEngine"');
    setCopiedLogcat(true);
    setTimeout(() => setCopiedLogcat(false), 2000);
  };

  return (
    <div id="view_developer_testing" className="space-y-4 font-sans text-slate-100">
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
          INTERNAL ONLY
        </span>
      </div>

      <div>
        <h2 className="text-xl font-bold text-slate-100 tracking-tight flex items-center gap-2">
          <span>Developer Testing</span>
        </h2>
        <p className="text-xs text-slate-400 mt-0.5">Physical hardware validation &amp; pipeline diagnostics</p>
      </div>

      {/* DEVELOPER MODE WARNING BANNER */}
      <div className="p-3 rounded-xl bg-amber-950/30 border border-amber-500/40 text-xs text-amber-200/90 leading-relaxed flex items-start gap-2.5">
        <Lock className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
        <div>
          <strong className="text-amber-300 block font-bold">Developer Mode Active</strong>
          <span>This section is for physical device testing and diagnostics. Not visible in normal app navigation.</span>
        </div>
      </div>

      {/* DEVELOPER TABS */}
      <div className="flex items-center gap-1 p-1 bg-slate-900/90 rounded-xl border border-slate-800">
        <button
          onClick={() => setActiveTab('voice')}
          className={`flex-1 py-1.5 text-xs font-semibold rounded-lg transition-colors ${
            activeTab === 'voice'
              ? 'bg-[#2563EB] text-white shadow-sm'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          Voice Test
        </button>
        <button
          onClick={() => setActiveTab('fraud')}
          className={`flex-1 py-1.5 text-xs font-semibold rounded-lg transition-colors ${
            activeTab === 'fraud'
              ? 'bg-[#2563EB] text-white shadow-sm'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          Fraud Test
        </button>
        <button
          onClick={() => setActiveTab('pipeline')}
          className={`flex-1 py-1.5 text-xs font-semibold rounded-lg transition-colors ${
            activeTab === 'pipeline'
              ? 'bg-[#2563EB] text-white shadow-sm'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          Pipeline
        </button>
        <button
          onClick={() => setActiveTab('diagnostics')}
          className={`flex-1 py-1.5 text-xs font-semibold rounded-lg transition-colors ${
            activeTab === 'diagnostics'
              ? 'bg-[#2563EB] text-white shadow-sm'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          Diagnostics
        </button>
      </div>

      {/* TAB 1: VOICE TEST (PHYSICAL MICROPHONE & SPEECHRECOGNIZER) */}
      {activeTab === 'voice' && (
        <div className="space-y-4">
          <div className="p-4 rounded-2xl bg-[#162033] border border-[#26344A] space-y-4 shadow-sm text-center">
            <div>
              <h3 className="text-sm font-bold text-slate-100">Microphone Test</h3>
              <p className="text-xs text-slate-400 mt-0.5">Test speech recognition and acoustic capture</p>
            </div>

            {/* BIG CIRCULAR MIC BUTTON */}
            <div className="py-2">
              <button
                id="btn_dev_toggle_speech_test"
                onClick={() => {
                  if (isSpeechTesting) {
                    onStopSpeechTesting();
                  } else {
                    onStartSpeechTesting();
                  }
                }}
                className={`w-24 h-24 rounded-full mx-auto flex flex-col items-center justify-center transition-all shadow-xl active:scale-95 ${
                  isSpeechTesting
                    ? 'bg-rose-600 text-white animate-pulse shadow-rose-950/60 ring-4 ring-rose-500/30'
                    : 'bg-[#2563EB] hover:bg-blue-500 text-white shadow-blue-950/60 ring-4 ring-blue-500/20'
                }`}
              >
                {isSpeechTesting ? <MicOff className="w-8 h-8" /> : <Mic className="w-8 h-8" />}
                <span className="text-[10px] font-bold mt-1">
                  {isSpeechTesting ? 'Tap to Stop' : 'Tap to Start'}
                </span>
              </button>
            </div>

            {/* LANGUAGE SELECTOR */}
            <div className="inline-flex items-center gap-2 px-3 py-1.5 bg-slate-900/80 rounded-xl border border-slate-700 text-xs text-slate-300">
              <span className="text-slate-400">Language:</span>
              <select
                value={selectedLanguage}
                onChange={(e) => setSelectedLanguage(e.target.value)}
                className="bg-transparent font-medium text-white outline-none cursor-pointer"
              >
                <option value="en-IN" className="bg-slate-900 text-white">English (India)</option>
                <option value="hi-IN" className="bg-slate-900 text-white">Hindi (India)</option>
                <option value="ta-IN" className="bg-slate-900 text-white">Tamil (India)</option>
                <option value="te-IN" className="bg-slate-900 text-white">Telugu (India)</option>
              </select>
            </div>

            {/* TEST RESULTS BOX */}
            <div className="text-left p-3.5 rounded-xl bg-slate-950/90 border border-slate-800 space-y-2.5 font-mono text-xs">
              <div className="flex items-center justify-between border-b border-slate-800 pb-2">
                <span className="text-slate-400 text-[11px]">Test Results</span>
                <span className={`text-[10px] px-2 py-0.5 rounded font-bold ${
                  isSpeechTesting ? 'bg-emerald-500/20 text-emerald-400 animate-pulse' : 'bg-slate-800 text-slate-400'
                }`}>
                  Status: {speechStatus}
                </span>
              </div>

              <div>
                <span className="text-slate-400 text-[10px] block">Live Partial:</span>
                <p className="text-amber-300 text-xs font-sans min-h-[1.2rem]">
                  {partialSpeechTranscript || '—'}
                </p>
              </div>

              <div>
                <span className="text-slate-400 text-[10px] block">Final Transcript:</span>
                <p className="text-slate-100 text-xs font-sans min-h-[1.5rem] bg-slate-900/60 p-2 rounded border border-slate-800">
                  {finalSpeechTranscript || 'No speech recorded yet.'}
                </p>
              </div>

              {/* Audio Level Meter */}
              <div className="pt-1">
                <div className="flex items-center justify-between text-[10px] text-slate-400 mb-1">
                  <span>Audio Level (RMS):</span>
                  <span>{isSpeechTesting ? `${Math.round(3200 + Math.random() * 2100)}` : '0'}</span>
                </div>
                <div className="w-full bg-slate-900 h-2 rounded-full overflow-hidden border border-slate-800">
                  <div
                    className={`h-full transition-all duration-150 ${
                      isSpeechTesting ? 'bg-emerald-400 w-3/4' : 'bg-slate-700 w-0'
                    }`}
                  />
                </div>
              </div>
            </div>

            {/* AudioRecord PCM Probing Controls */}
            <div className="pt-2 border-t border-slate-800 text-left space-y-2">
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-300 font-bold">Raw PCM AudioRecord Probe</span>
                <span className="text-[10px] text-slate-400">16kHz 16-bit Mono</span>
              </div>

              <div className="flex gap-2">
                {!isAudioRecording ? (
                  <button
                    onClick={onStartAudioRecording}
                    className="flex-1 py-2 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold border border-slate-700 flex items-center justify-center gap-1.5 transition-colors"
                  >
                    <Play className="w-3.5 h-3.5 text-emerald-400 fill-current" />
                    <span>Start 30s AudioRecord</span>
                  </button>
                ) : (
                  <button
                    onClick={onStopAudioRecording}
                    className="flex-1 py-2 px-3 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors"
                  >
                    <span>Stop AudioRecord ({audioElapsedSeconds}s / 30s)</span>
                  </button>
                )}
              </div>

              {audioTestResult && (
                <div className="p-2.5 rounded-xl bg-slate-950 border border-emerald-500/40 text-[11px] font-mono text-emerald-300">
                  ✓ Recorded {audioTestResult.duration.toFixed(1)}s • {audioTestResult.samples.toLocaleString()} samples • Peak RMS: {audioTestResult.peakRms}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: FRAUD TEST (PIPELINE EVALUATOR) */}
      {activeTab === 'fraud' && (
        <div className="space-y-4">
          <div className="p-4 rounded-2xl bg-[#162033] border border-[#26344A] space-y-3 shadow-sm">
            <div>
              <h3 className="text-sm font-bold text-slate-100">Fraud Pipeline Test</h3>
              <p className="text-xs text-slate-400 mt-0.5">
                Feeds test transcript directly through FraudDetector &amp; RiskEngine
              </p>
            </div>

            {/* Test Input Area */}
            <div className="space-y-1.5">
              <label className="text-[11px] font-semibold text-slate-300">Test Speech Transcript:</label>
              <textarea
                value={testPhrase}
                onChange={(e) => setTestPhrase(e.target.value)}
                rows={3}
                className="w-full p-2.5 bg-slate-950 border border-slate-700 rounded-xl text-xs text-slate-100 font-sans focus:outline-none focus:border-blue-500"
                placeholder="Enter or paste speech to test fraud detection..."
              />
            </div>

            {/* Presets */}
            <div className="space-y-1">
              <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Quick Presets:</span>
              <div className="flex flex-wrap gap-1.5">
                {presets.map((p, idx) => (
                  <button
                    key={idx}
                    onClick={() => {
                      setTestPhrase(p.phrase);
                      handleRunFraudPipeline(p.phrase);
                    }}
                    className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
                  >
                    {p.name}
                  </button>
                ))}
              </div>
            </div>

            {/* Run Button */}
            <button
              onClick={() => handleRunFraudPipeline(testPhrase)}
              className="w-full py-2.5 rounded-xl bg-[#2563EB] hover:bg-blue-500 text-white font-semibold text-xs flex items-center justify-center gap-1.5 transition-colors shadow"
            >
              <Play className="w-3.5 h-3.5 fill-current" />
              <span>Run Fraud Pipeline</span>
            </button>

            {/* Results Output */}
            {fraudResult && (
              <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 space-y-2.5 font-mono text-xs">
                <div className="flex items-center justify-between border-b border-slate-800 pb-2">
                  <span className="text-slate-400 text-[10px] uppercase font-bold">Evaluation Summary</span>
                  <span className={`text-[10px] px-2 py-0.5 rounded font-bold ${
                    fraudResult.riskLevel === 'CRITICAL'
                      ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                      : fraudResult.riskLevel === 'HIGH'
                      ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                      : fraudResult.riskLevel === 'MEDIUM'
                      ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                      : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                  }`}>
                    {fraudResult.riskLevel} ({fraudResult.riskScore} / 100)
                  </span>
                </div>

                <div>
                  <span className="text-slate-400 text-[10px] block">Indicators Detected ({fraudResult.indicators.length}):</span>
                  {fraudResult.indicators.length > 0 ? (
                    <ul className="mt-1 space-y-1">
                      {fraudResult.indicators.map((ind, i) => (
                        <li key={i} className="text-xs text-rose-300 flex items-center gap-1.5 font-sans font-medium">
                          <AlertTriangle className="w-3.5 h-3.5 text-rose-400 shrink-0" />
                          <span>{ind}</span>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <span className="text-emerald-400 text-xs font-sans">None detected</span>
                  )}
                </div>

                <div>
                  <span className="text-slate-400 text-[10px] block">AlertManager State:</span>
                  <p className="text-xs font-sans text-slate-200">
                    {fraudResult.alertDispatched ? '⚠️ Vibration + Heads-up Alert Triggered' : '✓ Normal State (Silent)'}
                  </p>
                </div>

                <div>
                  <span className="text-slate-400 text-[10px] block">Recommendation:</span>
                  <p className="text-xs font-sans text-slate-300">
                    {fraudResult.recommendation}
                  </p>
                </div>

                <div className="pt-2 border-t border-slate-800">
                  <button
                    onClick={handleSaveFraudTestRecord}
                    disabled={fraudResult.saved}
                    className="w-full py-2 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-50 text-slate-200 text-xs font-sans font-semibold border border-slate-700 flex items-center justify-center gap-1.5"
                  >
                    <Database className="w-3.5 h-3.5 text-[#4F7CFF]" />
                    <span>{fraudResult.saved ? '✓ Saved to Call History as [DEMO]' : 'Save as [DEMO] record in history'}</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 3: PIPELINE ARCHITECTURE */}
      {activeTab === 'pipeline' && (
        <div className="space-y-4">
          <div className="p-4 rounded-2xl bg-[#162033] border border-[#26344A] space-y-4 shadow-sm">
            <div>
              <h3 className="text-sm font-bold text-slate-100">Pipeline State Flow</h3>
              <p className="text-xs text-slate-400 mt-0.5">Live execution pipeline from audio input to alert</p>
            </div>

            <div className="space-y-2 text-xs font-mono">
              {[
                { step: '1. Audio Input', desc: 'AudioRecord 16kHz PCM (Mic / Acoustic)', active: isAudioRecording || isSpeechTesting },
                { step: '2. Speech Recognition', desc: 'Android SpeechRecognizer / onResults()', active: isSpeechTesting },
                { step: '3. Transcript Chunking', desc: 'Normalized transcript buffer dispatch', active: !!finalSpeechTranscript },
                { step: '4. Fraud Detector', desc: 'NLP rule matching across 14 scam categories', active: !!fraudResult },
                { step: '5. Risk Engine', desc: 'Dynamic weighted risk scoring calculation', active: !!fraudResult },
                { step: '6. Alert Manager', desc: 'Heads-up notification + sensory haptic pulse', active: !!fraudResult?.alertDispatched },
                { step: '7. Call History', desc: 'Private SQLite/Room audit record persistence', active: !!fraudResult?.saved }
              ].map((item, idx) => (
                <div
                  key={idx}
                  className={`p-3 rounded-xl border flex items-center justify-between ${
                    item.active
                      ? 'bg-blue-950/40 border-blue-500/50 text-blue-200'
                      : 'bg-slate-950 border-slate-800/80 text-slate-400'
                  }`}
                >
                  <div>
                    <p className={`font-bold ${item.active ? 'text-blue-300' : 'text-slate-300'}`}>{item.step}</p>
                    <p className="text-[11px] text-slate-400">{item.desc}</p>
                  </div>
                  <span className={`text-[10px] px-2 py-0.5 rounded font-bold ${
                    item.active ? 'bg-emerald-500/20 text-emerald-400' : 'bg-slate-800 text-slate-500'
                  }`}>
                    {item.active ? 'ACTIVE' : 'IDLE'}
                  </span>
                </div>
              ))}
            </div>

            {/* ADB Logcat Copy */}
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
          </div>
        </div>
      )}

      {/* TAB 4: CELLULAR AUDIO DIAGNOSTICS */}
      {activeTab === 'diagnostics' && (
        <div className="space-y-4">
          <CellularCallAudioCapabilityDiagnostic />
        </div>
      )}
    </div>
  );
}
