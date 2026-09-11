import React, { useState, useEffect, useRef } from 'react';
import {
  Shield,
  Phone,
  ShieldCheck,
  Settings,
  ChevronRight,
  Sparkles,
  Sun,
  Moon
} from 'lucide-react';
import {
  NavTab,
  SettingsSubView,
  RiskLevel,
  CallState,
  CallRecordItem,
  AudioMetrics,
  AudioTestResult,
  ThemeMode
} from './types';
import { HomeView } from './components/HomeView';
import { CallsView } from './components/CallsView';
import { ProtectionView } from './components/ProtectionView';
import { SettingsView } from './components/SettingsView';
import { DiagnosticsView } from './components/DiagnosticsView';
import { DemoTestingView } from './components/DemoTestingView';
import { DeveloperTestingView } from './components/DeveloperTestingView';
import { CallDetailsModal } from './components/CallDetailsModal';
import { LiveCallOverlay } from './components/LiveCallOverlay';

export default function App() {
  // Theme Mode ('light' default matching reference mockup, with 'dark' support)
  const [themeMode, setThemeMode] = useState<ThemeMode>(() => {
    const saved = localStorage.getItem('svara_theme_mode');
    return (saved === 'dark' || saved === 'light') ? saved : 'light';
  });

  useEffect(() => {
    if (themeMode === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
    localStorage.setItem('svara_theme_mode', themeMode);
  }, [themeMode]);

  // 4 Primary Navigation Tabs (Home, Calls, Protection, Settings)
  const [currentNav, setCurrentNav] = useState<NavTab>('home');
  const [settingsSubView, setSettingsSubView] = useState<SettingsSubView>('main');

  // Active Call State
  const [isCallActive, setIsCallActive] = useState(false);
  const [isCallMinimized, setIsCallMinimized] = useState(false);
  const [callState, setCallState] = useState<CallState>('IDLE');
  const [callerNumber, setCallerNumber] = useState('+91 98765 43210');
  const [liveScore, setLiveScore] = useState(8);
  const [liveLevel, setLiveLevel] = useState<RiskLevel>('LOW');
  const [liveTranscript, setLiveTranscript] = useState('');
  const [liveIndicators, setLiveIndicators] = useState<string[]>([]);
  const [liveRecommendation, setLiveRecommendation] = useState('Conversation verified. No threats detected.');
  const [callDuration, setCallDuration] = useState(0);

  // Audio Record Test State (Under Settings -> Advanced -> Diagnostics)
  const [selectedDurationOption, setSelectedDurationOption] = useState<number | 'custom'>(30);
  const [customDurationInput, setCustomDurationInput] = useState('45');
  const [isAudioRecording, setIsAudioRecording] = useState(false);
  const [audioElapsedSeconds, setAudioElapsedSeconds] = useState(0);
  const [audioTargetSeconds, setAudioTargetSeconds] = useState(30);
  const [audioMetrics, setAudioMetrics] = useState<AudioMetrics>({
    currentRms: 0,
    peakRms: 0,
    samplesRead: 0,
    nonZeroSamples: 0,
    signal: 'SILENCE'
  });
  const [audioTestResult, setAudioTestResult] = useState<AudioTestResult | null>(null);

  // Speech Recognition Test State (Under Settings -> Advanced -> Diagnostics)
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

  // Settings & System Permissions
  const [permissionPhone, setPermissionPhone] = useState(true);
  const [permissionMic, setPermissionMic] = useState(true);
  const [permissionNotif, setPermissionNotif] = useState(true);
  const [roleCallScreening, setRoleCallScreening] = useState(true);

  // Persistent Call History State
  const [callHistory, setCallHistory] = useState<CallRecordItem[]>([]);

  // Modal Details State
  const [selectedRecord, setSelectedRecord] = useState<CallRecordItem | null>(null);

  // Dynamic Protection Score (Calculated from real active capabilities)
  const protectionScore = Math.round(
    (permissionPhone ? 25 : 0) +
    (permissionMic ? 25 : 0) +
    (permissionNotif ? 25 : 0) +
    (roleCallScreening ? 25 : 0)
  );

  const isFullyProtected = permissionPhone && permissionMic && permissionNotif && roleCallScreening;

  // Active Call Timer
  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (callState === 'OFFHOOK') {
      interval = setInterval(() => {
        setCallDuration((prev) => prev + 1);
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [callState]);

  // Multi-Stage Scam Simulation Handler
  const startScamSimulation = () => {
    setIsCallActive(true);
    setIsCallMinimized(false);
    setCallState('RINGING');
    setCallerNumber('+91 98765 43210');
    setLiveScore(10);
    setLiveLevel('LOW');
    setLiveTranscript('Incoming call from +91 98765 43210...');
    setLiveIndicators([]);
    setLiveRecommendation('Monitoring conversation via microphone...');
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
        setLiveIndicators(['Banking institution claim', 'Authority impersonation']);
        setLiveRecommendation('Verify caller identity independently before continuing.');
      }, 4000);

      // Stage 3: Account Block Threat
      setTimeout(() => {
        setLiveScore(76);
        setLiveLevel('HIGH');
        setLiveTranscript('"Your debit card has unauthorized international transactions. Your account will be locked within 15 minutes unless verified."');
        setLiveIndicators([
          'Banking institution claim',
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
        setLiveRecommendation('Do not share OTPs or PINs. End the call immediately.');
      }, 10000);
    }, 1500);
  };

  const endActiveCall = () => {
    if (callState === 'OFFHOOK' || callState === 'RINGING') {
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
      setCallHistory((prev) => [newRec, ...prev]);
    }
    setCallState('IDLE');
    setIsCallActive(false);
    setIsCallMinimized(false);
    setCallDuration(0);
  };

  const seedDemoHistory = () => {
    setCallHistory([
      {
        id: 'demo-rec-1',
        caller: '+91 98230 11942',
        timestamp: Date.now() - 3600000 * 2,
        duration: 64,
        riskScore: 95,
        riskLevel: 'CRITICAL',
        indicators: [
          'Direct OTP credential request',
          'Account block threat ultimatum',
          'Impersonation of banking authority',
          'Psychological panic & urgency pressure'
        ],
        recommendation: 'Do not share OTPs or PINs. Contact bank via official number.',
        transcriptSnippet: 'This is bank security department. Your account will be blocked within 30 minutes unless you share the 6-digit OTP right now.',
        isDemo: true
      },
      {
        id: 'demo-rec-2',
        caller: '+91 98450 44321',
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

  // Audio Record Test logic
  const startAudioRecording = () => {
    if (isAudioRecording || isSpeechTesting) return;

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

      const chunkSamples = 1600;
      const nonZero = Math.round(chunkSamples * 0.994);
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

  const stopAudioRecording = (isUserInitiated = true) => {
    if (audioIntervalRef.current) {
      clearInterval(audioIntervalRef.current);
      audioIntervalRef.current = null;
    }
    setIsAudioRecording(false);

    const ref = audioMetricsRef.current;
    const durationMs = Date.now() - ref.startTime;
    const actualSeconds = durationMs / 1000;

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

  // Speech Recognition Test logic
  const startSpeechTesting = () => {
    if (isSpeechTesting || isAudioRecording) return;

    setIsSpeechTesting(true);
    setSpeechStatus('LISTENING...');
    setPartialSpeechTranscript('Listening for test speech...');
    setFinalSpeechTranscript('Speak naturally: "Hello, this is a Svara X microphone test..."');

    const phrases = [
      { delay: 1500, status: 'SPEECH DETECTED...' as const, partial: '"Hello, this is a Svara X..."', final: '' },
      { delay: 3200, status: 'LISTENING...' as const, partial: '—', final: '"Hello, this is a Svara X microphone test."' },
      { delay: 5500, status: 'SPEECH DETECTED...' as const, partial: '"I am checking whether the application..."', final: '"Hello, this is a Svara X microphone test."' },
      { delay: 8000, status: 'LISTENING...' as const, partial: '—', final: '"Hello, this is a Svara X microphone test. I am checking whether the application can recognize my speech."' }
    ];

    phrases.forEach((step) => {
      setTimeout(() => {
        setSpeechStatus((prev) => (prev === 'TEST COMPLETE' ? prev : step.status));
        setPartialSpeechTranscript((prev) => (prev === '—' ? prev : step.partial));
        if (step.final) setFinalSpeechTranscript(step.final);
      }, step.delay);
    });
  };

  const stopSpeechTesting = () => {
    setIsSpeechTesting(false);
    setSpeechStatus('TEST COMPLETE');
    setPartialSpeechTranscript('—');
  };

  const handleClearHistory = () => {
    setCallHistory([]);
  };

  const handleDeleteSingleRecord = (id: string) => {
    setCallHistory((prev) => prev.filter((r) => r.id !== id));
  };

  const handleVerifyAllPermissions = () => {
    setPermissionPhone(true);
    setPermissionMic(true);
    setPermissionNotif(true);
    setRoleCallScreening(true);
  };

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  return (
    <div
      id="svara_app_root"
      className="min-h-screen bg-slate-50 dark:bg-[#0B1220] text-slate-900 dark:text-[#F8FAFC] flex flex-col font-sans selection:bg-[#2563EB] selection:text-white transition-colors"
    >
      {/* TOP APPLICATION BAR */}
      <header
        id="top_app_bar"
        className="bg-white/95 dark:bg-[#0B1220]/95 backdrop-blur border-b border-slate-200 dark:border-slate-800/80 sticky top-0 z-30 px-4 py-3"
      >
        <div className="max-w-md mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-blue-50 dark:bg-[#4F7CFF]/10 border border-blue-200 dark:border-[#4F7CFF]/30 flex items-center justify-center text-[#2563EB] dark:text-[#4F7CFF]">
              <Shield className="w-4 h-4 fill-[#2563EB]/20 dark:fill-[#4F7CFF]/20" />
            </div>
            <div>
              <h1 className="text-sm font-serif font-bold text-slate-900 dark:text-slate-100 leading-tight tracking-tight">
                Svara_X
              </h1>
              <p className="text-[10px] text-slate-500 dark:text-slate-400 font-medium leading-none">
                AI Call Protection
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {/* Quick theme toggle */}
            <button
              id="btn_toggle_theme_quick"
              onClick={() => setThemeMode(themeMode === 'dark' ? 'light' : 'dark')}
              className="p-1.5 rounded-xl border border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white bg-slate-100/80 dark:bg-slate-800/60 transition-colors"
              title={`Switch to ${themeMode === 'dark' ? 'Light' : 'Dark'} mode`}
            >
              {themeMode === 'dark' ? (
                <Sun className="w-3.5 h-3.5 text-amber-400" />
              ) : (
                <Moon className="w-3.5 h-3.5 text-slate-600" />
              )}
            </button>

            <span className="text-[11px] font-semibold text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/20 px-2.5 py-1 rounded-full flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
              <span>Protected</span>
            </span>
          </div>
        </div>
      </header>

      {/* MAIN CONTAINER */}
      <main className="flex-1 max-w-md w-full mx-auto p-4 pb-24 flex flex-col">
        {/* ACTIVE LIVE CALL BANNER (WHEN CALL IS MINIMIZED) */}
        {callState !== 'IDLE' && isCallMinimized && (
          <div
            id="banner_active_call"
            className="mb-4 bg-white dark:bg-[#151E2E] border-2 border-[#F59E0B] rounded-2xl p-4 shadow-xl animate-in slide-in-from-top-2"
          >
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <span className="relative flex h-2.5 w-2.5">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75" />
                  <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-amber-500" />
                </span>
                <span className="text-xs font-bold uppercase tracking-wider text-amber-600 dark:text-amber-400">
                  {callState === 'RINGING' ? 'Incoming Call Screened' : 'Call in Progress'}
                </span>
              </div>
              <span className="text-xs font-mono font-bold text-slate-600 dark:text-slate-300">
                {callState === 'OFFHOOK' ? formatTime(callDuration) : 'Ringing...'}
              </span>
            </div>

            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-mono font-bold text-slate-900 dark:text-white">{callerNumber}</p>
                <p className="text-[11px] text-slate-500 dark:text-slate-400">
                  {callState === 'RINGING' ? 'Screening active' : `${liveLevel} RISK (${liveScore}%)`}
                </p>
              </div>
              <button
                id="btn_open_live_screen_modal"
                onClick={() => {
                  setIsCallActive(true);
                  setIsCallMinimized(false);
                }}
                className="text-xs px-3.5 py-1.5 rounded-xl bg-[#2563EB] hover:bg-blue-600 text-white font-semibold flex items-center gap-1 shadow transition-all"
              >
                <span>Live View</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        )}

        {/* TAB 1: HOME */}
        {currentNav === 'home' && (
          <HomeView
            isFullyProtected={isFullyProtected}
            protectionScore={protectionScore}
            callHistory={callHistory}
            themeMode={themeMode}
            onSelectRecord={(rec) => setSelectedRecord(rec)}
            onNavigate={(tab) => {
              setCurrentNav(tab);
              setSettingsSubView('main');
            }}
            onEnableProtection={handleVerifyAllPermissions}
          />
        )}

        {/* TAB 2: CALLS */}
        {currentNav === 'calls' && (
          <CallsView
            callHistory={callHistory}
            themeMode={themeMode}
            onSelectRecord={(rec) => setSelectedRecord(rec)}
            onClearHistory={handleClearHistory}
            onRunSimulation={startScamSimulation}
          />
        )}

        {/* TAB 3: PROTECTION */}
        {currentNav === 'protection' && (
          <ProtectionView
            isFullyProtected={isFullyProtected}
            protectionScore={protectionScore}
            permissionPhone={permissionPhone}
            permissionMic={permissionMic}
            permissionNotif={permissionNotif}
            roleCallScreening={roleCallScreening}
            onEnableProtection={handleVerifyAllPermissions}
          />
        )}

        {/* TAB 4: SETTINGS & SUB-VIEWS */}
        {currentNav === 'settings' && (
          <>
            {settingsSubView === 'main' && (
              <SettingsView
                permissionPhone={permissionPhone}
                permissionMic={permissionMic}
                permissionNotif={permissionNotif}
                roleCallScreening={roleCallScreening}
                themeMode={themeMode}
                onToggleTheme={(mode) => setThemeMode(mode)}
                onTogglePhone={() => setPermissionPhone(!permissionPhone)}
                onToggleMic={() => setPermissionMic(!permissionMic)}
                onToggleNotif={() => setPermissionNotif(!permissionNotif)}
                onToggleRole={() => setRoleCallScreening(!roleCallScreening)}
                onVerifyAllPermissions={handleVerifyAllPermissions}
                onClearHistory={handleClearHistory}
                onNavigateSubView={(sub) => setSettingsSubView(sub)}
              />
            )}

            {settingsSubView === 'developer_testing' && (
              <DeveloperTestingView
                onBack={() => setSettingsSubView('main')}
                isAudioRecording={isAudioRecording}
                audioElapsedSeconds={audioElapsedSeconds}
                audioTargetSeconds={audioTargetSeconds}
                audioMetrics={audioMetrics}
                audioTestResult={audioTestResult}
                onStartAudioRecording={startAudioRecording}
                onStopAudioRecording={() => stopAudioRecording(true)}
                isSpeechTesting={isSpeechTesting}
                speechStatus={speechStatus}
                partialSpeechTranscript={partialSpeechTranscript}
                finalSpeechTranscript={finalSpeechTranscript}
                onStartSpeechTesting={startSpeechTesting}
                onStopSpeechTesting={stopSpeechTesting}
                onSaveRecordToHistory={(record) => setCallHistory((prev) => [record, ...prev])}
              />
            )}

            {settingsSubView === 'diagnostics' && (
              <DiagnosticsView
                onBack={() => setSettingsSubView('main')}
                isAudioRecording={isAudioRecording}
                audioElapsedSeconds={audioElapsedSeconds}
                audioTargetSeconds={audioTargetSeconds}
                audioMetrics={audioMetrics}
                audioTestResult={audioTestResult}
                selectedDurationOption={selectedDurationOption}
                customDurationInput={customDurationInput}
                onSelectDurationOption={setSelectedDurationOption}
                onChangeCustomDuration={setCustomDurationInput}
                onStartAudioRecording={startAudioRecording}
                onStopAudioRecording={() => stopAudioRecording(true)}
                isSpeechTesting={isSpeechTesting}
                speechStatus={speechStatus}
                partialSpeechTranscript={partialSpeechTranscript}
                finalSpeechTranscript={finalSpeechTranscript}
                onStartSpeechTesting={startSpeechTesting}
                onStopSpeechTesting={stopSpeechTesting}
              />
            )}

            {settingsSubView === 'demo' && (
              <DemoTestingView
                onBack={() => setSettingsSubView('main')}
                callState={callState}
                onStartScamSimulation={startScamSimulation}
                onEndCall={endActiveCall}
                onSeedDemoHistory={seedDemoHistory}
              />
            )}
          </>
        )}
      </main>

      {/* CALL DETAILS MODAL */}
      <CallDetailsModal
        record={selectedRecord}
        themeMode={themeMode}
        onClose={() => setSelectedRecord(null)}
        onDeleteRecord={handleDeleteSingleRecord}
      />

      {/* LIVE CALL OVERLAY (FULL SCREEN WHEN ACTIVE) */}
      {isCallActive && !isCallMinimized && (
        <LiveCallOverlay
          callState={callState}
          callerNumber={callerNumber}
          callDuration={callDuration}
          liveScore={liveScore}
          liveLevel={liveLevel}
          liveTranscript={liveTranscript}
          liveIndicators={liveIndicators}
          liveRecommendation={liveRecommendation}
          onEndCall={endActiveCall}
          onMinimize={() => setIsCallMinimized(true)}
        />
      )}

      {/* BOTTOM NAVIGATION BAR (HOME, CALLS, PROTECTION, SETTINGS) */}
      <nav
        id="bottom_nav_bar"
        className="fixed bottom-0 left-0 right-0 z-20 bg-white/95 dark:bg-[#0B1220]/95 backdrop-blur border-t border-slate-200 dark:border-slate-800/80 py-2.5 px-4 shadow-lg"
      >
        <div className="max-w-md mx-auto grid grid-cols-4 items-center">
          {/* 1. Home Tab */}
          <button
            id="nav_item_home"
            onClick={() => {
              setCurrentNav('home');
              setSettingsSubView('main');
            }}
            className={`flex flex-col items-center gap-1 transition-colors ${
              currentNav === 'home'
                ? 'text-[#2563EB] dark:text-[#4F7CFF] font-semibold'
                : 'text-slate-400 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
            }`}
          >
            <Shield className="w-5 h-5" />
            <span className="text-[10px] tracking-tight">Home</span>
          </button>

          {/* 2. Calls Tab */}
          <button
            id="nav_item_calls"
            onClick={() => {
              setCurrentNav('calls');
              setSettingsSubView('main');
            }}
            className={`flex flex-col items-center gap-1 transition-colors ${
              currentNav === 'calls'
                ? 'text-[#2563EB] dark:text-[#4F7CFF] font-semibold'
                : 'text-slate-400 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
            }`}
          >
            <Phone className="w-5 h-5" />
            <span className="text-[10px] tracking-tight">Calls</span>
          </button>

          {/* 3. Protection Tab */}
          <button
            id="nav_item_protection"
            onClick={() => {
              setCurrentNav('protection');
              setSettingsSubView('main');
            }}
            className={`flex flex-col items-center gap-1 transition-colors ${
              currentNav === 'protection'
                ? 'text-[#2563EB] dark:text-[#4F7CFF] font-semibold'
                : 'text-slate-400 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
            }`}
          >
            <ShieldCheck className="w-5 h-5" />
            <span className="text-[10px] tracking-tight">Protection</span>
          </button>

          {/* 4. Settings Tab */}
          <button
            id="nav_item_settings"
            onClick={() => {
              setCurrentNav('settings');
              setSettingsSubView('main');
            }}
            className={`flex flex-col items-center gap-1 transition-colors ${
              currentNav === 'settings'
                ? 'text-[#2563EB] dark:text-[#4F7CFF] font-semibold'
                : 'text-slate-400 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
            }`}
          >
            <Settings className="w-5 h-5" />
            <span className="text-[10px] tracking-tight">Settings</span>
          </button>
        </div>
      </nav>
    </div>
  );
}
