package pl.dekrate.kofeino.tracker.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pl.dekrate.kofeino.tracker.R
import pl.dekrate.kofeino.tracker.data.local.DataStorePreferences
import pl.dekrate.kofeino.tracker.presentation.viewmodel.BackupUiState
import pl.dekrate.kofeino.tracker.presentation.viewmodel.SettingsEvent
import pl.dekrate.kofeino.tracker.presentation.viewmodel.SettingsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = context.findActivity()
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // SAF launcher: create a new backup file
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri)
        }
    }

    // SAF launcher: open an existing backup file
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(uri)
        }
    }

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
    }

    // Collect one-shot events (snackbar messages) — messages are pre-resolved in ViewModel
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is SettingsEvent.ShowSnackbar) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(state.currentLanguage) {
        if (state.languageChanged) {
            activity?.recreate()
            viewModel.consumeLanguageChanged()
        }
    }

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

    val isBackupInProgress = state.backupState is BackupUiState.Exporting ||
        state.backupState is BackupUiState.Importing

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.semantics { contentDescription = backDesc }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backDesc, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())
        ) {
            // ── Theme section ──
            SectionHeader(stringResource(R.string.app_theme), Modifier.padding(horizontal = 16.dp, vertical = 12.dp))

            ThemeOption(stringResource(R.string.theme_system), state.currentThemeMode == DataStorePreferences.THEME_SYSTEM,
                { viewModel.setThemeMode(DataStorePreferences.THEME_SYSTEM) }, Modifier.semantics { contentDescription = systemThemeDesc })
            ThemeOption(stringResource(R.string.theme_light), state.currentThemeMode == DataStorePreferences.THEME_LIGHT,
                { viewModel.setThemeMode(DataStorePreferences.THEME_LIGHT) }, Modifier.semantics { contentDescription = lightThemeDesc })
            ThemeOption(stringResource(R.string.theme_dark), state.currentThemeMode == DataStorePreferences.THEME_DARK,
                { viewModel.setThemeMode(DataStorePreferences.THEME_DARK) }, Modifier.semantics { contentDescription = darkThemeDesc })

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ── Language section ──
            SectionHeader(stringResource(R.string.app_language), Modifier.padding(horizontal = 16.dp, vertical = 12.dp))

            LanguageOption(stringResource(R.string.language_system), state.currentLanguage == DataStorePreferences.LANGUAGE_SYSTEM,
                { viewModel.setLanguage(DataStorePreferences.LANGUAGE_SYSTEM) }, Modifier.semantics { contentDescription = systemLangDesc })
            LanguageOption(stringResource(R.string.english), state.currentLanguage == DataStorePreferences.LANGUAGE_EN,
                { viewModel.setLanguage(DataStorePreferences.LANGUAGE_EN) }, Modifier.semantics { contentDescription = englishDesc })
            LanguageOption(stringResource(R.string.polish), state.currentLanguage == DataStorePreferences.LANGUAGE_PL,
                { viewModel.setLanguage(DataStorePreferences.LANGUAGE_PL) }, Modifier.semantics { contentDescription = polishDesc })

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ── Notification section ──
            SectionHeader(stringResource(R.string.notification_section), Modifier.padding(horizontal = 16.dp, vertical = 12.dp))

            NotifToggle(stringResource(R.string.notif_live), stringResource(R.string.notif_live_desc),
                state.notifLiveEnabled, { viewModel.setNotifLiveEnabled(it) })
            NotifToggle(stringResource(R.string.notif_morning), stringResource(R.string.notif_morning_desc),
                state.notifMorningEnabled, { viewModel.setNotifMorningEnabled(it) })
            NotifToggle(stringResource(R.string.notif_regular), stringResource(R.string.notif_regular_desc),
                state.notifRegularEnabled, { viewModel.setNotifRegularEnabled(it) })
            NotifToggle(stringResource(R.string.notif_evening), stringResource(R.string.notif_evening_desc),
                state.notifEveningEnabled, { viewModel.setNotifEveningEnabled(it) })

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ── Backup / Restore section ──
            SectionHeader(
                stringResource(R.string.backup_section),
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Export button
            BackupButton(
                label = stringResource(R.string.export_backup),
                loadingLabel = stringResource(R.string.backup_exporting),
                isLoading = state.backupState is BackupUiState.Exporting,
                enabled = !isBackupInProgress,
                onClick = {
                    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val filename = resources.getString(R.string.backup_file_name, today)
                    exportLauncher.launch(filename)
                }
            )

            // Import settings toggle
            NotifToggle(
                label = stringResource(R.string.backup_import_settings),
                description = stringResource(R.string.backup_import_settings_desc),
                checked = state.importSettingsEnabled,
                onCheckedChange = { viewModel.setImportSettingsEnabled(it) }
            )

            // Import button
            BackupButton(
                label = stringResource(R.string.import_backup),
                loadingLabel = stringResource(R.string.backup_importing),
                isLoading = state.backupState is BackupUiState.Importing,
                enabled = !isBackupInProgress,
                onClick = {
                    importLauncher.launch(arrayOf("application/json"))
                }
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ── Health disclaimer section ──
            HealthDisclaimerSection()

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ── About section ──
            SectionHeader(stringResource(R.string.about), Modifier.padding(horizontal = 16.dp, vertical = 12.dp))

            AboutRow(stringResource(R.string.app_name), stringResource(R.string.version_format, versionName),
                stringResource(R.string.version_format, versionName), Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp))
        }
    }
}

@Composable
internal fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = modifier)
}

@Composable
private fun LanguageOption(label: String, isSelected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().clickable { onSelect() }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = isSelected, onClick = onSelect)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ThemeOption(label: String, isSelected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().clickable { onSelect() }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = isSelected, onClick = onSelect)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun NotifToggle(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BackupButton(
    label: String,
    loadingLabel: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (isLoading) loadingLabel else label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String, contentDesc: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.semantics { contentDescription = contentDesc }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
