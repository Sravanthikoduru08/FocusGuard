import { useState, useEffect } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { X, Plus, Trash2, Key, Shield } from "lucide-react";
import { useCognitive } from "@/lib/cognitive-state";

export function SettingsModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { config, setConfig } = useCognitive();
  const [apiKey, setApiKey] = useState(config.apiKey);
  const [blockMessage, setBlockMessage] = useState(config.blockMessage);
  const [domains, setDomains] = useState<string[]>(config.restrictedDomains);
  const [newDomain, setNewDomain] = useState("");

  useEffect(() => {
    if (open) {
      setApiKey(config.apiKey);
      setBlockMessage(config.blockMessage);
      setDomains(config.restrictedDomains);
    }
  }, [open, config]);

  const save = () => {
    setConfig({ apiKey, blockMessage, restrictedDomains: domains });
    onClose();
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
            className="glass-strong max-h-[85vh] w-full max-w-md overflow-y-auto rounded-2xl p-6 neon-purple"
          >
            <div className="mb-5 flex items-center justify-between">
              <div>
                <div className="text-[10px] uppercase tracking-[0.3em] text-white/40">Configuration</div>
                <h2 className="text-xl font-bold text-white text-glow-purple">Neural Settings</h2>
              </div>
              <button onClick={onClose} className="text-white/50 hover:text-white">
                <X className="h-5 w-5" />
              </button>
            </div>

            <Section icon={<Key className="h-3.5 w-3.5 text-[#00F2FF]" />} label="AI Gateway Key">
              <input
                type="password"
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
                placeholder="Gemini / OpenAI API Key"
                className="glass w-full rounded-lg px-3 py-2.5 text-sm text-white placeholder:text-white/30 focus:outline-none focus:ring-1 focus:ring-[#00F2FF]/50"
              />
              <p className="mt-1 text-[10px] text-white/40">
                Used to generate personalized therapeutic interventions.
              </p>
            </Section>

            <Section icon={<Shield className="h-3.5 w-3.5 text-[#9D00FF]" />} label="Custom Block Message">
              <textarea
                value={blockMessage}
                onChange={(e) => setBlockMessage(e.target.value)}
                rows={3}
                className="glass w-full resize-none rounded-lg px-3 py-2.5 text-sm text-white placeholder:text-white/30 focus:outline-none focus:ring-1 focus:ring-[#9D00FF]/50"
              />
            </Section>

            <Section icon={<Shield className="h-3.5 w-3.5 text-[#FF007A]" />} label="Restricted Domains">
              <div className="mb-2 flex gap-2">
                <input
                  value={newDomain}
                  onChange={(e) => setNewDomain(e.target.value)}
                  placeholder="example.com"
                  className="glass flex-1 rounded-lg px-3 py-2 text-sm text-white placeholder:text-white/30 focus:outline-none"
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && newDomain.trim()) {
                      setDomains([...domains, newDomain.trim()]);
                      setNewDomain("");
                    }
                  }}
                />
                <button
                  onClick={() => {
                    if (newDomain.trim()) {
                      setDomains([...domains, newDomain.trim()]);
                      setNewDomain("");
                    }
                  }}
                  className="rounded-lg bg-[#FF007A]/20 px-3 text-[#FF007A] hover:bg-[#FF007A]/30"
                >
                  <Plus className="h-4 w-4" />
                </button>
              </div>
              <div className="flex flex-wrap gap-2">
                {domains.map((d, i) => (
                  <span
                    key={`${d}-${i}`}
                    className="glass flex items-center gap-2 rounded-full px-3 py-1 text-xs text-white/80"
                  >
                    {d}
                    <button
                      onClick={() => setDomains(domains.filter((_, idx) => idx !== i))}
                      className="text-white/40 hover:text-[#FF007A]"
                    >
                      <X className="h-3 w-3" />
                    </button>
                  </span>
                ))}
              </div>
            </Section>

            <div className="mt-6 flex gap-2">
              <button
                onClick={() => setDomains([])}
                className="glass flex flex-1 items-center justify-center gap-2 rounded-xl py-3 text-xs uppercase tracking-widest text-white/70 hover:text-white"
              >
                <Trash2 className="h-3.5 w-3.5" />
                Clear App List
              </button>
              <button
                onClick={save}
                className="flex flex-1 items-center justify-center rounded-xl bg-gradient-to-r from-[#9D00FF] to-[#2B6CFF] py-3 text-xs font-bold uppercase tracking-widest text-white neon-purple"
              >
                Save Neural Config
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

function Section({
  icon, label, children,
}: { icon: React.ReactNode; label: string; children: React.ReactNode }) {
  return (
    <div className="mb-4">
      <div className="mb-2 flex items-center gap-2 text-[10px] uppercase tracking-[0.25em] text-white/50">
        {icon}
        {label}
      </div>
      {children}
    </div>
  );
}
