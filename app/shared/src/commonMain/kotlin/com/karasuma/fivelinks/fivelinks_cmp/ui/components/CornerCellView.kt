package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BoardCellBg
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.CornerGold

@Composable
fun CornerCellView(
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(BoardCellBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "★",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = CornerGold
        )
    }
}
