package com.goalio.scores

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class LanguageOption(
    val tag: String, val name: String, val subtitle: String, val badge: String
)

private val languages = listOf(
    LanguageOption("de", "Deutsch", "German", "DEU"),
    LanguageOption("en-US", "English (US)", "Default Content Language", "🇺🇸"),
    LanguageOption("es", "Español", "Spanish", "🇪🇸"),
    LanguageOption("fr", "Français", "French", "FRA"),
    LanguageOption("it", "Italiano", "Italian", "ITA"),
    LanguageOption("pt", "Português", "Portuguese", "🇵🇹")
)

@Composable
fun LanguageScreen(onBack: () -> Unit, onDone: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf("en-US") }
    val filtered = remember(query) {
        languages.filter { it.name.contains(query, true) || it.subtitle.contains(query, true) }
    }
    BackHandler(onBack = onBack)
    GoalioBackground(.25f) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("‹", color = Color.White, fontSize = 51.sp,
                    modifier = Modifier.clickable(onClick = onBack).padding(end = 22.dp))
                Text("GOALIO", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 5.sp, modifier = Modifier.weight(1f))
                Button(
                    onClick = { onDone(selected) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 25.dp, vertical = 10.dp)
                ) { Text("DONE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
            }
            HorizontalDivider(color = Color(0xFF29261F))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Spacer(Modifier.height(20.dp))
                    Text("Select Language", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text("Personalize your experience with your preferred language.",
                        color = Color(0xFFAAA298), fontSize = 16.sp, lineHeight = 22.sp)
                    Spacer(Modifier.height(28.dp))
                    SearchBox(query) { query = it }
                    Spacer(Modifier.height(17.dp))
                    Text("DEVICE SETTING", color = Color(0xFFDDDDDD), fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.padding(start = 4.dp))
                    Spacer(Modifier.height(9.dp))
                    LanguageCard(
                        LanguageOption("system", "System Default", "English (UK)", "⚙"),
                        selected == "system"
                    ) { selected = "system" }
                    Spacer(Modifier.height(19.dp))
                }
                items(filtered, key = { it.tag }) { language ->
                    LanguageCard(language, selected == language.tag) { selected = language.tag }
                }
                item { Spacer(Modifier.navigationBarsPadding().height(12.dp)) }
            }
        }
    }
}

@Composable
private fun SearchBox(value: String, onValueChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(14.dp))
            .then(Modifier).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color.Transparent, shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF7B8494)),
            modifier = Modifier.fillMaxSize()
        ) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⌕", color = Color.White, fontSize = 32.sp)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text("Search language...", color = Color(0xFFD0CDD0), fontSize = 18.sp)
                    BasicTextField(value, onValueChange, singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 18.sp), modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun LanguageCard(language: LanguageOption, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0xE8171919),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp,
            if (selected) Color.White else Color(0xFF303434)),
        modifier = Modifier.fillMaxWidth().height(86.dp)
    ) {
        Row(Modifier.padding(horizontal = 17.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape, color = Color.Black,
                border = BorderStroke(1.dp, Color(0xFF282828)), modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(language.badge, color = Color.White, fontSize = if (language.badge.length > 3) 20.sp else 13.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(language.name, color = Color(0xFFF0EEF1), fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(language.subtitle, color = Color(0xFFCFCCCF), fontSize = 15.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (selected) {
                Box(Modifier.size(25.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                    Text("●", color = Color(0xFFC6A93B), fontSize = 23.sp)
                }
            }
        }
    }
}
