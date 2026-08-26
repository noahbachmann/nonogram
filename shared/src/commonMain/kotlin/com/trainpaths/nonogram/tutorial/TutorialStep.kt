package com.trainpaths.nonogram.tutorial

/**
 * One tutorial hint. Declaration order is priority order: when several steps are on screen at once,
 * the first unseen one in this list is shown, and dismissing it reveals the next.
 */
enum class TutorialStep(val title: String, val text: String) {
    MENU_PLAY(
        title = "Pick a puzzle",
        text = "Tap a card to start solving it. Your progress is saved as you go.",
    ),
    MENU_FILTER(
        title = "Filter and sort",
        text = "Narrow the list by difficulty or size, choose how it's sorted.",
    ),
    MENU_SWAP_TO_GENERATOR(
        title = "Your own nonograms",
        text = "Switch over to the generator to draw and edit puzzles of your own.",
    ),
    MENU_SETTINGS(
        title = "Settings",
        text = "Preferences like the color-theme live in here.",
    ),

    SETTINGS_THEME(
        title = "Colour theme",
        text = "Pick a color theme you like. Changeable anytime",
    ),
    SETTINGS_SHOW_NAMES(
        title = "Always show names",
        text = "On: puzzle descriptions are visible up front. Off: descriptions stays hidden until you solve it.",
    ),
    SETTINGS_REPLAY(
        title = "Repeat tutorials",
        text = "Lost? This brings all of these tips back from the beginning.",
    ),

    BOARD_AREA(
        title = "The board",
        text = "The numbers along the top and side are the clues. Drag across tiles to fill a run.",
    ),
    BOARD_LOCK(
        title = "Lock the board",
        text = "Locked: dragging draws on the board. Unlocked: dragging pans the board.",
    ),
    BOARD_DRAW_MODE(
        title = "Draw mode",
        text = "Cycle between Draw, Fill, Cross and Erase.",
    ),
    BOARD_ZOOM(
        title = "Zoom out",
        text = "Fits the whole board back on screen after you've zoomed or panned.",
    ),
    BOARD_UNDO(
        title = "Undo and redo",
        text = "Step back through your strokes. Saves up to 10 steps.",
    ),
    GENLIST_NEW(
        title = "Create a nonogram",
        text = "Create a new puzzle. You pick its size and name next.",
    ),
    GENLIST_EDIT(
        title = "Edit a puzzle",
        text = "Tap one of your puzzles to open it in the editor.",
    ),
    GENLIST_SWAP_TO_PUZZLES(
        title = "Back to puzzles",
        text = "Same button in reverse: takes you back to the puzzle list.",
    ),

    GENCONF_SIZE(
        title = "Grid size",
        text = "Rows and columns. Resizing later keeps whatever you've already drawn.",
    ),
    GENCONF_DONE(
        title = "Off you go",
        text = "This takes you to the drawing board and saves your changes when editing.",
    ),
    GENCONF_VALIDITY(
        title = "Validity",
        text = "Green means the puzzle is solvable. Only valid puzzles can be published.",
    ),
    GENCONF_PUBLISH(
        title = "Publishing",
        text = "Send a puzzle for review. Once approved, it will be published.",
    ),

    GEN_WRENCH(
        title = "Puzzle settings",
        text = "Reopen the name, size and publishing options for this puzzle.",
    ),
    GEN_SAVE(
        title = "Save",
        text = "Saves without leaving the board. It lights up whenever there's something to save.",
    );
}

/**
 * The step to show right now: the highest-priority one that is both unseen and currently on screen.
 */
fun nextStep(seen: Set<TutorialStep>, registered: Set<TutorialStep>): TutorialStep? =
    TutorialStep.entries.firstOrNull { it !in seen && it in registered }
