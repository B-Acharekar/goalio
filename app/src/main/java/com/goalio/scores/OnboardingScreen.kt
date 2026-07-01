package com.goalio.scores

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goalio.scores.ui.theme.GoalioColors
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val features: List<String>
)

private val onboardingPages = listOf(
    OnboardingPage(
        "Never Miss a Kick",
        "Experience the game as it happens with live scores, real-time stats, and instant match alerts.",
        listOf("LIVE MATCH CENTER", "DEEP STATS", "CUSTOM ALERTS")
    ),
    OnboardingPage(
        "Track Every Tournament",
        "Follow fixtures, standings, and match details from the World Cup and the biggest leagues.",
        listOf("GROUP TABLES", "FIXTURES", "MATCH DETAILS")
    ),
    OnboardingPage(
        "Build Your Dashboard",
        "Pin favorite teams and players so your home feed opens with the football you care about.",
        listOf("FAVORITES", "PLAYER HEROES", "SMART FEED")
    )
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val metrics = rememberGoalioMetrics()
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val page = pagerState.currentPage

    BackHandler(enabled = page > 0) {
        scope.launch { pagerState.animateScrollToPage(page - 1) }
    }

    Box(Modifier.fillMaxSize().background(GoalioColors.Background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            OnboardingHeader(onSkip = onComplete)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1
            ) { index ->
                OnboardingPageContent(index, onboardingPages[index])
            }
            PagerDots(selected = page, count = onboardingPages.size)
            Spacer(Modifier.height(metrics.dp(18)))
            Button(
                onClick = {
                    if (page == onboardingPages.lastIndex) onComplete()
                    else scope.launch { pagerState.animateScrollToPage(page + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(metrics.dp(62))
                    .padding(horizontal = metrics.horizontalPadding),
                shape = RoundedCornerShape(metrics.dp(20)),
                colors = ButtonDefaults.buttonColors(containerColor = GoalioColors.Accent, contentColor = Color.White)
            ) {
                Text(
                    if (page == onboardingPages.lastIndex) "Kick Off" else "Next  >",
                    fontSize = metrics.sp(19),
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(metrics.dp(20)))
            Text(
                "Premium Experience for Serious Fans",
                color = Color(0xFF8D8D8D),
                fontSize = metrics.sp(13),
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(metrics.dp(18)))
        }
    }
}

@Composable
private fun OnboardingHeader(onSkip: () -> Unit) {
    val metrics = rememberGoalioMetrics()
    Row(
        Modifier
            .fillMaxWidth()
            .height(metrics.dp(82))
            .padding(horizontal = metrics.horizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.football_ball),
            contentDescription = null,
            modifier = Modifier.size(metrics.dp(30))
        )
        Spacer(Modifier.width(metrics.dp(10)))
        Text("Goalio", color = Color.White, fontSize = metrics.sp(27), fontWeight = FontWeight.Black, letterSpacing = 3.sp)
        Spacer(Modifier.weight(1f))
        Surface(
            color = GoalioColors.Accent,
            contentColor = Color.White,
            shape = RoundedCornerShape(50),
            modifier = Modifier.clickable(onClick = onSkip)
        ) {
            Text(
                "Skip",
                fontSize = metrics.sp(15),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = metrics.dp(22), vertical = metrics.dp(10))
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(index: Int, page: OnboardingPage) {
    val metrics = rememberGoalioMetrics()
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = metrics.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when (index) {
                0 -> AlertPhoneIllustration(Modifier.fillMaxWidth(if (metrics.compact) .88f else .76f).aspectRatio(.72f))
                1 -> TournamentIllustration(Modifier.fillMaxWidth(.92f).aspectRatio(1.3f))
                else -> DashboardIllustration(Modifier.fillMaxWidth(.92f).aspectRatio(1.3f))
            }
        }
        Text(
            page.title,
            color = Color.White,
            fontSize = metrics.sp(27),
            lineHeight = metrics.sp(32),
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(metrics.dp(12)))
        Text(
            page.subtitle,
            color = Color(0xFFE0E0E0),
            fontSize = metrics.sp(16),
            lineHeight = metrics.sp(23),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(.94f)
        )
        Spacer(Modifier.height(metrics.dp(18)))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            page.features.forEach { FeaturePill(it) }
        }
        Spacer(Modifier.height(metrics.dp(18)))
    }
}

@Composable
private fun FeaturePill(text: String) {
    Surface(
        color = Color(0xED242826),
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoalioColors.Accent.copy(alpha = .42f))
    ) {
        Text(
            text,
            color = Color(0xFFF0F0F0),
            fontSize = 11.sp,
            letterSpacing = .7.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PagerDots(selected: Int, count: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = if (index == selected) 30.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (index == selected) Color(0xFF5C5F5C) else Color(0xFF353836))
            )
        }
    }
}

@Composable
private fun AlertPhoneIllustration(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "phone glow")
    val glow by transition.animateFloat(.2f, .62f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "glow")
    Canvas(modifier) {
        val phoneWidth = size.width * .68f
        val phoneHeight = size.height * .92f
        val left = (size.width - phoneWidth) / 2f
        val top = size.height * .04f
        drawCircle(GoalioColors.Accent.copy(alpha = glow * .28f), size.minDimension * .58f, Offset(size.width / 2f, size.height / 2f))
        drawRoundRect(Color(0xFF2B2F2E), Offset(left, top), Size(phoneWidth, phoneHeight), androidx.compose.ui.geometry.CornerRadius(52f, 52f))
        drawRoundRect(Color(0xFF0B0F0E), Offset(left + 14f, top + 14f), Size(phoneWidth - 28f, phoneHeight - 28f), androidx.compose.ui.geometry.CornerRadius(42f, 42f))
        drawRoundRect(Color(0xFF2B2F2E), Offset(size.width * .38f, top + 14f), Size(size.width * .24f, size.height * .07f), androidx.compose.ui.geometry.CornerRadius(18f, 18f))
        drawRoundRect(Color(0xFF292D2C), Offset(left + phoneWidth * .1f, top + phoneHeight * .13f), Size(phoneWidth * .8f, phoneHeight * .18f), androidx.compose.ui.geometry.CornerRadius(20f, 20f))
        drawCircle(Color(0xFFFF3030), size.minDimension * .035f, Offset(left + phoneWidth * .24f, top + phoneHeight * .22f))
        drawRoundRect(Color(0xFF727776), Offset(left + phoneWidth * .35f, top + phoneHeight * .205f), Size(phoneWidth * .26f, phoneHeight * .035f), androidx.compose.ui.geometry.CornerRadius(8f, 8f))
        drawCircle(Color(0xFF5B574F), size.minDimension * .055f, Offset(left + phoneWidth * .7f, top + phoneHeight * .22f))
        drawRoundRect(Color(0xFF272B2A), Offset(left + phoneWidth * .1f, top + phoneHeight * .42f), Size(phoneWidth * .72f, phoneHeight * .045f), androidx.compose.ui.geometry.CornerRadius(8f, 8f))
        drawRoundRect(Color(0xFF272B2A), Offset(left + phoneWidth * .1f, top + phoneHeight * .5f), Size(phoneWidth * .48f, phoneHeight * .045f), androidx.compose.ui.geometry.CornerRadius(8f, 8f))
    }
}

@Composable
private fun TournamentIllustration(modifier: Modifier = Modifier) = Canvas(modifier) {
    drawCircle(GoalioColors.Accent.copy(alpha = .16f), size.minDimension * .42f, center)
    repeat(4) { index ->
        val y = size.height * (.18f + index * .18f)
        drawRoundRect(GoalioColors.Surface2, Offset(size.width * .1f, y), Size(size.width * .32f, 30f), androidx.compose.ui.geometry.CornerRadius(14f, 14f))
        drawRoundRect(GoalioColors.Surface2, Offset(size.width * .58f, y), Size(size.width * .32f, 30f), androidx.compose.ui.geometry.CornerRadius(14f, 14f))
        drawLine(GoalioColors.Accent.copy(alpha = .65f), Offset(size.width * .42f, y + 15f), Offset(size.width * .5f, y + 15f), 3f, StrokeCap.Round)
        drawLine(GoalioColors.Accent.copy(alpha = .65f), Offset(size.width * .5f, y + 15f), Offset(size.width * .58f, y + 15f), 3f, StrokeCap.Round)
    }
    drawRoundRect(GoalioColors.Accent, Offset(size.width * .38f, size.height * .72f), Size(size.width * .24f, 42f), androidx.compose.ui.geometry.CornerRadius(18f, 18f))
}

@Composable
private fun DashboardIllustration(modifier: Modifier = Modifier) = Canvas(modifier) {
    drawCircle(GoalioColors.Accent.copy(alpha = .14f), size.minDimension * .44f, center)
    val card = androidx.compose.ui.geometry.CornerRadius(26f, 26f)
    drawRoundRect(GoalioColors.Surface1, Offset(size.width * .1f, size.height * .14f), Size(size.width * .8f, size.height * .66f), card)
    drawRoundRect(GoalioColors.CardBorder, Offset(size.width * .1f, size.height * .14f), Size(size.width * .8f, size.height * .66f), card, style = Stroke(2f))
    drawCircle(GoalioColors.Accent, size.minDimension * .055f, Offset(size.width * .24f, size.height * .3f))
    drawRoundRect(Color(0xFF343837), Offset(size.width * .34f, size.height * .26f), Size(size.width * .36f, 18f), androidx.compose.ui.geometry.CornerRadius(8f, 8f))
    drawRoundRect(Color(0xFF343837), Offset(size.width * .18f, size.height * .45f), Size(size.width * .64f, 18f), androidx.compose.ui.geometry.CornerRadius(8f, 8f))
    drawRoundRect(Color(0xFF343837), Offset(size.width * .18f, size.height * .58f), Size(size.width * .42f, 18f), androidx.compose.ui.geometry.CornerRadius(8f, 8f))
    val path = Path().apply {
        moveTo(size.width * .16f, size.height * .78f)
        lineTo(size.width * .26f, size.height * .67f)
        lineTo(size.width * .38f, size.height * .72f)
        lineTo(size.width * .54f, size.height * .55f)
        lineTo(size.width * .82f, size.height * .62f)
    }
    drawPath(path, GoalioColors.Accent.copy(alpha = .8f), style = Stroke(5f, cap = StrokeCap.Round))
}
