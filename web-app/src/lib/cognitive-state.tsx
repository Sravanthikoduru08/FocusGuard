import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";

export type BrainState = "CALM" | "BUSY" | "FRAGMENTED" | "RECOVERY";

export interface CognitiveProfile {
  photo: string | null;
  quote: string;
  onboarded: boolean;
}

export interface CognitiveConfig {
  apiKey: string;
  blockMessage: string;
  restrictedDomains: string[];
}

interface CognitiveContextValue {
  overload: number;
  brainState: BrainState;
  setBrainState: (s: BrainState) => void;
  addOverload: (n: number) => void;
  resetOverload: () => void;
  profile: CognitiveProfile;
  setProfile: (p: Partial<CognitiveProfile>) => void;
  config: CognitiveConfig;
  setConfig: (c: Partial<CognitiveConfig>) => void;
  // Intervention
  interventionActive: boolean;
  interventionMessage: string;
  triggerIntervention: (msg?: string) => void;
  dismissIntervention: () => void;
  // Study protocol
  studyActive: boolean;
  studyTopic: string;
  studyDurationSec: number;
  studyRemainingSec: number;
  startStudy: (topic: string, minutes: number) => void;
  stopStudy: () => void;
  // Passive tracking
  passiveSeconds: number;
  lastMilestone: string | null;
}

const Ctx = createContext<CognitiveContextValue | null>(null);

const defaultConfig: CognitiveConfig = {
  apiKey: "",
  blockMessage:
    "Your mind is currently in a high-focus protocol. This environment may disrupt your stabilization.",
  restrictedDomains: ["instagram.com", "youtube.com", "tiktok.com", "twitter.com"],
};

export function CognitiveProvider({ children }: { children: ReactNode }) {
  const [overload, setOverload] = useState(20);
  const [brainState, setBrainStateRaw] = useState<BrainState>("CALM");
  const [profile, setProfileRaw] = useState<CognitiveProfile>({
    photo: null,
    quote: "I build for my family's better tomorrow.",
    onboarded: false,
  });
  const [config, setConfigRaw] = useState<CognitiveConfig>(defaultConfig);
  const [interventionActive, setInterventionActive] = useState(false);
  const [interventionMessage, setInterventionMessage] = useState("");
  const [studyActive, setStudyActive] = useState(false);
  const [studyTopic, setStudyTopic] = useState("");
  const [studyDurationSec, setStudyDurationSec] = useState(0);
  const [studyEndAt, setStudyEndAt] = useState<number | null>(null);
  const [studyRemainingSec, setStudyRemainingSec] = useState(0);
  const [passiveSeconds, setPassiveSeconds] = useState(0);
  const [lastMilestone, setLastMilestone] = useState<string | null>(null);
  const firedMilestones = useRef<Set<string>>(new Set());

  // Study countdown
  useEffect(() => {
    if (!studyActive || studyEndAt == null) return;
    const tick = () => {
      const remaining = Math.max(0, Math.ceil((studyEndAt - Date.now()) / 1000));
      setStudyRemainingSec(remaining);
      if (remaining <= 0) {
        setStudyActive(false);
        setStudyEndAt(null);
      }
    };
    tick();
    const i = setInterval(tick, 500);
    return () => clearInterval(i);
  }, [studyActive, studyEndAt]);

  // Derive brain state from overload (unless RECOVERY)
  useEffect(() => {
    if (brainState === "RECOVERY") return;
    if (overload <= 50) setBrainStateRaw("CALM");
    else if (overload <= 100) setBrainStateRaw("BUSY");
    else setBrainStateRaw("FRAGMENTED");
  }, [overload, brainState]);

  // Passive consumption tracking — when tab hidden, increment
  useEffect(() => {
    const onVis = () => {
      if (document.hidden) {
        // started passive
      }
    };
    document.addEventListener("visibilitychange", onVis);
    const interval = setInterval(() => {
      if (document.hidden) {
        setPassiveSeconds((s) => s + 1);
        setOverload((o) => Math.min(150, o + 0.3));
      }
    }, 1000);
    return () => {
      clearInterval(interval);
      document.removeEventListener("visibilitychange", onVis);
    };
  }, []);

  // Milestone warnings
  useEffect(() => {
    const checks: Array<[number, string, string]> = [
      [300, "5m", "Neural activity patterns suggest rising fatigue. Maybe take a breath?"],
      [900, "15m", "Attention fragmentation increasing. Consider a neural reset."],
      [1800, "30m", "Passive consumption detected. Your goals are waiting for you."],
    ];
    for (const [sec, key, msg] of checks) {
      if (passiveSeconds >= sec && !firedMilestones.current.has(key)) {
        firedMilestones.current.add(key);
        setLastMilestone(msg);
        triggerIntervention(msg);
        break;
      }
    }
    const hour = new Date().getHours();
    if ((hour >= 23 || hour < 4) && !firedMilestones.current.has("night")) {
      firedMilestones.current.add("night");
      setLastMilestone("Neural overstimulation detected in nocturnal hours.");
    }
  }, [passiveSeconds]);

  const setBrainState = (s: BrainState) => setBrainStateRaw(s);
  const addOverload = (n: number) => setOverload((o) => Math.max(0, Math.min(150, o + n)));
  const resetOverload = () => {
    setOverload(15);
    firedMilestones.current.clear();
    setPassiveSeconds(0);
    setLastMilestone(null);
  };
  const setProfile = (p: Partial<CognitiveProfile>) =>
    setProfileRaw((prev) => ({ ...prev, ...p }));
  const setConfig = (c: Partial<CognitiveConfig>) =>
    setConfigRaw((prev) => ({ ...prev, ...c }));

  const triggerIntervention = (msg?: string) => {
    setInterventionMessage(msg ?? config.blockMessage);
    setInterventionActive(true);
    setBrainStateRaw("RECOVERY");
  };
  const dismissIntervention = () => {
    setInterventionActive(false);
    resetOverload();
    setBrainStateRaw("CALM");
  };

  const startStudy = (topic: string, minutes: number) => {
    const dur = Math.max(1, Math.round(minutes)) * 60;
    setStudyTopic(topic);
    setStudyDurationSec(dur);
    setStudyRemainingSec(dur);
    setStudyEndAt(Date.now() + dur * 1000);
    setStudyActive(true);
    setOverload(40);
  };
  const stopStudy = () => {
    setStudyActive(false);
    setStudyEndAt(null);
  };

  const value = useMemo<CognitiveContextValue>(
    () => ({
      overload,
      brainState,
      setBrainState,
      addOverload,
      resetOverload,
      profile,
      setProfile,
      config,
      setConfig,
      interventionActive,
      interventionMessage,
      triggerIntervention,
      dismissIntervention,
      studyActive,
      studyTopic,
      studyDurationSec,
      studyRemainingSec,
      startStudy,
      stopStudy,
      passiveSeconds,
      lastMilestone,
    }),
    [overload, brainState, profile, config, interventionActive, interventionMessage, studyActive, studyTopic, studyDurationSec, studyRemainingSec, passiveSeconds, lastMilestone],
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useCognitive() {
  const v = useContext(Ctx);
  if (!v) throw new Error("useCognitive must be used inside CognitiveProvider");
  return v;
}

export const STATE_COLORS: Record<BrainState, { color: string; label: string; glow: string }> = {
  CALM: { color: "#00F2FF", label: "Stable", glow: "neon-cyan" },
  BUSY: { color: "#9D00FF", label: "Engaged", glow: "neon-purple" },
  FRAGMENTED: { color: "#FF007A", label: "Fragmented", glow: "neon-pink" },
  RECOVERY: { color: "#00FF55", label: "Recovering", glow: "neon-green" },
};
