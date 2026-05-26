@file:Suppress("TooManyFunctions")

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
import androidx.compose.material3.OutlinedButton
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
import pl.dekrate.kofeino.tracker.data.sync.SyncStatusTracker
import pl.dekrate.kofeino.tracker.data.local.CaffeineLimitProfile
import pl.dekrate.kofeino.tracker.data.local.DataStorePreferences
import pl.dekrate.kofeino.tracker.presentation.viewmodel.BackupUiState
import pl.dekrate.kofeino.tracker.presentation.viewmodel.SettingsEvent
import pl.dekrate.kofeino.tracker.presentation.viewmodel.SettingsUiState
import pl.dekrate.kofeino.tracker.presentation.viewmodel.SettingsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Internal sealed interface for settings UI actions — enables state hoisting
 * instead of forwarding the ViewModel through composables.
 */
private sealed interface SettingsAction {
    data class SetThemeMode(val mode: String) : SettingsAction
    data class SetLanguage(val lang: String) : SettingsAction
    data class SetCaffeineProfile(val profile: CaffeineLimitProfile) : SettingsAction
    data class SetCustomCaffeineLimit(val mg: Int) : SettingsAction
    data class SetNotifLive(val enabled: Boolean) : SettingsAction
    data class SetNotifMorning(val enabled: Boolean) : SettingsAction
    data class SetNotifRegular(val enabled: Boolean) : SettingsAction
    data class SetNotifEvening(val enabled: Boolean) : SettingsAction
    data class SetImportSettings(val enabled: Boolean) : SettingsAction
}

private fun SettingsViewModel.toSettingsActionHandler(): (SettingsAction) -> Unit = { action ->
    when (action) {
        is SettingsAction.SetThemeMode -> setThemeMode(action.mode)
        is SettingsAction.SetLanguage -> setLanguage(action.lang)
        is SettingsAction.SetCaffeineProfile -> setCaffeineProfile(action.profile)
        is SettingsAction.SetCustomCaffeineLimit -> setCustomCaffeineLimit(action.mg)
        is SettingsAction.SetNotifLive -> setNotifLiveEnabled(action.enabled)
        is SettingsAction.SetNotifMorning -> setNotifMorningEnabled(action.enabled)
        is SettingsAction.SetNotifRegular -> setNotifRegularEnabled(action.enabled)
        is SettingsAction.SetNotifEvening -> setNotifEveningEnabled(action.enabled)
        is SettingsAction.SetImportSettings -> setImportSettingsEnabled(action.enabled)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    syncStatusTracker: SyncStatusTracker,
    onNavigateBack: () -> Unit,
    onNavigateToCrossDeviceStatus: () -> Unit = {},
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

    val onSettingsAction = viewModel.toSettingsActionHandler()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.semantics { contentDescription = backDesc }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backDesc, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    SyncStatusChip(syncStatusTracker = syncStatusTracker)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        SettingsContent(
            state = state,
            versionName = versionName,
            onExportClick = {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val filename = resources.getString(R.string.backup_file_name, today)
                exportLauncher.launch(filename)
            },
            onImportClick = {
                importLauncher.launch(arrayOf("application/json"))
            },
            onSettingsAction = onSettingsAction,
            onNavigateToCrossDeviceStatus = onNavigateToCrossDeviceStatus,
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    versionName: String,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onSettingsAction: (SettingsAction) -> Unit,
    onNavigateToCrossDeviceStatus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val themeSystemDesc = stringResource(R.string.theme_system_description)
    val themeLightDesc = stringResource(R.string.theme_light_description)
    val themeDarkDesc = stringResource(R.string.theme_dark_description)

    Column(modifier = modifier) {
        // ── Theme section ──
        SectionHeader(stringResource(R.string.app_theme), Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        RadioOption(stringResource(R.string.theme_system), state.currentThemeMode == DataStorePreferences.THEME_SYSTEM,
            { onSettingsAction(SettingsAction.SetThemeMode(DataStorePreferences.THEME_SYSTEM)) },
            Modifier.semantics { contentDescription = themeSystemDesc })
        RadioOption(stringResource(R.string.theme_light), state.currentThemeMode == DataStorePreferences.THEME_LIGHT,
            { onSettingsAction(SettingsAction.SetThemeMode(DataStorePreferences.THEME_LIGHT)) },
            Modifier.semantics { contentDescription = themeLightDesc })
        RadioOption(stringResource(R.string.theme_dark), state.currentThemeMode == DataStorePreferences.THEME_DARK,
            { onSettingsAction(SettingsAction.SetThemeMode(DataStorePreferences.THEME_DARK)) },
            Modifier.semantics { contentDescription = themeDarkDesc })

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        LanguageSection(state.currentLanguage, onSettingsAction)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        CaffeineLimitSection(
            currentCaffeineProfile = state.currentCaffeineProfile,
            currentCustomLimit = state.currentCustomLimit,
            onSettingsAction = onSettingsAction
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        NotificationSection(
            notifLiveEnabled = state.notifLiveEnabled,
            notifMorningEnabled = state.notifMorningEnabled,
            notifRegularEnabled = state.notifRegularEnabled,
            notifEveningEnabled = state.notifEveningEnabled,
            onSettingsAction = onSettingsAction
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        BackupSection(
            isExporting = state.backupState is BackupUiState.Exporting,
            isImporting = state.backupState is BackupUiState.Importing,
            importSettingsEnabled = state.importSettingsEnabled,
            onExportClick = onExportClick,
            onImportClick = onImportClick,
            onSettingsAction = onSettingsAction
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        CrossDeviceStatusSection(onNavigateToCrossDeviceStatus)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        HealthDisclaimerSection()
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // ── About section ──
        SectionHeader(stringResource(R.string.about), Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        AboutRow(stringResource(R.string.app_name), stringResource(R.string.version_format, versionName),
            stringResource(R.string.version_format, versionName), Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp))
    }
}

// ── Section composables ──

@Composable
private fun LanguageSection(
    currentLanguage: String,
    onSettingsAction: (SettingsAction) -> Unit
) {
    Column {
        val langSystemDesc = stringResource(R.string.language_system_description)
        val langEnglishDesc = stringResource(R.string.language_switch_description, stringResource(R.string.english))
        val langPolishDesc = stringResource(R.string.language_switch_description, stringResource(R.string.polish))

        SectionHeader(stringResource(R.string.app_language), Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        RadioOption(stringResource(R.string.language_system), currentLanguage == DataStorePreferences.LANGUAGE_SYSTEM,
            { onSettingsAction(SettingsAction.SetLanguage(DataStorePreferences.LANGUAGE_SYSTEM)) },
            Modifier.semantics { contentDescription = langSystemDesc })
        RadioOption(stringResource(R.string.english), currentLanguage == DataStorePreferences.LANGUAGE_EN,
            { onSettingsAction(SettingsAction.SetLanguage(DataStorePreferences.LANGUAGE_EN)) },
            Modifier.semantics { contentDescription = langEnglishDesc })
        RadioOption(stringResource(R.string.polish), currentLanguage == DataStorePreferences.LANGUAGE_PL,
            { onSettingsAction(SettingsAction.SetLanguage(DataStorePreferences.LANGUAGE_PL)) },
            Modifier.semantics { contentDescription = langPolishDesc })
    }
}

@Composable
private fun CaffeineLimitSection(
    currentCaffeineProfile: CaffeineLimitProfile,
    currentCustomLimit: Int,
    onSettingsAction: (SettingsAction) -> Unit
) {
    Column {
        val decreaseDesc = stringResource(R.string.custom_limit_decrease)
        val increaseDesc = stringResource(R.string.custom_limit_increase)

        SectionHeader(
            stringResource(R.string.caffeine_limit_title),
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        Text(
            text = stringResource(R.string.caffeine_limit_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        CaffeineLimitProfile.entries.forEach { profile ->
            ProfileOption(
                profile = profile,
                isSelected = currentCaffeineProfile == profile,
                onSelect = { onSettingsAction(SettingsAction.SetCaffeineProfile(profile)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (currentCaffeineProfile == CaffeineLimitProfile.CUSTOM) {
            CustomLimitControls(
                customLimit = currentCustomLimit,
                decreaseDesc = decreaseDesc,
                increaseDesc = increaseDesc,
                onDecrease = {
                    val newValue = (currentCustomLimit - 25)
                        .coerceAtLeast(DataStorePreferences.MIN_CUSTOM_LIMIT)
                    onSettingsAction(SettingsAction.SetCustomCaffeineLimit(newValue))
                },
                onIncrease = {
                    val newValue = (currentCustomLimit + 25)
                        .coerceAtMost(DataStorePreferences.MAX_CUSTOM_LIMIT)
                    onSettingsAction(SettingsAction.SetCustomCaffeineLimit(newValue))
                }
            )
        }
    }
}

@Composable
private fun NotificationSection(
    notifLiveEnabled: Boolean,
    notifMorningEnabled: Boolean,
    notifRegularEnabled: Boolean,
    notifEveningEnabled: Boolean,
    onSettingsAction: (SettingsAction) -> Unit
) {
    Column {
        SectionHeader(stringResource(R.string.notification_section), Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        NotifToggle(stringResource(R.string.notif_live), stringResource(R.string.notif_live_desc),
            notifLiveEnabled, { onSettingsAction(SettingsAction.SetNotifLive(it)) })
        NotifToggle(stringResource(R.string.notif_morning), stringResource(R.string.notif_morning_desc),
            notifMorningEnabled, { onSettingsAction(SettingsAction.SetNotifMorning(it)) })
        NotifToggle(stringResource(R.string.notif_regular), stringResource(R.string.notif_regular_desc),
            notifRegularEnabled, { onSettingsAction(SettingsAction.SetNotifRegular(it)) })
        NotifToggle(stringResource(R.string.notif_evening), stringResource(R.string.notif_evening_desc),
            notifEveningEnabled, { onSettingsAction(SettingsAction.SetNotifEvening(it)) })
    }
}

@Composable
private fun BackupSection(
    isExporting: Boolean,
    isImporting: Boolean,
    importSettingsEnabled: Boolean,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onSettingsAction: (SettingsAction) -> Unit
) {
    val isBackupInProgress = isExporting || isImporting

    Column {
        SectionHeader(
            stringResource(R.string.backup_section),
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        BackupButton(
            label = stringResource(R.string.export_backup),
            loadingLabel = stringResource(R.string.backup_exporting),
            isLoading = isExporting,
            enabled = !isBackupInProgress,
            onClick = onExportClick
        )
        NotifToggle(
            label = stringResource(R.string.backup_import_settings),
            description = stringResource(R.string.backup_import_settings_desc),
            checked = importSettingsEnabled,
            onCheckedChange = { onSettingsAction(SettingsAction.SetImportSettings(it)) }
        )
        BackupButton(
            label = stringResource(R.string.import_backup),
            loadingLabel = stringResource(R.string.backup_importing),
            isLoading = isImporting,
            enabled = !isBackupInProgress,
            onClick = onImportClick
        )
    }
}

@Composable
private fun CrossDeviceStatusSection(
    onNavigateToCrossDeviceStatus: () -> Unit
) {
    Column {
        SectionHeader(
            stringResource(R.string.cross_device_status_title),
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        Button(
            onClick = onNavigateToCrossDeviceStatus,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.view_cross_device_status),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
internal fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = modifier)
}

@Composable
private fun RadioOption(label: String, isSelected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.clickable { onSelect() }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = isSelected, onClick = onSelect)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ProfileOption(
    profile: CaffeineLimitProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable { onSelect() }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onSelect)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(profile.displayNameResId),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(profile.descriptionResId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CustomLimitControls(
    customLimit: Int,
    decreaseDesc: String,
    increaseDesc: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.custom_limit_value, customLimit),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onDecrease,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
                    .semantics { contentDescription = decreaseDesc }
            ) {
                Text("-25 mg")
            }
            OutlinedButton(
                onClick = onIncrease,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .semantics { contentDescription = increaseDesc }
            ) {
                Text("+25 mg")
            }
        }
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
