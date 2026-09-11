import React, { useState } from 'react';
import {
  Shield,
  Bell,
  Lock,
  Globe,
  Sliders,
  Info,
  ChevronRight,
  Radio,
  Play,
  Trash2,
  CheckCircle2,
  Volume2,
  Smartphone,
  Sun,
  Moon,
  Wrench
} from 'lucide-react';
import { SettingsSubView, ThemeMode } from '../types';

interface SettingsViewProps {
  permissionPhone: boolean;
  permissionMic: boolean;
  permissionNotif: boolean;
  roleCallScreening: boolean;
  themeMode: ThemeMode;
  onToggleTheme: (mode: ThemeMode) => void;
  onTogglePhone: () => void;
  onToggleMic: () => void;
  onToggleNotif: () => void;
  onToggleRole: () => void;
  onVerifyAllPermissions: () => void;
  onClearHistory: () => void;
  onNavigateSubView: (view: SettingsSubView) => void;
}

export function SettingsView({
  permissionPhone,
  permissionMic,
  permissionNotif,
  roleCallScreening,
  themeMode,
  onToggleTheme,
  onTogglePhone,
  onToggleMic,
  onToggleNotif,
  onToggleRole,
  onVerifyAllPermissions,
  onClearHistory,
  onNavigateSubView
}: SettingsViewProps) {
  const [hapticAlerts, setHapticAlerts] = useState(true);
  const [acousticScreening, setAcousticScreening] = useState(true);
  const [showClearConfirm, setShowClearConfirm] = useState(false);
  const [selectedLanguage, setSelectedLanguage] = useState('en');

  return (
    <div id="view_settings" className="space-y-4">
      {/* HEADER */}
      <div className="px-1 pt-1">
        <h2 className="text-2xl font-serif font-bold text-slate-900 dark:text-white tracking-tight">
          Settings
        </h2>
        <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
          Preferences, privacy controls &amp; system setup
        </p>
      </div>

      {/* APPEARANCE / THEME TOGGLE */}
      <div className="p-4 rounded-2xl bg-white dark:bg-[#162033] border border-slate-200 dark:border-[#26344A] space-y-3 shadow-sm">
        <div className="flex items-center gap-2 text-slate-900 dark:text-slate-200">
          <Sun className="w-4 h-4 text-amber-500" />
          <h3 className="text-xs font-serif font-bold uppercase tracking-wider">Appearance</h3>
        </div>

        <div className="flex items-center justify-between text-xs">
          <div>
            <p className="font-semibold text-slate-900 dark:text-slate-200">Theme Mode</p>
            <p className="text-[11px] text-slate-500 dark:text-slate-400">
              Select between light and dark interface
            </p>
          </div>

          <div className="flex items-center p-1 bg-slate-100 dark:bg-slate-850 rounded-xl border border-slate-200 dark:border-slate-700">
            <button
              onClick={() => onToggleTheme('light')}
              className={`flex items-center gap-1.5 px-3 py-1 rounded-lg font-semibold text-xs transition-colors ${
                themeMode === 'light'
                  ? 'bg-white text-slate-900 shadow-sm'
                  : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
              }`}
            >
              <Sun className="w-3.5 h-3.5 text-amber-500" />
              <span>Light</span>
            </button>
            <button
              onClick={() => onToggleTheme('dark')}
              className={`flex items-center gap-1.5 px-3 py-1 rounded-lg font-semibold text-xs transition-colors ${
                themeMode === 'dark'
                  ? 'bg-[#1C2638] text-white shadow-sm'
                  : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
              }`}
            >
              <Moon className="w-3.5 h-3.5 text-blue-400" />
              <span>Dark</span>
            </button>
          </div>
        </div>
      </div>

      {/* 1. PROTECTION SETTINGS */}
      <div className="p-4 rounded-2xl bg-[#151E2E] border border-slate-800 space-y-3 shadow-sm">
        <div className="flex items-center gap-2 text-slate-200">
          <Shield className="w-4 h-4 text-[#4F7CFF]" />
          <h3 className="text-xs font-bold uppercase tracking-wider">Protection</h3>
        </div>

        <div className="space-y-3 text-xs divide-y divide-slate-800/80">
          {/* Call Screening Role */}
          <div className="flex items-center justify-between pt-1">
            <div className="pr-3">
              <p className="font-semibold text-slate-200">Call Screening</p>
              <p className="text-[11px] text-slate-400 mt-0.5 leading-relaxed">
                Screens incoming callers using Android Telecom role
              </p>
            </div>
            <button
              onClick={onToggleRole}
              className={`w-11 h-6 flex items-center rounded-full p-1 transition-colors ${
                roleCallScreening ? 'bg-[#22C55E]' : 'bg-slate-700'
              }`}
              role="switch"
              aria-checked={roleCallScreening}
            >
              <div
                className={`bg-white w-4 h-4 rounded-full shadow-md transform transition-transform ${
                  roleCallScreening ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          {/* Acoustic Analysis via Mic */}
          <div className="flex items-center justify-between pt-3">
            <div className="pr-3">
              <p className="font-semibold text-slate-200">Acoustic Speech Analysis</p>
              <p className="text-[11px] text-slate-400 mt-0.5 leading-relaxed">
                Analyzes speech captured through device microphone
              </p>
            </div>
            <button
              onClick={() => setAcousticScreening(!acousticScreening)}
              className={`w-11 h-6 flex items-center rounded-full p-1 transition-colors ${
                acousticScreening ? 'bg-[#22C55E]' : 'bg-slate-700'
              }`}
              role="switch"
              aria-checked={acousticScreening}
            >
              <div
                className={`bg-white w-4 h-4 rounded-full shadow-md transform transition-transform ${
                  acousticScreening ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>
        </div>

        {(!permissionPhone || !permissionMic || !permissionNotif || !roleCallScreening) && (
          <div className="pt-2">
            <button
              onClick={onVerifyAllPermissions}
              className="w-full py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-xs transition-colors border border-slate-700"
            >
              Grant Missing Permissions
            </button>
          </div>
        )}
      </div>

      {/* 2. NOTIFICATIONS & ALERTS */}
      <div className="p-4 rounded-2xl bg-[#151E2E] border border-slate-800 space-y-3 shadow-sm">
        <div className="flex items-center gap-2 text-slate-200">
          <Bell className="w-4 h-4 text-[#4F7CFF]" />
          <h3 className="text-xs font-bold uppercase tracking-wider">Notifications</h3>
        </div>

        <div className="space-y-3 text-xs divide-y divide-slate-800/80">
          <div className="flex items-center justify-between pt-1">
            <div className="pr-3">
              <p className="font-semibold text-slate-200">Heads-up Scam Warnings</p>
              <p className="text-[11px] text-slate-400 mt-0.5 leading-relaxed">
                Displays high-priority security popups during active calls
              </p>
            </div>
            <button
              onClick={onToggleNotif}
              className={`w-11 h-6 flex items-center rounded-full p-1 transition-colors ${
                permissionNotif ? 'bg-[#22C55E]' : 'bg-slate-700'
              }`}
              role="switch"
              aria-checked={permissionNotif}
            >
              <div
                className={`bg-white w-4 h-4 rounded-full shadow-md transform transition-transform ${
                  permissionNotif ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          <div className="flex items-center justify-between pt-3">
            <div className="pr-3">
              <p className="font-semibold text-slate-200">Sensory Vibration Alert</p>
              <p className="text-[11px] text-slate-400 mt-0.5 leading-relaxed">
                Distinctive haptic pulse pattern upon critical threat detection
              </p>
            </div>
            <button
              onClick={() => setHapticAlerts(!hapticAlerts)}
              className={`w-11 h-6 flex items-center rounded-full p-1 transition-colors ${
                hapticAlerts ? 'bg-[#22C55E]' : 'bg-slate-700'
              }`}
              role="switch"
              aria-checked={hapticAlerts}
            >
              <div
                className={`bg-white w-4 h-4 rounded-full shadow-md transform transition-transform ${
                  hapticAlerts ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>
        </div>
      </div>

      {/* 3. PRIVACY CENTER */}
      <div className="p-4 rounded-2xl bg-[#151E2E] border border-slate-800 space-y-3 shadow-sm">
        <div className="flex items-center gap-2 text-slate-200">
          <Lock className="w-4 h-4 text-emerald-400" />
          <h3 className="text-xs font-bold uppercase tracking-wider">Privacy Center</h3>
        </div>

        <div className="space-y-2 text-xs">
          <div className="p-3 rounded-xl bg-[#1C2638] border border-slate-800/80 space-y-1">
            <div className="flex items-center gap-1.5 text-emerald-400 font-semibold text-[11px]">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>100% On-Device Processing</span>
            </div>
            <p className="text-[11px] text-slate-400 leading-relaxed">
              Audio is analyzed in temporary memory buffers. Raw audio is never recorded, saved, or uploaded to any server.
            </p>
          </div>

          <div className="p-3 rounded-xl bg-[#1C2638] border border-slate-800/80 space-y-1">
            <div className="flex items-center gap-1.5 text-emerald-400 font-semibold text-[11px]">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>Private Audit Storage</span>
            </div>
            <p className="text-[11px] text-slate-400 leading-relaxed">
              Call security records stay strictly within app-private local storage.
            </p>
          </div>
        </div>

        <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between">
          <span className="text-xs text-slate-400">Call audit records</span>
          {!showClearConfirm ? (
            <button
              onClick={() => setShowClearConfirm(true)}
              className="text-xs text-rose-400 hover:text-rose-300 font-medium"
            >
              Clear history
            </button>
          ) : (
            <div className="flex items-center gap-2">
              <button
                onClick={() => {
                  onClearHistory();
                  setShowClearConfirm(false);
                }}
                className="text-xs text-rose-400 font-bold px-2 py-1 rounded bg-rose-950/40 border border-rose-800/40"
              >
                Confirm Delete
              </button>
              <button
                onClick={() => setShowClearConfirm(false)}
                className="text-xs text-slate-400 hover:text-slate-200"
              >
                Cancel
              </button>
            </div>
          )}
        </div>
      </div>

      {/* 4. LANGUAGE */}
      <div className="p-4 rounded-2xl bg-[#151E2E] border border-slate-800 space-y-3 shadow-sm">
        <div className="flex items-center gap-2 text-slate-200">
          <Globe className="w-4 h-4 text-[#4F7CFF]" />
          <h3 className="text-xs font-bold uppercase tracking-wider">Language</h3>
        </div>

        <div className="flex items-center justify-between text-xs">
          <div>
            <p className="font-semibold text-slate-200">Analysis Language</p>
            <p className="text-[11px] text-slate-400">Speech recognition language model</p>
          </div>
          <span className="font-mono text-xs font-bold text-slate-300 bg-slate-800 px-2 py-1 rounded-lg border border-slate-700">
            English (India)
          </span>
        </div>

        <p className="text-[11px] text-slate-400 leading-relaxed">
          Indian language models (Hindi, Telugu, Tamil, Kannada, Marathi) will activate automatically when downloaded on-device.
        </p>
      </div>

      {/* 5. ABOUT SVARA_X */}
      <div className="p-4 rounded-2xl bg-[#151E2E] border border-slate-800 space-y-2 shadow-sm">
        <div className="flex items-center gap-2 text-slate-200">
          <Info className="w-4 h-4 text-[#4F7CFF]" />
          <h3 className="text-xs font-bold uppercase tracking-wider">About Svara_X</h3>
        </div>

        <p className="text-xs text-slate-300 font-medium">
          "Svara_X protects you from suspicious calls and social-engineering scams."
        </p>
        <p className="text-[11px] text-slate-400 leading-relaxed">
          Version 1.0.4-release • Built for privacy-first call protection.
        </p>
      </div>

      {/* 6. ADVANCED (DIAGNOSTICS & DEMO NAVIGATION) */}
      <div className="p-4 rounded-2xl bg-[#151E2E] border border-slate-800 space-y-2 shadow-sm">
        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
          Advanced
        </h3>

        <div className="space-y-1.5">
          {/* Developer Testing / Cellular Diagnostic Navigation */}
          <button
            id="btn_launch_developer_testing"
            onClick={() => onNavigateSubView('developer_testing')}
            className="w-full p-3 rounded-xl bg-slate-50 dark:bg-[#1C2638] hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors flex items-center justify-between text-left text-xs border border-slate-200 dark:border-slate-800/80"
          >
            <div className="flex items-center gap-2.5">
              <Wrench className="w-4 h-4 text-purple-600 dark:text-purple-400" />
              <div>
                <p className="font-semibold text-slate-900 dark:text-slate-200">
                  Developer Testing &amp; Cellular Call Diagnostic
                </p>
                <p className="text-[11px] text-slate-500 dark:text-slate-400">
                  Audio source availability (MIC, VOICE_COMMUNICATION, downlink)
                </p>
              </div>
            </div>
            <ChevronRight className="w-4 h-4 text-slate-400 dark:text-slate-500" />
          </button>

          {/* Diagnostics Navigation Button */}
          <button
            id="btn_launch_diagnostics"
            onClick={() => onNavigateSubView('diagnostics')}
            className="w-full p-3 rounded-xl bg-[#1C2638] hover:bg-slate-800 transition-colors flex items-center justify-between text-left text-xs border border-slate-800/80"
          >
            <div className="flex items-center gap-2.5">
              <Radio className="w-4 h-4 text-sky-400" />
              <div>
                <p className="font-semibold text-slate-200">Audio Diagnostics</p>
                <p className="text-[11px] text-slate-400">Hardware probes, AudioRecord test &amp; matrix</p>
              </div>
            </div>
            <ChevronRight className="w-4 h-4 text-slate-500" />
          </button>

          {/* Demo & Testing Navigation Button */}
          <button
            id="btn_launch_demo_testing"
            onClick={() => onNavigateSubView('demo')}
            className="w-full p-3 rounded-xl bg-[#1C2638] hover:bg-slate-800 transition-colors flex items-center justify-between text-left text-xs border border-slate-800/80"
          >
            <div className="flex items-center gap-2.5">
              <Play className="w-4 h-4 text-amber-400" />
              <div>
                <p className="font-semibold text-slate-200">Demo &amp; Testing</p>
                <p className="text-[11px] text-slate-400">Scam simulations and 20-point checklist</p>
              </div>
            </div>
            <ChevronRight className="w-4 h-4 text-slate-500" />
          </button>
        </div>
      </div>
    </div>
  );
}
