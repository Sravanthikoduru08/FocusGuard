import { motion } from "framer-motion";
import { Square, BookOpen } from "lucide-react";
import { useCognitive } from "@/lib/cognitive-state";
import { NeuralOrb } from "./NeuralOrb";

function fmt(sec: number) {
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  const mm = String(m).padStart(2, "0");
  const ss = String(s).padStart(2, "0");
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`;
}

export function StudyTimer() {
  const { studyTopic, studyDurationSec, studyRemainingSec, stopStudy } = useCognitive();
  const elapsed = studyDurationSec - studyRemainingSec;
  const pct = studyDurationSec > 0 ? (elapsed / studyDurationSec) * 100 : 0;
  const done = studyRemainingSec <= 0;

  const r = 130;
  const c = 2 * Math.PI * r;
  const offset = c - (pct / 100) * c;

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      className="min-h-screen px-5 pb-12 pt-10"
    >
      <div className="mx-auto flex max-w-md flex-col items-center">
        <div className="mb-2 text-[10px] uppercase tracking-[0.4em] text-[#00F2FF]/80">
          Study Protocol Active
        </div>
        <div className="mb-8 flex items-center gap-2 text-white">
          <BookOpen className="h-4 w-4 text-[#00F2FF]" />
          <h1 className="text-lg font-semibold text-glow-cyan">{studyTopic || "Focus Session"}</h1>
        </div>

        <div className="relative mb-8 flex h-[300px] w-[300px] items-center justify-center">
          <svg className="absolute h-full w-full -rotate-90" viewBox="0 0 300 300">
            <circle cx="150" cy="150" r={r} stroke="rgba(255,255,255,0.06)" strokeWidth="6" fill="none" />
            <motion.circle
              cx="150"
              cy="150"
              r={r}
              stroke="url(#timerGrad)"
              strokeWidth="6"
              strokeLinecap="round"
              fill="none"
              strokeDasharray={c}
              animate={{ strokeDashoffset: offset }}
              transition={{ duration: 0.6, ease: "linear" }}
              style={{ filter: "drop-shadow(0 0 10px #00F2FF)" }}
            />
            <defs>
              <linearGradient id="timerGrad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stopColor="#00F2FF" />
                <stop offset="100%" stopColor="#9D00FF" />
              </linearGradient>
            </defs>
          </svg>

          <div className="absolute inset-8 flex items-center justify-center opacity-70">
            <NeuralOrb size={180} />
          </div>

          <div className="absolute flex flex-col items-center">
            <div
              className="font-display text-5xl font-bold tabular-nums text-white"
              style={{ textShadow: "0 0 25px rgba(0,242,255,0.7)" }}
            >
              {fmt(studyRemainingSec)}
            </div>
            <div className="mt-1 text-[10px] uppercase tracking-[0.3em] text-white/40">
              Remaining
            </div>
          </div>
        </div>

        <div className="glass mb-6 grid w-full grid-cols-3 gap-3 rounded-2xl p-4 text-center">
          <Stat label="Elapsed" value={fmt(Math.max(0, elapsed))} />
          <Stat label="Total" value={fmt(studyDurationSec)} />
          <Stat label="Progress" value={`${Math.round(pct)}%`} />
        </div>

        {done && (
          <motion.div
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            className="glass-strong mb-4 w-full rounded-xl px-4 py-3 text-center text-sm text-[#00FF55] text-glow-green neon-green"
          >
            Session complete · Well done.
          </motion.div>
        )}

        <button
          onClick={stopStudy}
          className="glass flex w-full items-center justify-center gap-2 rounded-2xl py-4 text-xs font-semibold uppercase tracking-[0.25em] text-white/80 transition hover:text-white"
        >
          <Square className="h-3.5 w-3.5" />
          {done ? "Return to Dashboard" : "End Session Early"}
        </button>
      </div>
    </motion.div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="font-display text-base font-semibold tabular-nums text-white">{value}</div>
      <div className="mt-0.5 text-[9px] uppercase tracking-[0.25em] text-white/40">{label}</div>
    </div>
  );
}
