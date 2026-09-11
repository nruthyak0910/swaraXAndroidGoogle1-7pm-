export type NavTab = 'home' | 'calls' | 'protection' | 'settings';

export type SettingsSubView = 'main' | 'diagnostics' | 'developer_testing' | 'demo' | 'privacy' | 'about';

export type ThemeMode = 'system' | 'light' | 'dark';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type CallState = 'IDLE' | 'RINGING' | 'OFFHOOK';

export interface CallRecordItem {
  id: string;
  caller: string;
  timestamp: number;
  duration: number;
  riskScore: number;
  riskLevel: RiskLevel;
  indicators: string[];
  recommendation: string;
  transcriptSnippet?: string;
  isDemo?: boolean;
  category?: string;
  audioMode?: 'DIRECT_CALLER' | 'ACOUSTIC_MICROPHONE' | 'UNAVAILABLE';
}

export interface AudioMetrics {
  currentRms: number;
  peakRms: number;
  samplesRead: number;
  nonZeroSamples: number;
  signal: 'SIGNAL_PRESENT' | 'SILENCE';
}

export interface AudioTestResult {
  status: 'COMPLETE' | 'STOPPED';
  duration: number;
  samples: number;
  nonZero: number;
  peakRms: number;
  signalDetected: boolean;
  isWorking: boolean;
}

export interface ScamEducationCard {
  id: string;
  title: string;
  tag: string;
  summary: string;
  howItWorks: string;
  warningSigns: string[];
  whatToDo: string[];
}

