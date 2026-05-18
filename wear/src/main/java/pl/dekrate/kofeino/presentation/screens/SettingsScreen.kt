package pl.dekrate.kofeino.presentation.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import pl.dekrate.kofeino.KofeinoTrackerApplication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.data.local.LanguagePreferences

@Composable
fun SettingsScreen(
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context.findActivity()
    val prefs = remember { LanguagePreferences(context) }
    var currentLang by remember { mutableStateOf(prefs.getLanguage()) }

    val listScrollState = rememberTransformingLazyColumnState()
    val languageDesc = stringResource(R.string.language)

    ScreenScaffold(scrollState = listScrollState) { contentPadding ->
        TransformingLazyColumn(
            state = listScrollState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top)
        ) {

            // Header
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Language toggle — two buttons: filled = active, outlined = inactive
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.language),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isEnglish = currentLang == LanguagePreferences.LANGUAGE_EN
                        val isPolish = currentLang == LanguagePreferences.LANGUAGE_PL

                        // English button
                        if (isEnglish) {
                            Button(
                                onClick = { /* already active */ },
                                enabled = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = languageDesc }
                            ) {
                                Text(stringResource(R.string.english))
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    prefs.setLanguage(LanguagePreferences.LANGUAGE_EN)
                                    currentLang = LanguagePreferences.LANGUAGE_EN
                                    (context.applicationContext as? KofeinoTrackerApplication)?.refreshLocale()
                                    activity?.recreate()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = languageDesc }
                            ) {
                                Text(stringResource(R.string.english))
                            }
                        }

                        // Polish button
                        if (isPolish) {
                            Button(
                                onClick = { /* already active */ },
                                enabled = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = languageDesc }
                            ) {
                                Text(stringResource(R.string.polish))
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    prefs.setLanguage(LanguagePreferences.LANGUAGE_PL)
                                    currentLang = LanguagePreferences.LANGUAGE_PL
                                    (context.applicationContext as? KofeinoTrackerApplication)?.refreshLocale()
                                    activity?.recreate()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = languageDesc }
                            ) {
                                Text(stringResource(R.string.polish))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Recursively finds the Activity from a Context. */
internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
