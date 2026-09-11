import React, { useState } from 'react';
import {
  History,
  Trash2,
  ChevronRight,
  ShieldCheck,
  AlertTriangle,
  Play,
  Search,
  Phone,
  ShieldOff,
  Filter
} from 'lucide-react';
import { CallRecordItem, ThemeMode } from '../types';

interface CallsViewProps {
  callHistory: CallRecordItem[];
  themeMode: ThemeMode;
  onSelectRecord: (record: CallRecordItem) => void;
  onClearHistory: () => void;
  onRunSimulation: () => void;
}

export function CallsView({
  callHistory,
  themeMode,
  onSelectRecord,
  onClearHistory,
  onRunSimulation
}: CallsViewProps) {
  const [filter, setFilter] = useState<'ALL' | 'SAFE' | 'SUSPICIOUS' | 'LIMITED'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const filteredHistory = callHistory.filter((rec) => {
    // Search query filter
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchCaller = rec.caller.toLowerCase().includes(q);
      const matchIndicator = rec.indicators.some((ind) => ind.toLowerCase().includes(q));
      if (!matchCaller && !matchIndicator) return false;
    }

    // Category filter
    if (filter === 'SAFE') {
      return rec.riskLevel === 'LOW' && rec.audioMode !== 'UNAVAILABLE';
    }
    if (filter === 'SUSPICIOUS') {
      return rec.riskLevel === 'HIGH' || rec.riskLevel === 'CRITICAL' || rec.riskLevel === 'MEDIUM';
    }
    if (filter === 'LIMITED') {
      return rec.audioMode === 'UNAVAILABLE';
    }
    return true;
  });

  // Group into Today, Yesterday, and Earlier
  const todayStart = new Date().setHours(0, 0, 0, 0);
  const yesterdayStart = todayStart - 86400000;

  const todayCalls = filteredHistory.filter((c) => c.timestamp >= todayStart);
  const yesterdayCalls = filteredHistory.filter((c) => c.timestamp >= yesterdayStart && c.timestamp < todayStart);
  const earlierCalls = filteredHistory.filter((c) => c.timestamp < yesterdayStart);

  const formatTimeAndDuration = (rec: CallRecordItem) => {
    const d = new Date(rec.timestamp);
    const timeStr = d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
    const durationStr = `${Math.max(1, Math.round(rec.duration / 60))} min`;
    return `${timeStr} • ${durationStr}`;
  };

  const renderCallCard = (rec: CallRecordItem) => {
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

    return (
      <div
        key={rec.id}
        id={`card_history_record_${rec.id}`}
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
              {formatTimeAndDuration(rec)}
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
  };

  return (
    <div id="view_calls" className="space-y-4">
      {/* HEADER */}
      <div className="flex items-center justify-between px-1 pt-1">
        <div>
          <h2 className="text-2xl font-serif font-bold text-slate-900 dark:text-white tracking-tight">
            Call History
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Audit history of screened and protected calls
          </p>
        </div>

        {callHistory.length > 0 && (
          <button
            id="btn_clear_history_list"
            onClick={onClearHistory}
            className="text-xs text-rose-600 dark:text-rose-400 hover:text-rose-700 dark:hover:text-rose-300 flex items-center gap-1 font-medium px-2.5 py-1.5 rounded-xl bg-rose-50 dark:bg-slate-900 border border-rose-200 dark:border-slate-800 transition-colors"
          >
            <Trash2 className="w-3.5 h-3.5" />
            <span>Clear</span>
          </button>
        )}
      </div>

      {/* SEARCH BAR */}
      <div className="relative">
        <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Search by caller or warning..."
          className="w-full pl-9 pr-4 py-2 bg-white dark:bg-[#162033] border border-slate-200 dark:border-[#26344A] rounded-xl text-xs text-slate-800 dark:text-slate-100 placeholder-slate-400 focus:outline-none focus:border-blue-500 transition-colors shadow-sm"
        />
      </div>

      {/* FILTER CHIPS */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1">
        <button
          onClick={() => setFilter('ALL')}
          className={`px-3 py-1 text-xs font-semibold rounded-full whitespace-nowrap transition-colors ${
            filter === 'ALL'
              ? 'bg-[#2563EB] text-white shadow-sm'
              : 'bg-white dark:bg-[#162033] text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-[#26344A] hover:bg-slate-50'
          }`}
        >
          All
        </button>
        <button
          onClick={() => setFilter('SAFE')}
          className={`px-3 py-1 text-xs font-semibold rounded-full whitespace-nowrap transition-colors ${
            filter === 'SAFE'
              ? 'bg-[#16A34A] text-white shadow-sm'
              : 'bg-white dark:bg-[#162033] text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-[#26344A] hover:bg-slate-50'
          }`}
        >
          Safe
        </button>
        <button
          onClick={() => setFilter('SUSPICIOUS')}
          className={`px-3 py-1 text-xs font-semibold rounded-full whitespace-nowrap transition-colors ${
            filter === 'SUSPICIOUS'
              ? 'bg-[#DC2626] text-white shadow-sm'
              : 'bg-white dark:bg-[#162033] text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-[#26344A] hover:bg-slate-50'
          }`}
        >
          Suspicious
        </button>
        <button
          onClick={() => setFilter('LIMITED')}
          className={`px-3 py-1 text-xs font-semibold rounded-full whitespace-nowrap transition-colors ${
            filter === 'LIMITED'
              ? 'bg-slate-700 text-white shadow-sm'
              : 'bg-white dark:bg-[#162033] text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-[#26344A] hover:bg-slate-50'
          }`}
        >
          Limited
        </button>
      </div>

      {/* CALL LIST WITH GROUPING */}
      {callHistory.length === 0 ? (
        <div
          id="box_empty_history"
          className="bg-white dark:bg-[#162033] border border-slate-200 dark:border-[#26344A] rounded-2xl p-8 text-center my-4 space-y-3 shadow-sm"
        >
          <div className="w-12 h-12 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-400 mx-auto">
            <History className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-sm font-serif font-bold text-slate-900 dark:text-slate-200">
              No protected calls yet
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 max-w-xs mx-auto mt-1 leading-relaxed">
              When a call finishes, Svara_X saves a private audit record containing safety assessment results and warnings.
            </p>
          </div>
          <button
            id="btn_simulate_scam_from_empty_history"
            onClick={onRunSimulation}
            className="mt-2 text-xs font-semibold px-4 py-2 rounded-xl bg-[#2563EB] hover:bg-blue-600 text-white inline-flex items-center gap-1.5 shadow transition-all"
          >
            <Play className="w-3.5 h-3.5 fill-current" />
            <span>Test with Demo Simulation</span>
          </button>
        </div>
      ) : filteredHistory.length === 0 ? (
        <div className="bg-white dark:bg-[#162033] border border-slate-200 dark:border-[#26344A] rounded-2xl p-6 text-center text-xs text-slate-500 dark:text-slate-400 shadow-sm">
          No calls match the selected filter.
        </div>
      ) : (
        <div id="list_call_records" className="space-y-4">
          {/* TODAY GROUP */}
          {todayCalls.length > 0 && (
            <div className="space-y-2">
              <h3 className="text-xs font-serif font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 px-1">
                Today
              </h3>
              <div className="space-y-2">
                {todayCalls.map((rec) => renderCallCard(rec))}
              </div>
            </div>
          )}

          {/* YESTERDAY GROUP */}
          {yesterdayCalls.length > 0 && (
            <div className="space-y-2">
              <h3 className="text-xs font-serif font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 px-1">
                Yesterday
              </h3>
              <div className="space-y-2">
                {yesterdayCalls.map((rec) => renderCallCard(rec))}
              </div>
            </div>
          )}

          {/* EARLIER GROUP */}
          {earlierCalls.length > 0 && (
            <div className="space-y-2">
              <h3 className="text-xs font-serif font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 px-1">
                Earlier
              </h3>
              <div className="space-y-2">
                {earlierCalls.map((rec) => renderCallCard(rec))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

