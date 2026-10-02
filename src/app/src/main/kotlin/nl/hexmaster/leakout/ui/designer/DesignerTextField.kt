package nl.hexmaster.leakout.ui.designer

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles

/** A single-line field in the designer panel. A field in error is edged in `leak.500` and says why. */
@Composable
internal fun DesignerTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    numeric: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = LeakOutTextStyles.BodySm) },
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(R.string.designer_not_a_number), style = LeakOutTextStyles.BodySm) }
        } else {
            null
        },
        singleLine = true,
        textStyle = LeakOutTextStyles.BodyMd,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = LeakOutColors.TextPrimary,
            unfocusedTextColor = LeakOutColors.TextPrimary,
            focusedBorderColor = LeakOutColors.Liquid300,
            unfocusedBorderColor = LeakOutColors.LineStrong,
            focusedLabelColor = LeakOutColors.Liquid300,
            unfocusedLabelColor = LeakOutColors.TextSecondary,
            cursorColor = LeakOutColors.Liquid300,
            errorBorderColor = LeakOutColors.Leak500,
            errorLabelColor = LeakOutColors.Leak500,
            errorSupportingTextColor = LeakOutColors.Leak500,
            errorTextColor = LeakOutColors.TextPrimary,
            errorCursorColor = LeakOutColors.Leak500,
        ),
        modifier = modifier,
    )
}
