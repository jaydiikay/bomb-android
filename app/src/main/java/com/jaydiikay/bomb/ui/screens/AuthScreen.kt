package com.jaydiikay.bomb.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jaydiikay.bomb.data.AppDatabase
import com.jaydiikay.bomb.data.GameResult
import com.jaydiikay.bomb.ui.theme.GreenTable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("bomb_prefs", Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    var gameHistory by remember { mutableStateOf<List<GameResult>>(emptyList()) }

    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.gameResultDao() }

    LaunchedEffect(Unit) {
        dao.getAllResults().collectLatest { results ->
            gameHistory = results
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GreenTable),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "💣 Bomb Card Game",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Login / Register card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEEFFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isRegistering) "Create Account" else "Sign In",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Enter username and password", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (isRegistering) {
                                // Register: save credentials
                                val existing = prefs.getString("user_${username}", null)
                                if (existing != null) {
                                    Toast.makeText(context, "Username already exists", Toast.LENGTH_SHORT).show()
                                } else {
                                    prefs.edit().putString("user_${username}", password).apply()
                                    prefs.edit().putString("current_user", username).apply()
                                    Toast.makeText(context, "Registered! Welcome, $username", Toast.LENGTH_SHORT).show()
                                    navController.navigate("setup")
                                }
                            } else {
                                // Login: check credentials
                                val stored = prefs.getString("user_${username}", null)
                                if (stored == password) {
                                    prefs.edit().putString("current_user", username).apply()
                                    navController.navigate("setup")
                                } else {
                                    Toast.makeText(context, "Invalid credentials", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenTable)
                    ) {
                        Text(if (isRegistering) "Register" else "Login")
                    }

                    TextButton(
                        onClick = { isRegistering = !isRegistering },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            if (isRegistering) "Already have an account? Sign In"
                            else "No account? Register"
                        )
                    }

                    // Guest button
                    OutlinedButton(
                        onClick = {
                            prefs.edit().putString("current_user", "Guest").apply()
                            navController.navigate("setup")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Play as Guest")
                    }
                }
            }

            // Game history
            if (gameHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recent Games",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC000000))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(gameHistory.take(5)) { result ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Winner: ${result.winner}",
                                        color = Color(0xFF66BB6A),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Loser: ${result.loser}",
                                        color = Color(0xFFEF5350),
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = result.endReason.uppercase(),
                                    color = if (result.endReason == "bomb") Color(0xFFFF6D00) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
