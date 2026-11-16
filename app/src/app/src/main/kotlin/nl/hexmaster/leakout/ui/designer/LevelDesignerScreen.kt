package nl.hexmaster.leakout.ui.designer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.level.DesignStatus
import nl.hexmaster.leakout.core.level.EndpointKind
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.LevelDraft
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.ReservoirType
import nl.hexmaster.leakout.ui.pipes.LocalReservoirType
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.ui.components.CartoonButton
import nl.hexmaster.leakout.ui.components.CartoonButtonVariant
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/** Stable handles for UI tests, independent of copy and layout. */
object DesignerTags {
    const val SCREEN = "designer_screen"
    const val GRID = "designer_grid"
    const val PANEL = "designer_panel"
    const val BACK = "designer_back"
    const val STATUS = "designer_status"
    const val ERROR = "designer_error"
    const val TEST_PLAY = "designer_test_play"
    const val EXPORT = "designer_export"
    const val START_FROM = "designer_start_from"
    const val START_FROM_DIALOG = "designer_start_from_dialog"
    const val START_FROM_BLANK = "designer_start_from_blank"
    const val CLEAR_BLOCKED = "designer_clear_blocked"
    const val CLEAR_LEAKY = "designer_clear_leaky"
    const val LEAKY_NOTE = "designer_leaky_note"
    const val COLUMNS_DOWN = "designer_columns_down"
    const val COLUMNS_UP = "designer_columns_up"
    const val COLUMNS_VALUE = "designer_columns_value"
    const val ROWS_DOWN = "designer_rows_down"
    const val ROWS_UP = "designer_rows_up"
    const val ROWS_VALUE = "designer_rows_value"
    const val ALLOW_REPLACE = "designer_allow_replace"
    const val NAME = "designer_name"
    const val STAR_ONE = "designer_star_one"
    const val STAR_TWO = "designer_star_two"
    const val STAR_THREE = "designer_star_three"
    const val DISCARD_DIALOG = "designer_discard_dialog"
    const val DISCARD_CANCEL = "designer_discard_cancel"
    const val DISCARD_CONFIRM = "designer_discard_confirm"
    const val REMOVE_PAIR = "designer_remove_pair"

    fun tool(tool: DesignerTool): String = "designer_tool_${tool.name.lowercase()}"

    fun reservoirType(type: ReservoirType): String = "designer_reservoir_${type.name.lowercase()}"

    fun side(kind: EndpointKind, side: Side): String = "designer_side_${kind.name.lowercase()}_${side.name.lowercase()}"

    fun portalSide(side: Side): String = "designer_portal_side_${side.name.lowercase()}"

    fun field(field: NumberField): String = "designer_field_" + when (field) {
        is NumberField.Weight -> "weight_${field.section.name.lowercase()}"
        else -> field.toString().lowercase()
    }

    fun copyLevel(id: Int): String = "designer_copy_level_$id"
}

/**
 * The level designer: the design on the left, drawn like the playing field, and the controls in a
 * scrolling panel on the right. Test play hands the level to [onTestPlay]; back, and the system back
 * gesture, call [onBack], after asking to discard a design with unexported changes.
 */
@Composable
fun LevelDesignerScreen(
    viewModel: LevelDesignerViewModel,
    onTestPlay: (Level) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var startFrom by rememberSaveable { mutableStateOf(false) }
    val leave = { if (state.dirty) confirmDiscard = true else onBack() }
    BackHandler(onBack = leave)

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(LeakOutColors.SurfaceAbyss)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp)
            .testTag(DesignerTags.SCREEN),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val status = state.status
        CompositionLocalProvider(LocalReservoirType provides state.draft.reservoirType) {
        DesignerGrid(
            level = state.level,
            highlighted = (status as? DesignStatus.Invalid)?.highlightedCells.orEmpty(),
            path = (status as? DesignStatus.Solvable)?.path.orEmpty(),
            onTap = viewModel::onCellTap,
            onDragStart = viewModel::onDragStart,
            onDrag = viewModel::onDrag,
            onDragEnd = viewModel::onDragEnd,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            unpaired = state.draft.unpaired,
            selected = state.draft.selectedPortal.takeIf { state.tool == DesignerTool.PORTAL },
        )
        }
        DesignerPanel(
            state = state,
            viewModel = viewModel,
            onBack = leave,
            onTestPlay = { viewModel.testPlayLevel()?.let(onTestPlay) },
            onStartFrom = { startFrom = true },
            modifier = Modifier.width(PANEL_WIDTH).fillMaxHeight(),
        )
    }

    if (startFrom) {
        StartFromDialog(
            levelCount = viewModel.bundledLevelCount,
            onBlank = {
                startFrom = false
                viewModel.newBlank()
            },
            onCopy = { id ->
                startFrom = false
                viewModel.copyFrom(id)
            },
            onDismiss = { startFrom = false },
        )
    }
    if (confirmDiscard) {
        DiscardDialog(
            onConfirm = {
                confirmDiscard = false
                onBack()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
}

private val PANEL_WIDTH = 340.dp

@Composable
private fun DesignerPanel(
    state: LevelDesignerUiState,
    viewModel: LevelDesignerViewModel,
    onBack: () -> Unit,
    onTestPlay: () -> Unit,
    onStartFrom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .background(LeakOutColors.SurfaceBase, shape)
            .border(1.dp, LeakOutColors.LineSubtle, shape)
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
            .testTag(DesignerTags.PANEL),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundIconButton(R.drawable.ic_back, stringResource(R.string.designer_back), onBack, Modifier.testTag(DesignerTags.BACK))
            Text(stringResource(R.string.designer_title), style = LeakOutTextStyles.DisplaySm, color = LeakOutColors.TextPrimary)
        }
        StatusSection(state.status)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CartoonButton(
                text = stringResource(R.string.designer_test_play),
                onClick = onTestPlay,
                enabled = state.canPlay,
                variant = CartoonButtonVariant.Success,
                faceHeight = BUTTON_HEIGHT,
                textStyle = LeakOutTextStyles.LabelMd,
                modifier = Modifier.weight(1f).testTag(DesignerTags.TEST_PLAY),
            )
            CartoonButton(
                text = stringResource(R.string.designer_export),
                onClick = { viewModel.export() },
                enabled = state.canPlay,
                variant = CartoonButtonVariant.Primary,
                faceHeight = BUTTON_HEIGHT,
                textStyle = LeakOutTextStyles.LabelMd,
                modifier = Modifier.weight(1f).testTag(DesignerTags.EXPORT),
            )
        }
        CartoonButton(
            text = stringResource(R.string.designer_start_from),
            onClick = onStartFrom,
            variant = CartoonButtonVariant.Secondary,
            faceHeight = BUTTON_HEIGHT,
            textStyle = LeakOutTextStyles.LabelMd,
            modifier = Modifier.fillMaxWidth().testTag(DesignerTags.START_FROM),
        )

        Section(stringResource(R.string.designer_section_tools)) {
            // Start, Finish and Block on the first row, the two mandatory section tools and Portal on the second, Leaky tile on the third.
            DesignerTool.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { tool ->
                        Chip(
                            label = stringResource(toolLabel(tool)),
                            selected = state.tool == tool,
                            onClick = { viewModel.selectTool(tool) },
                            modifier = Modifier.weight(1f).testTag(DesignerTags.tool(tool)),
                        )
                    }
                }
            }
            Chip(
                label = stringResource(R.string.designer_clear_blocked),
                selected = false,
                onClick = viewModel::clearBlocked,
                modifier = Modifier.fillMaxWidth().testTag(DesignerTags.CLEAR_BLOCKED),
            )
            Chip(
                label = stringResource(R.string.designer_clear_leaky),
                selected = false,
                onClick = viewModel::clearLeaky,
                modifier = Modifier.fillMaxWidth().testTag(DesignerTags.CLEAR_LEAKY),
            )
        }

        Section(stringResource(R.string.designer_section_grid)) {
            Stepper(
                label = stringResource(R.string.designer_columns),
                value = state.level.grid.columns,
                canDecrease = state.canShrinkColumns,
                canIncrease = state.canGrowColumns,
                onChange = viewModel::changeColumns,
                tags = Triple(DesignerTags.COLUMNS_DOWN, DesignerTags.COLUMNS_VALUE, DesignerTags.COLUMNS_UP),
            )
            Stepper(
                label = stringResource(R.string.designer_rows),
                value = state.level.grid.rows,
                canDecrease = state.canShrinkRows,
                canIncrease = state.canGrowRows,
                onChange = viewModel::changeRows,
                tags = Triple(DesignerTags.ROWS_DOWN, DesignerTags.ROWS_VALUE, DesignerTags.ROWS_UP),
            )
        }

        Section(stringResource(R.string.designer_section_endpoints)) {
            SideSelector(stringResource(R.string.designer_start_side), EndpointKind.START, state.level.start.openSide, viewModel::setOpenSide)
            SideSelector(stringResource(R.string.designer_finish_side), EndpointKind.FINISH, state.level.finish.openSide, viewModel::setOpenSide)
        }

        val portal = state.draft.selectedPortalEndpoint.takeIf { state.tool == DesignerTool.PORTAL }
        if (portal != null) {
            Section(stringResource(R.string.designer_section_portal, state.draft.pairNumberAt(portal.position) ?: 0)) {
                SideChips(stringResource(R.string.designer_portal_side), portal.openSide, viewModel::setPortalOpenSide, DesignerTags::portalSide)
                Chip(
                    label = stringResource(R.string.designer_remove_pair),
                    selected = false,
                    onClick = viewModel::removeSelectedPair,
                    modifier = Modifier.fillMaxWidth().testTag(DesignerTags.REMOVE_PAIR),
                )
            }
        }

        Section(stringResource(R.string.designer_section_level)) {
            NumberInput(stringResource(R.string.designer_field_id), NumberField.Id, state, viewModel)
            DesignerTextField(
                label = stringResource(R.string.designer_field_name),
                value = state.level.name,
                onValueChange = viewModel::setName,
                isError = false,
                numeric = false,
                modifier = Modifier.fillMaxWidth().testTag(DesignerTags.NAME),
            )
        }

        Section(stringResource(R.string.designer_section_flow)) {
            NumberInput(stringResource(R.string.designer_field_initial_delay), NumberField.InitialDelay, state, viewModel)
            NumberInput(stringResource(R.string.designer_field_cells_per_second), NumberField.CellsPerSecond, state, viewModel)
            ReservoirTypeChips(state.draft.reservoirType, viewModel::setReservoirType)
            NumberInput(stringResource(R.string.designer_field_speed_ramp), NumberField.SpeedRampPerCell, state, viewModel)
        }

        Section(stringResource(R.string.designer_section_stack)) {
            NumberInput(stringResource(R.string.designer_field_visible_size), NumberField.VisibleStackSize, state, viewModel)
            state.level.stack.weights.keys.sorted().chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { section ->
                        NumberInput(section.name, NumberField.Weight(section), state, viewModel, Modifier.weight(1f))
                    }
                }
            }
        }

        Section(stringResource(R.string.designer_section_rules)) {
            Chip(
                label = stringResource(R.string.designer_field_allow_replace),
                selected = state.level.rules.allowReplace,
                onClick = { viewModel.setAllowReplace(!state.level.rules.allowReplace) },
                modifier = Modifier.fillMaxWidth().testTag(DesignerTags.ALLOW_REPLACE),
            )
            NumberInput(stringResource(R.string.designer_field_replace_penalty), NumberField.ReplacePenalty, state, viewModel)
        }

        Section(stringResource(R.string.designer_section_stars)) {
            Text(stringResource(R.string.designer_stars_calculated), style = LeakOutTextStyles.BodySm, color = LeakOutColors.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val stars = state.level.stars
                StarValue(stringResource(R.string.designer_field_star_one), stars.one, DesignerTags.STAR_ONE, Modifier.weight(1f))
                StarValue(stringResource(R.string.designer_field_star_two), stars.two, DesignerTags.STAR_TWO, Modifier.weight(1f))
                StarValue(stringResource(R.string.designer_field_star_three), stars.three, DesignerTags.STAR_THREE, Modifier.weight(1f))
            }
        }
    }
}

private val BUTTON_HEIGHT = 40.dp

private fun toolLabel(tool: DesignerTool): Int = when (tool) {
    DesignerTool.START -> R.string.designer_tool_start
    DesignerTool.FINISH -> R.string.designer_tool_finish
    DesignerTool.BLOCK -> R.string.designer_tool_block
    DesignerTool.MANDATORY_CROSS -> R.string.designer_tool_mandatory_cross
    DesignerTool.MANDATORY_RESERVOIR -> R.string.designer_tool_mandatory_reservoir
    DesignerTool.PORTAL -> R.string.designer_tool_portal
    DesignerTool.LEAKY -> R.string.designer_tool_leaky
}

/** A calculated star threshold: its label over its value, which cannot be edited. */
@Composable
private fun StarValue(label: String, value: Int, tag: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(LeakOutColors.SurfaceOverlay, shape)
            .border(1.dp, LeakOutColors.LineSubtle, shape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = LeakOutTextStyles.BodySm, color = LeakOutColors.TextMuted)
        Text(value.toString(), style = LeakOutTextStyles.NumericMd, color = LeakOutColors.TextPrimary, modifier = Modifier.testTag(tag))
    }
}

/**
 * The status line, and below it every validation error by field, the solver's reason, or for a
 * solvable design without a leak-free route a note that every route crosses a leaky tile.
 */
@Composable
private fun StatusSection(status: DesignStatus) {
    val (text, color) = when (status) {
        is DesignStatus.Solvable -> stringResource(R.string.designer_status_solvable) to LeakOutColors.Success500
        is DesignStatus.Invalid -> stringResource(R.string.designer_status_invalid, status.errors.size) to LeakOutColors.Leak500
        is DesignStatus.Unsolvable -> stringResource(R.string.designer_status_unsolvable) to LeakOutColors.Brass300
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, style = LeakOutTextStyles.LabelMd, color = color, modifier = Modifier.testTag(DesignerTags.STATUS))
        when (status) {
            is DesignStatus.Invalid -> status.errors.forEach { error ->
                Text(
                    text = "${error.field}: ${error.message}",
                    style = LeakOutTextStyles.BodySm,
                    color = LeakOutColors.TextSecondary,
                    modifier = Modifier.testTag(DesignerTags.ERROR),
                )
            }
            is DesignStatus.Unsolvable -> Text(
                text = status.reason,
                style = LeakOutTextStyles.BodySm,
                color = LeakOutColors.TextSecondary,
                modifier = Modifier.testTag(DesignerTags.ERROR),
            )
            is DesignStatus.Solvable -> if (!status.leakFree) {
                Text(
                    text = stringResource(R.string.designer_status_every_route_leaky),
                    style = LeakOutTextStyles.BodySm,
                    color = LeakOutColors.Brass300,
                    modifier = Modifier.testTag(DesignerTags.LEAKY_NOTE),
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = LeakOutTextStyles.LabelMd, color = LeakOutColors.TextMuted)
        content()
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(if (selected) LeakOutColors.Liquid700 else LeakOutColors.SurfaceOverlay, shape)
            .border(1.dp, if (selected) LeakOutColors.Liquid300 else LeakOutColors.LineStrong, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = LeakOutTextStyles.BodySm, color = LeakOutColors.TextPrimary, textAlign = TextAlign.Center)
    }
}

/** Minus, value, plus. A button that would leave the allowed range is disabled. */
@Composable
private fun Stepper(
    label: String,
    value: Int,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onChange: (Int) -> Unit,
    tags: Triple<String, String, String>,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = LeakOutTextStyles.BodyMd, color = LeakOutColors.TextPrimary, modifier = Modifier.weight(1f))
        StepButton("−", stringResource(R.string.designer_decrease, label.lowercase()), canDecrease, { onChange(-1) }, Modifier.testTag(tags.first))
        Text(
            value.toString(),
            style = LeakOutTextStyles.NumericMd,
            color = LeakOutColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp).testTag(tags.second),
        )
        StepButton("+", stringResource(R.string.designer_increase, label.lowercase()), canIncrease, { onChange(+1) }, Modifier.testTag(tags.third))
    }
}

@Composable
private fun StepButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .background(LeakOutColors.SurfaceOverlay, CircleShape)
            .border(1.dp, LeakOutColors.LineStrong, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = LeakOutTextStyles.DisplaySm, color = LeakOutColors.TextPrimary)
    }
}

@Composable
private fun SideSelector(label: String, kind: EndpointKind, current: Side, onSelect: (EndpointKind, Side) -> Unit) =
    SideChips(label, current, { onSelect(kind, it) }) { DesignerTags.side(kind, it) }

/** A label and one chip per side, [current] selected. */
@Composable
private fun SideChips(label: String, current: Side, onSelect: (Side) -> Unit, tag: (Side) -> String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = LeakOutTextStyles.BodySm, color = LeakOutColors.TextPrimary, modifier = Modifier.width(48.dp))
        Side.entries.forEach { side ->
            Chip(
                label = side.name,
                selected = side == current,
                onClick = { onSelect(side) },
                modifier = Modifier.weight(1f).testTag(tag(side)),
            )
        }
    }
}


/** A label and one chip per reservoir type, [current] selected: 10 %, 20 %, 30 % and 40 %. */
@Composable
private fun ReservoirTypeChips(current: ReservoirType, onSelect: (ReservoirType) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.designer_field_reservoir),
            style = LeakOutTextStyles.BodySm,
            color = LeakOutColors.TextPrimary,
            modifier = Modifier.width(72.dp),
        )
        ReservoirType.entries.forEach { type ->
            Chip(
                label = "${type.percent} %",
                selected = type == current,
                onClick = { onSelect(type) },
                modifier = Modifier.weight(1f).testTag(DesignerTags.reservoirType(type)),
            )
        }
    }
}
@Composable
private fun NumberInput(
    label: String,
    field: NumberField,
    state: LevelDesignerUiState,
    viewModel: LevelDesignerViewModel,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    DesignerTextField(
        label = label,
        value = state.text(field),
        onValueChange = { viewModel.setNumber(field, it) },
        isError = state.isInvalid(field),
        numeric = true,
        modifier = modifier.testTag(DesignerTags.field(field)),
    )
}

@Composable
private fun RoundIconButton(icon: Int, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(LeakOutColors.SurfaceOverlay, CircleShape)
            .border(1.dp, LeakOutColors.LineStrong, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = LeakOutColors.TextPrimary, modifier = Modifier.size(24.dp))
    }
}

/** The bundled levels to copy, and a blank level. */
@Composable
private fun StartFromDialog(levelCount: Int, onBlank: () -> Unit, onCopy: (Int) -> Unit, onDismiss: () -> Unit) {
    val title = stringResource(R.string.designer_start_from_title)
    DesignerDialog(title, DesignerTags.START_FROM_DIALOG, onDismiss) {
        LazyColumn(Modifier.heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Chip(stringResource(R.string.designer_start_from_blank), false, onBlank, Modifier.fillMaxWidth().testTag(DesignerTags.START_FROM_BLANK))
            }
            items(levelCount) { index ->
                val id = index + 1
                Chip(stringResource(R.string.designer_start_from_level, id), false, { onCopy(id) }, Modifier.fillMaxWidth().testTag(DesignerTags.copyLevel(id)))
            }
        }
        CartoonButton(
            text = stringResource(R.string.designer_start_from_cancel),
            onClick = onDismiss,
            variant = CartoonButtonVariant.Secondary,
            faceHeight = BUTTON_HEIGHT,
            textStyle = LeakOutTextStyles.LabelMd,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Confirms leaving with unexported changes. Back and a tap outside keep editing. */
@Composable
private fun DiscardDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    DesignerDialog(stringResource(R.string.designer_discard_title), DesignerTags.DISCARD_DIALOG, onDismiss) {
        Text(stringResource(R.string.designer_discard_body), style = LeakOutTextStyles.BodyMd, color = LeakOutColors.TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CartoonButton(
                text = stringResource(R.string.designer_discard_cancel),
                onClick = onDismiss,
                variant = CartoonButtonVariant.Secondary,
                faceHeight = BUTTON_HEIGHT,
                textStyle = LeakOutTextStyles.LabelMd,
                modifier = Modifier.weight(1f).testTag(DesignerTags.DISCARD_CANCEL),
            )
            CartoonButton(
                text = stringResource(R.string.designer_discard_confirm),
                onClick = onConfirm,
                variant = CartoonButtonVariant.Danger,
                faceHeight = BUTTON_HEIGHT,
                textStyle = LeakOutTextStyles.LabelMd,
                modifier = Modifier.weight(1f).testTag(DesignerTags.DISCARD_CONFIRM),
            )
        }
    }
}

@Composable
private fun DesignerDialog(title: String, tag: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)) {
        Column(
            modifier = Modifier
                .testTag(tag)
                .semantics { paneTitle = title }
                .widthIn(max = 380.dp)
                .background(LeakOutColors.SurfaceBase, shape)
                .border(1.dp, LeakOutColors.LineSubtle, shape)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = LeakOutTextStyles.DisplaySm, color = LeakOutColors.TextPrimary)
            content()
        }
    }
}

// Previews

private class PreviewLevels(count: Int) : DesignerLevels {
    private val levels = (1..count).map { LevelDraft.new(it).level }
    override fun levelCount() = levels.size
    override fun load(id: Int) = levels.getOrNull(id - 1)
}

@Composable
private fun DesignerPreview(edit: LevelDesignerViewModel.() -> Unit) {
    val viewModel = LevelDesignerViewModel(PreviewLevels(4), { _, _ -> }, SavedStateHandle()).apply(edit)
    LeakOutTheme { LevelDesignerScreen(viewModel, onTestPlay = {}, onBack = {}) }
}

@Preview(name = "Designer: blank", widthDp = 800, heightDp = 360)
@Composable
private fun BlankDesignPreview() = DesignerPreview {}

@Preview(name = "Designer: invalid", widthDp = 800, heightDp = 360)
@Composable
private fun InvalidDesignPreview() = DesignerPreview {
    setOpenSide(EndpointKind.START, Side.LEFT)
    setNumber(NumberField.InitialDelay, "-5")
}

@Preview(name = "Designer: mandatory sections", widthDp = 800, heightDp = 360)
@Composable
private fun MandatoryDesignPreview() = DesignerPreview {
    selectTool(DesignerTool.MANDATORY_CROSS)
    onCellTap(Position(3, 2))
    selectTool(DesignerTool.MANDATORY_RESERVOIR)
    onCellTap(Position(2, 1))
}

@Preview(name = "Designer: unsolvable", widthDp = 800, heightDp = 360)
@Composable
private fun UnsolvableDesignPreview() = DesignerPreview {
    selectTool(DesignerTool.BLOCK)
    onDragStart(Position(4, 0))
    (1..4).forEach { onDrag(Position(4, it)) }
    onDragEnd()
}
