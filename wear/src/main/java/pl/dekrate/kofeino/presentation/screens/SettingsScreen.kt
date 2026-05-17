package pl.dekrate.kofeino.presentation.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import pl.dekrate.kofeino.KofeinoTrackerApplication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
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

            // Language toggle
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.language),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = when (currentLang) {
                            "pl" -> stringResource(R.string.polish)
                            else -> stringResource(R.string.english)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            val newLang = if (currentLang == LanguagePreferences.LANGUAGE_EN) {
                                LanguagePreferences.LANGUAGE_PL
                            } else {
                                LanguagePreferences.LANGUAGE_EN
                            }
                            prefs.setLanguage(newLang)
                            currentLang = newLang
                            (context.applicationContext as? KofeinoTrackerApplication)?.refreshLocale()
                            activity?.recreate()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.semantics { contentDescription = languageDesc }
                    ) {
                        Text(
                            text = when (currentLang) {
                                "en" -> stringResource(R.string.polish)
                                else -> stringResource(R.string.english)
                            }
                        )
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
