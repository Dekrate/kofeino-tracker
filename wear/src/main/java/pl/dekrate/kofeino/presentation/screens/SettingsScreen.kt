package pl.dekrate.kofeino.presentation.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.data.local.CaffeinePreferences
import pl.dekrate.kofeino.data.local.LanguagePreferences
import pl.dekrate.kofeino.domain.model.CaffeineLimitProfile

@Composable
fun SettingsScreen(
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context.findActivity()
    val langPrefs = remember { LanguagePreferences(context) }
    val caffeinePrefs = remember { CaffeinePreferences(context) }
    var currentLang by remember { mutableStateOf(langPrefs.getLanguage()) }
    var currentProfile by remember { mutableStateOf(caffeinePrefs.getProfile()) }
    var customLimit by remember { mutableIntStateOf(caffeinePrefs.getCustomLimit()) }

    val listScrollState = rememberTransformingLazyColumnState()
    val languageDesc = stringResource(R.string.language)
    val profileDesc = stringResource(R.string.caffeine_limit_title)

    ScreenScaffold(scrollState = listScrollState) { contentPadding ->
        TransformingLazyColumn(
            state = listScrollState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top)
        ) {

            // ===== Language section =====
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

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
                                    langPrefs.setLanguage(LanguagePreferences.LANGUAGE_EN)
                                    currentLang = LanguagePreferences.LANGUAGE_EN
                                    activity?.recreate()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = languageDesc }
                            ) {
                                Text(stringResource(R.string.english))
                            }
                        }

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
                                    langPrefs.setLanguage(LanguagePreferences.LANGUAGE_PL)
                                    currentLang = LanguagePreferences.LANGUAGE_PL
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

            // ===== Caffeine limit section =====
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.caffeine_limit_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.caffeine_limit_summary),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            // Profile buttons
            CaffeineLimitProfile.entries.forEach { profile ->
                item {
                    val isActive = currentProfile == profile
                    val buttonModifier = Modifier
                        .padding(horizontal = 8.dp)
                        .semantics { contentDescription = profileDesc }

                    if (isActive) {
                        Button(
                            onClick = { /* already active */ },
                            enabled = false,
                            modifier = buttonModifier
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(profile.displayNameResId),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = stringResource(profile.descriptionResId),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                caffeinePrefs.setProfile(profile)
                                currentProfile = profile
                            },
                            modifier = buttonModifier
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(profile.displayNameResId),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = stringResource(profile.descriptionResId),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Custom limit controls (only visible when CUSTOM is selected)
            if (currentProfile == CaffeineLimitProfile.CUSTOM) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.custom_limit),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = stringResource(R.string.custom_limit_value, customLimit),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val newValue = (customLimit - 25).coerceAtLeast(CaffeinePreferences.MIN_CUSTOM_LIMIT)
                                    customLimit = newValue
                                    caffeinePrefs.setCustomLimit(newValue)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = stringResource(R.string.custom_limit_decrease) }
                            ) {
                                Text("-25")
                            }
                            OutlinedButton(
                                onClick = {
                                    val newValue = (customLimit + 25).coerceAtMost(CaffeinePreferences.MAX_CUSTOM_LIMIT)
                                    customLimit = newValue
                                    caffeinePrefs.setCustomLimit(newValue)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = stringResource(R.string.custom_limit_increase) }
                            ) {
                                Text("+25")
                            }
                        }
                    }
                }
            }

            // ===== Health disclaimer section =====
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.health_disclaimer_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.health_disclaimer_text),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Source references
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.health_references_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.health_references_text),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
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
