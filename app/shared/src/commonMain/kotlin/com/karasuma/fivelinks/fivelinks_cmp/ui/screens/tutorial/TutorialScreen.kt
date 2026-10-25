package com.karasuma.fivelinks.fivelinks_cmp.ui.screens.tutorial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.karasuma.fivelinks.fivelinks_cmp.PlatformBackHandler
import com.karasuma.fivelinks.fivelinks_cmp.domain.Board
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.ChipMap
import com.karasuma.fivelinks.fivelinks_cmp.domain.ChipSequence
import com.karasuma.fivelinks.fivelinks_cmp.domain.Deck
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Hand
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Rank
import com.karasuma.fivelinks.fivelinks_cmp.domain.Suit
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.BoardGrid
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.HandView
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.LineIcon
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.LineIconView
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.TacticalCraftingBar
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.TacticalAction

enum class TutorialStep(
    val stepIndex: Int,
    val stepLabel: String,
    val title: String,
    val instruction: String,
    val successText: String,
    val icon: LineIcon,
) {
    STEP_1_SEQUENCE(
        1,
        "01 / 04",
        "Tạo chuỗi 5 ô chiến thắng",
        "Chọn lá bài 9 Rô (9♦) trên tay, sau đó chạm vào ô nhấp nháy trên bàn cờ để hoàn thành chuỗi 5 ô liên tiếp!",
        "Xuất sắc! Bạn vừa hoàn thành chuỗi Sequence đầu tiên. 4 ô góc có biểu tượng ngôi sao luôn là ô tự do vĩnh viễn cho tất cả người chơi!",
        LineIcon.Sparkle
    ),
    STEP_2_JACK_WILD(
        2,
        "02 / 04",
        "Jack 2 mắt — Đặt quân vạn năng",
        "Jack 2 mắt (J rô ♦, J chuồn ♣) là quân bài tự do quyền lực nhất! Hãy chọn lá J♦ và chạm vào bất kỳ ô xanh nhấp nháy nào để đặt cờ.",
        "Chính xác! Dùng Jack 2 mắt đúng thời điểm sẽ giúp bạn nối chuỗi bất ngờ hoặc tạo thế cờ hiểm hóc!",
        LineIcon.Star
    ),
    STEP_3_JACK_SNIPE(
        3,
        "03 / 04",
        "Jack 1 mắt — Bắn tỉa quân đối thủ",
        "Đối thủ (màu Đỏ) đang có 4 quân sắp tạo chuỗi! Hãy chọn Jack 1 mắt (J bích ♠) và chạm vào 1 quân cờ Đỏ để bắn tỉa và gỡ bỏ nó!",
        "Phòng thủ hoàn hảo! Jack 1 mắt giúp bạn tiêu diệt quân của đối thủ (trừ những quân đã thuộc chuỗi 5 đã khóa).",
        LineIcon.Crosshair
    ),
    STEP_4_TACTICAL_CRAFTING(
        4,
        "04 / 04",
        "Ghép bài chiến thuật đỉnh cao",
        "Không có Jack? Chọn 2 lá bài cùng số (đôi 7) trên tay để GHÉP thành 1 Jack nhân tạo, sau đó đặt quân cờ lên bàn cờ!",
        "Tuyệt chiêu đỉnh cao! Bạn đã thuần thục tính năng Chế tạo chiến thuật độc quyền của FiveLink!",
        LineIcon.Bolt
    )
}

@Composable
fun TutorialScreen(
    onFinishTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlatformBackHandler(onBack = onFinishTutorial)

    var currentStep by remember { mutableStateOf(TutorialStep.STEP_1_SEQUENCE) }
    var stepSuccess by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    val bluePlayer = remember { Player(id = "player_blue", name = "Bạn", team = Team.BLUE) }
    val redPlayer = remember { Player(id = "player_red", name = "Đối thủ", team = Team.RED) }
    val players = remember { listOf(bluePlayer, redPlayer) }
    val baseConfig = remember { GameConfig.tactical(playerCount = 2, teams = 2, seed = 42L) }

    // Interactive step-specific board and hand state
    var selectedIndices by remember { mutableStateOf(emptySet<Int>()) }
    var activeTacticalAction by remember { mutableStateOf<TacticalAction?>(null) }
    var placedChips by remember(currentStep) {
        mutableStateOf(
            when (currentStep) {
                TutorialStep.STEP_1_SEQUENCE -> ChipMap()
                    .place(BoardPosition(0, 1), Team.BLUE)
                    .place(BoardPosition(0, 2), Team.BLUE)
                    .place(BoardPosition(0, 3), Team.BLUE)
                    .place(BoardPosition(2, 2), Team.RED)
                    .place(BoardPosition(2, 3), Team.RED)
                TutorialStep.STEP_2_JACK_WILD -> ChipMap()
                    .place(BoardPosition(0, 1), Team.BLUE)
                    .place(BoardPosition(0, 2), Team.BLUE)
                    .place(BoardPosition(0, 3), Team.BLUE)
                    .place(BoardPosition(0, 4), Team.BLUE)
                    .place(BoardPosition(2, 2), Team.RED)
                    .place(BoardPosition(2, 3), Team.RED)
                    .place(BoardPosition(3, 3), Team.RED)
                TutorialStep.STEP_3_JACK_SNIPE -> ChipMap()
                    .place(BoardPosition(5, 3), Team.RED)
                    .place(BoardPosition(5, 4), Team.RED)
                    .place(BoardPosition(5, 5), Team.RED)
                    .place(BoardPosition(5, 6), Team.RED)
                    .place(BoardPosition(1, 1), Team.BLUE)
                    .place(BoardPosition(1, 2), Team.BLUE)
                TutorialStep.STEP_4_TACTICAL_CRAFTING -> ChipMap()
                    .place(BoardPosition(3, 1), Team.BLUE)
                    .place(BoardPosition(3, 2), Team.BLUE)
                    .place(BoardPosition(4, 4), Team.RED)
                    .place(BoardPosition(4, 5), Team.RED)
            }
        )
    }

    var lockedSequences by remember(currentStep) {
        mutableStateOf(
            if (currentStep != TutorialStep.STEP_1_SEQUENCE) {
                listOf(
                    ChipSequence(
                        team = Team.BLUE,
                        positions = listOf(
                            BoardPosition(0, 0),
                            BoardPosition(0, 1),
                            BoardPosition(0, 2),
                            BoardPosition(0, 3),
                            BoardPosition(0, 4)
                        )
                    )
                )
            } else emptyList()
        )
    }

    val handCards by remember(currentStep) {
        mutableStateOf(
            when (currentStep) {
                TutorialStep.STEP_1_SEQUENCE -> listOf(
                    Card(Suit.DIAMONDS, Rank.NINE),
                    Card(Suit.HEARTS, Rank.FOUR),
                    Card(Suit.SPADES, Rank.KING),
                    Card(Suit.CLUBS, Rank.TEN),
                )
                TutorialStep.STEP_2_JACK_WILD -> listOf(
                    Card(Suit.DIAMONDS, Rank.JACK),
                    Card(Suit.HEARTS, Rank.FOUR),
                    Card(Suit.SPADES, Rank.KING),
                    Card(Suit.CLUBS, Rank.TEN),
                )
                TutorialStep.STEP_3_JACK_SNIPE -> listOf(
                    Card(Suit.SPADES, Rank.JACK),
                    Card(Suit.HEARTS, Rank.FOUR),
                    Card(Suit.CLUBS, Rank.EIGHT),
                    Card(Suit.DIAMONDS, Rank.ACE),
                )
                TutorialStep.STEP_4_TACTICAL_CRAFTING -> listOf(
                    Card(Suit.CLUBS, Rank.SEVEN),
                    Card(Suit.HEARTS, Rank.SEVEN),
                    Card(Suit.SPADES, Rank.KING),
                    Card(Suit.DIAMONDS, Rank.ACE),
                )
            }
        )
    }

    // Build synthetic GameState for rendering components
    val board = remember { Board.standard }
    val currentGameState = remember(placedChips, lockedSequences, handCards) {
        GameState(
            config = baseConfig,
            board = board,
            players = players,
            hands = mapOf(bluePlayer.id to Hand(handCards), redPlayer.id to Hand(emptyList())),
            deck = Deck(emptyList()),
            discard = emptyList(),
            chips = placedChips,
            completedSequence = lockedSequences,
            currentPlayerIndex = 0,
            turnNumber = 1,
            winner = null,
        )
    }

    // Calculate highlighted positions based on tutorial stage and card selection
    val validPlacementPositions = remember(currentStep, selectedIndices, activeTacticalAction, stepSuccess) {
        if (stepSuccess) return@remember emptySet<BoardPosition>()
        when (currentStep) {
            TutorialStep.STEP_1_SEQUENCE -> {
                if (0 in selectedIndices) setOf(BoardPosition(0, 4)) else emptySet()
            }
            TutorialStep.STEP_2_JACK_WILD -> {
                if (0 in selectedIndices) {
                    setOf(BoardPosition(4, 4), BoardPosition(4, 5), BoardPosition(3, 4))
                } else emptySet()
            }
            TutorialStep.STEP_3_JACK_SNIPE -> emptySet()
            TutorialStep.STEP_4_TACTICAL_CRAFTING -> {
                if (activeTacticalAction != null) {
                    setOf(BoardPosition(3, 3), BoardPosition(3, 4), BoardPosition(3, 5))
                } else emptySet()
            }
        }
    }

    val validSnipePositions = remember(currentStep, selectedIndices, stepSuccess) {
        if (stepSuccess) return@remember emptySet<BoardPosition>()
        if (currentStep == TutorialStep.STEP_3_JACK_SNIPE && 0 in selectedIndices) {
            setOf(BoardPosition(5, 5), BoardPosition(5, 4), BoardPosition(5, 3), BoardPosition(5, 6))
        } else emptySet()
    }

    // Step-by-step strict targeting and AAA guidance indicators
    val (tutorialEnabledIndices, tutorialGuidedIndices, tutorialCardLabel) = remember(currentStep, selectedIndices, stepSuccess) {
        if (stepSuccess) return@remember Triple(emptySet<Int>(), emptySet<Int>(), null)
        when (currentStep) {
            TutorialStep.STEP_1_SEQUENCE -> {
                Triple(setOf(0), if (0 !in selectedIndices) setOf(0) else emptySet(), "CHỌN 9♦")
            }
            TutorialStep.STEP_2_JACK_WILD -> {
                Triple(setOf(0), if (0 !in selectedIndices) setOf(0) else emptySet(), "CHỌN J♦")
            }
            TutorialStep.STEP_3_JACK_SNIPE -> {
                Triple(setOf(0), if (0 !in selectedIndices) setOf(0) else emptySet(), "CHỌN J♠")
            }
            TutorialStep.STEP_4_TACTICAL_CRAFTING -> {
                val unpicked = setOf(0, 1) - selectedIndices
                Triple(setOf(0, 1), unpicked, "GHÉP ĐÔI 7")
            }
        }
    }

    val (tutorialGuidedBoardPosition, tutorialBoardLabel) = remember(currentStep, selectedIndices, activeTacticalAction, stepSuccess) {
        if (stepSuccess) return@remember Pair(null, null)
        when (currentStep) {
            TutorialStep.STEP_1_SEQUENCE -> {
                if (0 in selectedIndices) BoardPosition(0, 4) to "ĐẶT VÀO ĐÂY" else null to null
            }
            TutorialStep.STEP_2_JACK_WILD -> {
                if (0 in selectedIndices) BoardPosition(4, 4) to "ĐẶT TỰ DO" else null to null
            }
            TutorialStep.STEP_3_JACK_SNIPE -> {
                if (0 in selectedIndices) BoardPosition(5, 5) to "BẮN TỈA CỜ ĐỎ" else null to null
            }
            TutorialStep.STEP_4_TACTICAL_CRAFTING -> {
                if (activeTacticalAction != null) BoardPosition(3, 4) to "ĐẶT QUÂN GHÉP" else null to null
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        val isWide = maxWidth >= 760.dp && maxWidth > maxHeight * 1.05f

        Column(modifier = Modifier.fillMaxSize()) {
            // Tutorial Header Coaching Banner
            TutorialCoachingCard(
                step = currentStep,
                isSuccess = stepSuccess,
                onSkip = onFinishTutorial,
                onNextStep = {
                    when (currentStep) {
                        TutorialStep.STEP_1_SEQUENCE -> {
                            currentStep = TutorialStep.STEP_2_JACK_WILD
                            selectedIndices = emptySet()
                            activeTacticalAction = null
                            stepSuccess = false
                        }
                        TutorialStep.STEP_2_JACK_WILD -> {
                            currentStep = TutorialStep.STEP_3_JACK_SNIPE
                            selectedIndices = emptySet()
                            activeTacticalAction = null
                            stepSuccess = false
                        }
                        TutorialStep.STEP_3_JACK_SNIPE -> {
                            currentStep = TutorialStep.STEP_4_TACTICAL_CRAFTING
                            selectedIndices = emptySet()
                            activeTacticalAction = null
                            stepSuccess = false
                        }
                        TutorialStep.STEP_4_TACTICAL_CRAFTING -> {
                            showCompletionDialog = true
                        }
                    }
                }
            )

            if (isWide) {
                // Wide landscape layout
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        BoardGrid(
                            gameState = currentGameState,
                            validPlacementPositions = validPlacementPositions,
                            validSnipePositions = validSnipePositions,
                            guidedPosition = tutorialGuidedBoardPosition,
                            guidedLabel = tutorialBoardLabel,
                            onCellClick = { pos ->
                                handleCellClick(
                                    step = currentStep,
                                    pos = pos,
                                    selectedIndices = selectedIndices,
                                    activeAction = activeTacticalAction,
                                    onSuccess = {
                                        stepSuccess = true
                                        when (currentStep) {
                                            TutorialStep.STEP_1_SEQUENCE -> {
                                                placedChips = placedChips.place(pos, Team.BLUE)
                                                lockedSequences = listOf(
                                                    ChipSequence(
                                                        Team.BLUE,
                                                        listOf(
                                                            BoardPosition(0, 0),
                                                            BoardPosition(0, 1),
                                                            BoardPosition(0, 2),
                                                            BoardPosition(0, 3),
                                                            BoardPosition(0, 4)
                                                        )
                                                    )
                                                )
                                            }
                                            TutorialStep.STEP_2_JACK_WILD -> {
                                                placedChips = placedChips.place(pos, Team.BLUE)
                                            }
                                            TutorialStep.STEP_3_JACK_SNIPE -> {
                                                placedChips = placedChips.remove(pos)
                                            }
                                            TutorialStep.STEP_4_TACTICAL_CRAFTING -> {
                                                placedChips = placedChips.place(pos, Team.BLUE)
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TacticalCraftingBar(
                            tacticalAction = activeTacticalAction,
                            onTriggerDivineWipe = {}
                        )
                        Spacer(Modifier.height(8.dp))
                        HandView(
                            hand = Hand(handCards),
                            owner = bluePlayer,
                            selectedIndices = selectedIndices,
                            gameState = currentGameState,
                            isInteractive = !stepSuccess,
                            enabledIndices = tutorialEnabledIndices,
                            guidedIndices = tutorialGuidedIndices,
                            guideLabel = tutorialCardLabel,
                            onCardClick = { index ->
                                selectedIndices = toggleSelection(selectedIndices, index, currentStep) { action ->
                                    activeTacticalAction = action
                                }
                            },
                            onSwapDeadCard = {}
                        )
                    }
                }
            } else {
                // Portrait layout
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BoardGrid(
                            gameState = currentGameState,
                            validPlacementPositions = validPlacementPositions,
                            validSnipePositions = validSnipePositions,
                            guidedPosition = tutorialGuidedBoardPosition,
                            guidedLabel = tutorialBoardLabel,
                            onCellClick = { pos ->
                                handleCellClick(
                                    step = currentStep,
                                    pos = pos,
                                    selectedIndices = selectedIndices,
                                    activeAction = activeTacticalAction,
                                    onSuccess = {
                                        stepSuccess = true
                                        when (currentStep) {
                                            TutorialStep.STEP_1_SEQUENCE -> {
                                                placedChips = placedChips.place(pos, Team.BLUE)
                                                lockedSequences = listOf(
                                                    ChipSequence(
                                                        Team.BLUE,
                                                        listOf(
                                                            BoardPosition(0, 0),
                                                            BoardPosition(0, 1),
                                                            BoardPosition(0, 2),
                                                            BoardPosition(0, 3),
                                                            BoardPosition(0, 4)
                                                        )
                                                    )
                                                )
                                            }
                                            TutorialStep.STEP_2_JACK_WILD -> {
                                                placedChips = placedChips.place(pos, Team.BLUE)
                                            }
                                            TutorialStep.STEP_3_JACK_SNIPE -> {
                                                placedChips = placedChips.remove(pos)
                                            }
                                            TutorialStep.STEP_4_TACTICAL_CRAFTING -> {
                                                placedChips = placedChips.place(pos, Team.BLUE)
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    }

                    TacticalCraftingBar(
                        tacticalAction = activeTacticalAction,
                        onTriggerDivineWipe = {}
                    )

                    Spacer(Modifier.height(4.dp))

                    HandView(
                        hand = Hand(handCards),
                        owner = bluePlayer,
                        selectedIndices = selectedIndices,
                        gameState = currentGameState,
                        isInteractive = !stepSuccess,
                        enabledIndices = tutorialEnabledIndices,
                        guidedIndices = tutorialGuidedIndices,
                        guideLabel = tutorialCardLabel,
                        onCardClick = { index ->
                            selectedIndices = toggleSelection(selectedIndices, index, currentStep) { action ->
                                activeTacticalAction = action
                            }
                        },
                        onSwapDeadCard = {}
                    )
                }
            }
        }
    }

    if (showCompletionDialog) {
        TutorialCompletionDialog(onFinish = onFinishTutorial)
    }
}

private fun toggleSelection(
    current: Set<Int>,
    tappedIndex: Int,
    step: TutorialStep,
    onTacticalActionChange: (TacticalAction?) -> Unit
): Set<Int> {
    if (step == TutorialStep.STEP_4_TACTICAL_CRAFTING) {
        val next = if (tappedIndex in current) current - tappedIndex else current + tappedIndex
        if (0 in next && 1 in next) {
            onTacticalActionChange(
                TacticalAction.PairWild(
                    Card(Suit.CLUBS, Rank.SEVEN),
                    Card(Suit.HEARTS, Rank.SEVEN)
                )
            )
        } else {
            onTacticalActionChange(null)
        }
        return next
    } else {
        onTacticalActionChange(null)
        return if (tappedIndex in current) emptySet() else setOf(tappedIndex)
    }
}

private fun handleCellClick(
    step: TutorialStep,
    pos: BoardPosition,
    selectedIndices: Set<Int>,
    activeAction: TacticalAction?,
    onSuccess: () -> Unit
) {
    when (step) {
        TutorialStep.STEP_1_SEQUENCE -> {
            if (0 in selectedIndices && pos == BoardPosition(0, 4)) onSuccess()
        }
        TutorialStep.STEP_2_JACK_WILD -> {
            if (0 in selectedIndices && pos in setOf(BoardPosition(4, 4), BoardPosition(4, 5), BoardPosition(3, 4))) {
                onSuccess()
            }
        }
        TutorialStep.STEP_3_JACK_SNIPE -> {
            if (0 in selectedIndices && pos in setOf(BoardPosition(5, 3), BoardPosition(5, 4), BoardPosition(5, 5), BoardPosition(5, 6))) {
                onSuccess()
            }
        }
        TutorialStep.STEP_4_TACTICAL_CRAFTING -> {
            if (activeAction != null && pos in setOf(BoardPosition(3, 3), BoardPosition(3, 4), BoardPosition(3, 5))) {
                onSuccess()
            }
        }
    }
}

@Composable
private fun TutorialCoachingCard(
    step: TutorialStep,
    isSuccess: Boolean,
    onSkip: () -> Unit,
    onNextStep: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(BrandRed.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        LineIconView(step.icon, color = BrandRed, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "HƯỚNG DẪN TƯƠNG TÁC • ${step.stepLabel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandRed,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "BỎ QUA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onSkip)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = step.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = if (isSuccess) step.successText else step.instruction,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(visible = isSuccess, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onNextStep,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandRed,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (step == TutorialStep.STEP_4_TACTICAL_CRAFTING) "HOÀN THÀNH HUẤN LUYỆN ➔" else "BÀI HỌC TIẾP THEO ➔",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialCompletionDialog(
    onFinish: () -> Unit
) {
    Dialog(onDismissRequest = onFinish) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp).shadow(12.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(BrandRed.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    LineIconView(LineIcon.Sparkle, color = BrandRed, modifier = Modifier.size(32.dp))
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "XUẤT SẮC TỐT NGHIỆP!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = BrandRed,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Bạn đã nắm vững: Tạo chuỗi 5 ô, quân Jack 1 mắt/2 mắt, và Tuyệt kỹ ghép bài chiến thuật. Giờ hãy bước vào đấu trường FiveLink!",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed, contentColor = PureWhite),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("BẮT ĐẦU CHƠI NGAY ➔", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }
        }
    }
}
