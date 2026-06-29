package com.goalio.scores

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PersonalizedHomeScreen(
    fallbackName: String?,
    fallbackTeams: Set<String>,
    fallbackPlayers: Set<String>
) {
    var home by remember { mutableStateOf<BackendHome?>(null) }
    LaunchedEffect(Unit) {
        runCatching { GoalioBackendApi.getHome() }.onSuccess { home = it }
    }
    val name = home?.profile?.name ?: fallbackName ?: "Football fan"
    val teams = home?.profile?.favoriteTeams ?: fallbackTeams.toList()
    val players = home?.profile?.favoritePlayers ?: fallbackPlayers.toList()

    GoalioBackground(.28f) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text("GOALIO", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                Spacer(Modifier.height(30.dp))
                Text(home?.greeting ?: "Welcome back, ${name.split(' ').first()}", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Text("Your football world, all in one place.", color = Color(0xFFB7B7B7), fontSize = 15.sp)
            }
            item { FavoriteSection("Your teams", teams, Color(0xFF817344)) }
            item { FavoriteSection("Pinned players", players, Color(0xFF315D70)) }
            item {
                Surface(
                    color = Color(0xE91A1D1C),
                    border = BorderStroke(1.dp, Color(0xFF403B2E)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(190.dp)
                ) {
                    Box(Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MATCH CENTER", color = Color(0xFFB8A15A), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(Modifier.height(10.dp))
                            Text("Personalized fixtures and scores will appear here.", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteSection(title: String, values: List<String>, accent: Color) {
    Column {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        if (values.isEmpty()) {
            Text("No favorites selected yet", color = Color(0xFF999999), fontSize = 14.sp)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(values) { value ->
                    Surface(color = Color(0xFF242726), border = BorderStroke(1.dp, accent), shape = RoundedCornerShape(50)) {
                        Row(Modifier.padding(horizontal = 15.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
