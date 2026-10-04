package com.karasuma.fivelinks.fivelinks_cmp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import fivelinks_cmp.app.shared.generated.resources.Res
import fivelinks_cmp.app.shared.generated.resources.fivelink_app_icon
import org.jetbrains.compose.resources.painterResource

/** FiveLink app icon (red cards on charcoal) for hosts that show one, e.g. desktop windows. */
@Composable
fun fiveLinkAppIcon(): Painter = painterResource(Res.drawable.fivelink_app_icon)
