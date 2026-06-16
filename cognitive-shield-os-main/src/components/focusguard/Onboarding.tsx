import { useRef, useState } from "react";
import { motion } from "framer-motion";
import { Upload, Sparkles } from "lucide-react";
import { useCognitive } from "@/lib/cognitive-state";

export function Onboarding() {
  const { profile, setProfile } = useCognitive();
  const [photo, setPhoto] = useState<string | null>(profile.photo);
  const [quote, setQuote] = useState(profile.quote);
  const fileRef = useRef<HTMLInputElement>(null);

  const handleFile = (f: File | null) => {
    if (!f) return;
    const reader = new FileReader();
    reader.onload = (e) => setPhoto(e.target?.result as string);
    reader.readAsDataURL(f);
  };

  const submit = () => {
    setProfile({ photo, quote: quote.trim() || "I build for my family's better tomorrow.", onboarded: true });
  };
  const skip = () => setProfile({ onboarded: true });

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      className="mx-auto flex min-h-screen max-w-md flex-col items-center justify-center px-6 py-12"
    >
      <motion.div
        className="mb-6 flex items-center gap-2 text-xs uppercase tracking-[0.3em] text-white/50"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.2 }}
      >
        <Sparkles className="h-3 w-3 text-[#00F2FF]" />
        FocusGuard Cognitive OS
      </motion.div>

      <h1 className="mb-2 text-center text-4xl font-bold text-white text-glow-cyan">
        Establish Connection
      </h1>
      <p className="mb-8 text-center text-sm text-white/60">
        Anchor your protocol to what matters most.
      </p>

      <motion.div
        whileHover={{ scale: 1.02 }}
        className="glass-strong relative mb-4 flex h-[220px] w-[220px] cursor-pointer items-center justify-center overflow-hidden rounded-2xl neon-cyan"
        onClick={() => fileRef.current?.click()}
      >
        {photo ? (
          <img src={photo} alt="anchor" className="h-full w-full object-cover" />
        ) : (
          <div className="flex flex-col items-center gap-2 text-white/60">
            <Upload className="h-8 w-8" />
            <span className="text-xs">Upload Anchor</span>
          </div>
        )}
        <input
          ref={fileRef}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={(e) => handleFile(e.target.files?.[0] ?? null)}
        />
      </motion.div>

      <p className="mb-8 max-w-xs text-center text-xs leading-relaxed text-white/50">
        Upload photo for family, son, dream, goal. Visual reminders of why you focus help sustain long-term discipline.
      </p>

      <input
        id="quote-input"
        value={quote}
        onChange={(e) => setQuote(e.target.value)}
        placeholder="Type your motivation here..."
        className="glass mb-6 w-full rounded-xl px-4 py-3 text-sm text-white placeholder:text-white/40 focus:outline-none focus:ring-2 focus:ring-[#00F2FF]/50"
      />

      <button
        id="establish-button"
        onClick={submit}
        className="mb-3 w-full rounded-xl bg-gradient-to-r from-[#2B6CFF] to-[#00F2FF] py-4 text-sm font-semibold uppercase tracking-widest text-white transition hover:brightness-110 neon-cyan"
      >
        Establish Connection
      </button>
      <button
        id="skip-button"
        onClick={skip}
        className="text-xs text-white/40 transition hover:text-white/70"
      >
        Skip setup for now
      </button>
    </motion.div>
  );
}
