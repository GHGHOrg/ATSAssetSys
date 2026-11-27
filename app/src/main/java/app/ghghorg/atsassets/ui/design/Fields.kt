package app.ghghorg.atsassets.ui.design

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

// 44a step 6 (spec 6, Visual design and Error states (1); D-16, D-18, D-27, D-28, D-30, D-31): the catalog's fields. A
// refused field has a 2 dp bronze outline, Lucide's circle-alert in bronze and its message under it; never red, which
// stays for losses. Its note (the New York date note, an original value) sits under it in secondary 12.

/** The standard text field: 56 tall, its label on the outline (D-17). */
@Composable
internal fun AtsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    refusal: String? = null,
    note: String? = null,
    prefix: String? = null,
    trailing: LucideIcon? = null,
    trailingLabel: String? = null,
    onTrailing: (() -> Unit)? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = AtsTheme.colors
    val refused = refusal != null
    Column(modifier = modifier.unavailable(enabled)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            enabled = enabled,
            singleLine = true,
            isError = refused,
            textStyle = typeStyle("body"),
            prefix = prefix?.let { { Text(it, color = colors.textSecondary) } },
            trailingIcon = fieldMarks(refused, trailing, trailingLabel, onTrailing, dense = false),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(tokenDp("radiusDefault")),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.focus,
                unfocusedBorderColor = colors.fieldLine,
                disabledBorderColor = colors.fieldLine,
                errorBorderColor = colors.error,
                focusedLabelColor = colors.text,
                unfocusedLabelColor = colors.textSecondary,
                disabledLabelColor = colors.textSecondary,
                errorLabelColor = colors.error,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                disabledTextColor = colors.text,
                errorTextColor = colors.text,
                cursorColor = colors.focus,
                errorCursorColor = colors.error,
                focusedContainerColor = colors.page,
                unfocusedContainerColor = colors.page,
                disabledContainerColor = colors.page,
                errorContainerColor = colors.page,
            ),
        )
        FieldLines(refusal, note, dense = false)
    }
}

/** The dense text field: 40 tall inside a 48 touch area, Asap 14, at least 120 wide; its label sits beside it (D-18, D-28). */
@Composable
internal fun AtsDenseField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    refusal: String? = null,
    note: String? = null,
    trailing: LucideIcon? = null,
    trailingLabel: String? = null,
    onTrailing: (() -> Unit)? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = AtsTheme.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val refused = refusal != null
    val line = when {
        refused -> colors.error
        focused -> colors.focus
        else -> colors.fieldLine
    }
    val width = if (refused || focused) tokenDp("fieldFocusOutline") else tokenDp("fieldOutline")
    Column(modifier = modifier.unavailable(enabled), horizontalAlignment = Alignment.End) {
        Box(modifier = Modifier.heightIn(min = tokenDp("spaceTouch")), contentAlignment = Alignment.Center) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = typeStyle("small").copy(color = colors.text),
                cursorBrush = SolidColor(if (refused) colors.error else colors.focus),
                interactionSource = source,
                keyboardOptions = keyboardOptions,
                decorationBox = { inner ->
                    Row(
                        modifier = Modifier
                            .widthIn(min = tokenDp("fieldDenseWidth"))
                            .heightIn(min = tokenDp("fieldDenseHeight"))
                            .border(width, line, RoundedCornerShape(tokenDp("radiusDefault")))
                            .padding(horizontal = tokenDp("fieldDensePadding")),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding")),
                    ) {
                        Box(modifier = Modifier.weight(1f, fill = false)) { inner() }
                        fieldMarks(refused, trailing, trailingLabel, onTrailing, dense = true)?.invoke()
                    }
                },
            )
        }
        FieldLines(refusal, note, dense = true)
    }
}

/** A choice from a list, never typed: the field's look with a chevron-down; it opens its list inside the window (D-30, D-31). */
@Composable
internal fun AtsPickField(
    label: String,
    value: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    dense: Boolean = false,
    enabled: Boolean = true,
    spoken: String? = null,
) {
    val colors = AtsTheme.colors
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    Box(
        modifier = modifier
            .unavailable(enabled)
            .heightIn(min = tokenDp("spaceTouch")),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = tokenDp(if (dense) "fieldDenseHeight" else "fieldHeight"))
                .clip(shape)
                .border(tokenDp("fieldOutline"), colors.fieldLine, shape)
                .clickable(enabled = enabled, role = Role.DropdownList, onClickLabel = label, onClick = onOpen)
                // With [spoken], TalkBack reads only that sentence ("Allocation by, Holding"), as one button, and the
                // field's own texts are not separate nodes (44a step 8 group (a); AllocationChoiceFocusTest).
                .then(
                    if (spoken != null) {
                        Modifier.clearAndSetSemantics {
                            contentDescription = spoken
                            role = Role.DropdownList
                            if (!enabled) disabled()
                            onClick(label) {
                                onOpen()
                                true
                            }
                        }
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = tokenDp(if (dense) "fieldDensePadding" else "fieldPadding")),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding")),
        ) {
            if (!dense) Text(label, style = typeStyle("note"), color = colors.textSecondary)
            Text(value, style = typeStyle(if (dense) "small" else "body"), color = colors.text, modifier = Modifier.weight(1f))
            AtsIcon(LucideIcon.CHEVRON_DOWN, tint = colors.iconAction, size = tokenDp(if (dense) "fieldMarkSize" else "iconSize"))
        }
    }
}

private fun fieldMarks(
    refused: Boolean,
    trailing: LucideIcon?,
    trailingLabel: String?,
    onTrailing: (() -> Unit)?,
    dense: Boolean,
): (@Composable () -> Unit)? {
    if (!refused && trailing == null) return null
    return {
        val colors = AtsTheme.colors
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (refused) {
                AtsIcon(LucideIcon.CIRCLE_EXCLAMATION_POINT, tint = colors.error, size = tokenDp(if (dense) "fieldDenseMarkSize" else "fieldMarkSize"))
            }
            if (trailing != null) {
                if (onTrailing != null && trailingLabel != null) {
                    AtsIconButton(trailing, trailingLabel, onTrailing)
                } else {
                    AtsIcon(trailing, tint = colors.iconAction, size = tokenDp(if (dense) "fieldMarkSize" else "iconSize"))
                }
            }
        }
    }
}

@Composable
private fun FieldLines(
    refusal: String?,
    note: String?,
    dense: Boolean,
) {
    val colors = AtsTheme.colors
    val align = if (dense) TextAlign.End else TextAlign.Start
    val inset = if (dense) Modifier else Modifier.padding(horizontal = tokenDp("fieldPadding"))
    if (refusal != null) {
        Text(refusal, style = typeStyle("message"), color = colors.error, textAlign = align, modifier = inset)
    }
    if (note != null) {
        Text(note, style = typeStyle("note"), color = colors.textSecondary, textAlign = align, modifier = inset)
    }
}
