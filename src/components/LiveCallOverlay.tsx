import React, { useState } from 'react';
import {
  ShieldCheck,
  ShieldAlert,
  PhoneOff,
  AlertTriangle,
  ChevronDown,
  ChevronUp,
  FileText,
  Volume2,
  Minimize2,
  ChevronLeft,
  MoreVertical,
  Mic,
  MicOff,
  VolumeX,
  Grid,
  Shield
} from 'lucide-react';
import { RiskLevel, CallState } from '../types';

interface LiveCallOverlayProps {
  callState: CallState;
  callerNumber: string;
  callDuration: number;
  liveScore: number;
  liveLevel: RiskLevel;
  liveTranscript: string;
  liveIndicators: string[];
  liveRecommendation: string;
  onEndCall: () => void;
  onMinimize: () => void;
}

export function LiveCallOverlay({
  callState,
  callerNumber,
  callDuration,
  liveScore,
  liveLevel,
  liveTranscript,
  liveIndicators,
  liveRecommendation,
  onEndCall,
  onMinimize
}: LiveCallOverlayProps) {
  const [showConversation, setShowConversation] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [isSpeaker, setIsSpeaker] = useState(false);

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins < 10 ? '0' : ''}${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  const isCritical = liveLevel === 'CRITICAL';
  const isHigh = liveLevel === 'HIGH';
  const isSuspicious = liveLevel === 'MEDIUM';
  const isDanger = isCritical || isHigh;

  // Caller avatar initial or alert symbol
  const avatarText = isDanger ? '!' : callerNumber.slice(-2) || 'C';

  return (
    <div
      id="overlay_live_call_activity"
      className="fixed inset-0 z-50 bg-[#0E172A] text-slate-100 flex flex-col p-4 animate-in fade-in duration-200 overflow-y-auto"
    >
      <div className="max-w-sm w-full mx-auto flex-1 flex flex-col justify-between py-2 space-y-4">
        {/* TOP BAR: BACK ARROW, CALLER AVATAR, MENU */}
        <div className="flex items-center justify-between pt-1">
          <button
            onClick={onMinimize}
            className="p-2 rounded-full hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
            title="Minimize to floating pill"
          >
            <ChevronLeft className="w-6 h-6" />
          </button>

          <div
            className={`w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm transition-transform ${
              isDanger
                ? 'bg-rose-600 text-white animate-pulse'
                : 'bg-slate-700 text-slate-200'
            }`}
          >
            {avatarText}
          </div>

          <button
            onClick={onMinimize}
            className="p-2 rounded-full hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
          >
            <MoreVertical className="w-5 h-5" />
          </button>
        </div>

        {/* CALLER NUMBER & DURATION */}
        <div className="text-center">
          <h2 className="text-2xl font-bold font-sans tracking-wide text-white">
            {callerNumber}
          </h2>
          <p className="text-xs font-mono text-slate-400 mt-1">
            {callState === 'RINGING' ? 'Ringing...' : formatTime(callDuration)}
          </p>
        </div>

        {/* MAIN BODY: SCAM ALERT STATE OR NORMAL SAFE STATE */}
        {isDanger ? (
          /* SCAM ALERT LIVE STATE (Screen 4 in mockup) */
          <div className="space-y-3">
            {/* Red Potential Scam Detected Card */}
            <div className="p-4 rounded-2xl bg-rose-600 text-white shadow-xl shadow-rose-950/60 text-center space-y-2">
              <div className="w-12 h-12 rounded-full bg-white/20 mx-auto flex items-center justify-center text-white">
                <AlertTriangle className="w-7 h-7 stroke-[2.5]" />
              </div>
              <div>
                <h3 className="text-xl font-serif font-bold tracking-tight">
                  Potential Scam Detected
                </h3>
                <p className="text-xs text-rose-100 mt-1 leading-relaxed">
                  The caller may be attempting to defraud you.
                </p>
              </div>
              <div className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-rose-800/80 text-[11px] font-bold uppercase tracking-wider text-rose-100 border border-rose-400/40">
                High Risk
              </div>
            </div>

            {/* Detected Warning Signs Card */}
            <div className="p-4 rounded-2xl bg-[#162033] border border-rose-500/40 space-y-2">
              <h4 className="text-xs font-serif font-bold uppercase tracking-wider text-slate-300">
                Detected Warning Signs
              </h4>
              <ul className="space-y-1.5 text-xs text-rose-300">
                {liveIndicators.length > 0 ? (
                  liveIndicators.map((ind, idx) => (
                    <li key={idx} className="flex items-center gap-2 font-medium">
                      <AlertTriangle className="w-3.5 h-3.5 text-rose-400 shrink-0" />
                      <span>{ind}</span>
                    </li>
                  ))
                ) : (
                  <>
                    <li className="flex items-center gap-2 font-medium">
                      <AlertTriangle className="w-3.5 h-3.5 text-rose-400 shrink-0" />
                      <span>OTP request</span>
                    </li>
                    <li className="flex items-center gap-2 font-medium">
                      <AlertTriangle className="w-3.5 h-3.5 text-rose-400 shrink-0" />
                      <span>Account threat</span>
                    </li>
                    <li className="flex items-center gap-2 font-medium">
                      <AlertTriangle className="w-3.5 h-3.5 text-rose-400 shrink-0" />
                      <span>Urgency pressure</span>
                    </li>
                  </>
                )}
              </ul>
            </div>

            {/* Warning Advice Card */}
            <div className="p-3.5 rounded-2xl bg-[#162033] border border-[#26344A] flex items-center gap-3 text-xs text-slate-300">
              <Shield className="w-5 h-5 text-blue-400 shrink-0" />
              <p className="leading-relaxed">
                Do not share OTPs, PINs or banking credentials.
              </p>
            </div>
          </div>
        ) : (
          /* NORMAL CALL PROTECTED STATE (Screen 3 in mockup) */
          <div className="space-y-3">
            {/* Green Hero Card */}
            <div className="p-6 rounded-2xl bg-[#162033] border border-emerald-500/30 text-center space-y-3 shadow-lg">
              <div className="w-14 h-14 rounded-full bg-emerald-500/20 border border-emerald-500/40 mx-auto flex items-center justify-center text-emerald-400">
                <ShieldCheck className="w-8 h-8 stroke-[2.2]" />
              </div>
              <div>
                <h3 className="text-xl font-serif font-bold text-white">
                  Call Protected
                </h3>
                <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                  No suspicious activity detected so far.
                </p>
              </div>
            </div>

            {/* Audio Analysis Card */}
            <div className="p-4 rounded-2xl bg-[#162033] border border-[#26344A] flex items-center justify-between text-xs">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
                  <Volume2 className="w-5 h-5" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-serif font-bold text-slate-100">Audio Analysis</span>
                    <span className="text-[10px] font-bold px-1.5 py-0.2 rounded bg-emerald-500/20 text-emerald-400">
                      Active
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-400 mt-0.5">
                    Using microphone (acoustic mode)
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* VIEW TRANSCRIPT ACCORDION */}
        <div className="rounded-2xl border border-slate-800 bg-[#162033] overflow-hidden text-left">
          <button
            onClick={() => setShowConversation(!showConversation)}
            className="w-full p-3 flex items-center justify-between text-slate-300 hover:text-white transition-colors"
          >
            <div className="flex items-center gap-2">
              <FileText className="w-4 h-4 text-slate-400" />
              <span className="text-xs font-serif font-bold">View conversation</span>
            </div>
            {showConversation ? (
              <ChevronUp className="w-4 h-4 text-slate-400" />
            ) : (
              <ChevronDown className="w-4 h-4 text-slate-400" />
            )}
          </button>

          {showConversation && (
            <div className="p-3 pt-0 border-t border-slate-800 text-xs text-slate-300 leading-relaxed font-mono">
              {liveTranscript ? (
                <p className="bg-slate-950/60 p-2.5 rounded-xl border border-slate-800">
                  "{liveTranscript}"
                </p>
              ) : (
                <p className="text-slate-500 italic font-sans text-xs">
                  Awaiting speaker audio through microphone...
                </p>
              )}
            </div>
          )}
        </div>

        {/* IN-CALL CONTROLS: MUTE, SPEAKER, MORE */}
        <div className="flex items-center justify-center gap-6 pt-2">
          <button
            onClick={() => setIsMuted(!isMuted)}
            className={`w-12 h-12 rounded-full flex items-center justify-center transition-colors border ${
              isMuted
                ? 'bg-rose-600/30 text-rose-400 border-rose-500/40'
                : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'
            }`}
            title="Mute microphone"
          >
            {isMuted ? <MicOff className="w-5 h-5" /> : <Mic className="w-5 h-5" />}
          </button>

          <button
            onClick={() => setIsSpeaker(!isSpeaker)}
            className={`w-12 h-12 rounded-full flex items-center justify-center transition-colors border ${
              isSpeaker
                ? 'bg-blue-600/30 text-blue-400 border-blue-500/40'
                : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'
            }`}
            title="Speakerphone"
          >
            <Volume2 className="w-5 h-5" />
          </button>

          <button
            onClick={() => setShowConversation(!showConversation)}
            className="w-12 h-12 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 flex items-center justify-center transition-colors"
            title="Keypad & More"
          >
            <Grid className="w-5 h-5" />
          </button>
        </div>

        {/* DOMINANT END CALL BUTTON */}
        <div className="pt-2">
          <button
            id="btn_hangup_live_call"
            onClick={onEndCall}
            className="w-full py-3.5 rounded-full bg-[#DC2626] hover:bg-rose-700 text-white font-bold text-sm flex items-center justify-center gap-2 shadow-lg shadow-rose-950/60 transition-all active:scale-[0.98]"
          >
            <PhoneOff className="w-5 h-5" />
            <span>End Call</span>
          </button>
        </div>
      </div>
    </div>
  );
}

