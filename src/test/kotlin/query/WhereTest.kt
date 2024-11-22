package query

import dialect.SQLiteDialect
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WhereTests {
    private val dialect = SQLiteDialect()

    private fun Where.toFragment(useTableAlias: Boolean = true) =
        expression?.accept(dialect, useTableAlias)

    @Test
    fun `test not equals comparison`() {
        val where = Where(WizardsTable) {
            WizardsTable.guild neq "Dark Magic"
        }.toFragment()

        assertEquals("w.guild <> ?", where?.sql)
        assertEquals(listOf("Dark Magic"), where?.args)
    }

    @Test
    fun `test less than comparison`() {
        val where = Where(WizardsTable) {
            WizardsTable.powerLevel lt 100.0f
        }.toFragment()

        assertEquals("w.power_level < ?", where?.sql)
        assertEquals(listOf(100.0f), where?.args)
    }

    @Test
    fun `test less than or equal comparison`() {
        val where = Where(WizardsTable) {
            WizardsTable.manaCapacity lte 500.0f
        }.toFragment()

        assertEquals("w.mana_capacity <= ?", where?.sql)
        assertEquals(listOf(500.0f), where?.args)
    }

    @Test
    fun `test greater than comparison`() {
        val where = Where(SpellsTable) {
            SpellsTable.damage gt 50
        }.toFragment()

        assertEquals("s.damage > ?", where?.sql)
        assertEquals(listOf(50), where?.args)
    }

    @Test
    fun `test greater than or equal comparison`() {
        val where = Where(SpellsTable) {
            SpellsTable.successRate gte 0.95f
        }.toFragment()

        assertEquals("s.success_rate >= ?", where?.sql)
        assertEquals(listOf(0.95f), where?.args)
    }

    @Test
    fun `test null value handling`() {
        val nullLevel: Int? = null
        val where = Where(WizardsTable) {
            WizardsTable.level eq nullLevel
        }.toFragment()

        assertNull(where)
    }

    @Test
    fun `test LIKE exact match`() {
        val where = Where(WizardsTable) {
            WizardsTable.specialization like "Pyromancer"
        }.toFragment()

        assertEquals("w.specialization LIKE ?", where?.sql)
        assertEquals(listOf("Pyromancer"), where?.args)
    }

    @Test
    fun `test LIKE contains pattern`() {
        val where = Where(SpellsTable) {
            SpellsTable.description likeContains "fire"
        }.toFragment()

        assertEquals("s.description LIKE ?", where?.sql)
        assertEquals(listOf("%fire%"), where?.args)
    }

    @Test
    fun `test LIKE starts with pattern`() {
        val where = Where(WizardsTable) {
            WizardsTable.name likeStarts "Dark"
        }.toFragment()

        assertEquals("w.name LIKE ?", where?.sql)
        assertEquals(listOf("Dark%"), where?.args)
    }

    @Test
    fun `test LIKE ends with pattern`() {
        val where = Where(SpellsTable) {
            SpellsTable.name likeEnds "bolt"
        }.toFragment()

        assertEquals("s.name LIKE ?", where?.sql)
        assertEquals(listOf("%bolt"), where?.args)
    }

    @Test
    fun `test ILIKE operation`() {
        val where = Where(WizardsTable) {
            WizardsTable.alignment ilike "neutral"
        }.toFragment()

        assertEquals("w.alignment ILIKE ?", where?.sql)
        assertEquals(listOf("neutral"), where?.args)
    }

    @Test
    fun `test NOT LIKE operation`() {
        val where = Where(SpellsTable) {
            SpellsTable.element notLike "dark"
        }.toFragment()

        assertEquals("s.element NOT LIKE ?", where?.sql)
        assertEquals(listOf("dark"), where?.args)
    }

    @Test
    fun `test IN list`() {
        val elements = listOf("fire", "water", "earth")
        val where = Where(SpellsTable) {
            SpellsTable.element inList elements
        }.toFragment()

        assertEquals("s.element IN (?, ?, ?)", where?.sql)
        assertEquals(elements, where?.args)
    }

    @Test
    fun `test NOT IN list`() {
        val tiers = listOf(1, 2)
        val where = Where(SpellsTable) {
            SpellsTable.tier notInList tiers
        }.toFragment()

        assertEquals("s.tier NOT IN (?, ?)", where?.sql)
        assertEquals(tiers, where?.args)
    }

    @Test
    fun `test empty list handling`() {
        val emptyList = emptyList<String>()
        val where = Where(WizardsTable) {
            WizardsTable.guild inList emptyList
        }.toFragment()

        assertNull(where)
    }

    @Test
    fun `test null list handling`() {
        val nullList: List<Int>? = null
        val where = Where(SpellsTable) {
            SpellsTable.tier inList nullList
        }.toFragment()

        assertNull(where)
    }

    @Test
    fun `test IS NULL condition`() {
        val where = Where(WizardsTable) {
            WizardsTable.lastLogin.isNull()
        }.toFragment()

        assertEquals("w.last_login IS NULL", where?.sql)
        assertEquals(emptyList<Any>(), where?.args)
    }

    @Test
    fun `test IS NOT NULL condition`() {
        val where = Where(SpellsTable) {
            SpellsTable.description.isNotNull()
        }.toFragment()

        assertEquals("s.description IS NOT NULL", where?.sql)
        assertEquals(emptyList<Any>(), where?.args)
    }

    @Test
    fun `test BETWEEN with valid range`() {
        val where = Where(WizardsTable) {
            WizardsTable.level between (1 to 10)
        }.toFragment()

        assertEquals("w.level BETWEEN ? AND ?", where?.sql)
        assertEquals(listOf(1, 10), where?.args)
    }

    @Test
    fun `test BETWEEN with null boundaries`() {
        val range: Pair<Int?, Int?> = null to 10
        val where = Where(WizardsTable) {
            WizardsTable.level between range
        }.toFragment()

        assertNull(where)
    }

    @Test
    fun `test BETWEEN with invalid range`() {
        val nullRange: Pair<Int?, Int?>? = null
        val where = Where(WizardsTable) {
            WizardsTable.level between nullRange
        }.toFragment()

        assertNull(where)
    }

    @Test
    fun `test AND combinations`() {
        val where = Where(WizardsTable) {
            WizardsTable.level gte 5
            and {
                WizardsTable.powerLevel gt 100.0f
                WizardsTable.manaCapacity gt 200.0f
            }
        }.toFragment()

        assertEquals("w.level >= ? AND w.power_level > ? AND w.mana_capacity > ?", where?.sql)
        assertEquals(listOf(5, 100.0f, 200.0f), where?.args)
    }

    @Test
    fun `test OR combinations`() {
        val where = Where(SpellsTable) {
            or {
                SpellsTable.element eq "fire"
                SpellsTable.element eq "water"
            }
        }.toFragment()

        assertEquals("s.element = ? OR s.element = ?", where?.sql)
        assertEquals(listOf("fire", "water"), where?.args)
    }

    @Test
    fun `test NOT conditions`() {
        val where = Where(SpellsTable) {
            not {
                SpellsTable.isBanned eq true
            }
        }.toFragment()

        assertEquals("NOT (s.is_banned = ?)", where?.sql)
        assertEquals(listOf(true), where?.args)
    }

    @Test
    fun `test complex nested conditions`() {
        val where = Where(WizardsTable) {
            and {
                WizardsTable.level gte 10
                or {
                    WizardsTable.guild eq "Fire Mages"
                    WizardsTable.guild eq "Water Mages"
                }
                not {
                    WizardsTable.alignment eq "Evil"
                }
            }
        }.toFragment()

        assertEquals(
            "w.level >= ? AND (w.guild = ? OR w.guild = ?) AND NOT (w.alignment = ?)",
            where?.sql
        )
        assertEquals(listOf(10, "Fire Mages", "Water Mages", "Evil"), where?.args)
    }

    @Test
    fun `test group conditions`() {
        val where = Where(WizardsTable) {
            group {
                WizardsTable.level gt 5
                WizardsTable.powerLevel gt 100.0f
            }
        }.toFragment()

        assertEquals("(w.level > ? AND w.power_level > ?)", where?.sql)
        assertEquals(listOf(5, 100.0f), where?.args)
    }

    @Test
    fun `test withOr conditions`() {
        val where = Where(WizardsTable) {
            withOr {
                WizardsTable.level gt 50
                WizardsTable.powerLevel gt 1000.0f
                WizardsTable.guild eq "Archmages"
            }
        }.toFragment()

        assertEquals("(w.level > ? OR w.power_level > ? OR w.guild = ?)", where?.sql)
        assertEquals(listOf(50, 1000.0f, "Archmages"), where?.args)
    }

    @Test
    fun `test withAnd conditions`() {
        val where = Where(WizardsTable) {
            withAnd {
                WizardsTable.powerLevel gt 800.0f
                WizardsTable.manaCapacity gt 600.0f
                WizardsTable.experiencePoints gte 1000.0f
            }
        }.toFragment()

        assertEquals("(w.power_level > ? AND w.mana_capacity > ? AND w.experience_points >= ?)", where?.sql)
        assertEquals(listOf(800.0f, 600.0f, 1000.0f), where?.args)
    }
}