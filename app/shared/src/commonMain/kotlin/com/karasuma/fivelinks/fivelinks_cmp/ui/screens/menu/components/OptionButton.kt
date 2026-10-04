package com.karasuma.fivelinks.fivelinks_cmp.ui.screens.menu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.SurfaceElevated
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamBlue
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TextSecondary

@Composable
fun OptionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isSelected) TeamBlue else SurfaceElevated)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) GoldAccent else Color.Transparent,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}