package com.zimapp.zim.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Work
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zimapp.zim.R
import com.zimapp.zim.presentation.components.material.MaterialScaffold
import com.zimapp.zim.presentation.components.material.MaterialBar
import com.zimapp.zim.presentation.navigation.NavRoutes
import com.zimapp.zim.presentation.screens.settings.model.SettingsViewModel
import com.zimapp.zim.presentation.screens.settings.widgets.SectionBlock
import com.zimapp.zim.presentation.screens.settings.widgets.SettingSection

@Composable
fun SettingsScaffold(
    settingsViewModel: SettingsViewModel,
    title: String,
    onBackNavClicked: () -> Unit,
    content: @Composable () -> Unit
) {
    MaterialScaffold(
        topBar = {
            key(settingsViewModel.settings.value) {
                MaterialBar(
                    title = title,
                    onBackNavClicked = onBackNavClicked
                )
            }
        },
        content = {
            Box(Modifier.padding(16.dp, 8.dp, 16.dp)) {
                content()
            }
        }
    )
}

@Composable
fun MainSettings(settingsViewModel: SettingsViewModel, navController: NavController) {
    SettingsScaffold(
        settingsViewModel = settingsViewModel,
        title = stringResource(id = R.string.screen_settings),
        onBackNavClicked = { navController.navigateUp() }
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item {
                SectionBlock(
                    listOf(
                        SettingSection(
                            title = stringResource(id = R.string.color_styles),
                            features = listOf(
                                stringResource(R.string.description_color_styles)
                            ),
                            icon = Icons.Rounded.Palette,
                            onClick = { navController.navigate(NavRoutes.ColorStyles.route) }
                        ),
                        SettingSection(
                            title = stringResource(id = R.string.Behavior),
                            features = listOf(
                                stringResource(id = R.string.description_markdown)
                            ),
                            icon = Icons.Rounded.TextFields,
                            onClick = { navController.navigate(NavRoutes.Markdown.route) }
                        ),
                        SettingSection(
                            title = stringResource(id = R.string.language),
                            features = listOf(
                                stringResource(R.string.description_language)
                            ),
                            icon = Icons.Rounded.Language,
                            onClick = { navController.navigate(NavRoutes.Language.route) }
                        )
                    )
                )
            }
            item {
                SectionBlock(
                    listOf(
                        SettingSection(
                            title = stringResource(id = R.string.backup),
                            features = listOf(
                                stringResource(R.string.description_cloud)
                            ),
                            icon = Icons.Rounded.Cloud,
                            onClick = { navController.navigate(NavRoutes.Cloud.route) }
                        ),
                        SettingSection(
                            title = stringResource(id = R.string.privacy),
                            features = listOf(
                                stringResource(id = R.string.screen_protection)
                            ),
                            icon = ImageVector.vectorResource(id = R.drawable.incognito_fill),
                            onClick = { navController.navigate(NavRoutes.Privacy.route) }
                        ),
                        SettingSection(
                            title = stringResource(id = R.string.tools),
                            features = listOf(
                                stringResource(R.string.description_tools)
                            ),
                            icon = Icons.Rounded.Work,
                            onClick = { navController.navigate(NavRoutes.Tools.route) }
                        )
                    )
                )
            }
            item {
                SectionBlock(
                    listOf(
                        SettingSection(
                            title = stringResource(id = R.string.about),
                            features = listOf(
                                stringResource(R.string.description_about)
                            ),
                            icon = Icons.Rounded.Info,
                            onClick = { navController.navigate(NavRoutes.About.route) }
                        ),
                        SettingSection(
                            title = stringResource(id = R.string.app_updates),
                            features = listOf(
                                stringResource(R.string.description_updates)
                            ),
                            icon = Icons.Rounded.SystemUpdate,
                            onClick = { navController.navigate(NavRoutes.Updates.route) }
                        )
                    )
                )
            }
        }
    }
}
