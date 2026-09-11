import React, { useState } from 'react';
import {
  X,
  ShieldCheck,
  ShieldAlert,
  AlertTriangle,
  FileText,
  ChevronDown,
  ChevronUp,
  Ban,
  Flag,
  Trash2,
  CheckCircle2,
  Lock,
  Clock,
  ChevronLeft,
  MoreVertical,
  Phone
} from 'lucide-react';
import { CallRecordItem, ThemeMode } from '../types';

interface CallDetailsModalProps {
  record: CallRecordItem | null;
  themeMode: ThemeMode;
  onClose: () => void;
  onDeleteRecord: (id: string) => void;
}

export function CallDetailsModal({
  record,
  themeMode,
  onClose,
  onDeleteRecord
}: CallDetailsModalProps) {
  const [showTranscript, setShowTranscript] = useState(false);
  const [actionFeedback, setActionFeedback] = useState<string | null>(null);

  if (!record) return null;

  const isCritical = record.riskLevel === 'CRITICAL';
  const isHigh = record.riskLevel === 'HIGH';
  const isMedium = record.riskLevel === 'MEDIUM';

  let statusTitle = 'Call Protected';
  let riskBadgeText = 'Safe';
  let badgeClasses = 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800/40';

  if (isCritical || isHigh) {
    statusTitle = 'Potential Scam';
    riskBadgeText = 'High Risk';
    badgeClasses = 'bg-rose-100 text-[#DC2626] dark:bg-rose-950/60 dark:text-rose-400 border border-rose-200 dark:border-rose-800/40';
  } else if (isMedium) {
    statusTitle = 'Suspicious Call';
    riskBadgeText = 'Medium Risk';
    badgeClasses = 'bg-amber-100 text-[#F59E0B] dark:bg-amber-950/60 dark:text-amber-400 border border-amber-200 dark:border-amber-800/40';
  }

  const formatDate = (ts: number) => {
    const d = new Date(ts);
    return (
      d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' }) +
      ', ' +
      d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' })
    );
  };

  const formatDuration = (seconds: number) => {
    const mins = Math.max(1, Math.round(seconds / 60));
    return `${mins} min`;
  };

  const handleBlock = () => {
    setActionFeedback(`Caller ${record.caller} added to blocked list.`);
    setTimeout(() => setActionFeedback(null), 3000);
  };

  const handleReport = () => {
    setActionFeedback('Scam details prepared for reporting (1930 / cybercrime.gov.in).');
    setTimeout(() => setActionFeedback(null), 3500);
  };

  const handleDelete = () => {
    onDeleteRecord(record.id);
    onClose();
  };

  return (
    <div
      id="modal_call_details"
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-3 animate-in fade-in duration-200"
    >
      <div className="bg-white dark:bg-[#162033] border border-slate-200 dark:border-[#26344A] rounded-3xl max-w-sm w-full max-h-[92vh] flex flex-col shadow-2xl overflow-hidden font-sans">
        {/* TOP BAR WITH BACK ARROW & MENU */}
        <div className="p-3.5 border-b border-slate-200 dark:border-[#26344A] flex items-center justify-between">
          <button
            id="btn_back_call_details_modal"
            onClick={onClose}
            className="text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors flex items-center gap-1 text-xs font-semibold"
          >
            <ChevronLeft className="w-5 h-5" />
            <span>Back</span>
          </button>

          <span className="font-serif font-bold text-sm text-slate-800 dark:text-slate-200">
            Call Details
          </span>

          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* SCROLLABLE BODY */}
        <div className="p-4 overflow-y-auto space-y-4 text-xs flex-1">
          {actionFeedback && (
            <div className="p-3 rounded-xl bg-blue-50 dark:bg-blue-950/60 border border-blue-200 dark:border-blue-800 text-[#2563EB] dark:text-blue-300 text-xs flex items-center gap-2 animate-in fade-in">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>{actionFeedback}</span>
            </div>
          )}

          {/* CALLER AVATAR & INFO CARD */}
          <div className="text-center p-4 rounded-2xl bg-slate-50 dark:bg-[#1D293D] border border-slate-200 dark:border-[#26344A] space-y-2">
            <div
              className={`w-14 h-14 rounded-full mx-auto flex items-center justify-center ${
                isHigh || isCritical
                  ? 'bg-rose-100 text-[#DC2626] dark:bg-rose-950/60 dark:text-rose-400'
                  : isMedium
                  ? 'bg-amber-100 text-[#F59E0B] dark:bg-amber-950/60 dark:text-amber-400'
                  : 'bg-emerald-100 text-[#16A34A] dark:bg-emerald-950/60 dark:text-emerald-400'
              }`}
            >
              {isHigh || isCritical ? (
                <AlertTriangle className="w-7 h-7" />
              ) : isMedium ? (
                <AlertTriangle className="w-7 h-7" />
              ) : (
                <Phone className="w-7 h-7" />
              )}
            </div>

            <p className="text-lg font-serif font-bold text-slate-900 dark:text-white">
              {record.caller}
            </p>

            <div className="text-[11px] text-slate-500 dark:text-slate-400 space-y-0.5">
              <p>{formatDate(record.timestamp)}</p>
              <p>Duration: {formatDuration(record.duration)}</p>
            </div>
          </div>

          {/* STATUS BANNER */}
          <div className="p-3 rounded-xl bg-white dark:bg-[#162033] border border-slate-200 dark:border-[#26344A] flex items-center justify-between shadow-sm">
            <div className="flex items-center gap-2.5">
              {isHigh || isCritical ? (
                <ShieldAlert className="w-5 h-5 text-[#DC2626] dark:text-rose-400" />
              ) : (
                <ShieldCheck className="w-5 h-5 text-[#16A34A] dark:text-emerald-400" />
              )}
              <span className="font-serif font-bold text-sm text-slate-900 dark:text-slate-100">
                {statusTitle}
              </span>
            </div>

            <span className={`text-[11px] font-bold px-2.5 py-0.5 rounded-full ${badgeClasses}`}>
              {riskBadgeText}
            </span>
          </div>

          {/* WHY WE FLAGGED THIS CALL? */}
          <div className="space-y-2">
            <h4 className="text-sm font-serif font-bold text-slate-900 dark:text-slate-100">
              Why we flagged this call?
            </h4>

            {record.indicators.length > 0 ? (
              <div className="space-y-2">
                {record.indicators.map((ind, idx) => (
                  <div
                    key={idx}
                    className="p-3 rounded-xl bg-rose-50/60 dark:bg-rose-950/20 border border-rose-200/80 dark:border-rose-900/30 flex items-start gap-2.5"
                  >
                    <AlertTriangle className="w-4 h-4 text-[#DC2626] dark:text-rose-400 shrink-0 mt-0.5" />
                    <div>
                      <p className="font-serif font-bold text-xs text-slate-900 dark:text-rose-100">
                        {ind}
                      </p>
                      <p className="text-[11px] text-slate-600 dark:text-rose-300/80 mt-0.5 leading-relaxed">
                        Caller exhibited aggressive social-engineering patterns correlated with fraud.
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="p-3 rounded-xl bg-emerald-50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-900/30 text-emerald-800 dark:text-emerald-300 flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0" />
                <span>No threat indicators detected during this call. Safe interaction.</span>
              </div>
            )}
          </div>

          {/* RECOMMENDED ACTION */}
          <div className="space-y-1.5">
            <h4 className="text-sm font-serif font-bold text-slate-900 dark:text-slate-100">
              Recommended Action
            </h4>
            <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-[#1D293D] border border-slate-200 dark:border-[#26344A] text-slate-700 dark:text-slate-200 leading-relaxed font-sans">
              {record.recommendation}
            </div>
          </div>

          {/* VIEW TRANSCRIPT ACCORDION */}
          <div className="rounded-xl border border-slate-200 dark:border-[#26344A] overflow-hidden bg-white dark:bg-[#162033]">
            <button
              onClick={() => setShowTranscript(!showTranscript)}
              className="w-full p-3 flex items-center justify-between text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
            >
              <div className="flex items-center gap-2">
                <FileText className="w-4 h-4 text-slate-400" />
                <span className="font-serif font-bold text-xs">View transcript (if available)</span>
              </div>
              {showTranscript ? (
                <ChevronUp className="w-4 h-4 text-slate-400" />
              ) : (
                <ChevronDown className="w-4 h-4 text-slate-400" />
              )}
            </button>

            {showTranscript && (
              <div className="p-3 pt-0 border-t border-slate-200 dark:border-[#26344A] text-[11px] text-slate-600 dark:text-slate-300 font-sans leading-relaxed">
                {record.transcriptSnippet ? (
                  <blockquote className="p-3 rounded-lg bg-slate-50 dark:bg-slate-950/60 border border-slate-200 dark:border-slate-800 italic">
                    "{record.transcriptSnippet}"
                  </blockquote>
                ) : (
                  <p className="text-slate-400 italic">
                    Conversation transcript unavailable for this call.
                  </p>
                )}
              </div>
            )}
          </div>

          {/* ACTION BUTTONS: BLOCK CALLER & REPORT SCAM */}
          <div className="space-y-2 pt-2 border-t border-slate-200 dark:border-[#26344A]">
            <div className="grid grid-cols-2 gap-2">
              <button
                onClick={handleBlock}
                className="py-2.5 px-3 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 font-semibold flex items-center justify-center gap-1.5 transition-colors border border-slate-200 dark:border-slate-700 text-xs"
              >
                <Ban className="w-3.5 h-3.5 text-amber-600 dark:text-amber-400" />
                <span>Block caller</span>
              </button>

              <button
                onClick={handleReport}
                className="py-2.5 px-3 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 font-semibold flex items-center justify-center gap-1.5 transition-colors border border-slate-200 dark:border-slate-700 text-xs"
              >
                <Flag className="w-3.5 h-3.5 text-[#DC2626] dark:text-rose-400" />
                <span>Report scam</span>
              </button>
            </div>

            <button
              onClick={handleDelete}
              className="w-full py-2 px-3 rounded-xl text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 font-medium flex items-center justify-center gap-1.5 transition-colors text-xs"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>Delete from history</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

