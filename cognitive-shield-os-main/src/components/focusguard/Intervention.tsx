import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Check, RotateCcw } from "lucide-react";
import { useCognitive } from "@/lib/cognitive-state";

const TASKS = [
  { id: "water", label: "Drink Water" },
  { id: "stretch", label: "Stretch" },
  { id: "breath", label: "3 Min Breathing" },
];

export function Intervention() {
  const { interventionActive, interventionMessage, dismissIntervention, profile } = useCognitive();
  const [checked, setChecked] = useState<Record<string, boolean>>({});
  const [phase, setPhase] = useState<"Inhale" | "Exhale">("Inhale");

  useEffect(() => {
    if (!interventionActive) {
      setChecked({});
      return;
    }
    const i = setInterval(() => {
      setPhase((p) => (p === "Inhale" ? "Exhale" : "Inhale"));
    }, 4000);
    return () => clearInterval(i);
  }, [interventionActive]);

  const allDone = TASKS.every((t) => checked[t.id]);

  return (
    <AnimatePresence>
      {interventionActive && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[60] overflow-y-auto"
          style={{
            background:
              "radial-gradient(circle at 50% 40%, rgba(0,255,85,0.12), transparent 50%), #02060a",
          }}
        >
          <div className="mx-auto flex min-h-screen max-w-md flex-col items-center px-6 py-10">
            {/* Anchor */}
            <motion.div
              initial={{ opacity: 0, y: -8 }}
              animate={{ opacity: 1, y: 0 }}
              className="mb-6 flex flex-col items-center"
            >
              {profile.photo ? (
                <img
                  src={profile.photo}
                  alt="anchor"
                  className="mb-3 h-16 w-16 rounded-full object-cover ring-2 ring-[#00FF55]/40"
                  style={{ boxShadow: "0 0 30px rgba(0,255,85,0.4)" }}
                />
              ) : (
                <div className="mb-3 h-16 w-16 rounded-full bg-gradient-to-br from-[#00FF55]/30 to-[#00F2FF]/20" />
              )}
              <p className="max-w-xs text-center text-sm italic text-white/70">
                "{profile.quote}"
              </p>
            </motion.div>

            <div className="mb-2 text-[10px] uppercase tracking-[0.4em] text-[#00FF55]/80">
              Recovery Protocol
            </div>
            <h1 className="mb-8 text-center text-2xl font-bold text-white text-glow-green">
              Calming Therapeutic UI
            </h1>

            {/* Breathing visualizer */}
            <div className="relative mb-4 flex h-64 w-64 items-center justify-center">
              <motion.div
                className="absolute rounded-full"
                style={{
                  background:
                    "radial-gradient(circle, rgba(0,255,85,0.5), rgba(0,242,255,0.1) 60%, transparent 75%)",
                  boxShadow: "0 0 80px rgba(0,255,85,0.5), inset 0 0 60px rgba(0,255,85,0.3)",
                }}
                animate={{ width: phase === "Inhale" ? 256 : 120, height: phase === "Inhale" ? 256 : 120 }}
                transition={{ duration: 4, ease: "easeInOut" }}
              />
              <motion.div
                className="absolute rounded-full border-2 border-[#00FF55]/40"
                animate={{ width: phase === "Inhale" ? 256 : 120, height: phase === "Inhale" ? 256 : 120 }}
                transition={{ duration: 4, ease: "easeInOut" }}
              />
              <motion.span
                key={phase}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                className="relative text-lg font-medium text-white text-glow-green"
              >
                {phase}...
              </motion.span>
            </div>

            {/* AI message */}
            <motion.div
              key={interventionMessage}
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              className="glass-strong mb-6 w-full rounded-2xl p-4 text-center text-sm leading-relaxed text-white/85"
              style={{ boxShadow: "0 0 30px rgba(0,255,85,0.15)" }}
            >
              {interventionMessage}
            </motion.div>

            {/* Tasks */}
            <div className="glass mb-6 w-full rounded-2xl p-4">
              <div className="mb-3 text-[10px] uppercase tracking-[0.3em] text-white/50">
                Restoration Checklist
              </div>
              <div className="flex flex-col gap-2">
                {TASKS.map((t) => {
                  const done = !!checked[t.id];
                  return (
                    <button
                      key={t.id}
                      onClick={() => setChecked({ ...checked, [t.id]: !done })}
                      className="flex items-center gap-3 rounded-xl bg-white/5 px-3 py-3 text-left text-sm text-white transition hover:bg-white/10"
                    >
                      <span
                        className={`flex h-5 w-5 items-center justify-center rounded-md border transition ${
                          done
                            ? "border-[#00FF55] bg-[#00FF55]/20 text-[#00FF55]"
                            : "border-white/20"
                        }`}
                        style={done ? { boxShadow: "0 0 10px rgba(0,255,85,0.6)" } : {}}
                      >
                        {done && <Check className="h-3.5 w-3.5" />}
                      </span>
                      <span className={done ? "text-white/60 line-through" : ""}>{t.label}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            <motion.button
              disabled={!allDone}
              onClick={dismissIntervention}
              whileHover={allDone ? { scale: 1.02 } : {}}
              whileTap={allDone ? { scale: 0.98 } : {}}
              className={`flex w-full items-center justify-center gap-2 rounded-2xl py-4 text-sm font-bold uppercase tracking-[0.25em] transition ${
                allDone
                  ? "bg-gradient-to-r from-[#00FF55] to-[#00F2FF] text-black neon-green"
                  : "cursor-not-allowed bg-white/5 text-white/30"
              }`}
            >
              <RotateCcw className="h-4 w-4" />
              Restart
            </motion.button>
            {!allDone && (
              <p className="mt-3 text-[10px] uppercase tracking-widest text-white/30">
                Complete all rituals to continue
              </p>
            )}
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
