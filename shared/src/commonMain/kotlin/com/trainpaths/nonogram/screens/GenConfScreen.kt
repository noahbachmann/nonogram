package com.trainpaths.nonogram.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.auth.AuthState
import com.trainpaths.nonogram.navigation.NonogramAppBar
import com.trainpaths.nonogram.screens.viewModel.GenViewModel

@Composable
fun GenConfScreen(
    genViewModel: GenViewModel,
    editing: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    var rows by remember { mutableStateOf(genViewModel.height.toString()) }
    var cols by remember { mutableStateOf(genViewModel.width.toString()) }
    var isPublic by remember {
        mutableStateOf(genViewModel.nonogram.valid == 1L && genViewModel.nonogram.status == 1L)
    }
    val authState by genViewModel.authState.collectAsState()

    LaunchedEffect(
        genViewModel.isSaving,
        genViewModel.nonogram.valid,
        genViewModel.nonogram.status,
    ) {
        if (!genViewModel.isSaving) {
            isPublic =
                genViewModel.nonogram.valid == 1L && genViewModel.nonogram.status == 1L
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NonogramAppBar(
            onBack = onBack,
            backArrow = true,
            showSettings = true,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onPrimary,
                cursorColor = MaterialTheme.colorScheme.secondary,
            )

            OutlinedTextField(
                value = rows,
                onValueChange = { rows = it.filter { c -> c.isDigit() } },
                label = { Text("Rows") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
                modifier = Modifier.width(200.dp),
            )

            OutlinedTextField(
                value = cols,
                onValueChange = { cols = it.filter { c -> c.isDigit() } },
                label = { Text("Columns") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
                modifier = Modifier.width(200.dp).padding(top = 16.dp),
            )

            if (editing) {
                val isValid = genViewModel.nonogram.valid == 1L
                val canMakePublic =
                    !genViewModel.isSaving &&
                        genViewModel.saveError == null &&
                        isValid &&
                        authState == AuthState.SIGNED_IN

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Validity", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    if (genViewModel.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(20.dp).height(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            "Checking…",
                            modifier = Modifier.padding(start = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            when {
                                genViewModel.saveError != null -> "Unavailable"
                                isValid -> "Valid"
                                else -> "Invalid"
                            },
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Visibility", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (isPublic) "Public" else "Private",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        enabled = !genViewModel.isSaving && (isPublic || canMakePublic),
                    )
                }

                if (!genViewModel.isSaving && !isPublic && !canMakePublic) {
                    Text(
                        text = if (authState != AuthState.SIGNED_IN) {
                            "Sign in to make this nonogram public."
                        } else {
                            "Only valid nonograms can be made public."
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }

                genViewModel.saveError?.let { error ->
                    Text(
                        text = "Validity could not be checked: $error",
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Button(
                onClick = {
                    val h = rows.toIntOrNull() ?: return@Button
                    val w = cols.toIntOrNull() ?: return@Button
                    if (editing) {
                        genViewModel.resizeNonogram(h, w)
                        genViewModel.setPublic(isPublic)
                        genViewModel.onSave(onDone)
                    } else {
                        genViewModel.setNonogram(h, w)
                        onDone()
                    }
                },
                enabled = !genViewModel.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(if (editing) "Save" else "Generate", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
