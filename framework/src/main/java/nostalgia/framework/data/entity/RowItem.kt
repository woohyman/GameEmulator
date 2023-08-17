package nostalgia.framework.data.entity

import nostalgia.framework.data.database.GameDescription

data class RowItem(
    var game: GameDescription? = null,
    var firstLetter: Char = 0.toChar()
)