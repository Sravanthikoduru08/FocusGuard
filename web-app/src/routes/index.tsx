import { createFileRoute } from "@tanstack/react-router";
import { CognitiveProvider, useCognitive } from "@/lib/cognitive-state";
import { Onboarding } from "@/components/focusguard/Onboarding";
import { Dashboard } from "@/components/focusguard/Dashboard";
import { StudyTimer } from "@/components/focusguard/StudyTimer";
import { Intervention } from "@/components/focusguard/Intervention";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "FocusGuard Cognitive OS" },
      {
        name: "description",
        content:
          "A focus timer and cognitive intervention OS. Emotional anchors, live neural visualization, therapeutic recovery.",
      },
      { property: "og:title", content: "FocusGuard Cognitive OS" },
      {
        property: "og:description",
        content: "Prevent passive consumption. Anchor your discipline. Restore your mind.",
      },
    ],
  }),
  component: AppShell,
});

function AppShell() {
  return (
    <CognitiveProvider>
      <Inner />
    </CognitiveProvider>
  );
}

function Inner() {
  const { profile, studyActive } = useCognitive();
  if (!profile.onboarded) return (<><Onboarding /><Intervention /></>);
  return (
    <>
      {studyActive ? <StudyTimer /> : <Dashboard />}
      <Intervention />
    </>
  );
}
