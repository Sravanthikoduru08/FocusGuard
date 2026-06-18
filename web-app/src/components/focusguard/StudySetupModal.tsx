import { useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { X, BookOpen, Clock, Zap } from "lucide-react";
import { useCognitive } from "@/lib/cognitive-state";

const PRESETS = [15, 25, 45, 60, 90];

export function StudySetupModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { startStudy } = useCognitive();
  const [topic, setTopic] = useState("");
  const [minutes, setMinutes] = useState(25);

  const begin = () => {
    if (!topic.trim()) return;
    startStudy(topic.trim(), minutes);
    onClose();
    setTopic("");
  };

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 px-4 backdrop-blur-md"
          onClick={onClose}
        >
          <motion.div
            initial={{ scale: 0.95, y: 10 }}
            animate={{ scale: 1, y: 0 }}
            exit={{ scale: 0.95, y: 10 }}
            onClick={(e) => e.stopPropagation()}
            className="glass-strong w-full max-w-md rounded-2xl p-6 neon-cyan"
          >
            <div className="mb-5 flex items-center justify-between">
              <div>
                <div className="text-[10px] uppercase tracking-[0.3em] text-white/40">
                  Protocol Setup
                </div>
                <h2 className="text-xl font-bold text-white text-glow-cyan">Study Session</h2>
              </div>
              <button onClick={onClose} className="text-white/50 hover:text-white">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="mb-4">
              <div className="mb-2 flex items-center gap-2 text-[10px] uppercase tracking-[0.25em] text-white/50">
                <BookOpen className="h-3.5 w-3.5 text-[#00F2FF]" />
                Topic / Subject
              </div>
              <input
                autoFocus
                value={topic}
                onChange={(e) => setTopic(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && begin()}
                placeholder="e.g. Linear Algebra · Chapter 4"
                className="glass w-full rounded-lg px-3 py-3 text-sm text-white placeholder:text-white/30 focus:outline-none focus:ring-1 focus:ring-[#00F2FF]/50"
              />
            </div>

            <div className="mb-5">
              <div className="mb-2 flex items-center justify-between text-[10px] uppercase tracking-[0.25em] text-white/50">
                <span className="flex items-center gap-2">
                  <Clock className="h-3.5 w-3.5 text-[#9D00FF]" />
                  Duration
                </span>
                <span className="text-white/70">{minutes} min</span>
              </div>

              <div className="mb-3 flex flex-wrap gap-2">
                {PRESETS.map((m) => (
                  <button
                    key={m}
                    onClick={() => setMinutes(m)}
                    className={`rounded-full px-3 py-1.5 text-xs font-medium transition ${
                      minutes === m
                        ? "bg-gradient-to-r from-[#9D00FF] to-[#00F2FF] text-white neon-purple"
                        : "glass text-white/70 hover:text-white"
                    }`}
                  >
                    {m}m
                  </button>
                ))}
              </div>

              <input
                type="range"
                min={1}
                max={180}
                step={1}
                value={minutes}
                onChange={(e) => setMinutes(Number(e.target.value))}
                className="w-full accent-[#00F2FF]"
              />
              <div className="mt-1 flex justify-between text-[10px] text-white/40">
                <span>1m</span>
                <span>180m</span>
              </div>
            </div>

            <button
              disabled={!topic.trim()}
              onClick={begin}
              className={`flex w-full items-center justify-center gap-2 rounded-xl py-4 text-sm font-bold uppercase tracking-[0.25em] transition ${
                topic.trim()
                  ? "bg-gradient-to-r from-[#2B6CFF] via-[#5B5BFF] to-[#9D00FF] text-white neon-purple"
                  : "cursor-not-allowed bg-white/5 text-white/30"
              }`}
            >
              <Zap className="h-4 w-4" />
              Begin Session
            </button>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
