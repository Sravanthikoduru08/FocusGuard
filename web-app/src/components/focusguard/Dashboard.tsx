import { useState } from "react";
import { motion } from "framer-motion";
import { Menu, Settings, Zap, Brain, Activity, ShieldAlert } from "lucide-react";
import { useCognitive, STATE_COLORS } from "@/lib/cognitive-state";
import { NeuralOrb } from "./NeuralOrb";
import { StatRing } from "./StatRing";
import { SettingsModal } from "./SettingsModal";
import { StudySetupModal } from "./StudySetupModal";

export function Dashboard() {
  const { brainState, overload, config, triggerIntervention } = useCognitive();
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [studySetupOpen, setStudySetupOpen] = useState(false);
  const stateInfo = STATE_COLORS[brainState];

  const focusVal = Math.max(0, 100 - overload * 0.6);
  const dopamineVal = Math.min(100, 30 + overload * 0.4);
  const chaosVal = (overload / 150) * 100;

  const openStudySetup = () => setStudySetupOpen(true);

  const simulateDistraction = () => {
    triggerIntervention(
      `Restricted domain detected (${config.restrictedDomains[0] ?? "distracting.app"}). ${config.blockMessage}`,
    );
  };

  return (
    <div id="dashboard-container" className="min-h-screen px-5 pb-12 pt-6">
      {/* Header */}
      <header className="mx-auto flex max-w-md items-center justify-between">
        <button
          onClick={() => setMenuOpen((v) => !v)}
          className="glass flex h-10 w-10 items-center justify-center rounded-xl text-white/70 transition hover:text-white"
        >
          <Menu className="h-4 w-4" />
        </button>
        <div className="text-center">
          <div className="text-[10px] uppercase tracking-[0.3em] text-white/40">FocusGuard</div>
          <div className="text-xs font-semibold text-white">Cognitive OS</div>
        </div>
        <button
          onClick={() => setSettingsOpen(true)}
          className="glass flex h-10 w-10 items-center justify-center rounded-xl text-white/70 transition hover:text-white"
        >
          <Settings className="h-4 w-4" />
        </button>
      </header>

      {menuOpen && (
        <motion.div
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
          className="glass mx-auto mt-3 max-w-md rounded-xl p-3 text-sm text-white/80"
        >
          <button
            onClick={simulateDistraction}
            className="flex w-full items-center gap-2 rounded-lg px-2 py-2 text-left hover:bg-white/5"
          >
            <ShieldAlert className="h-4 w-4 text-[#FF007A]" />
            Simulate distracting website
          </button>
        </motion.div>
      )}

      <main className="mx-auto mt-6 flex max-w-md flex-col items-center gap-8">
        {/* Live Neural Visualization */}
        <div className="flex flex-col items-center">
          <div className="mb-2 text-[10px] uppercase tracking-[0.4em] text-white/40">
            Live Neural Field
          </div>
          <NeuralOrb size={240} />
          <motion.div
            key={brainState}
            initial={{ opacity: 0, y: 4 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-4 flex items-center gap-2"
          >
            <span
              className="h-2 w-2 rounded-full"
              style={{ background: stateInfo.color, boxShadow: `0 0 10px ${stateInfo.color}` }}
            />
            <span
              className="text-sm font-medium"
              style={{ color: stateInfo.color, textShadow: `0 0 10px ${stateInfo.color}` }}
            >
              {stateInfo.label} · {brainState}
            </span>
          </motion.div>
          <div className="mt-1 text-[11px] text-white/40">
            Overload Score: {Math.round(overload)} / 150
          </div>
        </div>

        {/* Stat Rings */}
        <div className="glass flex w-full items-center justify-around rounded-2xl py-5">
          <StatRing label="Focus" value={focusVal} color="#00F2FF" />
          <StatRing label="Dopamine" value={dopamineVal} color="#9D00FF" />
          <StatRing label="Chaos" value={chaosVal} color="#FF007A" />
        </div>

        {/* Insight cards */}
        <div className="grid w-full grid-cols-2 gap-3">
          <InsightCard
            icon={<Brain className="h-4 w-4 text-[#00F2FF]" />}
            title="Cognitive Rest"
            body="Nature mediated in cognitive rest."
            accent="#00F2FF"
          />
          <InsightCard
            icon={<Activity className="h-4 w-4 text-[#FF007A]" />}
            title="Distraction"
            body="Distraction level high today."
            accent="#FF007A"
          />
        </div>

        {/* Action button */}
        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={openStudySetup}
          className="mt-2 flex w-full items-center justify-center gap-3 rounded-2xl bg-gradient-to-r from-[#2B6CFF] via-[#5B5BFF] to-[#9D00FF] py-5 text-sm font-bold uppercase tracking-[0.25em] text-white neon-purple"
        >
          <Zap className="h-4 w-4" />
          Initiate Study Protocol
        </motion.button>
      </main>

      <SettingsModal open={settingsOpen} onClose={() => setSettingsOpen(false)} />
      <StudySetupModal open={studySetupOpen} onClose={() => setStudySetupOpen(false)} />
    </div>
  );
}

function InsightCard({
  icon, title, body, accent,
}: { icon: React.ReactNode; title: string; body: string; accent: string }) {
  return (
    <div
      className="glass rounded-2xl p-4"
      style={{ boxShadow: `inset 0 0 0 1px ${accent}20` }}
    >
      <div className="mb-2 flex items-center gap-2">
        {icon}
        <span className="text-[10px] uppercase tracking-widest text-white/50">{title}</span>
      </div>
      <p className="text-xs leading-relaxed text-white/80">{body}</p>
    </div>
  );
}
