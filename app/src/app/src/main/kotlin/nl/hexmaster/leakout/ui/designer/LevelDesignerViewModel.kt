package nl.hexmaster.leakout.ui.designer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import nl.hexmaster.leakout.BundledLevels
import nl.hexmaster.leakout.core.level.DesignStatus
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.EndpointKind
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.LevelDraft
import nl.hexmaster.leakout.core.level.LevelParser
import nl.hexmaster.leakout.core.level.LevelWriter
import nl.hexmaster.leakout.core.level.ParseResult
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.ReservoirType
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.ui.levels.LevelCatalog

/** What a tap or drag on the designer grid does. */
enum class DesignerTool { START, FINISH, BLOCK, MANDATORY_CROSS, MANDATORY_RESERVOIR, PORTAL, LEAKY }

/** The bundled levels as the designer uses them: how many there are, and a copy of one to start from. */
interface DesignerLevels : LevelCatalog {
    fun load(id: Int): Level?
}

/** A numeric level field the designer edits as text. */
sealed interface NumberField {
    data object Id : NumberField
    data object InitialDelay : NumberField
    data object CellsPerSecond : NumberField
    data object SpeedRampPerCell : NumberField
    data object VisibleStackSize : NumberField
    data object ReplacePenalty : NumberField
    data class Weight(val section: PipeSection) : NumberField
}

/**
 * What the designer screen shows. Numeric fields hold the developer's raw text in [fieldText] once
 * edited; [invalidFields] are the ones whose text is not a number and so did not change the draft.
 */
data class LevelDesignerUiState(
    val draft: LevelDraft,
    val tool: DesignerTool,
    val dirty: Boolean,
    val fieldText: Map<NumberField, String> = emptyMap(),
    val invalidFields: Set<NumberField> = emptySet(),
) {
    val level: Level get() = draft.level

    val status: DesignStatus get() = draft.status

    /** Test play and export are only offered for a valid, solvable design. */
    val canPlay: Boolean get() = status.isPlayable

    val canShrinkColumns: Boolean get() = level.grid.columns > LevelDraft.MIN_GRID_SIZE
    val canGrowColumns: Boolean get() = level.grid.columns < LevelDraft.MAX_GRID_COLUMNS
    val canShrinkRows: Boolean get() = level.grid.rows > LevelDraft.MIN_GRID_SIZE
    val canGrowRows: Boolean get() = level.grid.rows < LevelDraft.MAX_GRID_ROWS

    /** The text a numeric field shows: what was typed, or the draft's value. */
    fun text(field: NumberField): String = fieldText[field] ?: valueText(field, level)

    fun isInvalid(field: NumberField): Boolean = field in invalidFields
}

/**
 * Holds the level being designed. Every edit goes through [LevelDraft]; [LevelDesignerUiState.status]
 * reports whether the result is valid and solvable. The draft, tool and dirty flag are kept in
 * [savedState], so they survive the activity being recreated and process death. Nothing is stored
 * permanently: a design only leaves the app through [export].
 */
class LevelDesignerViewModel(
    private val levels: DesignerLevels,
    private val exporter: LevelExporter,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(restore() ?: LevelDesignerUiState(blank(), DesignerTool.START, dirty = false))
    val uiState: StateFlow<LevelDesignerUiState> = _uiState.asStateFlow()

    // The state a Block drag paints every cell to: decided by the cell the drag started on.
    private var paintTarget: Boolean? = null

    init {
        save(_uiState.value)
    }

    // Starting points

    /** How many bundled levels a design can be copied from, ids `1..bundledLevelCount`. */
    val bundledLevelCount: Int get() = levels.levelCount()

    /** Replaces the design with a blank level. */
    fun newBlank() = replace(blank())

    /** Replaces the design with a copy of bundled level [id]. Unknown ids are ignored. */
    fun copyFrom(id: Int) {
        levels.load(id)?.let { replace(LevelDraft(it)) }
    }

    // Grid and tools

    fun selectTool(tool: DesignerTool) = updateState { it.copy(tool = tool) }

    fun changeColumns(delta: Int) = edit { it.resize(it.level.grid.columns + delta, it.level.grid.rows) }

    fun changeRows(delta: Int) = edit { it.resize(it.level.grid.columns, it.level.grid.rows + delta) }

    fun setOpenSide(kind: EndpointKind, side: Side) = edit { it.setOpenSide(kind, side) }

    fun clearBlocked() = edit { it.clearBlocked() }

    fun clearLeaky() = edit { it.clearLeaky() }

    /** Sets the open side of the portal the Portal tool has selected. */
    fun setPortalOpenSide(side: Side) = edit { it.setPortalOpenSide(side) }

    /** Removes the pair of the portal the Portal tool has selected. */
    fun removeSelectedPair() = edit { it.removeSelectedPair() }

    /** A tap on [cell] with the selected tool. */
    fun onCellTap(cell: Position) {
        when (_uiState.value.tool) {
            DesignerTool.START -> edit { it.placeStart(cell) }
            DesignerTool.FINISH -> edit { it.placeFinish(cell) }
            DesignerTool.BLOCK -> edit { it.toggleBlocked(cell) }
            DesignerTool.MANDATORY_CROSS -> edit { it.toggleMandatory(cell, PipeSection.X) }
            DesignerTool.MANDATORY_RESERVOIR -> edit { it.toggleMandatory(cell, PipeSection.RES) }
            DesignerTool.PORTAL -> edit { it.tapPortal(cell) }
            DesignerTool.LEAKY -> edit { it.toggleLeaky(cell) }
        }
    }

    /**
     * A drag starts on [cell]. With the Block and Leaky tile tools every cell passed is set to the
     * opposite of this one.
     */
    fun onDragStart(cell: Position) {
        val state = _uiState.value
        when (state.tool) {
            DesignerTool.BLOCK -> {
                paintTarget = !state.level.isBlocked(cell)
                onDrag(cell)
            }
            DesignerTool.LEAKY -> {
                paintTarget = !state.level.isLeaky(cell)
                onDrag(cell)
            }
            else -> onCellTap(cell)
        }
    }

    /**
     * The drag has moved onto [cell]. The mandatory and portal tools ignore it: they only act on the
     * cell the drag started on, so a drag does not toggle a trail of sections or portals.
     */
    fun onDrag(cell: Position) {
        when (_uiState.value.tool) {
            DesignerTool.BLOCK -> paintTarget?.let { target -> edit { it.paintBlocked(listOf(cell), target) } }
            DesignerTool.LEAKY -> paintTarget?.let { target -> edit { it.paintLeaky(listOf(cell), target) } }
            DesignerTool.MANDATORY_CROSS, DesignerTool.MANDATORY_RESERVOIR, DesignerTool.PORTAL -> Unit
            else -> onCellTap(cell)
        }
    }

    fun onDragEnd() {
        paintTarget = null
    }

    // Other fields

    fun setName(name: String) = edit { it.withName(name) }

    /** The developer chose [type]: `flow.reservoirSpeedFactor` becomes its factor. */
    fun setReservoirType(type: ReservoirType) = edit { it.withReservoirType(type) }

    fun setAllowReplace(allow: Boolean) = edit { it.withRules(it.level.rules.copy(allowReplace = allow)) }

    /**
     * The developer typed [text] into [field]. Text that parses updates the draft; other text is
     * kept and marked invalid, and the draft keeps its previous value.
     */
    fun setNumber(field: NumberField, text: String) {
        val state = _uiState.value
        val updated = apply(field, text.trim(), state.draft)
        val next = state.copy(
            fieldText = state.fieldText + (field to text),
            invalidFields = if (updated == null) state.invalidFields + field else state.invalidFields - field,
        )
        commit(if (updated == null || updated == state.draft) next else next.copy(draft = updated, dirty = true))
    }

    // Leaving the designer

    /** Writes the design and hands it to the exporter. Only a playable design is exported; it then counts as saved. */
    fun export(): Boolean {
        val state = _uiState.value
        if (!state.canPlay) return false
        exporter.export(fileName(state.level.id), LevelWriter.write(state.level))
        updateState { it.copy(dirty = false) }
        return true
    }

    /** The level to test play, or `null` when the design is not valid and solvable. */
    fun testPlayLevel(): Level? = _uiState.value.takeIf { it.canPlay }?.level

    private fun blank(): LevelDraft = LevelDraft.new(id = levels.levelCount() + 1)

    private fun replace(draft: LevelDraft) =
        commit(LevelDesignerUiState(draft, _uiState.value.tool, dirty = false))

    /**
     * Applies a draft edit; the design only becomes dirty when the edit changed the design itself.
     * Selecting a portal changes the draft but not the design.
     */
    private fun edit(change: (LevelDraft) -> LevelDraft) = updateState { state ->
        val draft = change(state.draft)
        val changed = draft.level != state.draft.level || draft.unpaired != state.draft.unpaired
        if (draft == state.draft) state else state.copy(draft = draft, dirty = state.dirty || changed)
    }

    private fun updateState(change: (LevelDesignerUiState) -> LevelDesignerUiState) {
        val before = _uiState.value
        val after = change(before)
        if (after != before) commit(after)
    }

    private fun commit(state: LevelDesignerUiState) {
        _uiState.update { state }
        save(state)
    }

    private fun save(state: LevelDesignerUiState) {
        savedState[KEY_DRAFT] = LevelWriter.write(state.level)
        savedState[KEY_TOOL] = state.tool.name
        savedState[KEY_DIRTY] = state.dirty
        // The level holds complete portal pairs only; the unpaired and the selected portal go alongside.
        savedState[KEY_UNPAIRED] = state.draft.unpaired?.let { "${it.position.x},${it.position.y},${it.openSide.name}" }
        savedState[KEY_SELECTED_PORTAL] = state.draft.selectedPortal?.let { "${it.x},${it.y}" }
    }

    private fun restore(): LevelDesignerUiState? {
        val json = savedState.get<String>(KEY_DRAFT) ?: return null
        val level = (LevelParser.parse(json) as? ParseResult.Success)?.level ?: return null
        val tool = savedState.get<String>(KEY_TOOL)?.let { name -> DesignerTool.entries.firstOrNull { it.name == name } }
        val unpaired = savedState.get<String>(KEY_UNPAIRED)?.split(',')?.let { parts ->
            val side = Side.entries.firstOrNull { it.name == parts.getOrNull(2) }
            val x = parts.getOrNull(0)?.toIntOrNull()
            val y = parts.getOrNull(1)?.toIntOrNull()
            if (side != null && x != null && y != null) Endpoint(Position(x, y), side) else null
        }
        val selected = savedState.get<String>(KEY_SELECTED_PORTAL)?.split(',')?.let { parts ->
            val x = parts.getOrNull(0)?.toIntOrNull()
            val y = parts.getOrNull(1)?.toIntOrNull()
            if (x != null && y != null) Position(x, y) else null
        }
        // A design kept from before the grid limits shrank may be too large: shrink it to fit.
        // A design saved with another reservoir speed factor snaps to the nearest reservoir type.
        val draft = LevelDraft(level, unpaired, selected).let { it.resize(it.level.grid.columns, it.level.grid.rows) }.snapReservoirType()
        return LevelDesignerUiState(
            draft,
            tool ?: DesignerTool.START,
            savedState.get<Boolean>(KEY_DIRTY) ?: false,
        )
    }

    companion object {
        internal const val KEY_DRAFT = "designer.draft"
        internal const val KEY_TOOL = "designer.tool"
        internal const val KEY_DIRTY = "designer.dirty"
        internal const val KEY_UNPAIRED = "designer.unpairedPortal"
        internal const val KEY_SELECTED_PORTAL = "designer.selectedPortal"

        /** The suggested file name for a level with [id]: `level_005.json`. */
        fun fileName(id: Int): String = BundledLevels.path(id).substringAfterLast('/')
    }
}

/** The draft with [field] set from [text], or `null` when the text is not a number of the field's kind. */
private fun apply(field: NumberField, text: String, draft: LevelDraft): LevelDraft? {
    val level = draft.level
    fun int(set: (Int) -> LevelDraft) = text.toIntOrNull()?.let(set)
    fun long(set: (Long) -> LevelDraft) = text.toLongOrNull()?.let(set)
    fun double(set: (Double) -> LevelDraft) = text.toDoubleOrNull()?.takeIf { it.isFinite() }?.let(set)
    return when (field) {
        NumberField.Id -> int(draft::withId)
        NumberField.InitialDelay -> long { draft.withFlow(level.flow.copy(initialDelayMs = it)) }
        NumberField.CellsPerSecond -> double { draft.withFlow(level.flow.copy(cellsPerSecond = it)) }
        NumberField.SpeedRampPerCell -> double { draft.withFlow(level.flow.copy(speedRampPerCell = it)) }
        NumberField.VisibleStackSize -> int(draft::withVisibleStackSize)
        NumberField.ReplacePenalty -> long { draft.withRules(level.rules.copy(replacePenaltyMs = it)) }
        is NumberField.Weight -> int { draft.withWeight(field.section, it) }
    }
}

/** The draft's current value of [field] as text. */
private fun valueText(field: NumberField, level: Level): String = when (field) {
    NumberField.Id -> level.id.toString()
    NumberField.InitialDelay -> level.flow.initialDelayMs.toString()
    NumberField.CellsPerSecond -> level.flow.cellsPerSecond.toString()
    NumberField.SpeedRampPerCell -> level.flow.speedRampPerCell.toString()
    NumberField.VisibleStackSize -> level.stack.visibleSize.toString()
    NumberField.ReplacePenalty -> level.rules.replacePenaltyMs.toString()
    is NumberField.Weight -> level.stack.weightOf(field.section).toString()
}
