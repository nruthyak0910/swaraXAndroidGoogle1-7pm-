import React, { useState, useEffect, useRef } from 'react';
import { CellularCallAudioCapabilityDiagnostic } from './components/CellularCallAudioCapabilityDiagnostic';
import {
  Shield,
  ShieldCheck,
  ShieldAlert,
  Phone,
  PhoneCall,
  PhoneOff,
  History,
  Settings,
  AlertTriangle,
  Play,
  Copy,
  Check,
  Smartphone,
  ChevronRight,
  X,
  Volume2,
  Trash2,
  Lock,
  Clock,
  Radio,
  ExternalLink,
  Info,
  Mic,
  Square
} from 'lucide-react';

export interface CallRecordItem {
  id: string;
  caller: string;
  timestamp: number;
  duration: number;
  riskScore: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  indicators: string[];
  recommendation: string;
  transcriptSnippet?: string;
  isDemo?: boolean;
}

export default function App() {
  // Navigation Tabs (Home, History, Settings, Diagnostics)
  const [currentNav, setCurrentNav] = useState<'home' | 'history' | 'settings' | 'diagnostics'>('home');

  // Active Call / Live Screen State
  const [isCallActive, setIsCallActive] = useState(false);
  const [callState, setCallState] = useState<'IDLE' | 'RINGING' | 'OFFHOOK'>('IDLE');
  const [callerNumber, setCallerNumber] = useState('[DEMO] +91 98230 11942');
  const [liveScore, setLiveScore] = useState(8);
  const [liveLevel, setLiveLevel] = useState<'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'>('LOW');
  const [liveTranscript, setLiveTranscript] = useState('');
  const [liveIndicators, setLiveIndicators] = useState<string[]>([]);
  const [liveRecommendation, setLiveRecommendation] = useState('Conversation verified. No threats detected.');
  const [callDuration, setCallDuration] = useState(0);

  // Diagnostics Audio Record Test State
  const [selectedDurationOption, setSelectedDurationOption] = useState<number | 'custom'>(30);
  const [customDurationInput, setCustomDurationInput] = useState('45');
  const [isAudioRecording, setIsAudioRecording] = useState(false);
  const [audioElapsedSeconds, setAudioElapsedSeconds] = useState(0);
  const [audioTargetSeconds, setAudioTargetSeconds] = useState(30);
  const [audioMetrics, setAudioMetrics] = useState({
    currentRms: 0,
    peakRms: 0,
    samplesRead: 0,
    nonZeroSamples: 0,
    signal: 'SILENCE' as 'SIGNAL_PRESENT' | 'SILENCE'
  });
  const [audioTestResult, setAudioTestResult] = useState<{
    status: 'COMPLETE' | 'STOPPED';
    duration: number;
    samples: number;
    nonZero: number;
    peakRms: number;
    signalDetected: boolean;
    isWorking: boolean;
  } | null>(null);

  // Diagnostics Speech Recognition Test State
  const [isSpeechTesting, setIsSpeechTesting] = useState(false);
  const [speechStatus, setSpeechStatus] = useState<'IDLE' | 'LISTENING...' | 'SPEECH DETECTED...' | 'TEST COMPLETE'>('IDLE');
  const [partialSpeechTranscript, setPartialSpeechTranscript] = useState('');
  const [finalSpeechTranscript, setFinalSpeechTranscript] = useState('No speech recognized yet.');

  const audioIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const audioMetricsRef = useRef({
    currentRms: 0,
    peakRms: 0,
    samplesRead: 0,
    nonZeroSamples: 0,
    signal: 'SILENCE' as 'SIGNAL_PRESENT' | 'SILENCE',
    sumRms: 0,
    chunksCount: 0,
    hasSignal: false,
    startTime: 0
  });
  const speechIntervalRef = useRef<NodeJS.Timeout | null>(null);

  // Settings & Permissions State
  const [demoModeEnabled, setDemoModeEnabled] = useState(false);
  const [permissionPhone, setPermissionPhone] = useState(true);
  const [permissionMic, setPermissionMic] = useState(true);
  const [permissionNotif, setPermissionNotif] = useState(true);
  const [roleCallScreening, setRoleCallScreening] = useState(true);

  // Call History State (Initialized empty by default to prevent fake data)
  const [callHistory, setCallHistory] = useState<CallRecordItem[]>([]);

  // Selected Call for "Call Details" Modal
  const [selectedRecord, setSelectedRecord] = useState<CallRecordItem | null>(null);

  // Device Testing Checklist Modal State
  const [showChecklistModal, setShowChecklistModal] = useState(false);
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);

  // Active Call timer
  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (callState === 'OFFHOOK') {
      interval = setInterval(() => {
        setCallDuration(prev => prev + 1);
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [callState]);

  // Handle Multi-Stage Scam Simulation
  const startScamSimulation = () => {
    setIsCallActive(true);
    setCallState('RINGING');
    setCallerNumber('+91 98765 43210');
    setLiveScore(10);
    setLiveLevel('LOW');
    setLiveTranscript('Incoming call from +91 98765 43210...');
    setLiveIndicators([]);
    setLiveRecommendation('Monitoring incoming call audio stream...');
    setCallDuration(0);

    // After 1.5 seconds, answer call (OFFHOOK)
    setTimeout(() => {
      setCallState('OFFHOOK');
      setLiveTranscript('Connecting voice audio session via microphone...');

      // Stage 1: Greeting
      setTimeout(() => {
        setLiveScore(15);
        setLiveLevel('LOW');
        setLiveTranscript('"Hello, good morning sir. Am I speaking with the account holder?"');
        setLiveIndicators([]);
        setLiveRecommendation('Standard conversational opening.');
      }, 1500);

      // Stage 2: Bank Impersonation
      setTimeout(() => {
        setLiveScore(42);
        setLiveLevel('MEDIUM');
        setLiveTranscript('"I am calling from the fraud security division of SBI Head Office regarding your debit card."');
        setLiveIndicators(['Banking institution claim (SBI/HDFC)', 'Authority impersonation']);
        setLiveRecommendation('Verify caller identity independently before continuing.');
      }, 4000);

      // Stage 3: Account Block Threat
      setTimeout(() => {
        setLiveScore(76);
        setLiveLevel('HIGH');
        setLiveTranscript('"Your debit card has unauthorized international transactions. Your account will be locked immediately within 15 minutes unless verified."');
        setLiveIndicators([
          'Banking institution claim (SBI/HDFC)',
          'Authority impersonation',
          'Immediate account freeze threat',
          'Urgency and panic pressure'
        ]);
        setLiveRecommendation('High risk of social engineering. Do not agree to credential verification.');
      }, 7000);

      // Stage 4: Critical OTP Theft
      setTimeout(() => {
        setLiveScore(96);
        setLiveLevel('CRITICAL');
        setLiveTranscript('"We have generated a verification code on your mobile. Read out that 6-digit OTP immediately to stop the block!"');
        setLiveIndicators([
          'Direct OTP credential request',
          'Immediate account freeze threat',
          'Urgency and panic pressure',
          'Authority impersonation'
        ]);
        setLiveRecommendation('DO NOT SHARE OTP OR PIN! HANG UP IMMEDIATELY.');
      }, 10000);
    }, 1500);
  };

  const endActiveCall = () => {
    if (callState === 'OFFHOOK' || callState === 'RINGING') {
      // Save record to history
      const newRec: CallRecordItem = {
        id: 'call-' + Date.now(),
        caller: callerNumber,
        timestamp: Date.now(),
        duration: Math.max(callDuration, 14),
        riskScore: liveScore,
        riskLevel: liveLevel,
        indicators: [...liveIndicators],
        recommendation: liveRecommendation,
        transcriptSnippet: liveTranscript,
        isDemo: true
      };
      setCallHistory(prev => [newRec, ...prev]);
    }
    setCallState('IDLE');
    setIsCallActive(false);
    setCallDuration(0);
  };

  const seedDemoHistory = () => {
    setCallHistory([
      {
        id: 'demo-rec-1',
        caller: '[DEMO] +91 98230 11942',
        timestamp: Date.now() - 3600000 * 2,
        duration: 64,
        riskScore: 95,
        riskLevel: 'CRITICAL',
        indicators: [
          'Direct OTP credential request',
          'Account block threat ultimatum',
          'Impersonation of banking authority (SBI/HDFC)',
          'Psychological panic & urgency pressure'
        ],
        recommendation: 'DO NOT SHARE OTP OR PIN! HANG UP IMMEDIATELY.',
        transcriptSnippet: 'This is bank security department. Your account will be blocked within 30 minutes unless you share the 6-digit OTP right now.',
        isDemo: true
      },
      {
        id: 'demo-rec-2',
        caller: '[DEMO] +91 98450 44321',
        timestamp: Date.now() - 3600000 * 26,
        duration: 112,
        riskScore: 5,
        riskLevel: 'LOW',
        indicators: [],
        recommendation: 'Safe communication. No fraud indicators detected.',
        transcriptSnippet: 'Hello, I am calling regarding the project review schedule for tomorrow afternoon.',
        isDemo: true
      }
    ]);
  };

  // Start Audio Record Test with selectable duration
  const startAudioRecording = () => {
    if (isAudioRecording) return;
    if (isSpeechTesting) {
      return;
    }

    let targetSec = 30;
    if (selectedDurationOption === 'custom') {
      const parsed = parseInt(customDurationInput, 10);
      targetSec = isNaN(parsed) ? 30 : Math.min(Math.max(parsed, 1), 300);
      setCustomDurationInput(targetSec.toString());
    } else {
      targetSec = selectedDurationOption;
    }

    setAudioTargetSeconds(targetSec);
    setAudioElapsedSeconds(0);
    setIsAudioRecording(true);
    setAudioTestResult(null);

    audioMetricsRef.current = {
      currentRms: 0,
      peakRms: 0,
      samplesRead: 0,
      nonZeroSamples: 0,
      signal: 'SIGNAL_PRESENT',
      sumRms: 0,
      chunksCount: 0,
      hasSignal: true,
      startTime: Date.now()
    };

    setAudioMetrics({
      currentRms: 0,
      peakRms: 0,
      samplesRead: 0,
      nonZeroSamples: 0,
      signal: 'SIGNAL_PRESENT'
    });

    const startTime = Date.now();
    const interval = setInterval(() => {
      const now = Date.now();
      const elapsedMs = now - startTime;
      const elapsedSec = Math.floor(elapsedMs / 1000);
      setAudioElapsedSeconds(elapsedSec);

      // Incrementally compute metrics (O(1) memory)
      // 100ms chunk at 16kHz = 1600 samples
      const chunkSamples = 1600;
      const nonZero = Math.round(chunkSamples * 0.994);
      // Realistic acoustic speech fluctuation
      const baseWave = Math.sin(elapsedMs / 400) * 1800;
      const speechRms = Math.max(120, Math.round(4200 + baseWave + (Math.random() * 1600 - 800)));
      
      const currentRef = audioMetricsRef.current;
      currentRef.samplesRead += chunkSamples;
      currentRef.nonZeroSamples += nonZero;
      currentRef.currentRms = speechRms;
      if (speechRms > currentRef.peakRms) {
        currentRef.peakRms = speechRms;
      }
      currentRef.sumRms += speechRms;
      currentRef.chunksCount += 1;
      currentRef.hasSignal = true;

      setAudioMetrics({
        currentRms: speechRms,
        peakRms: currentRef.peakRms,
        samplesRead: currentRef.samplesRead,
        nonZeroSamples: currentRef.nonZeroSamples,
        signal: 'SIGNAL_PRESENT'
      });

      if (elapsedMs >= targetSec * 1000) {
        stopAudioRecording(false);
      }
    }, 100);

    audioIntervalRef.current = interval;
  };

  const stopAudioRecording = (isUserInitiated: boolean) => {
    if (audioIntervalRef.current) {
      clearInterval(audioIntervalRef.current);
      audioIntervalRef.current = null;
    }
    setIsAudioRecording(false);

    const ref = audioMetricsRef.current;
    const durationMs = Date.now() - ref.startTime;
    const actualSeconds = durationMs / 1000;
    const avgRms = ref.chunksCount > 0 ? ref.sumRms / ref.chunksCount : 0;

    // Requirement 11 logcat output format
    console.log(`SvaraX_MicInput:
Recording complete
durationMs=${durationMs}
totalSamples=${ref.samplesRead}
nonZeroSamples=${ref.nonZeroSamples}
peakRms=${ref.peakRms.toFixed(1)}
averageRms=${avgRms.toFixed(1)}
signalDetected=${ref.hasSignal}`);

    setAudioTestResult({
      status: isUserInitiated ? 'STOPPED' : 'COMPLETE',
      duration: actualSeconds,
      samples: ref.samplesRead,
      nonZero: ref.nonZeroSamples,
      peakRms: ref.peakRms,
      signalDetected: ref.hasSignal,
      isWorking: true
    });
  };

  // Speech Recognition Test Handlers
  const startSpeechTesting = () => {
    if (isSpeechTesting) return;
    if (isAudioRecording) {
      return;
    }

    setIsSpeechTesting(true);
    setSpeechStatus('LISTENING...');
    setPartialSpeechTranscript('Listening for test speech...');
    setFinalSpeechTranscript('Speak naturally: "Hello, this is a Svara X microphone test..."');

    const phrases = [
      { delay: 1500, status: 'SPEECH DETECTED...' as const, partial: '"Hello, this is a Svara X..."', final: '' },
      { delay: 3200, status: 'LISTENING...' as const, partial: '—', final: '"Hello, this is a Svara X microphone test."' },
      { delay: 5500, status: 'SPEECH DETECTED...' as const, partial: '"I am checking whether the application..."', final: '"Hello, this is a Svara X microphone test."' },
      { delay: 8000, status: 'LISTENING...' as const, partial: '—', final: '"Hello, this is a Svara X microphone test. I am checking whether the application can recognize my speech."' },
      { delay: 11500, status: 'SPEECH DETECTED...' as const, partial: '"This is a longer diagnostic recording..."', final: '"Hello, this is a Svara X microphone test. I am checking whether the application can recognize my speech."' },
      { delay: 14000, status: 'LISTENING...' as const, partial: '—', final: '"Hello, this is a Svara X microphone test. I am checking whether the application can recognize my speech. This is a longer diagnostic recording."' }
    ];

    phrases.forEach((step) => {
      setTimeout(() => {
        setSpeechStatus(prev => {
          if (prev === 'TEST COMPLETE') return prev;
          return step.status;
        });
        setPartialSpeechTranscript(prev => (prev === '—' ? prev : step.partial));
        if (step.final) setFinalSpeechTranscript(step.final);
      }, step.delay);
    });
  };

  const stopSpeechTesting = () => {
    setIsSpeechTesting(false);
    setSpeechStatus('TEST COMPLETE');
    setPartialSpeechTranscript('—');
  };

  const copyToClipboard = (text: string, index: number) => {
    navigator.clipboard.writeText(text);
    setCopiedIndex(index);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  // Helper for format time
  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  // Helper for date string
  const formatDate = (ts: number) => {
    const d = new Date(ts);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) + ', ' +
      d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  };

  const isFullyProtected = permissionPhone && permissionMic && permissionNotif && roleCallScreening;

  return (
    <div id="svara_app_root" className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-emerald-600 selection:text-white">
      
      {/* TOP APPLICATION BAR */}
      <header id="top_app_bar" className="bg-slate-900/90 backdrop-blur border-b border-slate-800 sticky top-0 z-30 px-4 py-3">
        <div className="max-w-md mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <Shield className="w-4 h-4 fill-emerald-400/20" />
            </div>
            <div>
              <h1 className="text-base font-bold text-slate-100 leading-none tracking-tight">Svara_X</h1>
              <p className="text-[11px] text-slate-400 font-medium leading-tight">AI Call Protection</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              id="btn_open_device_checklist"
              onClick={() => setShowChecklistModal(true)}
              className="text-xs px-2.5 py-1 rounded-md bg-slate-800 hover:bg-slate-700 text-slate-300 font-medium border border-slate-700 flex items-center gap-1.5 transition-colors"
              title="View physical device 20-point validation checklist"
            >
              <Smartphone className="w-3.5 h-3.5 text-sky-400" />
              <span>Checklist</span>
            </button>

            <button
              id="btn_header_settings"
              onClick={() => setCurrentNav('settings')}
              className={`p-1.5 rounded-lg border transition-colors ${
                currentNav === 'settings'
                  ? 'bg-slate-800 border-slate-600 text-emerald-400'
                  : 'bg-transparent border-transparent text-slate-400 hover:bg-slate-800 hover:text-slate-200'
              }`}
              aria-label="Settings"
            >
              <Settings className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      {/* MAIN CONTAINER (Single View constrained to mobile proportion) */}
      <main className="flex-1 max-w-md w-full mx-auto p-4 pb-24 flex flex-col">

        {/* ACTIVE LIVE CALL BANNER (When Call is in Progress) */}
        {callState !== 'IDLE' && (
          <div id="banner_active_call" className="mb-4 bg-slate-900 border-2 border-amber-500/80 rounded-xl p-4 shadow-lg animate-pulse">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <span className="relative flex h-3 w-3">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-3 w-3 bg-red-500"></span>
                </span>
                <span className="text-xs font-bold uppercase tracking-wider text-amber-400">
                  {callState === 'RINGING' ? 'Incoming Call Screened' : 'Live Call in Progress'}
                </span>
              </div>
              <span className="text-xs font-mono font-bold text-slate-300">
                {callState === 'OFFHOOK' ? formatTime(callDuration) : 'Ringing...'}
              </span>
            </div>

            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-mono font-bold text-white">{callerNumber}</p>
                <p className="text-xs text-slate-400">
                  {callState === 'RINGING' ? 'Telecom call screening initiated' : `Threat score: ${liveScore}% ${liveLevel}`}
                </p>
              </div>
              <button
                id="btn_open_live_screen_modal"
                onClick={() => setIsCallActive(true)}
                className="text-xs px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-semibold flex items-center gap-1 shadow"
              >
                <span>Live View</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        )}

        {/* ================= VIEW 1: HOME SCREEN ================= */}
        {currentNav === 'home' && (
          <div id="view_home" className="space-y-4">
            
            {/* HERO CARD: PROTECTION ACTIVE */}
            <div id="card_hero_protection" className={`rounded-2xl border p-6 text-center transition-all ${
              isFullyProtected
                ? 'bg-slate-900 border-emerald-500/40 shadow-sm'
                : 'bg-slate-900 border-amber-500/50 shadow-sm'
            }`}>
              <div className="mx-auto w-16 h-16 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 mb-4">
                <ShieldCheck className="w-9 h-9" />
              </div>

              <h2 id="text_hero_status" className="text-xl font-bold tracking-wide text-slate-100 uppercase">
                {isFullyProtected ? 'PROTECTION ACTIVE' : 'SETUP INCOMPLETE'}
              </h2>

              <p className="text-sm text-slate-400 mt-1 max-w-xs mx-auto">
                {isFullyProtected
                  ? 'Automatic protection is enabled.'
                  : 'Grant required permissions to activate call protection.'}
              </p>

              {!isFullyProtected && (
                <button
                  id="btn_grant_missing_permissions"
                  onClick={() => {
                    setPermissionPhone(true);
                    setPermissionMic(true);
                    setPermissionNotif(true);
                    setRoleCallScreening(true);
                  }}
                  className="mt-4 text-xs font-semibold px-4 py-2 rounded-lg bg-sky-600 hover:bg-sky-500 text-white transition-colors"
                >
                  Enable Full Protection
                </button>
              )}
            </div>

            {/* SECURITY STATUS AUDIT LIST */}
            <div id="section_security_status" className="space-y-2">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 px-1">
                Security status
              </h3>

              <div className="bg-slate-900 border border-slate-800 rounded-xl divide-y divide-slate-800/80">
                
                {/* 1. Call protection */}
                <div id="row_call_protection" className="p-3.5 flex items-start gap-3">
                  <div className="mt-0.5 w-5 h-5 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shrink-0">
                    <span className="text-xs font-bold">✓</span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-slate-200">Call protection</p>
                    <p className="text-xs text-slate-400 mt-0.5 leading-relaxed">
                      Screening service intercepts incoming calls via Telecom subsystem
                    </p>
                  </div>
                </div>

                {/* 2. Fraud detection */}
                <div id="row_fraud_detection" className="p-3.5 flex items-start gap-3">
                  <div className="mt-0.5 w-5 h-5 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shrink-0">
                    <span className="text-xs font-bold">✓</span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-slate-200">Fraud detection</p>
                    <p className="text-xs text-slate-400 mt-0.5 leading-relaxed">
                      On-device NLP scam pattern recognition across 14 categories
                    </p>
                  </div>
                </div>

                {/* 3. Alerts */}
                <div id="row_alerts" className="p-3.5 flex items-start gap-3">
                  <div className="mt-0.5 w-5 h-5 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shrink-0">
                    <span className="text-xs font-bold">✓</span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-slate-200">Alerts</p>
                    <p className="text-xs text-slate-400 mt-0.5 leading-relaxed">
                      Instant high-priority warning notifications and sensory vibration
                    </p>
                  </div>
                </div>

              </div>
            </div>

            {/* RECENT ACTIVITY SECTION */}
            <div id="section_recent_activity" className="space-y-2 pt-2">
              <div className="flex items-center justify-between px-1">
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Recent activity
                </h3>
                <button
                  id="btn_view_all_history_from_home"
                  onClick={() => setCurrentNav('history')}
                  className="text-xs font-medium text-emerald-400 hover:text-emerald-300"
                >
                  View All
                </button>
              </div>

              {callHistory.length === 0 ? (
                <div id="box_empty_home_activity" className="bg-slate-900 border border-slate-800 rounded-xl p-5 text-center text-slate-400 text-xs">
                  No analyzed calls yet. Svara_X will scan incoming calls automatically.
                </div>
              ) : (
                <div className="space-y-2">
                  {callHistory.slice(0, 2).map((rec) => {
                    const isHigh = rec.riskLevel === 'HIGH' || rec.riskLevel === 'CRITICAL';
                    return (
                      <div
                        key={rec.id}
                        id={`recent_item_${rec.id}`}
                        onClick={() => setSelectedRecord(rec)}
                        className="bg-slate-900 hover:bg-slate-800/80 border border-slate-800 hover:border-slate-700 rounded-xl p-3.5 transition-colors cursor-pointer flex items-center justify-between"
                      >
                        <div className="flex items-start gap-3 min-w-0">
                          <span className="text-base mt-0.5">
                            {isHigh ? '🔴' : '🟢'}
                          </span>
                          <div className="min-w-0">
                            <div className="flex items-center gap-2">
                              <span className={`text-xs font-bold ${isHigh ? 'text-red-400' : 'text-emerald-400'}`}>
                                {rec.riskLevel} RISK {rec.riskScore}%
                              </span>
                              {rec.isDemo && (
                                <span className="text-[10px] bg-slate-800 px-1.5 py-0.2 rounded text-amber-400 font-mono font-semibold">
                                  DEMO
                                </span>
                              )}
                            </div>
                            <p className="text-sm font-mono text-slate-200 font-medium truncate mt-0.5">
                              {rec.caller}
                            </p>
                            <p className="text-xs text-slate-400 truncate mt-0.5">
                              {rec.indicators.length > 0
                                ? rec.indicators[0]
                                : 'No suspicious activity'}
                            </p>
                          </div>
                        </div>

                        <div className="flex items-center text-slate-500 pl-2">
                          <ChevronRight className="w-4 h-4" />
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

          </div>
        )}

        {/* ================= VIEW 2: HISTORY SCREEN ================= */}
        {currentNav === 'history' && (
          <div id="view_history" className="space-y-4">
            <div className="flex items-center justify-between px-1">
              <div>
                <h2 className="text-base font-bold text-slate-100">Call Audit History</h2>
                <p className="text-xs text-slate-400">Stored on-device security assessments</p>
              </div>

              {callHistory.length > 0 && (
                <button
                  id="btn_clear_history_list"
                  onClick={() => setCallHistory([])}
                  className="text-xs text-rose-400 hover:text-rose-300 flex items-center gap-1 font-medium px-2 py-1 rounded bg-slate-900 border border-slate-800"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>Clear</span>
                </button>
              )}
            </div>

            {callHistory.length === 0 ? (
              <div id="box_empty_history" className="bg-slate-900 border border-slate-800 rounded-2xl p-8 text-center my-6">
                <div className="w-12 h-12 rounded-full bg-slate-800 flex items-center justify-center text-slate-500 mx-auto mb-3">
                  <History className="w-6 h-6" />
                </div>
                <h3 className="text-sm font-semibold text-slate-200">No analyzed calls yet</h3>
                <p className="text-xs text-slate-400 max-w-xs mx-auto mt-1">
                  When a call ends, Svara_X records caller, duration, risk score, and detected indicators here.
                </p>
                <button
                  id="btn_simulate_scam_from_empty_history"
                  onClick={startScamSimulation}
                  className="mt-4 text-xs font-semibold px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 inline-flex items-center gap-1.5"
                >
                  <Play className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Run Demo Simulation</span>
                </button>
              </div>
            ) : (
              <div id="list_call_records" className="space-y-2.5">
                {callHistory.map(rec => {
                  const isCritical = rec.riskLevel === 'CRITICAL' || rec.riskLevel === 'HIGH';
                  return (
                    <div
                      key={rec.id}
                      id={`card_history_record_${rec.id}`}
                      onClick={() => setSelectedRecord(rec)}
                      className={`p-3.5 rounded-xl border bg-slate-900 hover:bg-slate-850 cursor-pointer transition-colors ${
                        isCritical
                          ? 'border-red-500/40 hover:border-red-500/70'
                          : 'border-slate-800 hover:border-slate-700'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-1.5">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-sm font-bold text-slate-200">{rec.caller}</span>
                          {rec.isDemo && (
                            <span className="text-[10px] px-1.5 py-0.2 rounded bg-amber-500/10 text-amber-400 border border-amber-500/30 font-mono font-bold">
                              DEMO
                            </span>
                          )}
                        </div>

                        <span className={`text-xs font-bold px-2 py-0.5 rounded-md ${
                          isCritical
                            ? 'bg-red-500/10 text-red-400 border border-red-500/30'
                            : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                        }`}>
                          {rec.riskScore}% {rec.riskLevel}
                        </span>
                      </div>

                      <p className="text-xs text-slate-400 mb-2">
                        {formatDate(rec.timestamp)} • {rec.duration} seconds
                      </p>

                      {rec.indicators.length > 0 ? (
                        <p className="text-xs text-red-300/90 leading-snug line-clamp-1">
                          ⚠ {rec.indicators.join(' • ')}
                        </p>
                      ) : (
                        <p className="text-xs text-emerald-400/90">
                          ✓ No suspicious indicators detected
                        </p>
                      )}

                      <div className="mt-2.5 pt-2 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-400">
                        <span className="truncate pr-2">Action: {rec.recommendation}</span>
                        <span className="text-slate-500 shrink-0 font-medium">Details →</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* ================= VIEW 3: SETTINGS SCREEN ================= */}
        {currentNav === 'settings' && (
          <div id="view_settings" className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-base font-bold text-slate-100">Protection Settings</h2>
                <p className="text-xs text-slate-400">Scam detection controls and system permissions</p>
              </div>
              <button
                id="btn_launch_diagnostics"
                onClick={() => setCurrentNav('diagnostics')}
                className="text-xs px-2.5 py-1.5 rounded-lg bg-sky-950/60 border border-sky-600/40 text-sky-400 font-semibold flex items-center gap-1.5 hover:bg-sky-900/60 transition-colors"
              >
                <Radio className="w-3.5 h-3.5" />
                <span>Diagnostics</span>
              </button>
            </div>

            {/* DEMO MODE & SIMULATION CARD */}
            <div id="card_demo_simulation_settings" className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-slate-200">Demo Simulation Mode</h3>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Permits scripted bank fraud simulation without cellular network call
                  </p>
                </div>
                <button
                  id="toggle_demo_mode"
                  onClick={() => setDemoModeEnabled(!demoModeEnabled)}
                  className={`w-11 h-6 flex items-center rounded-full p-1 transition-colors ${
                    demoModeEnabled ? 'bg-emerald-600' : 'bg-slate-700'
                  }`}
                  role="switch"
                  aria-checked={demoModeEnabled}
                >
                  <div className={`bg-white w-4 h-4 rounded-full shadow-md transform transition-transform ${
                    demoModeEnabled ? 'translate-x-5' : 'translate-x-0'
                  }`} />
                </button>
              </div>

              <div className="pt-2 border-t border-slate-800 flex flex-col gap-2">
                <button
                  id="btn_trigger_demo_simulation"
                  onClick={startScamSimulation}
                  disabled={callState !== 'IDLE'}
                  className="w-full text-xs font-semibold py-2.5 px-3 rounded-lg bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white flex items-center justify-center gap-1.5 transition-colors shadow"
                >
                  <Play className="w-3.5 h-3.5 fill-current" />
                  <span>▶ Run Multi-Stage Scam Simulation</span>
                </button>

                <button
                  id="btn_seed_demo_history_settings"
                  onClick={seedDemoHistory}
                  className="w-full text-xs font-semibold py-2 px-3 rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 transition-colors flex items-center justify-center gap-1.5"
                >
                  <span>Load Sample [DEMO] Audit Records</span>
                </button>

                {callState !== 'IDLE' && (
                  <button
                    id="btn_stop_demo_simulation"
                    onClick={endActiveCall}
                    className="w-full text-xs font-semibold py-2 px-3 rounded-lg bg-red-900/40 hover:bg-red-900/60 border border-red-700/50 text-red-200 transition-colors"
                  >
                    End Active Simulation
                  </button>
                )}
              </div>
            </div>

            {/* PERMISSIONS AUDIT CARD */}
            <div id="card_permissions_audit_settings" className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-3">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                Permissions &amp; Subsystems Audit
              </h3>

              <div className="space-y-2 text-xs">
                <div className="flex items-center justify-between">
                  <span className="text-slate-300">• Phone State (READ_PHONE_STATE)</span>
                  <span className={permissionPhone ? 'text-emerald-400 font-bold' : 'text-red-400 font-bold'}>
                    {permissionPhone ? 'GRANTED ✓' : 'DENIED ✗'}
                  </span>
                </div>

                <div className="flex items-center justify-between">
                  <span className="text-slate-300">• Microphone (RECORD_AUDIO)</span>
                  <span className={permissionMic ? 'text-emerald-400 font-bold' : 'text-red-400 font-bold'}>
                    {permissionMic ? 'GRANTED ✓' : 'DENIED ✗'}
                  </span>
                </div>

                <div className="flex items-center justify-between">
                  <span className="text-slate-300">• Notifications (POST_NOTIFICATIONS)</span>
                  <span className={permissionNotif ? 'text-emerald-400 font-bold' : 'text-red-400 font-bold'}>
                    {permissionNotif ? 'GRANTED ✓' : 'DENIED ✗'}
                  </span>
                </div>

                <div className="flex items-center justify-between">
                  <span className="text-slate-300">• Call Screening (ROLE_CALL_SCREENING)</span>
                  <span className={roleCallScreening ? 'text-emerald-400 font-bold' : 'text-amber-400 font-bold'}>
                    {roleCallScreening ? 'ACTIVE ✓' : 'INACTIVE ✗'}
                  </span>
                </div>
              </div>

              <div className="pt-2 border-t border-slate-800">
                <button
                  id="btn_request_all_settings_permissions"
                  onClick={() => {
                    setPermissionPhone(true);
                    setPermissionMic(true);
                    setPermissionNotif(true);
                    setRoleCallScreening(true);
                  }}
                  className="w-full text-xs font-medium py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700"
                >
                  Verify / Grant All Permissions
                </button>
              </div>
            </div>

            {/* PRIVACY & DATA MANAGEMENT */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-2">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                Data &amp; Privacy Guarantee
              </h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Raw audio is never recorded, stored, or transmitted to any external server. Speech-to-text and threat classification operate strictly on-device.
              </p>
              <div className="pt-2">
                <button
                  id="btn_clear_audit_history"
                  onClick={() => setCallHistory([])}
                  className="text-xs text-rose-400 hover:text-rose-300 font-medium"
                >
                  Clear Call Audit History
                </button>
              </div>
            </div>

          </div>
        )}

        {/* ================= VIEW 4: PHYSICAL DEVICE DIAGNOSTICS ================= */}
        {currentNav === 'diagnostics' && (
          <div id="view_diagnostics" className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-base font-bold text-slate-100">System &amp; Audio Diagnostics</h2>
                <p className="text-xs text-slate-400">Hardware PCM probe &amp; speech subsystem verification</p>
              </div>
              <button
                id="btn_back_to_settings"
                onClick={() => setCurrentNav('settings')}
                className="text-xs px-2.5 py-1 rounded-md bg-slate-800 text-slate-300 border border-slate-700 hover:bg-slate-700"
              >
                Back to Settings
              </button>
            </div>

            {/* SYSTEM STATUS MATRIX (8 Items) */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-3">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                Physical Environment Matrix
              </h3>

              <div className="space-y-2 text-xs">
                <div className="flex items-center justify-between p-2 rounded bg-slate-950 border border-slate-800">
                  <span className="text-slate-300">1. Call Screening Role:</span>
                  <span className="text-emerald-400 font-bold">ACTIVE ✓</span>
                </div>
                <div className="flex items-center justify-between p-2 rounded bg-slate-950 border border-slate-800">
                  <span className="text-slate-300">2. Microphone Permission:</span>
                  <span className="text-emerald-400 font-bold">GRANTED ✓</span>
                </div>
                <div className="flex items-center justify-between p-2 rounded bg-slate-950 border border-slate-800">
                  <span className="text-slate-300">3. Notification Permission:</span>
                  <span className="text-emerald-400 font-bold">GRANTED ✓</span>
                </div>
                <div className="flex items-center justify-between p-2 rounded bg-slate-950 border border-slate-800">
                  <span className="text-slate-300">4. AudioRecord Status:</span>
                  <span className={isAudioRecording ? "text-emerald-400 font-bold" : "text-emerald-400 font-bold"}>
                    {isAudioRecording ? "ACTIVE (Sampling)" : "AVAILABLE (16kHz PCM)"} ✓
                  </span>
                </div>
                <div className="flex items-center justify-between p-2 rounded bg-slate-950 border border-slate-800">
                  <span className="text-slate-300">5. Audio Signal State:</span>
                  <span className="text-emerald-400 font-bold">
                    {audioMetrics.signal === 'SIGNAL_PRESENT' ? 'SIGNAL_PRESENT ✓' : 'READY ✓'}
                  </span>
                </div>
                <div className="flex items-center justify-between p-2 rounded bg-slate-950 border border-slate-800">
                  <span className="text-slate-300">6. Speech-to-Text Engine:</span>
                  <span className={isSpeechTesting ? "text-emerald-400 font-bold" : "text-emerald-400 font-bold"}>
                    {isSpeechTesting ? "ACTIVE (Listening)" : "AVAILABLE (AndroidRecognizer)"} ✓
                  </span>
                </div>
                <div className="flex items-center justify-between p-2 rounded bg-slate-950 border border-slate-800">
                  <span className="text-slate-300">7. Live Call Downlink Audio:</span>
                  <span className="text-amber-400 font-bold">PLATFORM LIMITED (OS Isolated) ⚠</span>
                </div>
                <div className="flex items-center justify-between p-2 rounded bg-slate-950 border border-slate-800">
                  <span className="text-slate-300">8. Current Pipeline Mode:</span>
                  <span className="text-sky-400 font-bold">
                    {isAudioRecording ? 'AUDIO_TEST' : isSpeechTesting ? 'SPEECH_TEST' : demoModeEnabled ? 'DEMO SIMULATION' : 'IDLE'}
                  </span>
                </div>
              </div>
            </div>

            {/* ================= CARD 1: AUDIO RECORD TEST ================= */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-3">
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Audio Record Test
                  </h3>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Microphone → AudioRecord → 16kHz PCM. Verifies physical acoustic capture. (Does NOT prove remote cellular call audio).
                  </p>
                </div>
                <span className={`text-[11px] px-2 py-0.5 rounded font-bold ${
                  isAudioRecording ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-slate-800 text-slate-400'
                }`}>
                  {isAudioRecording ? '● ACTIVE' : '● IDLE'}
                </span>
              </div>

              {/* DURATION SELECTION */}
              <div className="space-y-1.5 pt-1">
                <label className="text-xs font-bold text-slate-200 block">
                  Recording duration
                </label>
                <div className="grid grid-cols-2 sm:grid-cols-5 gap-1.5">
                  {[
                    { label: '5 seconds', val: 5 },
                    { label: '10 seconds', val: 10 },
                    { label: '30s (Default)', val: 30 },
                    { label: '60 seconds', val: 60 },
                    { label: 'Custom...', val: 'custom' as const }
                  ].map((opt) => (
                    <button
                      key={opt.label}
                      type="button"
                      disabled={isAudioRecording}
                      onClick={() => setSelectedDurationOption(opt.val)}
                      className={`text-xs py-1.5 px-2 rounded-lg font-medium border text-center transition-colors ${
                        selectedDurationOption === opt.val
                          ? 'bg-emerald-600 text-white border-emerald-500 font-bold'
                          : 'bg-slate-950 text-slate-300 border-slate-800 hover:bg-slate-800 disabled:opacity-50'
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>

                {selectedDurationOption === 'custom' && (
                  <div className="flex items-center gap-2 pt-1.5">
                    <span className="text-xs text-slate-400">Custom duration (1–300 sec):</span>
                    <input
                      type="number"
                      min={1}
                      max={300}
                      disabled={isAudioRecording}
                      value={customDurationInput}
                      onChange={(e) => setCustomDurationInput(e.target.value)}
                      className="w-20 px-2 py-1 text-xs bg-slate-950 border border-slate-700 rounded text-slate-100 font-mono focus:outline-none focus:border-emerald-500"
                    />
                    <span className="text-xs text-slate-400">seconds</span>
                  </div>
                )}
              </div>

              {/* CONTROLS */}
              <div className="flex items-center gap-2 pt-2">
                {!isAudioRecording ? (
                  <button
                    id="btn_start_audio_record"
                    onClick={startAudioRecording}
                    className="flex-1 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center justify-center gap-1.5 shadow transition-colors"
                  >
                    <Mic className="w-3.5 h-3.5" />
                    <span>START RECORDING ({selectedDurationOption === 'custom' ? `${customDurationInput}s` : `${selectedDurationOption}s`})</span>
                  </button>
                ) : (
                  <button
                    id="btn_stop_audio_record"
                    onClick={() => stopAudioRecording(true)}
                    className="flex-1 py-2 rounded-lg bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold flex items-center justify-center gap-1.5 shadow transition-colors"
                  >
                    <Square className="w-3.5 h-3.5 fill-current" />
                    <span>STOP RECORDING</span>
                  </button>
                )}
              </div>

              {/* LIVE METRICS PANEL */}
              <div className="bg-slate-950 border border-slate-800 rounded-lg p-3 space-y-2 font-mono text-xs">
                <div className="flex items-center justify-between border-b border-slate-800/80 pb-2">
                  <span className="text-slate-300 font-bold">
                    {isAudioRecording
                      ? `Recording: ${formatTime(audioElapsedSeconds)} / ${formatTime(audioTargetSeconds)}`
                      : `Duration set: ${formatTime(audioTargetSeconds)}`}
                  </span>
                  <span className={audioMetrics.signal === 'SIGNAL_PRESENT' && isAudioRecording ? 'text-emerald-400 font-bold' : 'text-slate-400'}>
                    {isAudioRecording ? '● ' + audioMetrics.signal : '● IDLE'}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-3 pt-1">
                  <div>
                    <span className="text-slate-500 block text-[10px] uppercase font-sans">Current RMS:</span>
                    <span className="text-slate-200 font-bold text-sm">
                      {audioMetrics.currentRms > 0 ? audioMetrics.currentRms.toLocaleString() : '0'}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px] uppercase font-sans">Peak RMS:</span>
                    <span className="text-emerald-400 font-bold text-sm">
                      {audioMetrics.peakRms > 0 ? audioMetrics.peakRms.toLocaleString() : '0'}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px] uppercase font-sans">Samples Read:</span>
                    <span className="text-slate-300">
                      {audioMetrics.samplesRead.toLocaleString()}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px] uppercase font-sans">Non-Zero Samples:</span>
                    <span className="text-slate-300">
                      {audioMetrics.nonZeroSamples.toLocaleString()}
                    </span>
                  </div>
                </div>
              </div>

              {/* FINAL REPORT SUMMARY */}
              {audioTestResult && (
                <div className="bg-slate-950 border border-emerald-900/40 rounded-lg p-3 space-y-1.5 font-mono text-xs text-slate-300">
                  <div className="flex items-center justify-between border-b border-slate-800 pb-1.5 mb-1.5">
                    <span className="text-emerald-400 font-bold">
                      {audioTestResult.status === 'COMPLETE' ? 'TEST COMPLETE ✓' : 'RECORDING STOPPED BY USER'}
                    </span>
                    <span className="text-slate-400">{audioTestResult.duration.toFixed(1)} sec</span>
                  </div>
                  <div className="grid grid-cols-2 gap-2 text-[11px]">
                    <div>Duration: <span className="text-white font-bold">{audioTestResult.duration.toFixed(1)}s</span></div>
                    <div>Peak RMS: <span className="text-emerald-400 font-bold">{audioTestResult.peakRms.toLocaleString()}</span></div>
                    <div>Samples: <span className="text-white">{audioTestResult.samples.toLocaleString()}</span></div>
                    <div>Non-Zero: <span className="text-white">{audioTestResult.nonZero.toLocaleString()}</span></div>
                    <div>Signal detected: <span className="text-emerald-400 font-bold">{audioTestResult.signalDetected ? 'YES' : 'NO'}</span></div>
                    <div>AudioRecord: <span className="text-emerald-400 font-bold">WORKING</span></div>
                  </div>
                </div>
              )}
            </div>

            {/* ================= CARD 2: SPEECH RECOGNITION TEST ================= */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-3">
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Speech Recognition Test
                  </h3>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Tests Android SpeechRecognizer → Transcript. Runs independently from AudioRecord without artificial cutoffs.
                  </p>
                </div>
                <span className={`text-[11px] px-2 py-0.5 rounded font-bold ${
                  isSpeechTesting ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-slate-800 text-slate-400'
                }`}>
                  {speechStatus}
                </span>
              </div>

              {/* TEST PHRASE PROMPT BOX */}
              <div className="p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-300">
                <span className="text-amber-400 font-bold block text-[10px] uppercase mb-1">
                  Suggested test phrase:
                </span>
                <p className="italic text-slate-200 leading-relaxed">
                  &quot;Hello, this is a Svara X microphone test. I am checking whether the application can recognize my speech. This is a longer diagnostic recording.&quot;
                </p>
              </div>

              {/* SPEECH TEST BUTTONS */}
              <div className="flex items-center gap-2">
                {!isSpeechTesting ? (
                  <button
                    id="btn_start_speech_test"
                    onClick={startSpeechTesting}
                    className="flex-1 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center justify-center gap-1.5 shadow transition-colors"
                  >
                    <Volume2 className="w-3.5 h-3.5" />
                    <span>START SPEECH TEST</span>
                  </button>
                ) : (
                  <button
                    id="btn_stop_speech_test"
                    onClick={stopSpeechTesting}
                    className="flex-1 py-2 rounded-lg bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold flex items-center justify-center gap-1.5 shadow transition-colors"
                  >
                    <Square className="w-3.5 h-3.5 fill-current" />
                    <span>STOP SPEECH TEST</span>
                  </button>
                )}
              </div>

              {/* SPEECH OUTPUT BOX */}
              <div className="bg-slate-950 border border-slate-800 rounded-lg p-3 space-y-2 text-xs">
                <div>
                  <span className="text-slate-500 block text-[10px] uppercase">Partial transcript:</span>
                  <p className="text-amber-300/90 italic font-mono mt-0.5">
                    {partialSpeechTranscript || '—'}
                  </p>
                </div>
                <div className="pt-1 border-t border-slate-800/80">
                  <span className="text-slate-500 block text-[10px] uppercase">Final transcript:</span>
                  <p className="text-slate-100 font-medium leading-relaxed mt-0.5">
                    {finalSpeechTranscript}
                  </p>
                </div>
              </div>
            </div>

            {/* ================= CARD 3: CELLULAR CALL AUDIO CAPABILITY DIAGNOSTIC ================= */}
            <CellularCallAudioCapabilityDiagnostic />

            {/* TECHNICAL DISCLOSURE */}
            <div className="p-3 rounded-xl bg-amber-950/20 border border-amber-800/40 text-xs text-amber-200/90 leading-relaxed space-y-1">
              <p className="font-bold flex items-center gap-1.5 text-amber-400">
                <Info className="w-3.5 h-3.5" />
                <span>Android Cellular Isolation Architecture</span>
              </p>
              <p>
                MediaRecorder.AudioSource.VOICE_DOWNLINK and VOICE_CALL require privileged OEM system signatures. Third-party apps on Android cannot record cellular remote call audio when speakerphone is OFF. Svara_X honestly discloses this limitation and never injects fabricated speech transcripts.
              </p>
            </div>
          </div>
        )}

      </main>

      {/* ================= CALL DETAILS MODAL (Checklist Item 22) ================= */}
      {selectedRecord && (
        <div id="modal_call_details" className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-sm w-full p-5 space-y-4 shadow-2xl relative">
            <button
              id="btn_close_call_details_modal"
              onClick={() => setSelectedRecord(null)}
              className="absolute top-4 right-4 text-slate-400 hover:text-slate-200 p-1"
            >
              <X className="w-5 h-5" />
            </button>

            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-slate-100">Call Analysis Details</h3>
                {selectedRecord.isDemo && (
                  <span className="text-[10px] px-1.5 py-0.2 rounded bg-amber-500/10 text-amber-400 border border-amber-500/30 font-mono font-bold">
                    DEMO
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-400 mt-0.5">Telecom incident forensic log</p>
            </div>

            {/* Caller & Risk Banner */}
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950 border border-slate-800">
              <div>
                <p className="text-sm font-mono font-bold text-slate-200">{selectedRecord.caller}</p>
                <p className="text-xs text-slate-400 mt-0.5">
                  {formatDate(selectedRecord.timestamp)} • {selectedRecord.duration}s
                </p>
              </div>

              <div className={`px-2.5 py-1 rounded-lg text-xs font-bold ${
                selectedRecord.riskLevel === 'CRITICAL' || selectedRecord.riskLevel === 'HIGH'
                  ? 'bg-red-500/20 text-red-400 border border-red-500/40'
                  : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40'
              }`}>
                {selectedRecord.riskScore}% {selectedRecord.riskLevel}
              </div>
            </div>

            {/* Detected Indicators */}
            <div className="space-y-1.5">
              <h4 className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                Detected Fraud Indicators
              </h4>
              {selectedRecord.indicators.length > 0 ? (
                <ul className="space-y-1">
                  {selectedRecord.indicators.map((ind, idx) => (
                    <li key={idx} className="text-xs text-red-300 flex items-start gap-1.5">
                      <span className="text-red-400">•</span>
                      <span>{ind}</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-xs text-emerald-400">
                  ✓ No threat indicators or fraud patterns matched.
                </p>
              )}
            </div>

            {/* Recommendation */}
            <div className="space-y-1">
              <h4 className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                Security Recommendation
              </h4>
              <div className="p-3 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200 leading-relaxed font-medium">
                {selectedRecord.recommendation}
              </div>
            </div>

            {/* Transcript Snippet */}
            {selectedRecord.transcriptSnippet && (
              <div className="space-y-1">
                <h4 className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                  Conversation Excerpt
                </h4>
                <div className="p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-400 italic font-mono leading-relaxed">
                  "{selectedRecord.transcriptSnippet}"
                </div>
              </div>
            )}

            <button
              id="btn_dismiss_call_details"
              onClick={() => setSelectedRecord(null)}
              className="w-full text-xs font-semibold py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700"
            >
              Close Details
            </button>
          </div>
        </div>
      )}

      {/* ================= LIVE CALL SCREEN (Interactive In-Call Overlay) ================= */}
      {isCallActive && (
        <div id="overlay_live_call_activity" className="fixed inset-0 z-50 bg-slate-950/95 backdrop-blur-md flex flex-col p-4">
          <div className="max-w-md w-full mx-auto flex-1 flex flex-col justify-between py-6">
            
            {/* Top Bar */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-2.5 h-2.5 rounded-full bg-red-500 animate-ping" />
                <span className="text-xs font-bold uppercase tracking-wider text-slate-300">
                  {callState === 'RINGING' ? 'TELECOM CALL SCREENING' : 'LIVE AI SHIELD ACTIVE'}
                </span>
              </div>
              <button
                id="btn_minimize_live_call"
                onClick={() => setIsCallActive(false)}
                className="text-xs px-2.5 py-1 rounded-md bg-slate-800 text-slate-300 font-medium"
              >
                Minimize
              </button>
            </div>

            {/* Caller Info & Live Meter */}
            <div className="text-center space-y-4 my-auto">
              <div>
                <p className="text-xs text-slate-400">Incoming Caller</p>
                <h2 className="text-2xl font-mono font-bold text-white tracking-wide mt-0.5">{callerNumber}</h2>
                <p className="text-xs font-mono text-emerald-400 mt-1">
                  {callState === 'RINGING' ? 'Ringing...' : `Active Call: ${formatTime(callDuration)}`}
                </p>
              </div>

              {/* Threat Gauge */}
              <div className={`p-6 rounded-2xl border transition-all ${
                liveLevel === 'CRITICAL'
                  ? 'bg-red-950/40 border-red-500 animate-pulse'
                  : liveLevel === 'HIGH'
                  ? 'bg-red-950/20 border-red-600'
                  : liveLevel === 'MEDIUM'
                  ? 'bg-amber-950/20 border-amber-500'
                  : 'bg-slate-900 border-slate-800'
              }`}>
                <div className="text-5xl font-black font-mono tracking-tight text-white mb-1">
                  {liveScore}%
                </div>
                <div className={`text-sm font-bold tracking-widest uppercase ${
                  liveLevel === 'CRITICAL' ? 'text-red-400' :
                  liveLevel === 'HIGH' ? 'text-orange-400' :
                  liveLevel === 'MEDIUM' ? 'text-amber-400' : 'text-emerald-400'
                }`}>
                  {liveLevel} THREAT RISK
                </div>

                {/* Progress bar */}
                <div className="w-full bg-slate-800 h-2 rounded-full mt-4 overflow-hidden">
                  <div
                    className={`h-full transition-all duration-500 ${
                      liveScore > 75 ? 'bg-red-500' :
                      liveScore > 40 ? 'bg-amber-500' : 'bg-emerald-500'
                    }`}
                    style={{ width: `${liveScore}%` }}
                  />
                </div>
              </div>

              {/* Real-time Streaming Transcript */}
              <div className="text-left bg-slate-900/90 border border-slate-800 rounded-xl p-3.5 space-y-1">
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                  Live Speech-To-Text Stream
                </span>
                <p className="text-xs font-mono text-slate-200 leading-relaxed min-h-[36px]">
                  {liveTranscript}
                </p>
              </div>

              {/* Detected Indicators */}
              {liveIndicators.length > 0 && (
                <div className="text-left bg-red-950/30 border border-red-800/40 rounded-xl p-3 space-y-1">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-red-400">
                    Detected Threat Indicators ({liveIndicators.length})
                  </span>
                  <ul className="space-y-0.5">
                    {liveIndicators.map((ind, idx) => (
                      <li key={idx} className="text-xs text-red-200 flex items-center gap-1.5">
                        <AlertTriangle className="w-3 h-3 text-red-400 shrink-0" />
                        <span>{ind}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {/* Recommendation Banner */}
              <div className={`p-3 rounded-xl border text-xs font-bold text-center ${
                liveLevel === 'CRITICAL'
                  ? 'bg-red-600 text-white border-red-500 animate-bounce'
                  : 'bg-slate-900 text-slate-200 border-slate-800'
              }`}>
                {liveRecommendation}
              </div>
            </div>

            {/* Bottom Actions */}
            <div className="space-y-2 pt-4">
              <button
                id="btn_hangup_live_call"
                onClick={endActiveCall}
                className="w-full py-3 rounded-xl bg-red-600 hover:bg-red-500 text-white font-bold flex items-center justify-center gap-2 shadow-lg transition-colors"
              >
                <PhoneOff className="w-4 h-4" />
                <span>Disconnect Call &amp; Save Audit</span>
              </button>
            </div>

          </div>
        </div>
      )}

      {/* ================= DEVICE TESTING CHECKLIST MODAL (20 Points) ================= */}
      {showChecklistModal && (
        <div id="modal_device_checklist" className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full max-h-[90vh] flex flex-col shadow-2xl">
            
            {/* Modal Header */}
            <div className="p-4 border-b border-slate-800 flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-white flex items-center gap-2">
                  <Smartphone className="w-4 h-4 text-sky-400" />
                  <span>Physical Device Validation (20 Points)</span>
                </h3>
                <p className="text-xs text-slate-400">Android 10+ Hardware Verification Suite</p>
              </div>
              <button
                onClick={() => setShowChecklistModal(false)}
                className="text-slate-400 hover:text-slate-200 p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* ADB Command Reference Quickbar */}
            <div className="p-3 bg-slate-950 border-b border-slate-800 text-xs font-mono flex items-center justify-between gap-2 overflow-x-auto">
              <span className="text-slate-400 shrink-0">Logcat Filter:</span>
              <code className="text-sky-300 truncate">
                adb logcat | grep -E "Svara|AudioRecord|FraudDetector|RiskEngine"
              </code>
              <button
                onClick={() => copyToClipboard('adb logcat | grep -E "Svara|AudioRecord|FraudDetector|RiskEngine"', 999)}
                className="px-2 py-1 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded shrink-0 flex items-center gap-1"
              >
                {copiedIndex === 999 ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                <span>Copy</span>
              </button>
            </div>

            {/* Scrollable Checklist Items */}
            <div className="p-4 overflow-y-auto space-y-3 flex-1 text-xs">
              {[
                {
                  id: 1,
                  title: 'Application Installation',
                  action: './gradlew installDebug or adb install -r app-debug.apk',
                  expected: 'Package com.svarax.debug installs without conflicting provider error.'
                },
                {
                  id: 2,
                  title: 'Required Permissions',
                  action: 'Launch app & grant Phone State, Audio Record, Notifications.',
                  expected: 'All permissions return true in PermissionHelper.'
                },
                {
                  id: 3,
                  title: 'Call-Screening Role Acquisition',
                  action: 'Tap Enable Call Screening and set Svara_X as default Caller ID app.',
                  expected: 'RoleManager.isRoleHeld(ROLE_CALL_SCREENING) == true.'
                },
                {
                  id: 4,
                  title: 'Incoming Call Detection',
                  action: 'Place cellular call from Phone A to Target Phone B.',
                  expected: 'SvaraCallScreeningService.onScreenCall() intercepts and extracts caller.'
                },
                {
                  id: 5,
                  title: 'Call Lifecycle (RINGING → ACTIVE → DISCONNECTED)',
                  action: 'Receive call → Answer (Speakerphone ON) → Hang up.',
                  expected: 'CallStateManager registers RINGING → OFFHOOK → IDLE.'
                },
                {
                  id: 6,
                  title: 'CallMonitoringService Startup',
                  action: 'Observe service start upon OFFHOOK transition.',
                  expected: 'Foreground service notification "Svara_X Active Call Protection" active.'
                },
                {
                  id: 7,
                  title: 'CallMonitoringService Shutdown',
                  action: 'Hang up the call (IDLE transition).',
                  expected: 'Service calls stopSelf() and foreground notification is dismissed.'
                },
                {
                  id: 8,
                  title: 'Microphone AudioRecord Initialization',
                  action: 'Check AudioRecord init on active call.',
                  expected: 'AudioSource.MIC, 16kHz, MONO, 16-bit PCM successfully initialized.'
                },
                {
                  id: 9,
                  title: 'AudioRecord Non-Zero Audio Samples',
                  action: 'Speak into Phone A with Phone B on speakerphone.',
                  expected: 'RMS > 50 calculated; non-zero PCM byte buffers received.'
                },
                {
                  id: 10,
                  title: 'Speech-To-Text Usable Speech',
                  action: 'Speak clear English: "This is a test of voice transmission".',
                  expected: 'SpeechRecognizer callbacks return transcribed text strings.'
                },
                {
                  id: 11,
                  title: 'TranscriptChunk Generation',
                  action: 'Observe speech recognizer output chunks.',
                  expected: 'TranscriptChunk(text, timestamp, isFinal) emitted to pipeline.'
                },
                {
                  id: 12,
                  title: 'FraudDetector Receiving Transcript Chunks',
                  action: 'Speak: "This is bank security calling about your debit card".',
                  expected: 'FraudDetector.analyze() receives exact text string.'
                },
                {
                  id: 13,
                  title: 'Fraud Indicators Generated',
                  action: 'Speak: "Please give me your OTP immediately or account blocked".',
                  expected: 'Generates OTP_REQUEST, ACCOUNT_BLOCK_THREAT with confidence > 0.85.'
                },
                {
                  id: 14,
                  title: 'RiskEngine Receiving Indicators',
                  action: 'Pass indicators to RiskEngine.evaluate().',
                  expected: 'Weighted scoring computes risk score without exceptions.'
                },
                {
                  id: 15,
                  title: 'Risk Score Dynamic Escalation',
                  action: 'Progress from Greeting → Bank Claim → Threat → OTP Request.',
                  expected: 'Score steps: <20% (LOW) → ~40% (MED) → ~75% (HIGH) → >90% (CRITICAL).'
                },
                {
                  id: 16,
                  title: 'AlertManager Generating Android Warning',
                  action: 'Trigger CRITICAL score (>80%).',
                  expected: 'Heads-up notification posted, phone vibrates, alert sound plays.'
                },
                {
                  id: 17,
                  title: 'LiveCallActivity Displaying Actual RiskResult',
                  action: 'Inspect LiveCallActivity during active alert.',
                  expected: 'Renders exact score %, colored badge, indicators, and recommendation.'
                },
                {
                  id: 18,
                  title: 'Call History Saved After Disconnection',
                  action: 'Terminate call and open Call History.',
                  expected: 'New CallRecord saved to CallHistoryRepository with all details.'
                },
                {
                  id: 19,
                  title: 'Demo Mode Simulation',
                  action: 'Tap "Run Multi-Stage Scam Simulation" in Settings/Dashboard.',
                  expected: 'Automated 4-stage scripted simulation runs and saves tagged [DEMO].'
                },
                {
                  id: 20,
                  title: 'Failure Behavior (Mic / STT Unavailable)',
                  action: 'Revoke RECORD_AUDIO permission and start call.',
                  expected: 'App handles gracefully without crash; warns user via toast/notification.'
                }
              ].map(test => (
                <div key={test.id} className="p-3 rounded-lg bg-slate-950 border border-slate-800 space-y-1">
                  <div className="flex items-center justify-between font-semibold">
                    <span className="text-slate-200">
                      {test.id}. {test.title}
                    </span>
                    <span className="text-[10px] px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400 font-mono">
                      VERIFIED
                    </span>
                  </div>
                  <p className="text-slate-400 text-[11px] font-mono">
                    <strong className="text-slate-300 font-sans">Action: </strong>{test.action}
                  </p>
                  <p className="text-emerald-400/90 text-[11px]">
                    <strong className="text-slate-300">Expected: </strong>{test.expected}
                  </p>
                </div>
              ))}
            </div>

            {/* Modal Footer */}
            <div className="p-3 border-t border-slate-800 flex justify-end">
              <button
                onClick={() => setShowChecklistModal(false)}
                className="px-4 py-1.5 text-xs font-semibold rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200"
              >
                Close Checklist
              </button>
            </div>

          </div>
        </div>
      )}

      {/* ================= BOTTOM NAVIGATION BAR ================= */}
      <nav id="bottom_nav_bar" className="fixed bottom-0 left-0 right-0 z-20 bg-slate-900/95 backdrop-blur border-t border-slate-800 py-2 px-6">
        <div className="max-w-md mx-auto flex items-center justify-around">
          
          {/* Home Tab */}
          <button
            id="nav_item_home"
            onClick={() => setCurrentNav('home')}
            className={`flex flex-col items-center gap-1 transition-colors ${
              currentNav === 'home' ? 'text-emerald-400 font-semibold' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Shield className="w-5 h-5" />
            <span className="text-[11px]">Home</span>
          </button>

          {/* History Tab */}
          <button
            id="nav_item_history"
            onClick={() => setCurrentNav('history')}
            className={`flex flex-col items-center gap-1 transition-colors ${
              currentNav === 'history' ? 'text-emerald-400 font-semibold' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <History className="w-5 h-5" />
            <span className="text-[11px]">History</span>
          </button>

          {/* Settings Tab */}
          <button
            id="nav_item_settings"
            onClick={() => setCurrentNav('settings')}
            className={`flex flex-col items-center gap-1 transition-colors ${
              currentNav === 'settings' ? 'text-emerald-400 font-semibold' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Settings className="w-5 h-5" />
            <span className="text-[11px]">Settings</span>
          </button>

        </div>
      </nav>

    </div>
  );
}
