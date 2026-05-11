package com.jaydiikay.bomb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaydiikay.bomb.game.Card

@Composable
fun DrawDiscardPile(
    topCard: Card,
    drawPileCount: Int,
    onDrawClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Draw pile (face down)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                CardView(
                    card = topCard, // dummy card for face-down visual
                    faceDown = true,
                    enabled = true,
                    onClick = onDrawClick
                )
                // Count badge
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color(0xFFE53935), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (drawPileCount > 99) "99+" else drawPileCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "Draw",
                color = Color.White,
                fontSize = 12.sp
            )
        }

        // Discard pile / top card (face up)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CardView(
                card = topCard,
                faceDown = false,
                enabled = false
            )
            Text(
                text = "Discard",
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}
