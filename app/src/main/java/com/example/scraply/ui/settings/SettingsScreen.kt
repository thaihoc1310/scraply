package com.example.scraply.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scraply.R
import com.example.scraply.data.preferences.AppLanguage
import com.example.scraply.data.preferences.AppThemeMode
import com.example.scraply.ui.vm.scraplyViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenRecentLikes: () -> Unit,
    onOpenRecentComments: () -> Unit,
    onOpenAbout: () -> Unit,
    onSignOut: () -> Unit,
    settingsVm: SettingsViewModel = scraplyViewModel(),
) {
    val themeMode by settingsVm.themeMode.collectAsState()
    val appLanguage by settingsVm.appLanguage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top bar ──────────────────────────────────────────────
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.calendar_back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = stringResource(R.string.profile_settings),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Scrollable content ───────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // PROFILE section
            SettingsSectionHeader(title = stringResource(R.string.settings_profile_section))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Filled.Edit,
                    label = stringResource(R.string.settings_edit_profile),
                    onClick = onEditProfile
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Filled.Favorite,
                    label = stringResource(R.string.settings_recent_likes),
                    onClick = onOpenRecentLikes
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Filled.ChatBubbleOutline,
                    label = stringResource(R.string.settings_recent_comments),
                    onClick = onOpenRecentComments
                )
            }

            Spacer(Modifier.height(20.dp))

            // APPEARANCE section
            SettingsSectionHeader(title = stringResource(R.string.settings_appearance_section))
            SettingsCard {
                SettingsSelectionRow(
                    icon = Icons.Filled.Palette,
                    label = stringResource(R.string.settings_theme_system),
                    selected = themeMode == AppThemeMode.SYSTEM,
                    onClick = { settingsVm.setThemeMode(AppThemeMode.SYSTEM) }
                )
                SettingsDivider()
                SettingsSelectionRow(
                    icon = Icons.Filled.Palette,
                    label = stringResource(R.string.settings_theme_light),
                    selected = themeMode == AppThemeMode.LIGHT,
                    onClick = { settingsVm.setThemeMode(AppThemeMode.LIGHT) }
                )
                SettingsDivider()
                SettingsSelectionRow(
                    icon = Icons.Filled.Palette,
                    label = stringResource(R.string.settings_theme_dark),
                    selected = themeMode == AppThemeMode.DARK,
                    onClick = { settingsVm.setThemeMode(AppThemeMode.DARK) }
                )
            }

            Spacer(Modifier.height(20.dp))

            // LANGUAGE section
            SettingsSectionHeader(title = stringResource(R.string.settings_language_section))
            SettingsCard {
                SettingsSelectionRow(
                    icon = Icons.Filled.Language,
                    label = stringResource(R.string.settings_language_english),
                    selected = appLanguage == AppLanguage.ENGLISH,
                    onClick = { settingsVm.setAppLanguage(AppLanguage.ENGLISH) }
                )
                SettingsDivider()
                SettingsSelectionRow(
                    icon = Icons.Filled.Language,
                    label = stringResource(R.string.settings_language_vietnamese),
                    selected = appLanguage == AppLanguage.VIETNAMESE,
                    onClick = { settingsVm.setAppLanguage(AppLanguage.VIETNAMESE) }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ABOUT section
            SettingsSectionHeader(title = stringResource(R.string.settings_about_section))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Filled.Info,
                    label = stringResource(R.string.settings_about),
                    onClick = onOpenAbout
                )
            }

            Spacer(Modifier.height(20.dp))

            // SESSION section
            SettingsSectionHeader(title = stringResource(R.string.settings_session_section))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Filled.Logout,
                    label = stringResource(R.string.settings_sign_out),
                    destructive = true,
                    onClick = {
                        onBack()
                        onSignOut()
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.about_version),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Reusable helper composables ──────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                shape = RoundedCornerShape(22.dp)
            )
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                RoundedCornerShape(22.dp)
            )
            .padding(vertical = 4.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val contentColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.8f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsSelectionRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    )
}
