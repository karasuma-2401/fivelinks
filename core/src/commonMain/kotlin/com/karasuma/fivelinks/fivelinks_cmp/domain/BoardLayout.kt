package com.karasuma.fivelinks.fivelinks_cmp.domain

object BoardLayout {
    val standardLayout: Board by lazy { parse(CANONICAL_LAYOUT) }

    val CANONICAL_LAYOUT: Array<Array<String>> = arrayOf(
        arrayOf("*", "6D", "7D", "8D", "9D", "10D", "QD", "KD", "AD", "*"),
        arrayOf("5D", "3H", "2H", "2S", "3S", "4S", "5S", "6S", "7S", "AC"),
        arrayOf("4D", "4H", "KD", "AD", "AC", "KC", "QC", "10C", "8S", "KC"),
        arrayOf("3D", "5H", "QD", "QH", "10H", "9H", "8H", "9C", "9S", "QC"),
        arrayOf("2D", "6H", "10D", "KH", "3H", "2H", "7H", "8C", "10S", "10C"),
        arrayOf("AS", "7H", "9D", "AH", "4H", "5H", "6H", "7C", "QS", "9C"),
        arrayOf("KS", "8H", "8D", "2C", "3C", "4C", "5C", "6C", "KS", "8C"),
        arrayOf("QS", "9H", "7D", "6D", "5D", "4D", "3D", "2D", "AS", "7C"),
        arrayOf("10S", "10H", "QH", "KH", "AH", "2C", "3C", "4C", "5C", "6C"),
        arrayOf("*", "9S", "8S", "7S", "6S", "5S", "4S", "3S", "2S", "*"),
    )

    fun parse(codes: Array<Array<String>>) : Board {
        val cells = ArrayList<Cell>(100)
        for (row in codes) {
            require(row.size == 10) { "Each row must have 10 cells"}
            for (cell in row) {
                val item = if (cell == "*") Cell.Corner else Cell.Slot(parseCode(cell))
                cells.add(item)
            }
        }
        return Board(cells)
    }
    fun parseCode(code: String): Card {
        val suitCode = code.last().toString()
        val rankCode = code.dropLast(1)
        val suit = when(suitCode) {
            "S" -> Suit.SPADES
            "H" -> Suit.HEARTS
            "D" -> Suit.DIAMONDS
            "C" -> Suit.CLUBS
            else -> error("bad suit code: $code")
        }
        val rank = when (rankCode) {
            "A" -> Rank.ACE
            "K" -> Rank.KING
            "Q" -> Rank.QUEEN
            "J" -> Rank.JACK
            "10" -> Rank.TEN
            "9" -> Rank.NINE
            "8" -> Rank.EIGHT
            "7" -> Rank.SEVEN
            "6" -> Rank.SIX
            "5" -> Rank.FIVE
            "4" -> Rank.FOUR
            "3" -> Rank.THREE
            "2" -> Rank.TWO
            else -> error("bad rank code: $code")
        }
        return Card(suit, rank)
    }
}