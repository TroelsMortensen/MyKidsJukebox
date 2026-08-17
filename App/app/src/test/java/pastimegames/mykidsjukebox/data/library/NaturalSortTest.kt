package pastimegames.mykidsjukebox.data.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Collator
import java.util.Locale
import kotlin.random.Random

class NaturalSortTest {
    @Test
    fun chapters_are_numeric_not_lexicographic() {
        assertEquals(
            listOf("Chapter 1", "Chapter 2", "Chapter 10", "Chapter 11", "Chapter 20"),
            sort("Chapter 11", "Chapter 2", "Chapter 20", "Chapter 1", "Chapter 10")
        )
    }

    @Test
    fun case_is_ignored() {
        assertEquals(
            listOf("chapter 2", "Chapter 10"),
            sort("Chapter 10", "chapter 2")
        )
    }

    @Test
    fun leading_zeros_keep_numeric_order() {
        assertEquals(
            listOf("Track 02", "Track 2", "Track 10"),
            sort("Track 10", "Track 2", "Track 02")
        )
    }

    @Test
    fun numbers_in_the_middle() {
        assertEquals(
            listOf(
                "Season 1 Episode 2",
                "Season 1 Episode 10",
                "Season 2 Episode 1"
            ),
            sort("Season 1 Episode 10", "Season 2 Episode 1", "Season 1 Episode 2")
        )
    }

    @Test
    fun shorter_prefix_comes_first() {
        assertEquals(
            listOf("Book", "Book 1"),
            sort("Book 1", "Book")
        )
    }

    @Test
    fun names_without_numbers_are_alphabetical() {
        assertEquals(
            listOf("Aladdin", "Moana", "Zootopia"),
            sort("Zootopia", "Aladdin", "Moana")
        )
    }

    @Test
    fun empty_and_plain_digits() {
        assertEquals(
            listOf("", "1", "2", "10"),
            sort("10", "", "2", "1")
        )
    }

    @Test
    fun shuffled_verdens_pinligste_far_keeps_julens_magi_in_numeric_order() {
        val names = loadNames("verdens-pinligste-far.txt")
        val sorted = shuffleAndSort(names)
        val magiNumbers = numberedValues(sorted, JULENS_MAGI)

        assertEquals((1..24).toList(), magiNumbers)
        assertLexicographicSortBreaksNumericOrder(names, JULENS_MAGI)
    }

    @Test
    fun shuffled_bamsen_geo_keeps_numbered_episodes_in_numeric_order() {
        val names = loadNames("bamsen-geo.txt")
        val sorted = shuffleAndSort(names)

        assertTrue(names.any { it.startsWith(" 4. december") })
        assertTrue(names.any { it.startsWith(" 22. Nisseværkstedet") })

        val decemberNumbers = numberedValues(sorted, DECEMBER_EPISODE)
        assertTrue(decemberNumbers.containsAll(listOf(1, 2, 3, 10, 11, 24)))
        assertTrue(isNonDecreasing(decemberNumbers))

        val julestjerneNumbers = numberedValues(sorted, JULESTJERNEN_EPISODE)
        assertTrue(julestjerneNumbers.containsAll(listOf(1, 2, 10, 11, 24)))
        assertTrue(isNonDecreasing(julestjerneNumbers))

        assertLexicographicSortBreaksNumericOrder(names, DECEMBER_EPISODE)
        assertLexicographicSortBreaksNumericOrder(names, JULESTJERNEN_EPISODE)
    }

    @Test
    fun shuffled_radio_jesperhus_keeps_numbered_series_in_numeric_order() {
        val names = loadNames("radio-jesperhus.txt")
        val sorted = shuffleAndSort(names)

        assertEquals((1..24).toList(), numberedValues(sorted, ADVENT_EPISODE))
        assertEquals(listOf(1, 2, 3), numberedValues(sorted, AGENT_HUGO))
        assertEquals(listOf(1, 2, 3), numberedValues(sorted, AGENTHULEN))
        assertEquals(listOf(1, 2, 3), numberedValues(sorted, PIRATLAERLINGEN_LILJA))

        assertLexicographicSortBreaksNumericOrder(names, ADVENT_EPISODE)
    }

    @Test
    fun danish_letters_follow_danish_alphabet() {
        assertEquals(
            listOf("æble", "øre", "å"),
            sort(DANISH, "å", "øre", "æble")
        )
    }

    @Test
    fun danish_aa_uses_collator_not_first_letter() {
        val collator = Collator.getInstance(DANISH).apply {
            strength = Collator.SECONDARY
        }
        val sorted = sort(DANISH, "Aalborg", "Zoo")
        if (collator.compare("Aalborg", "Zoo") > 0) {
            assertEquals(listOf("Zoo", "Aalborg"), sorted)
        } else {
            assertEquals(listOf("Aalborg", "Zoo"), sorted)
        }
    }

    @Test
    fun english_keeps_aaron_with_a() {
        assertEquals(
            listOf("Aaron", "Moana", "Zoo"),
            sort(Locale.ENGLISH, "Zoo", "Aaron", "Moana")
        )
    }

    private fun sort(vararg names: String): List<String> {
        return sort(Locale.ENGLISH, *names)
    }

    private fun sort(locale: Locale, vararg names: String): List<String> {
        return names.sortedWith(NaturalSort.comparator(locale))
    }

    private fun shuffleAndSort(names: List<String>): List<String> {
        return names.shuffled(Random(42)).sortedWith(NaturalSort.comparator(Locale.ENGLISH))
    }

    private fun loadNames(resourceName: String): List<String> {
        val classLoader = checkNotNull(javaClass.classLoader) { "Missing class loader" }
        val stream = checkNotNull(classLoader.getResourceAsStream("natural-sort/$resourceName")) {
            "Missing test resource natural-sort/$resourceName"
        }
        return stream.bufferedReader().use { reader ->
            reader.readLines().filter { it.isNotEmpty() }
        }
    }

    private fun numberedValues(names: List<String>, pattern: Regex): List<Int> {
        return names.mapNotNull { name ->
            pattern.find(name)?.groupValues?.get(1)?.toInt()
        }
    }

    private fun isNonDecreasing(values: List<Int>): Boolean {
        return values.zipWithNext().all { (left, right) -> left <= right }
    }

    private fun assertLexicographicSortBreaksNumericOrder(names: List<String>, pattern: Regex) {
        val lexicographicNumbers = numberedValues(
            names.sortedBy { it.lowercase() },
            pattern
        )
        assertFalse(
            "Lexicographic sort should not keep $pattern in numeric order",
            isNonDecreasing(lexicographicNumbers)
        )
    }

    companion object {
        private val DANISH = Locale.forLanguageTag("da-DK")

        // Fixtures are transcribed from FileSortingData.md (.mp3 basenames only).
        private val JULENS_MAGI = Regex("""julens magi (\d+) """)
        private val DECEMBER_EPISODE = Regex("""^(\d+)\. december""")
        private val JULESTJERNEN_EPISODE = Regex("""^(\d+)\. .+Julestjernen""")
        private val ADVENT_EPISODE = Regex("""^(\d+)_ """)
        private val AGENT_HUGO = Regex("""^Agent Hugo (\d+)_""")
        private val AGENTHULEN = Regex("""^Agenthulen (\d+)_""")
        private val PIRATLAERLINGEN_LILJA = Regex("""^Piratlærlingen Lilja (\d+)""")
    }
}
