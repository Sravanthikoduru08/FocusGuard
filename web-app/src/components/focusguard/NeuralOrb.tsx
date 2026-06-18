import { motion } from "framer-motion";
import { useCognitive, STATE_COLORS } from "@/lib/cognitive-state";

export function NeuralOrb({ size = 240 }: { size?: number }) {
  const { brainState } = useCognitive();
  const { color } = STATE_COLORS[brainState];

  return (
    <div
      className="relative flex items-center justify-center"
      style={{ width: size, height: size }}
    >
      {/* Outer rings */}
      {[0, 1, 2].map((i) => (
        <motion.div
          key={i}
          className="absolute rounded-full border"
          style={{
            width: size - i * 24,
            height: size - i * 24,
            borderColor: `${color}40`,
            boxShadow: `0 0 ${30 - i * 8}px ${color}80`,
          }}
          animate={{ rotate: i % 2 === 0 ? 360 : -360 }}
          transition={{ duration: 18 + i * 6, repeat: Infinity, ease: "linear" }}
        />
      ))}

      {/* Core orb */}
      <motion.div
        className="absolute rounded-full"
        style={{
          width: size * 0.55,
          height: size * 0.55,
          background: `radial-gradient(circle at 35% 35%, ${color}, ${color}40 60%, transparent 75%)`,
          boxShadow: `0 0 60px ${color}, 0 0 120px ${color}80, inset 0 0 40px ${color}80`,
        }}
        animate={{ scale: [1, 1.08, 1], opacity: [0.85, 1, 0.85] }}
        transition={{ duration: 2.4, repeat: Infinity, ease: "easeInOut" }}
      />

      {/* Inner core dot */}
      <motion.div
        className="absolute rounded-full bg-white"
        style={{ width: 16, height: 16, boxShadow: `0 0 30px white, 0 0 50px ${color}` }}
        animate={{ scale: [1, 1.4, 1] }}
        transition={{ duration: 1.2, repeat: Infinity, ease: "easeInOut" }}
      />

      {/* Particles */}
      {Array.from({ length: 8 }).map((_, i) => {
        const angle = (i / 8) * Math.PI * 2;
        return (
          <motion.div
            key={i}
            className="absolute rounded-full"
            style={{
              width: 4,
              height: 4,
              background: color,
              boxShadow: `0 0 8px ${color}`,
            }}
            animate={{
              x: [Math.cos(angle) * 40, Math.cos(angle) * (size / 2 - 10), Math.cos(angle) * 40],
              y: [Math.sin(angle) * 40, Math.sin(angle) * (size / 2 - 10), Math.sin(angle) * 40],
              opacity: [0.2, 1, 0.2],
            }}
            transition={{ duration: 3 + i * 0.2, repeat: Infinity, ease: "easeInOut" }}
          />
        );
      })}
    </div>
  );
}
