import { motion } from "framer-motion";

interface Props {
  label: string;
  value: number; // 0-100
  color: string;
  unit?: string;
}

export function StatRing({ label, value, color, unit = "%" }: Props) {
  const r = 38;
  const c = 2 * Math.PI * r;
  const pct = Math.max(0, Math.min(100, value));
  const offset = c - (pct / 100) * c;

  return (
    <div className="flex flex-col items-center gap-2">
      <div className="relative h-24 w-24">
        <svg className="h-24 w-24 -rotate-90" viewBox="0 0 100 100">
          <circle cx="50" cy="50" r={r} stroke="rgba(255,255,255,0.08)" strokeWidth="6" fill="none" />
          <motion.circle
            cx="50"
            cy="50"
            r={r}
            stroke={color}
            strokeWidth="6"
            strokeLinecap="round"
            fill="none"
            strokeDasharray={c}
            initial={{ strokeDashoffset: c }}
            animate={{ strokeDashoffset: offset }}
            transition={{ duration: 1.2, ease: "easeOut" }}
            style={{ filter: `drop-shadow(0 0 6px ${color})` }}
          />
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-lg font-semibold text-white" style={{ textShadow: `0 0 10px ${color}` }}>
            {Math.round(pct)}
            <span className="text-xs text-white/60">{unit}</span>
          </span>
        </div>
      </div>
      <span className="text-xs uppercase tracking-widest text-white/60">{label}</span>
    </div>
  );
}
