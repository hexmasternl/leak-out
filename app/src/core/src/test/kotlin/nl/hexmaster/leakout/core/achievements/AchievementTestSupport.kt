package nl.hexmaster.leakout.core.achievements

/**
 * A won attempt on level 5 that unlocks no skill, lifetime or hidden achievement: one unused
 * section, a replacement tapped once, a pause, no cross passed twice, a short route one section
 * longer than the shortest one, short runs of straights and corners and a small score. Tests
 * change the one fact they are about.
 */
internal fun summary(
    levelId: Int = 5,
    outcome: AttemptOutcome = AttemptOutcome.WON,
    finalTotal: Int? = if (outcome == AttemptOutcome.WON) 120 else null,
    finalStars: Int = if (outcome == AttemptOutcome.WON) 1 else 0,
    unusedSections: Int = if (outcome == AttemptOutcome.WON) 1 else 0,
    distinctTilesLeft: Int = 8,
    sectionsLeft: Int = distinctTilesLeft,
    crossesLeftTwice: Int = 0,
    placements: Int = 10,
    replacements: Int = 1,
    everPaused: Boolean = true,
    fastForwardAtWin: Boolean = false,
    routeCompletedBeforeFlow: Boolean = false,
    photoFinish: Boolean = false,
    freeCells: Int = 46,
    piecesOnBoard: Int = 10,
    record: RecordOutcome? = if (outcome == AttemptOutcome.WON) RecordOutcome.FIRST_COMPLETION else null,
    longestCrossChain: Int = 0,
    longestStraightRun: Int = 2,
    longestCornerRun: Int = 2,
    reservoirsLeft: Int = 0,
    mostReplacementTaps: Int = 1,
    minimumSections: Int? = 9,
) = AttemptSummary(
    levelId, outcome, finalTotal, finalStars, unusedSections, distinctTilesLeft, sectionsLeft, crossesLeftTwice,
    placements, replacements, everPaused, fastForwardAtWin, routeCompletedBeforeFlow, photoFinish, freeCells,
    piecesOnBoard, record, longestCrossChain, longestStraightRun, longestCornerRun, reservoirsLeft,
    mostReplacementTaps, minimumSections,
)

internal fun lostSummary(levelId: Int = 5, sectionsLeft: Int = 4, crossesLeftTwice: Int = 0, placements: Int = 6, mostReplacementTaps: Int = 1) =
    summary(
        levelId = levelId, outcome = AttemptOutcome.LOST, sectionsLeft = sectionsLeft, crossesLeftTwice = crossesLeftTwice,
        placements = placements, mostReplacementTaps = mostReplacementTaps,
    )

/**
 * Evaluates [summary] the way the app does after an attempt: the stats with the attempt added, and
 * the history after recording it.
 */
internal fun evaluateAttempt(
    summary: AttemptSummary?,
    statsBefore: PlayerStats = PlayerStats.NONE,
    history: Map<Int, Int> = emptyMap(),
    stored: Map<Achievement, StoredAchievement> = emptyMap(),
): Evaluation = AchievementRules.evaluate(
    summary,
    statsBefore,
    summary?.let(statsBefore::after) ?: statsBefore,
    history,
    stored,
)

/** A history with a record of [stars] stars for each of [levels]. */
internal fun history(levels: Iterable<Int>, stars: Int = 0): Map<Int, Int> = levels.associateWith { stars }
