package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.karasuma.fivelinks.fivelinks_cmp.audio.LocalVibrationEnabled
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Hand
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.isJack
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.primaryColor
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HandView(
    hand: Hand,
    owner: Player,
    selectedIndices: Set<Int>,
    gameState: GameState,
    isInteractive: Boolean,
    onCardClick: (Int) -> Unit,
    onSwapDeadCard: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabledIndices: Set<Int>? = null,
    guidedIndices: Set<Int>? = null,
    guideLabel: String? = null,
    onReorderHand: ((fromIndex: Int, toIndex: Int) -> Unit)? = null,
    onCardDragHover: ((cardIndex: Int, globalOffset: Offset) -> Unit)? = null,
    onCardDrop: ((cardIndex: Int, globalOffset: Offset) -> Unit)? = null,
) {
    fun isCardDead(card: Card): Boolean {
        if (card.isJack()) return false
        val positions = gameState.board.positionsOf(card)
        if (positions.isEmpty()) return false
        return positions.all { gameState.chips.contains(it) } && !gameState.deck.isEmpty()
    }

    val singleSelectedIndex = if (selectedIndices.size == 1) selectedIndices.first() else null
    val deadCardIndex = singleSelectedIndex?.takeIf { isInteractive && isCardDead(hand[it]) }
    // The hand stays visible during the opponent's turn, just quieter.
    val handAlpha by animateFloatAsState(if (isInteractive) 1f else 0.6f)
    val haptic = LocalHapticFeedback.current
    val vibrationEnabled = LocalVibrationEnabled.current

    val infiniteTransition = rememberInfiniteTransition()
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var touchInCard by remember { mutableStateOf(Offset.Zero) }
    var cardPositions by remember { mutableStateOf(mapOf<Int, Offset>()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = deadCardIndex != null,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            Button(
                onClick = { deadCardIndex?.let(onSwapDeadCard) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed, contentColor = PureWhite),
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .height(36.dp)
            ) {
                Text(
                    text = "ĐỔI BÀI CHẾT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Fanned hand: cards overlap like physical cards and follow a gentle arc.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(HandCardWidth * 1.5f + 48.dp)
                .alpha(handAlpha)
                .padding(top = 26.dp)
        ) {
            val totalCards = hand.cards.size
            if (totalCards > 0) {
                val cardWidth = HandCardWidth
                val available = maxWidth - cardWidth - 56.dp
                val idealStep = 44.dp
                val step = if (totalCards > 1) minOf(idealStep, available / (totalCards - 1)) else idealStep
                val startX = (maxWidth - (cardWidth + step * (totalCards - 1))) / 2
                val middle = (totalCards - 1) / 2f

                hand.cards.forEachIndexed { index, card ->
                    val isSelected = index in selectedIndices
                    val isCardEnabled = isInteractive && (enabledIndices == null || index in enabledIndices)
                    val isGuided = guidedIndices != null && index in guidedIndices && !isSelected
                    val cardAlpha = if (enabledIndices != null && index !in enabledIndices) 0.35f else 1f
                    val fromMiddle = index - middle
                    val isDraggingThis = draggingIndex == index
                    val dragY = if (isDraggingThis) dragOffset.y else 0f
                    val dragX = if (isDraggingThis) dragOffset.x else 0f

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = startX + step * index, y = (fromMiddle * fromMiddle * 1.4f).dp - 10.dp)
                            .offset {
                                if (isDraggingThis) {
                                    IntOffset(dragX.roundToInt(), dragY.roundToInt())
                                } else {
                                    IntOffset.Zero
                                }
                            }
                            .zIndex(if (isDraggingThis) 150f else if (isSelected) 50f else if (isGuided) 45f else index.toFloat())
                            .onGloballyPositioned { coordinates ->
                                if (draggingIndex == null) {
                                    cardPositions = cardPositions + (index to coordinates.positionInRoot())
                                }
                            }
                            .pointerInput(index, isCardEnabled) {
                                if (!isCardEnabled) return@pointerInput
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        draggingIndex = index
                                        dragOffset = Offset.Zero
                                        touchInCard = offset
                                        if (index !in selectedIndices) {
                                            onCardClick(index)
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount
                                        val rootPos = cardPositions[index] ?: Offset.Zero
                                        val globalPos = rootPos + dragOffset + touchInCard
                                        if (dragOffset.y < -30f) {
                                            onCardDragHover?.invoke(index, globalPos)
                                        }
                                    },
                                    onDragEnd = {
                                        val finalOffset = dragOffset
                                        val wasDragging = draggingIndex == index
                                        val rootPos = cardPositions[index] ?: Offset.Zero
                                        val finalGlobal = rootPos + finalOffset + touchInCard
                                        draggingIndex = null
                                        dragOffset = Offset.Zero

                                        if (wasDragging) {
                                            if (finalOffset.y < -50f) {
                                                onCardDrop?.invoke(index, finalGlobal)
                                            } else if (abs(finalOffset.x) > 24f && onReorderHand != null) {
                                                val shift = (finalOffset.x / step.toPx()).roundToInt()
                                                val target = (index + shift).coerceIn(0, totalCards - 1)
                                                if (target != index) {
                                                    if (vibrationEnabled) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                                    }
                                                    onReorderHand(index, target)
                                                }
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        dragOffset = Offset.Zero
                                    }
                                )
                            }
                    ) {
                        HandCardView(
                            card = card,
                            isSelected = isSelected,
                            isDead = isCardDead(card),
                            isCraftHighlight = isSelected && selectedIndices.size >= 2,
                            enabled = isCardEnabled,
                            onClick = {
                                if (vibrationEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                                onCardClick(index)
                            },
                            modifier = Modifier
                                .alpha(cardAlpha)
                                .then(
                                    if (isGuided) {
                                        Modifier
                                            .shadow(16.dp, RoundedCornerShape(10.dp), ambientColor = GoldAccent, spotColor = GoldAccent)
                                            .border(2.5.dp, GoldAccent, RoundedCornerShape(10.dp))
                                    } else Modifier
                                )
                                .graphicsLayer {
                                    if (isDraggingThis) {
                                        scaleX = 1.12f
                                        scaleY = 1.12f
                                        rotationZ = 0f
                                    } else {
                                        rotationZ = fromMiddle * 2.6f
                                    }
                                    transformOrigin = TransformOrigin(0.5f, 1f)
                                }
                        )

                        if (isGuided) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-36).dp + bounceOffset.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BrandRed,
                                    shadowElevation = 8.dp
                                ) {
                                    Text(
                                        text = guideLabel ?: "CHỌN",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PureWhite,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Text(
                                    text = "▼",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BrandRed,
                                    modifier = Modifier.offset(y = (-3).dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Whose hand this is (matters in pass-and-play).
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape)
                .padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(owner.team.primaryColor())
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = owner.name.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
