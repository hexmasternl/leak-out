package nl.hexmaster.leakout.ui.game

import kotlin.random.Random
import nl.hexmaster.leakout.audio.GameSound
import nl.hexmaster.leakout.audio.RecordingSoundEffectPlayer
import nl.hexmaster.leakout.audio.SoundCatalog
import nl.hexmaster.leakout.audio.SoundEffects
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.FlowSettings
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PlacementRules
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.StackSettings
import nl.hexmaster.leakout.core.level.StarBenchmarks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** [GameSession.place] tells a placement on an empty tile, a replacement and a refusal apart, which picks the sound. */
class GameSessionPlaceTest {
    private val blocked = Position(1, 1)

    private fun level(rules: PlacementRules = PlacementRules(replacePenaltyMs = 2_000)) = Level(
        id = 3,
        name = "Test",
        grid = GridSize(4, 4),
        start = Endpoint(Position(0, 0), Side.RIGHT),
        finish = Endpoint(Position(3, 3), Side.LEFT),
        blocked = listOf(blocked),
        flow = FlowSettings(initialDelayMs = 10_000, cellsPerSecond = 1.0),
        stack = StackSettings(visibleSize = 5),
        rules = rules,
        stars = StarBenchmarks(100, 200, 350),
    )

    private fun session(rules: PlacementRules = PlacementRules(replacePenaltyMs = 2_000)) = GameSession(level(rules), Random(42))

    private fun GameSession.assertRejected(at: Position) {
        val before = state
        assertEquals(PlaceResult.Rejected, place(at))
        assertSame(before, state)
    }

    @Test
    fun `an empty tile is placed`() {
        val session = session()
        val next = session.state.stack.next
        assertEquals(PlaceResult.Placed, session.place(Position(2, 0)))
        assertEquals(Cell.Pipe(next), session.state.board[Position(2, 0)])
    }

    @Test
    fun `a tile holding an unfilled section is replaced after the penalty`() {
        val session = session()
        session.placeSection(PipeSection.H, Position(2, 0))
        val next = session.state.stack.next
        assertEquals(PlaceResult.Replaced, session.place(Position(2, 0)))
        assertEquals(Cell.Empty, session.state.board[Position(2, 0)])
        assertTrue(session.isReplacing)
        session.send(GameCommand.Tick(2_000))
        assertEquals(Cell.Pipe(next), session.state.board[Position(2, 0)])
        assertFalse(session.isReplacing)
    }

    @Test
    fun `blocked, start and finish tiles are rejected`() {
        val session = session()
        session.assertRejected(blocked)
        session.assertRejected(Position(0, 0))
        session.assertRejected(Position(3, 3))
    }

    @Test
    fun `a section the liquid has entered is rejected`() {
        val session = session()
        session.state = session.state.copy(board = session.state.board.with(Position(1, 0), Cell.Pipe(PipeSection.H, liquidEntered = true)))
        session.assertRejected(Position(1, 0))
    }

    @Test
    fun `a placed section in a level without replacing is rejected`() {
        val session = session(PlacementRules(allowReplace = false))
        session.placeSection(PipeSection.H, Position(2, 0))
        session.assertRejected(Position(2, 0))
    }

    @Test
    fun `any tile is rejected while a replacement runs`() {
        val session = session()
        session.placeSection(PipeSection.H, Position(2, 0))
        assertEquals(PlaceResult.Replaced, session.place(Position(2, 0)))
        session.assertRejected(Position(3, 0))
        val before = session.state
        assertEquals(PlaceResult.Rejected, session.place(Position(2, 0)))
        // Only the tap is counted: nothing is placed and the stack is unchanged.
        assertEquals(before.copy(replacement = before.replacement?.copy(taps = 1), mostReplacementTaps = 1), session.state)
    }

    @Test
    fun `taps on the replaced tile are counted and stay silent`() {
        val player = RecordingSoundEffectPlayer()
        val sounds = recordingSounds(player)
        val session = session()
        session.placeSection(PipeSection.H, Position(2, 0))
        assertTrue(session.placeWithSound(Position(2, 0), sounds))
        repeat(5) { assertFalse(session.placeWithSound(Position(2, 0), sounds)) }
        assertEquals(5, session.state.replacement?.taps)
        assertEquals(Cell.Empty, session.state.board[Position(2, 0)])
        // Only the destroy of the replacement itself: no placement or failure sound for the taps.
        assertEquals(listOf("section_destroy_001.mp3"), player.played)
        session.send(GameCommand.Tick(2_000))
        assertFalse(session.isReplacing)
        assertEquals(5, session.state.mostReplacementTaps)
    }

    @Test
    fun `a refusal is silent during a replacement`() {
        val session = session()
        session.placeSection(PipeSection.H, Position(2, 0))
        session.place(Position(2, 0))
        assertNull(placementSound(PlaceResult.Rejected, session.state))
    }

    @Test
    fun `each landing is recorded once with its tile and time`() {
        val session = session()
        session.placeSection(PipeSection.H, Position(2, 0))
        session.send(GameCommand.Tick(500))
        session.place(Position(2, 0))
        assertNull(session.lastLanding)
        session.send(GameCommand.Tick(1_999))
        assertNull(session.lastLanding)
        session.send(GameCommand.Tick(1))
        assertEquals(Landing(attempt = 1, at = Position(2, 0), atMs = 2_500), session.lastLanding)
        val landing = session.lastLanding
        session.send(GameCommand.Tick(5_000))
        assertSame(landing, session.lastLanding)
    }

    @Test
    fun `a replacement that ends with a restart does not land`() {
        val session = session()
        session.placeSection(PipeSection.H, Position(2, 0))
        session.place(Position(2, 0))
        val before = session.state
        session.send(GameCommand.Restart)
        assertFalse(landed(before, session.state))
        session.send(GameCommand.Tick(5_000))
        assertNull(session.lastLanding)
    }

    @Test
    fun `a replacement that ends with a leak does not land`() {
        // No route: the liquid leaks out of the start at 11 s, before the replacement lands at 12 s.
        val session = session()
        session.send(GameCommand.Tick(9_000))
        session.placeSection(PipeSection.H, Position(2, 0))
        session.send(GameCommand.Tick(1_000))
        session.place(Position(2, 0))
        session.send(GameCommand.Tick(5_000))
        assertTrue(session.state.outcome is Outcome.Lost)
        assertNull(session.lastLanding)
        assertEquals(Cell.Empty, session.state.board[Position(2, 0)])
    }

    @Test
    fun `the fast forward button is disabled during a replacement`() {
        val session = session()
        assertTrue(session.fastForwardEnabled)
        session.placeSection(PipeSection.H, Position(2, 0))
        session.place(Position(2, 0))
        assertFalse(session.fastForwardEnabled)
        session.send(GameCommand.Tick(2_000))
        assertTrue(session.fastForwardEnabled)
    }

    @Test
    fun `each result has its sound while playing`() {
        val playing = session().state
        assertEquals(GameSound.SectionPlaced, placementSound(PlaceResult.Placed, playing))
        assertEquals(GameSound.SectionDestroyed, placementSound(PlaceResult.Replaced, playing))
        assertEquals(GameSound.PlacementFailed, placementSound(PlaceResult.Rejected, playing))
    }

    @Test
    fun `a refusal is silent when the game takes no sections`() {
        val state = session().state
        assertNull(placementSound(PlaceResult.Rejected, state.copy(paused = true)))
        assertNull(placementSound(PlaceResult.Rejected, state.copy(fastForward = true)))
        assertNull(placementSound(PlaceResult.Rejected, state.copy(outcome = Outcome.Won(atMs = 0))))
    }

    private fun recordingSounds(player: RecordingSoundEffectPlayer) = SoundEffects(
        SoundCatalog.from(listOf("section_placement_001.mp3", "section_destroy_001.mp3", "section_placement_failed.mp3")),
        player,
    )

    @Test
    fun `placing with sound plays what the placement did`() {
        val player = RecordingSoundEffectPlayer()
        val sounds = recordingSounds(player)
        val session = session()
        assertTrue(session.placeWithSound(Position(2, 0), sounds))
        assertTrue(session.placeWithSound(Position(2, 0), sounds))
        // During the replacement a tap does nothing and stays silent.
        assertFalse(session.placeWithSound(Position(3, 0), sounds))
        session.send(GameCommand.Tick(2_000))
        assertFalse(session.placeWithSound(blocked, sounds))
        assertEquals(listOf("section_placement_001.mp3", "section_destroy_001.mp3", "section_placement_failed.mp3"), player.played)
    }

    @Test
    fun `a replacement without a penalty plays the destroy and then the placement sound`() {
        val player = RecordingSoundEffectPlayer()
        val sounds = recordingSounds(player)
        val session = session(PlacementRules(replacePenaltyMs = 0))
        session.placeSection(PipeSection.H, Position(2, 0))
        assertTrue(session.placeWithSound(Position(2, 0), sounds))
        assertFalse(session.isReplacing)
        assertEquals(listOf("section_destroy_001.mp3", "section_placement_001.mp3"), player.played)
    }
}
