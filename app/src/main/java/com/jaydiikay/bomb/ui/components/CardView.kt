package com.jaydiikay.bomb.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaydiikay.bomb.game.Card as GameCard
import com.jaydiikay.bomb.ui.theme.CardBlack
import com.jaydiikay.bomb.ui.theme.CardRed
import com.jaydiikay.bomb.ui.theme.CardWhite
import com.jaydiikay.bomb.ui.theme.BombOrange

@Composable
fun CardView(
    card: GameCard,
    onClick: () -> Unit = {},
    selected: Boolean = false,
    enabled: Boolean = true,
    faceDown: Boolean = false
) {
    val shape = RoundedCornerShape(8.dp)
    val borderColor = when {
        selected -> BombOrange
        else -> Color.Gray
    }
    val borderWidth = if (selected) 3.dp else 1.dp

    Card(
        modifier = Modifier
            .width(60.dp)
            .height(90.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .clickable(enabled = enabled) { onClick() },
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 8.dp else 4.dp),
        border = BorderStroke(borderWidth, borderColor),
        colors = CardDefaults.cardColors(containerColor = if (faceDown) Color(0xFF1565C0) else CardWhite)
    ) {
        if (faceDown) {
            // Card back pattern
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .background(Color(0xFF0D47A1), shape = RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = "🂠",
                        fontSize = 30.sp,
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Card face
            val textColor = if (card.isRed) CardRed else CardBlack
            val bombColor = BombOrange

            Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                // Top-left rank + suit
                Column(
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = card.rank.display,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (card.isBomb) bombColor else textColor,
                        lineHeight = 14.sp
                    )
                    Text(
                        text = card.suitSymbol,
                        fontSize = 12.sp,
                        color = if (card.isBomb) bombColor else textColor,
                        lineHeight = 12.sp
                    )
                }

                // Center symbol
                Text(
                    text = if (card.isBomb) "💣" else card.suitSymbol,
                    fontSize = if (card.isBomb) 20.sp else 24.sp,
                    color = if (card.isBomb) bombColor else textColor,
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center
                )

                // Bottom-right rank + suit (upside down effect via rotation not needed, just mirror)
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = card.suitSymbol,
                        fontSize = 12.sp,
                        color = if (card.isBomb) bombColor else textColor,
                        lineHeight = 12.sp
                    )
                    Text(
                        text = card.rank.display,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (card.isBomb) bombColor else textColor,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
