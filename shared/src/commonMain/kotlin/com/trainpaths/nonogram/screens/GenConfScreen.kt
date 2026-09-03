package com.trainpaths.nonogram.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.BUTTON_SHAPE
import com.trainpaths.nonogram.outlinedFieldColors
import com.trainpaths.nonogram.switchColors
import com.trainpaths.nonogram.auth.AuthState
import com.trainpaths.nonogram.classes.MAX_NONOGRAM_SIDE
import com.trainpaths.nonogram.classes.MIN_NONOGRAM_SIDE
import com.trainpaths.nonogram.classes.PublishStatus
import com.trainpaths.nonogram.classes.normalizeNonogramName
import com.trainpaths.nonogram.classes.sanitizeNameInput
import com.trainpaths.nonogram.dialogs.PublicEditConfirmDialog
import com.trainpaths.nonogram.icons.build
import com.trainpaths.nonogram.navigation.TopAppBar
import com.trainpaths.nonogram.screens.viewModel.GenViewModel
import com.trainpaths.nonogram.screens.viewModel.ValidationState
import com.trainpaths.nonogram.tutorial.TutorialStep
import com.trainpaths.nonogram.tutorial.tutorialAnchor

/**
 * Digits the size fields accept \u2014 as many as [MAX_NONOGRAM_SIDE] needs, so a number too long to
 * fit an `Int` can't reach `toIntOrNull()` and make the Done button silently do nothing.
 */
private const val SIDE_RANGE_HINT = "$MIN_NONOGRAM_SIDE-$MAX_NONOGRAM_SIDE"

@Composable
fun GenConfScreen(
    genViewModel: GenViewModel,
    editing: Boolean,
    isPublishBanned: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    var name by remember { mutableStateOf(genViewModel.nonogram.name.orEmpty()) }
    var rows by remember { mutableStateOf(genViewModel.height.toString()) }
    var cols by remember { mutableStateOf(genViewModel.width.toString()) }
    var pendingPublicSave by remember { mutableStateOf<(() -> Unit)?>(null) }
    val authState by genViewModel.authState.collectAsState()

    val textFieldColors = outlinedFieldColors()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopAppBar(
            titleIcon = build,
            onBack = { if (!genViewModel.isSaving) onBack() },
            backArrow = true,
            showSettings = true,
        )

        Column(
            modifier = Modifier.fillMaxHeight().width(260.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = sanitizeNameInput(it) },
                label = { Text("Name") },
                placeholder = { Text("...") },
                singleLine = true,
                enabled = !genViewModel.isSaving,
                colors = textFieldColors,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .tutorialAnchor(TutorialStep.GENCONF_SIZE),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = rows,
                    onValueChange = { rows = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Rows") },
                    supportingText = { Text(SIDE_RANGE_HINT) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !genViewModel.isSaving,
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f),
                )

                OutlinedTextField(
                    value = cols,
                    onValueChange = { cols = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Columns") },
                    supportingText = { Text(SIDE_RANGE_HINT) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !genViewModel.isSaving,
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f),
                )
            }

            if (editing) {
                PublishSection(
                    genViewModel = genViewModel,
                    isPublishBanned = isPublishBanned,
                    isSignedIn = authState == AuthState.SIGNED_IN,
                )
            }

            Button(
                onClick = {
                    val h = (rows.toIntOrNull() ?: return@Button)
                        .coerceIn(MIN_NONOGRAM_SIDE, MAX_NONOGRAM_SIDE)
                    val w = (cols.toIntOrNull() ?: return@Button)
                        .coerceIn(MIN_NONOGRAM_SIDE, MAX_NONOGRAM_SIDE)
                    rows = h.toString()
                    cols = w.toString()
                    val normalizedName = normalizeNonogramName(name)
                    if (!editing) {
                        genViewModel.setNonogram(h, w, normalizedName)
                        onDone()
                        return@Button
                    }
                    val save = {
                        genViewModel.updateName(normalizedName)
                        genViewModel.resizeNonogram(h, w)
                        genViewModel.onSave(onDone = onDone)
                    }
                    // Checked before the edits are applied, so cancelling leaves the board untouched.
                    val changesContent = genViewModel.isDirty ||
                            normalizedName != genViewModel.nonogram.name ||
                            h != genViewModel.height ||
                            w != genViewModel.width
                    if (genViewModel.needsPublicEditConfirmation(changesContent)) {
                        pendingPublicSave = save
                    } else {
                        save()
                    }
                },
                enabled = !genViewModel.isSaving,
                shape = BUTTON_SHAPE,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (editing) 12.dp else 32.dp)
                    .height(48.dp)
                    .tutorialAnchor(TutorialStep.GENCONF_DONE),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(if (editing) "Save" else "Generate", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    pendingPublicSave?.let { save ->
        PublicEditConfirmDialog(
            onConfirm = {
                pendingPublicSave = null
                save()
            },
            onCancel = { pendingPublicSave = null },
        )
    }
}

/**
 * The publishing controls, shown only when editing a saved puzzle
 */
@Composable
private fun PublishSection(
    genViewModel: GenViewModel,
    isPublishBanned: Boolean,
    isSignedIn: Boolean,
) {
    val validationState = genViewModel.validationState
    val isValid = validationState == ValidationState.VALID &&
            genViewModel.saveError == null
    val status = genViewModel.nonogram.publishStatus
    val canRequest = !genViewModel.isSaving && isSignedIn && isValid && !isPublishBanned
    val hint = when {
        genViewModel.isSaving -> null
        status == PublishStatus.PENDING -> "Waiting for review."
        status == PublishStatus.DENIED -> "Edit the puzzle to request again."
        status != PublishStatus.NONE -> null
        isPublishBanned -> "You can no longer request publishing."
        !isSignedIn -> "Sign in to publish this nonogram."
        !isValid -> "Only valid nonograms can be published."
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp)
            .tutorialAnchor(TutorialStep.GENCONF_VALIDITY),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "Validity",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(Modifier.weight(1f))
        if (validationState == ValidationState.CHECKING) {
            CircularProgressIndicator(
                modifier = Modifier.width(20.dp).height(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                "Checking…",
                modifier = Modifier.padding(start = 8.dp),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Box(
                modifier = Modifier
                    .background(
                        color =
                            if (validationState == ValidationState.VALID) MaterialTheme.colorScheme.onTertiary
                            else MaterialTheme.colorScheme.tertiaryFixed,
                        RoundedCornerShape(6.dp)
                    ).size(30.dp)
                    .border(
                        1.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        RoundedCornerShape(6.dp),
                    ),
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .tutorialAnchor(TutorialStep.GENCONF_PUBLISH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "Publish",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(Modifier.weight(1f))
        when (status) {
            PublishStatus.APPROVED, PublishStatus.UNLISTED -> {
                val isPublic = genViewModel.nonogram.isPublic
                Text(
                    text = if (isPublic) "Public" else "Private",
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        if (isPublic) MaterialTheme.colorScheme.onTertiary
                        else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Switch(
                    checked = isPublic,
                    onCheckedChange = { genViewModel.setPublic(it) },
                    enabled = !genViewModel.isSaving,
                    colors = switchColors(),
                )
            }

            PublishStatus.DENIED -> Text(
                "Denied",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiaryFixed,
            )

            else -> Button(
                onClick = { genViewModel.requestPublish() },
                enabled = status == PublishStatus.NONE && canRequest &&
                        !genViewModel.isRequestingPublish,
                shape = BUTTON_SHAPE,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    if (status == PublishStatus.PENDING) "Sent" else "Request publish",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    // Reserved so a hint or error drops into the gap instead of pushing Save down.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .heightIn(min = 36.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        hint?.let {
            Text(
                text = it,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }

        genViewModel.publishError?.let { error ->
            Text(
                text = error,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        genViewModel.validationError?.let { error ->
            Text(
                text = error,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        genViewModel.saveError?.let { error ->
            Text(
                text = "Save failed: $error",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
