package pl.dekrate.kofeino.tracker.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pl.dekrate.kofeino.tracker.R
import pl.dekrate.kofeino.tracker.data.local.DataStorePreferences
import pl.dekrate.kofeino.tracker.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context.findActivity()
    val state by viewModel.uiState.collectAsState()

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
    }

    // Handle language change — triggers activity recreation.
    // The Activity's attachBaseContext() picks up the new locale from preferences,
    // no Application-level resource mutation needed.
    LaunchedEffect(state.currentLanguage) {
        if (state.languageChanged) {
            activity?.recreate()
            viewModel.consumeLanguageChanged()
        }
    }

    // Handle theme change — triggers activity recreation
    LaunchedEffect(state.currentThemeMode) {
        if (state.themeChanged) {
            activity?.recreate()
            viewModel.consumeThemeChanged()
        }
    }

    val backDesc = stringResource(R.string.back)
    val englishDesc = stringResource(R.string.language_switch_description, stringResource(R.string.english))
    val polishDesc = stringResource(R.string.language_switch_description, stringResource(R.string.polish))
    val systemLangDesc = stringResource(R.string.language_system_description)
    val systemThemeDesc = stringResource(R.string.theme_system_description)
    val lightThemeDesc = stringResource(R.string.theme_light_description)
    val darkThemeDesc = stringResource(R.string.theme_dark_description)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = backDesc }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backDesc,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Theme section ---
            SectionHeader(
                text = stringResource(R.string.app_theme),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            ThemeOption(
                label = stringResource(R.string.theme_system),
                isSelected = state.currentThemeMode == DataStorePreferences.THEME_SYSTEM,
                onSelect = { viewModel.setThemeMode(DataStorePreferences.THEME_SYSTEM) },
                modifier = Modifier.semantics { contentDescription = systemThemeDesc }
            )

            ThemeOption(
                label = stringResource(R.string.theme_light),
                isSelected = state.currentThemeMode == DataStorePreferences.THEME_LIGHT,
                onSelect = { viewModel.setThemeMode(DataStorePreferences.THEME_LIGHT) },
                modifier = Modifier.semantics { contentDescription = lightThemeDesc }
            )

            ThemeOption(
                label = stringResource(R.string.theme_dark),
                isSelected = state.currentThemeMode == DataStorePreferences.THEME_DARK,
                onSelect = { viewModel.setThemeMode(DataStorePreferences.THEME_DARK) },
                modifier = Modifier.semantics { contentDescription = darkThemeDesc }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Language section ---
            SectionHeader(
                text = stringResource(R.string.app_language),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            LanguageOption(
                label = stringResource(R.string.language_system),
                isSelected = state.currentLanguage == DataStorePreferences.LANGUAGE_SYSTEM,
                onSelect = { viewModel.setLanguage(DataStorePreferences.LANGUAGE_SYSTEM) },
                modifier = Modifier.semantics { contentDescription = systemLangDesc }
            )

            LanguageOption(
                label = stringResource(R.string.english),
                isSelected = state.currentLanguage == DataStorePreferences.LANGUAGE_EN,
                onSelect = { viewModel.setLanguage(DataStorePreferences.LANGUAGE_EN) },
                modifier = Modifier.semantics { contentDescription = englishDesc }
            )

            LanguageOption(
                label = stringResource(R.string.polish),
                isSelected = state.currentLanguage == DataStorePreferences.LANGUAGE_PL,
                onSelect = { viewModel.setLanguage(DataStorePreferences.LANGUAGE_PL) },
                modifier = Modifier.semantics { contentDescription = polishDesc }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // --- About section ---
            SectionHeader(
                text = stringResource(R.string.about),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            AboutRow(
                label = stringResource(R.string.app_name),
                value = stringResource(R.string.version_format, versionName),
                contentDesc = stringResource(R.string.version_format, versionName),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
private fun LanguageOption(
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ThemeOption(
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AboutRow(
    label: String,
    value: String,
    contentDesc: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.semantics { contentDescription = contentDesc },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Recursively finds the Activity from a Context. */
internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
