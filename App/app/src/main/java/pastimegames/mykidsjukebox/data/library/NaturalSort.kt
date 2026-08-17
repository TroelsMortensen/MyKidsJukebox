package pastimegames.mykidsjukebox.data.library

import java.text.Collator
import java.util.Locale

object NaturalSort {
    val comparator: Comparator<String> = Comparator { left, right ->
        compare(left, right)
    }

    private val collatorLock = Any()
    private val collators = mutableMapOf<Locale, Collator>()

    fun comparator(locale: Locale): Comparator<String> {
        return Comparator { left, right ->
            compare(left, right, locale)
        }
    }

    fun compare(left: String, right: String): Int {
        return compare(left, right, Locale.getDefault())
    }

    fun compare(left: String, right: String, locale: Locale): Int {
        var i = 0
        var j = 0
        while (i < left.length && j < right.length) {
            val leftDigit = left[i].isDigit()
            val rightDigit = right[j].isDigit()

            if (leftDigit && rightDigit) {
                val leftNumber = readNumber(left, i)
                val rightNumber = readNumber(right, j)
                val numberCmp = compareNumbers(leftNumber, rightNumber)
                if (numberCmp != 0) {
                    return numberCmp
                }
                i += leftNumber.length
                j += rightNumber.length
            } else {
                val leftChunk = readText(left, i)
                val rightChunk = readText(right, j)
                when {
                    leftChunk.isEmpty() -> return -1
                    rightChunk.isEmpty() -> return 1
                    else -> {
                        val textCmp = compareText(leftChunk, rightChunk, locale)
                        if (textCmp != 0) {
                            return textCmp
                        }
                        i += leftChunk.length
                        j += rightChunk.length
                    }
                }
            }
        }
        return (left.length - i).compareTo(right.length - j)
    }

    private fun compareText(left: String, right: String, locale: Locale): Int {
        synchronized(collatorLock) {
            return collatorFor(locale).compare(left, right)
        }
    }

    private fun collatorFor(locale: Locale): Collator {
        return collators.getOrPut(locale) {
            Collator.getInstance(locale).apply {
                strength = Collator.SECONDARY
            }
        }
    }
}

fun sortLibraryItems(items: List<FolderGridItem>): List<FolderGridItem> {
    return items.sortedWith(
        compareBy<FolderGridItem> { it.kind != LibraryItemKind.Folder }
            .thenBy(NaturalSort.comparator) { it.name }
    )
}

private fun readNumber(value: String, start: Int): String {
    var end = start
    while (end < value.length && value[end].isDigit()) {
        end++
    }
    return value.substring(start, end)
}

private fun readText(value: String, start: Int): String {
    var end = start
    while (end < value.length && !value[end].isDigit()) {
        end++
    }
    return value.substring(start, end)
}

private fun compareNumbers(left: String, right: String): Int {
    val leftSignificant = left.trimStart('0').ifEmpty { "0" }
    val rightSignificant = right.trimStart('0').ifEmpty { "0" }

    val lengthCmp = leftSignificant.length.compareTo(rightSignificant.length)
    if (lengthCmp != 0) {
        return lengthCmp
    }

    val valueCmp = leftSignificant.compareTo(rightSignificant)
    if (valueCmp != 0) {
        return valueCmp
    }

    // Same numeric value: more leading zeros first ("02" before "2")
    return right.length.compareTo(left.length)
}
