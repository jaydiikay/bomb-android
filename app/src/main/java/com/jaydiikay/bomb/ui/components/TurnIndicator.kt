package com.jaydiikay.bomb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaydiikay.bomb.ui.theme.BombOrange

@Composable
fun TurnIndicator(
    playerName: String,
    direction: Int,           // 1 = anti-clockwise, -1 = clockwise
    pendingDraw: Int,
    modifier: Modifier = Modifier
) {
    // direction 1 = anti-clockwise = ↺, -1 = clockwise = ↻
    val directionArrow = if (direction == 1) "↺" else "↻"
    val directionLabel = if (direction == 1) "Anti-clockwise" else "Clockwise"

    Column(
        modifier = modifier
            .background(Color(0x88000000), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = directionArrow,
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = playerName,
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = directionLabel,
            fontSize = 11.sp,
            color = Color(0xFFBBBBBB)
        )
        if (pendingDraw > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Draw $pendingDraw cards!",
                fontSize = 13.sp,
                color = BombOrange,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
