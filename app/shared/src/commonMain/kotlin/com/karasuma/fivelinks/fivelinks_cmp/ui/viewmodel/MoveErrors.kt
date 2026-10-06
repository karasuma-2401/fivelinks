package com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel

/**
 * The engine reports rule violations in English (it is shared with the server),
 * so translate the ones a player can trigger before showing them.
 */
internal fun localizeMoveError(message: String?): String {
    val text = message.orEmpty()
    return when {
        text.startsWith("Game is over") -> "Ván đấu đã kết thúc."
        text.startsWith("It's not your turn") -> "Chưa đến lượt của bạn."
        text.startsWith("Card not in hand") -> "Lá bài này không có trên tay."
        text.startsWith("Cannot place on a corner") -> "Không thể đặt quân vào ô góc."
        text.startsWith("Position is already occupied") -> "Ô này đã có quân."
        text.startsWith("Cannot place a one-eyed jack") -> "J 1 mắt không dùng để đặt quân."
        text.startsWith("Cannot place a card that is not the same") -> "Lá bài không khớp với ô trên bàn cờ."
        text.startsWith("Remove requires a one-eyed Jack") -> "Cần J 1 mắt để gỡ quân."
        text.startsWith("no chip at") -> "Ô này không có quân để gỡ."
        text.startsWith("cannot remove own team's chip") -> "Không thể gỡ quân của đội mình."
        text.startsWith("cannot remove a chip that is locked") -> "Quân đã khóa trong hàng, không thể gỡ."
        text.startsWith("Jacks are never dead") -> "Lá J không bao giờ là bài chết."
        text.endsWith("is not on the board") -> "Lá bài này không có trên bàn cờ."
        text.endsWith("is not dead") -> "Lá bài này chưa phải bài chết."
        text.startsWith("cannot swap") -> "Hết bài để đổi."
        text.startsWith("Tactical crafting is disabled") -> "Chế độ chiến thuật đang tắt."
        text.startsWith("Player does not hold both") -> "Bạn không có đủ 2 lá để ghép."
        text.startsWith("CraftPlace requires") -> "Ghép đôi cần 2 lá cùng số."
        text.startsWith("CraftRemove requires") -> "Cần 2 lá liên tiếp cùng chất."
        text.startsWith("DivineWipe requires exactly") -> "Thiên Phạt cần đúng 5 lá."
        text.startsWith("Player does not hold all 5") -> "Bạn không có đủ 5 lá."
        text.startsWith("DivineWipe requires") -> "Thiên Phạt cần sảnh đồng chất 5 lá."
        else -> "Nước đi không hợp lệ!"
    }
}
