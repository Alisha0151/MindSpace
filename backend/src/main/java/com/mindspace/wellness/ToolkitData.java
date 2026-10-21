package com.mindspace.wellness;

import com.mindspace.wellness.WellnessDtos.BreathingPattern;
import com.mindspace.wellness.WellnessDtos.GroundingStep;

import java.util.List;

/**
 * Static, non-clinical self-help content. It is served by the API (not hard-coded in the UI) so the
 * breathing timings and prompts have a single source of truth. Nothing here is personal data.
 */
public final class ToolkitData {

    private ToolkitData() {
    }

    public static final String DISCLAIMER =
            "These exercises are general self-care tools, not medical advice or treatment. "
                    + "If you feel unsafe or in crisis, contact local emergency services or a crisis line.";

    public static final List<BreathingPattern> BREATHING = List.of(
            new BreathingPattern("box", "Box breathing", "Equal four-second phases. Steady and easy to remember.", 4, 4, 4, 4, 4),
            new BreathingPattern("calm-4-6", "Longer exhale (4-6)", "A slightly longer out-breath can feel settling.", 4, 0, 6, 0, 6),
            new BreathingPattern("relax-4-7-8", "4-7-8", "Inhale 4, hold 7, exhale 8. Skip it if holding your breath is uncomfortable.", 4, 7, 8, 0, 4));

    public static final List<GroundingStep> GROUNDING = List.of(
            new GroundingStep(5, "see", "Name 5 things you can see around you."),
            new GroundingStep(4, "feel", "Notice 4 things you can feel (your feet on the floor, fabric, air)."),
            new GroundingStep(3, "hear", "Listen for 3 sounds, near or far."),
            new GroundingStep(2, "smell", "Find 2 things you can smell, or recall 2 smells you like."),
            new GroundingStep(1, "taste", "Notice 1 thing you can taste, or take a slow sip of water."));

    public static final List<String> REFLECTION_PROMPTS = List.of(
            "What is taking up most of my attention right now, and how much of it is in my control?",
            "What is one small thing that would make today feel a little easier?",
            "When did I last feel like myself, and what was I doing?",
            "What do I need more of this week? What do I need less of?",
            "What would I say to a friend who felt the way I do now?",
            "What am I avoiding, and what is the smallest first step toward it?",
            "What boundary would protect my energy this week?");

    public static final List<String> GRATITUDE_PROMPTS = List.of(
            "Name one thing that went okay today, however small.",
            "Who did something kind for you recently?",
            "What is something in your surroundings you are glad to have?",
            "What ability or skill of yours helped you this week?");
}
