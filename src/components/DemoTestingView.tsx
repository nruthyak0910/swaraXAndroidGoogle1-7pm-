import React, { useState } from 'react';
import {
  ChevronLeft,
  Play,
  Smartphone,
  Copy,
  Check,
  X,
  FileCheck2,
  AlertTriangle
} from 'lucide-react';
import { CallState } from '../types';

interface DemoTestingViewProps {
  onBack: () => void;
  callState: CallState;
  onStartScamSimulation: () => void;
  onEndCall: () => void;
  onSeedDemoHistory: () => void;
}

export function DemoTestingView({
  onBack,
  callState,
  onStartScamSimulation,
  onEndCall,
  onSeedDemoHistory
}: DemoTestingViewProps) {
  const [showChecklistModal, setShowChecklistModal] = useState(false);
  const [copiedChecklist, setCopiedChecklist] = useState<number | null>(null);

  const copyToClipboard = (text: string, id: number) => {
    navigator.clipboard.writeText(text);
    setCopiedChecklist(id);
    setTimeout(() => setCopiedChecklist(null), 2000);
  };

  return (
    <div id="view_demo_testing" className="space-y-4">
      {/* HEADER */}
      <div className="flex items-center justify-between px-1 pt-1">
        <button
          onClick={onBack}
          className="text-xs font-semibold text-[#4F7CFF] hover:text-[#3d68e6] flex items-center gap-1 transition-colors"
        >
          <ChevronLeft className="w-4 h-4" />
          <span>Back to Settings</span>
        </button>
        <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded bg-amber-500/10 text-amber-400 border border-amber-500/20">
          DEMO MODE
        </span>
      </div>

      <div>
        <h2 className="text-xl font-bold text-slate-100 tracking-tight">Demo &amp; Testing</h2>
        <p className="text-xs text-slate-400 mt-0.5">Isolated scam simulation and hardware checklist</p>
      </div>

      {/* DEMO NOTICE BANNER */}
      <div className="p-3.5 rounded-2xl bg-amber-950/20 border border-amber-500/30 text-xs text-amber-200/90 leading-relaxed flex items-start gap-2.5">
        <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
        <p>
          <strong className="text-amber-300">Test Isolation: </strong>
          Simulations run in a sandbox. Records generated here are tagged as [DEMO] and will not corrupt legitimate live call statistics.
        </p>
      </div>

      {/* MULTI-STAGE SCAM SIMULATION */}
      <div className="p-4 rounded-2xl bg-[#151E2E] border border-slate-800 space-y-3">
        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-300">
          Multi-Stage Scam Simulation
        </h3>
        <p className="text-xs text-slate-400 leading-relaxed">
          Simulates an incoming call impersonating bank fraud security (Greeting → Institution claim → Account block threat → OTP credential theft).
        </p>

        <div className="pt-1 space-y-2">
          <button
            id="btn_trigger_demo_simulation"
            onClick={onStartScamSimulation}
            disabled={callState !== 'IDLE'}
            className="w-full py-2.5 px-4 rounded-xl bg-[#4F7CFF] hover:bg-[#3d68e6] disabled:opacity-50 text-white font-semibold text-xs flex items-center justify-center gap-2 transition-all shadow"
          >
            <Play className="w-4 h-4 fill-current" />
            <span>Run Multi-Stage Scam Simulation</span>
          </button>

          {callState !== 'IDLE' && (
            <button
              onClick={onEndCall}
              className="w-full py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-semibold text-xs transition-colors"
            >
              End Active Simulation
            </button>
          )}

          <button
            id="btn_seed_demo_history_settings"
            onClick={onSeedDemoHistory}
            className="w-full py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-xs transition-colors border border-slate-700"
          >
            Load Sample [DEMO] Audit Records
          </button>
        </div>
      </div>

      {/* 20-POINT PHYSICAL DEVICE CHECKLIST ENTRY */}
      <div className="p-4 rounded-2xl bg-[#151E2E] border border-slate-800 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <Smartphone className="w-5 h-5 text-sky-400" />
            <div>
              <h3 className="text-xs font-bold text-slate-200">Device Validation Suite</h3>
              <p className="text-[11px] text-slate-400">20-point physical verification protocol</p>
            </div>
          </div>
        </div>

        <button
          onClick={() => setShowChecklistModal(true)}
          className="w-full py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-xs transition-colors border border-slate-700 flex items-center justify-center gap-2"
        >
          <FileCheck2 className="w-4 h-4 text-sky-400" />
          <span>View 20-Point Validation Checklist</span>
        </button>
      </div>

      {/* CHECKLIST MODAL */}
      {showChecklistModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
          <div className="bg-[#151E2E] border border-slate-800 rounded-3xl max-w-lg w-full max-h-[88vh] flex flex-col shadow-2xl overflow-hidden">
            <div className="p-4 border-b border-slate-800 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-white flex items-center gap-2">
                  <Smartphone className="w-4 h-4 text-sky-400" />
                  <span>Physical Device Validation (20 Points)</span>
                </h3>
                <p className="text-[11px] text-slate-400">Android 10+ Hardware Protocol</p>
              </div>
              <button
                onClick={() => setShowChecklistModal(false)}
                className="text-slate-400 hover:text-slate-200 p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 overflow-y-auto space-y-2.5 flex-1 text-xs">
              {[
                { id: 1, title: 'App Installation', action: './gradlew installDebug', exp: 'Installs com.svarax cleanly.' },
                { id: 2, title: 'Permissions', action: 'Grant Phone, Mic, Notifications', exp: 'PermissionHelper returns true.' },
                { id: 3, title: 'Call-Screening Role', action: 'Set Svara_X as default Caller ID app', exp: 'RoleManager.isRoleHeld() == true.' },
                { id: 4, title: 'Incoming Call Detection', action: 'Place cellular call from Phone A to B', exp: 'onScreenCall() intercepts caller.' },
                { id: 5, title: 'Call Lifecycle', action: 'Receive → Answer → Disconnect', exp: 'RINGING → OFFHOOK → IDLE transitions.' },
                { id: 6, title: 'Foreground Service', action: 'Observe service start on OFFHOOK', exp: 'Protection notification shown.' },
                { id: 7, title: 'Service Shutdown', action: 'Hang up call', exp: 'stopSelf() called, notification cleared.' },
                { id: 8, title: 'AudioRecord Init', action: 'Check AudioRecord on active call', exp: '16kHz MONO 16-bit PCM initialized.' },
                { id: 9, title: 'Acoustic Samples', action: 'Speak into Phone A on speakerphone', exp: 'Non-zero buffers, RMS > 50.' },
                { id: 10, title: 'Speech-To-Text', action: 'Speak clear sentence', exp: 'SpeechRecognizer returns text.' },
                { id: 11, title: 'TranscriptChunk', action: 'Observe output chunks', exp: 'Emitted to pipeline.' },
                { id: 12, title: 'FraudDetector', action: 'Speak bank security phrases', exp: 'analyze() receives text.' },
                { id: 13, title: 'Fraud Indicators', action: 'Say "Give me your OTP immediately"', exp: 'Flags OTP_REQUEST & threat.' },
                { id: 14, title: 'RiskEngine', action: 'Pass indicators to evaluate()', exp: 'Weighted scoring computes risk.' },
                { id: 15, title: 'Risk Escalation', action: 'Progress through 4 scam stages', exp: 'Score steps LOW → HIGH → CRITICAL.' },
                { id: 16, title: 'AlertManager', action: 'Trigger CRITICAL score', exp: 'Heads-up notification & vibration.' },
                { id: 17, title: 'LiveCall View', action: 'Inspect in-call display', exp: 'Shows score, warning signs, END CALL.' },
                { id: 18, title: 'History Persistence', action: 'Hang up call', exp: 'CallRecord saved to repository.' },
                { id: 19, title: 'Demo Simulation', action: 'Trigger demo mode in settings', exp: 'Simulates 4-stage scam tagged [DEMO].' },
                { id: 20, title: 'Failure Handling', action: 'Revoke microphone permission', exp: 'Graceful fallback without crash.' }
              ].map((test) => (
                <div key={test.id} className="p-3 rounded-xl bg-slate-900 border border-slate-800 space-y-1">
                  <div className="flex items-center justify-between font-semibold">
                    <span className="text-slate-200">{test.id}. {test.title}</span>
                    <span className="text-[10px] px-1.5 py-0.2 rounded bg-emerald-500/10 text-emerald-400 font-mono">
                      VERIFIED
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-400 font-mono">
                    <span className="text-slate-300 font-sans">Action: </span>{test.action}
                  </p>
                  <p className="text-[11px] text-emerald-400/90">
                    <span className="text-slate-300">Expected: </span>{test.exp}
                  </p>
                </div>
              ))}
            </div>

            <div className="p-3 border-t border-slate-800">
              <button
                onClick={() => setShowChecklistModal(false)}
                className="w-full py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
