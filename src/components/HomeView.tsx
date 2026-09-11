import React from 'react';
import {
  ShieldCheck,
  ShieldAlert,
  ChevronRight,
  Sparkles,
  BookOpen,
  Volume2,
  Info,
  Phone,
  AlertTriangle,
  ShieldOff,
  Bell
} from 'lucide-react';
import { CallRecordItem, NavTab, ThemeMode } from '../types';

interface HomeViewProps {
  isFullyProtected: boolean;
  protectionScore: number;
  callHistory: CallRecordItem[];
  themeMode: ThemeMode;
  onSelectRecord: (record: CallRecordItem) => void;
  onNavigate: (tab: NavTab) => void;
  onEnableProtection: () => void;
}

export function HomeView({
  isFullyProtected,
  protectionScore,
  callHistory,
  themeMode,
  onSelectRecord,
  onNavigate,
  onEnableProtection
}: HomeViewProps) {
  // Determine time-of-day greeting
  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good Morning' : hour < 17 ? 'Good Afternoon' : 'Good Evening';

  const isDark = themeMode === 'dark';

  return (
    <div id="view_home" className="space-y-4">
      {/* BRAND & HEADER WITH NOTIFICATION BELL */}
      <div className="flex items-center justify-between px-1 pt-1">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-full bg-blue-600/10 border border-blue-600/20 flex items-center justify-center text-[#2563EB]">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-lg font-serif font-bold tracking-tight leading-none text-slate-900 dark:text-slate-100">
              Svara_X
            </h1>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 font-sans mt-0.5">
              AI Call Protection
            </p>
          </div>
        </div>

        <button
          onClick={() => onNavigate('protection')}
          className="w-9 h-9 rounded-full flex items-center justify-center text-slate-600 dark:text-slate-300 hover:bg-black/5 dark:hover:bg-white/5 transition-colors relative"
          title="Notifications & Protection Alerts"
          aria-label="Notifications"
        >
          <Bell className="w-4 h-4" />
          <span className="w-2 h-2 rounded-full bg-blue-600 absolute top-2 right-2 ring-2 ring-white dark:ring-slate-900" />
        </button>
      </div>

      {/* GREETING */}
      <div className="px-1">
        <h2 className="text-2xl font-serif font-bold text-slate-900 dark:text-white tracking-tight">
          {greeting}
        </h2>
        <p className="text-xs text-slate-600 dark:text-slate-400 mt-0.5 font-serif italic">
          Stay safe. We're watching the calls for you.
        </p>
      </div>

      {/* HERO PROTECTION CARD */}
      <div
        id="card_hero_protection"
        className={`rounded-2xl border p-6 text-center transition-all shadow-sm ${
          isFullyProtected
            ? isDark
              ? 'bg-[#162033] border-emerald-500/30'
              : 'bg-white border-emerald-100 shadow-emerald-50/50'
            : isDark
            ? 'bg-[#162033] border-amber-500/40'
            : 'bg-white border-amber-100'
        }`}
      >
        {/* Large Central Icon */}
        <div
          className={`mx-auto w-16 h-16 rounded-full flex items-center justify-center mb-3 transition-transform ${
            isFullyProtected
              ? isDark
                ? 'bg-emerald-950/60 text-emerald-400 border border-emerald-500/30'
                : 'bg-emerald-600 text-white shadow-md shadow-emerald-600/20'
              : isDark
              ? 'bg-amber-950/60 text-amber-400 border border-amber-500/30'
              : 'bg-amber-500 text-white shadow-md shadow-amber-500/20'
          }`}
        >
          {isFullyProtected ? (
            <ShieldCheck className="w-8 h-8 stroke-[2.2]" />
          ) : (
            <ShieldAlert className="w-8 h-8 stroke-[2.2]" />
          )}
        </div>

        {/* Hero Title in Serif */}
        <h3 className="text-xl font-serif font-bold text-slate-900 dark:text-slate-100">
          {isFullyProtected ? "You're Protected" : 'Action Required'}
        </h3>

        {/* Descriptive Body */}
        <p className="text-xs text-slate-600 dark:text-slate-400 mt-1 max-w-xs mx-auto leading-relaxed">
          {isFullyProtected
            ? 'Svara_X is actively monitoring your calls for scams and fraud patterns.'
            : 'Enable call screening and microphone permissions to start real-time scam detection.'}
        </p>

        {/* Status Pill Badge */}
        <div className="mt-3.5 inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800/50">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
          <span>{isFullyProtected ? 'Protection Active' : 'Setup Incomplete'}</span>
        </div>

        {!isFullyProtected && (
          <div className="mt-4">
            <button
              id="btn_grant_missing_permissions"
              onClick={onEnableProtection}
              className="text-xs font-semibold px-4 py-2 rounded-xl bg-[#2563EB] hover:bg-blue-600 text-white shadow-md transition-all active:scale-[0.98]"
            >
              Enable Full Protection
            </button>
          </div>
        )}
      </div>

      {/* RECENT CALLS SECTION */}
      <div id="section_recent_activity" className="space-y-2 pt-1">
        <div className="flex items-center justify-between px-1">
          <h3 className="text-base font-serif font-bold text-slate-900 dark:text-slate-100">
            Recent Calls
          </h3>
          <button
            id="btn_view_all_history_from_home"
            onClick={() => onNavigate('calls')}
            className="text-xs font-semibold text-[#2563EB] dark:text-[#4F7CFF] hover:underline flex items-center gap-0.5 transition-colors"
          >
            <span>View All</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </button>
        </div>

        {callHistory.length === 0 ? (
          <div
            id="box_empty_home_activity"
            className="bg-white dark:bg-[#162033] border border-slate-200 dark:border-[#26344A] rounded-2xl p-6 text-center space-y-2"
          >
            <div className="w-10 h-10 rounded-full bg-slate-100 dark:bg-slate-800 mx-auto flex items-center justify-center text-slate-400">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <h4 className="text-xs font-serif font-bold text-slate-800 dark:text-slate-200">
              No protected calls yet
            </h4>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 max-w-xs mx-auto leading-relaxed">
              When a call connects, Svara_X assesses potential threats in real time. Recorded assessments will appear here.
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {callHistory.slice(0, 3).map((rec) => {
              const isCritical = rec.riskLevel === 'CRITICAL';
              const isHigh = rec.riskLevel === 'HIGH';
              const isMedium = rec.riskLevel === 'MEDIUM';

              let statusText = 'No suspicious activity';
              let statusTextColor = 'text-[#16A34A] dark:text-[#22C55E]';
              let iconBg = 'bg-emerald-100 dark:bg-emerald-950/60 text-[#16A34A] dark:text-emerald-400';
              let iconElement = <Phone className="w-4 h-4" />;

              if (isCritical || isHigh) {
                statusText = 'Potential scam detected';
                statusTextColor = 'text-[#DC2626] dark:text-[#EF4444]';
                iconBg = 'bg-rose-100 dark:bg-rose-950/60 text-[#DC2626] dark:text-rose-400';
                iconElement = <AlertTriangle className="w-4 h-4" />;
              } else if (isMedium) {
                statusText = 'Suspicious activity detected';
                statusTextColor = 'text-[#F59E0B] dark:text-amber-400';
                iconBg = 'bg-amber-100 dark:bg-amber-950/60 text-[#F59E0B] dark:text-amber-400';
                iconElement = <AlertTriangle className="w-4 h-4" />;
              } else if (rec.audioMode === 'UNAVAILABLE') {
                statusText = 'Limited analysis';
                statusTextColor = 'text-slate-500 dark:text-slate-400';
                iconBg = 'bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400';
                iconElement = <ShieldOff className="w-4 h-4" />;
              }

              const d = new Date(rec.timestamp);
              const dateStr = d.toLocaleDateString('en-US', { weekday: 'short', hour: '2-digit', minute: '2-digit' });
              const durationStr = `${Math.max(1, Math.round(rec.duration / 60))} min`;

              return (
                <div
                  key={rec.id}
                  id={`recent_item_${rec.id}`}
                  onClick={() => onSelectRecord(rec)}
                  className="bg-white dark:bg-[#162033] hover:bg-slate-50 dark:hover:bg-[#1D293D] border border-slate-200 dark:border-[#26344A] rounded-2xl p-3.5 transition-all cursor-pointer flex items-center justify-between group shadow-sm"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <div className={`w-9 h-9 rounded-full flex items-center justify-center shrink-0 ${iconBg}`}>
                      {iconElement}
                    </div>

                    <div className="min-w-0">
                      <div className="flex items-center gap-1.5">
                        <span className="text-xs font-bold text-slate-900 dark:text-slate-100 font-sans truncate">
                          {rec.caller}
                        </span>
                        {rec.isDemo && (
                          <span className="text-[9px] bg-slate-100 dark:bg-slate-800 text-amber-600 dark:text-amber-400 px-1 py-0.2 rounded font-mono font-bold">
                            DEMO
                          </span>
                        )}
                      </div>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                        {dateStr} • {durationStr}
                      </p>
                      <p className={`text-[11px] font-medium mt-0.5 ${statusTextColor}`}>
                        {statusText}
                      </p>
                    </div>
                  </div>

                  <div className="pl-2 shrink-0">
                    <ChevronRight className="w-4 h-4 text-slate-400 group-hover:text-slate-600 dark:group-hover:text-slate-200 transition-colors" />
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* CORE PRODUCT STATEMENT CARD */}
      <div className="p-3.5 rounded-2xl bg-white dark:bg-[#162033] border border-slate-200 dark:border-[#26344A] flex items-start gap-3 shadow-sm">
        <div className="w-8 h-8 rounded-xl bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-800/40 flex items-center justify-center text-[#2563EB] dark:text-[#4F7CFF] shrink-0 mt-0.5">
          <Info className="w-4 h-4" />
        </div>
        <div className="text-xs text-slate-700 dark:text-slate-300 space-y-0.5">
          <p className="font-serif font-bold text-slate-900 dark:text-slate-100">How Svara_X works</p>
          <p className="text-slate-600 dark:text-slate-400 leading-relaxed text-[11px]">
            Svara_X screens incoming callers and analyzes conversations on-device for urgent payment demands, OTP theft, and authority impersonation.
          </p>
        </div>
      </div>

      {/* KNOW THE SCAMS BANNER */}
      <div
        onClick={() => onNavigate('protection')}
        className="p-3.5 rounded-2xl bg-white dark:bg-[#162033] hover:bg-slate-50 dark:hover:bg-[#1D293D] border border-slate-200 dark:border-[#26344A] transition-all cursor-pointer flex items-center justify-between shadow-sm"
      >
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-purple-100 dark:bg-purple-950/40 border border-purple-200 dark:border-purple-800/40 flex items-center justify-center text-purple-700 dark:text-purple-400 shrink-0">
            <BookOpen className="w-4 h-4" />
          </div>
          <div>
            <p className="text-xs font-serif font-bold text-slate-900 dark:text-slate-100">Know the scams</p>
            <p className="text-[11px] text-slate-500 dark:text-slate-400">
              Learn how to spot OTP, UPI, and fake police scams
            </p>
          </div>
        </div>
        <ChevronRight className="w-4 h-4 text-slate-400" />
      </div>

      {/* AUDIT PRIVACY NOTICE */}
      <div className="p-3 rounded-xl bg-slate-100/70 dark:bg-slate-900/50 border border-slate-200 dark:border-[#26344A] text-[11px] text-slate-600 dark:text-slate-400 leading-relaxed flex items-start gap-2.5">
        <Volume2 className="w-3.5 h-3.5 text-slate-500 shrink-0 mt-0.5" />
        <p>
          <strong className="text-slate-800 dark:text-slate-200 font-semibold">Privacy note: </strong>
          Audio analysis runs strictly on your device. Svara_X never stores or uploads voice recordings to any cloud server.
        </p>
      </div>
    </div>
  );
}

