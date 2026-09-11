import React, { useState } from 'react';
import {
  Shield,
  ShieldCheck,
  AlertCircle,
  Sparkles,
  ChevronDown,
  ChevronUp,
  Volume2,
  CheckCircle2,
  PhoneOff,
  Lock,
  ExternalLink
} from 'lucide-react';
import { SCAM_EDUCATION_DATA } from '../data/scamEducationData';
import { ScamEducationCard } from '../types';

interface ProtectionViewProps {
  isFullyProtected: boolean;
  protectionScore: number;
  permissionPhone: boolean;
  permissionMic: boolean;
  permissionNotif: boolean;
  roleCallScreening: boolean;
  onEnableProtection: () => void;
}

export function ProtectionView({
  isFullyProtected,
  protectionScore,
  permissionPhone,
  permissionMic,
  permissionNotif,
  roleCallScreening,
  onEnableProtection
}: ProtectionViewProps) {
  const [selectedCardId, setSelectedCardId] = useState<string | null>(SCAM_EDUCATION_DATA[0].id);

  return (
    <div id="view_protection" className="space-y-4">
      {/* HEADER */}
      <div className="px-1 pt-1">
        <h2 className="text-xl font-bold text-slate-100 tracking-tight">Protection Center</h2>
        <p className="text-xs text-slate-400 mt-0.5">Device safety assessment, scam guidance &amp; education</p>
      </div>

      {/* PROTECTION SCORE HERO */}
      <div className="p-5 rounded-2xl bg-[#151E2E] border border-slate-800 shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <span className="text-[11px] font-mono uppercase font-bold text-slate-400 tracking-wider">
              Your Protection Score
            </span>
            <div className="flex items-baseline gap-2 mt-1">
              <span className="text-3xl font-bold text-white tracking-tight">
                {protectionScore}
              </span>
              <span className="text-xs text-slate-400 font-medium">/ 100</span>
            </div>
          </div>

          <div className="w-12 h-12 rounded-2xl bg-[#1C2638] border border-slate-700/60 flex items-center justify-center text-[#4F7CFF]">
            <Sparkles className="w-6 h-6" />
          </div>
        </div>

        {/* Progress Bar */}
        <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
          <div
            className={`h-full transition-all duration-500 ${
              protectionScore >= 80 ? 'bg-[#22C55E]' : 'bg-[#F59E0B]'
            }`}
            style={{ width: `${protectionScore}%` }}
          />
        </div>

        {/* Breakdown of real active capabilities */}
        <div className="grid grid-cols-2 gap-2 pt-1 text-[11px]">
          <div className="p-2.5 rounded-xl bg-[#1C2638] border border-slate-800/80 flex items-center gap-2">
            <span className={`w-2 h-2 rounded-full ${roleCallScreening ? 'bg-emerald-400' : 'bg-amber-400'}`} />
            <span className="text-slate-300">Call Screening</span>
          </div>

          <div className="p-2.5 rounded-xl bg-[#1C2638] border border-slate-800/80 flex items-center gap-2">
            <span className={`w-2 h-2 rounded-full ${permissionMic ? 'bg-emerald-400' : 'bg-rose-400'}`} />
            <span className="text-slate-300">Speech Analysis</span>
          </div>

          <div className="p-2.5 rounded-xl bg-[#1C2638] border border-slate-800/80 flex items-center gap-2">
            <span className={`w-2 h-2 rounded-full ${permissionPhone ? 'bg-emerald-400' : 'bg-rose-400'}`} />
            <span className="text-slate-300">Call Detection</span>
          </div>

          <div className="p-2.5 rounded-xl bg-[#1C2638] border border-slate-800/80 flex items-center gap-2">
            <span className={`w-2 h-2 rounded-full ${permissionNotif ? 'bg-emerald-400' : 'bg-amber-400'}`} />
            <span className="text-slate-300">Scam Warnings</span>
          </div>
        </div>

        {!isFullyProtected && (
          <button
            onClick={onEnableProtection}
            className="w-full py-2 rounded-xl bg-[#4F7CFF] hover:bg-[#3d68e6] text-white font-semibold text-xs transition-colors"
          >
            Activate Missing Protections
          </button>
        )}
      </div>

      {/* SAFETY COACH CARD */}
      <div className="p-4 rounded-2xl bg-[#151E2E] border border-amber-500/30 shadow-sm space-y-3">
        <div className="flex items-center gap-2 text-amber-400">
          <Shield className="w-4 h-4" />
          <h3 className="text-xs font-bold uppercase tracking-wider">Safety Coach: Stop &amp; Verify</h3>
        </div>

        <p className="text-xs text-slate-300 leading-relaxed font-medium">
          If a caller triggers suspicion, follow these golden rules:
        </p>

        <ol className="space-y-2 text-xs text-slate-300 pl-1">
          <li className="flex items-start gap-2">
            <span className="w-5 h-5 rounded-full bg-slate-800 text-amber-400 flex items-center justify-center font-bold text-[10px] shrink-0 mt-0.5">1</span>
            <span><strong className="text-white">Never share OTPs:</strong> Banks and police never request verification codes or PINs.</span>
          </li>
          <li className="flex items-start gap-2">
            <span className="w-5 h-5 rounded-full bg-slate-800 text-amber-400 flex items-center justify-center font-bold text-[10px] shrink-0 mt-0.5">2</span>
            <span><strong className="text-white">End the call:</strong> Break the psychological urgency immediately by hanging up.</span>
          </li>
          <li className="flex items-start gap-2">
            <span className="w-5 h-5 rounded-full bg-slate-800 text-amber-400 flex items-center justify-center font-bold text-[10px] shrink-0 mt-0.5">3</span>
            <span><strong className="text-white">Call official numbers:</strong> Verify directly using numbers printed on your bank card or official app.</span>
          </li>
        </ol>
      </div>

      {/* VOICE AUTHENTICITY STATUS CARD */}
      <div className="p-4 rounded-2xl bg-[#151E2E] border border-slate-800 space-y-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Volume2 className="w-4 h-4 text-slate-400" />
            <h3 className="text-xs font-bold text-slate-200">Voice Authenticity Analysis</h3>
          </div>
          <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700">
            Unavailable
          </span>
        </div>
        <p className="text-[11px] text-slate-400 leading-relaxed">
          Synthetic voice / deepfake detection requires specialized on-device ML models. Svara_X does not fabricate voice authenticity claims until verified models are active.
        </p>
      </div>

      {/* SCAM EDUCATION: KNOW THE SCAMS */}
      <div className="space-y-2 pt-1">
        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 px-1">
          Know the scams ({SCAM_EDUCATION_DATA.length})
        </h3>

        <div className="space-y-2">
          {SCAM_EDUCATION_DATA.map((scam) => {
            const isExpanded = selectedCardId === scam.id;
            return (
              <div
                key={scam.id}
                className="rounded-2xl border border-slate-800 bg-[#151E2E] overflow-hidden transition-all shadow-sm"
              >
                <button
                  onClick={() => setSelectedCardId(isExpanded ? null : scam.id)}
                  className="w-full p-4 flex items-center justify-between text-left hover:bg-[#1C2638]/50 transition-colors"
                >
                  <div className="space-y-0.5 pr-2">
                    <span className="text-[10px] font-semibold text-[#4F7CFF] uppercase tracking-wider block">
                      {scam.tag}
                    </span>
                    <h4 className="text-xs font-bold text-slate-100">{scam.title}</h4>
                    <p className="text-[11px] text-slate-400 line-clamp-1">{scam.summary}</p>
                  </div>
                  <div className="text-slate-500 pl-2 shrink-0">
                    {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                  </div>
                </button>

                {isExpanded && (
                  <div className="p-4 pt-0 border-t border-slate-800/60 space-y-3 text-xs text-slate-300">
                    <div>
                      <p className="font-semibold text-slate-200 mb-1">How it works</p>
                      <p className="text-slate-400 text-[11px] leading-relaxed">{scam.howItWorks}</p>
                    </div>

                    <div className="space-y-1">
                      <p className="font-semibold text-rose-300">Common warning signs</p>
                      <ul className="space-y-1 text-[11px] text-slate-400">
                        {scam.warningSigns.map((w, i) => (
                          <li key={i} className="flex items-start gap-1.5">
                            <span className="text-rose-400 shrink-0">•</span>
                            <span>{w}</span>
                          </li>
                        ))}
                      </ul>
                    </div>

                    <div className="space-y-1 pt-1 border-t border-slate-800/80">
                      <p className="font-semibold text-emerald-300">What to do</p>
                      <ul className="space-y-1 text-[11px] text-slate-400">
                        {scam.whatToDo.map((todo, i) => (
                          <li key={i} className="flex items-start gap-1.5">
                            <span className="text-emerald-400 shrink-0">✓</span>
                            <span>{todo}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
