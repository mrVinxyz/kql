import org.junit.jupiter.api.Test
import query.Where
import kotlin.test.assertEquals

class WhereTest {
    private val dialect = SQLiteDialect()

    private fun Where.toFragment(useTableAlias: Boolean = true) =
        expression?.accept(dialect, useTableAlias)

    @Test
    fun `test not equals comparison`() {
        val nullGuild: String? = null
        val where = Where(WizardsTable) {
            WizardsTable.guild neq "Dark Magic"
            nullable {
                WizardsTable.specialization neq nullGuild
            }
        }.toFragment()

        assertEquals("w.guild <> ?", where?.sql)
        assertEquals(listOf("Dark Magic"), where?.args)
    }

    @Test
    fun `test less than comparison`() {
        val nullPower: Float? = null
        val where = Where(WizardsTable) {
            WizardsTable.powerLevel lt 100.0f
            nullable {
                WizardsTable.manaCapacity lt nullPower
            }
        }.toFragment()

        assertEquals("w.power_level < ?", where?.sql)
        assertEquals(listOf(100.0f), where?.args)
    }

    @Test
    fun `test less than or equal comparison`() {
        val nullMana: Float? = null
        val where = Where(WizardsTable) {
            WizardsTable.manaCapacity lte 500.0f
            nullable {
                WizardsTable.powerLevel lte nullMana
            }
        }.toFragment()

        assertEquals("w.mana_capacity <= ?", where?.sql)
        assertEquals(listOf(500.0f), where?.args)
    }

    @Test
    fun `test greater than comparison`() {
        val nullDamage: Int? = null
        val where = Where(SpellsTable) {
            SpellsTable.damage gt 50
            nullable {
                SpellsTable.tier gt nullDamage
            }
        }.toFragment()

        assertEquals("s.damage > ?", where?.sql)
        assertEquals(listOf(50), where?.args)
    }

    @Test
    fun `test greater than or equal comparison`() {
        val nullRate: Float? = null
        val where = Where(SpellsTable) {
            SpellsTable.successRate gte 0.95f
            nullable {
                SpellsTable.cooldown gte nullRate
            }
        }.toFragment()

        assertEquals("s.success_rate >= ?", where?.sql)
        assertEquals(listOf(0.95f), where?.args)
    }

    @Test
    fun `test LIKE exact match`() {
        val nullSpec: String? = null
        val where = Where(WizardsTable) {
            WizardsTable.specialization like "Pyromancer"
            nullable {
                WizardsTable.guild like nullSpec
            }
        }.toFragment()

        assertEquals("w.specialization LIKE ?", where?.sql)
        assertEquals(listOf("Pyromancer"), where?.args)
    }

    @Test
    fun `test LIKE contains pattern`() {
        val nullDesc: String? = null
        val where = Where(SpellsTable) {
            SpellsTable.description likeContains "fire"
            nullable {
                SpellsTable.name likeContains nullDesc
            }
        }.toFragment()

        assertEquals("s.description LIKE ?", where?.sql)
        assertEquals(listOf("%fire%"), where?.args)
    }

    @Test
    fun `test LIKE starts with pattern`() {
        val nullPattern: String? = null
        val where = Where(WizardsTable) {
            WizardsTable.name likeStarts "Dark"
            nullable {
                WizardsTable.guild likeStarts nullPattern
            }
        }.toFragment()

        assertEquals("w.name LIKE ?", where?.sql)
        assertEquals(listOf("Dark%"), where?.args)
    }

    @Test
    fun `test LIKE ends with pattern`() {
        val nullPattern: String? = null
        val where = Where(SpellsTable) {
            SpellsTable.name likeEnds "bolt"
            nullable {
                SpellsTable.element likeEnds nullPattern
            }
        }.toFragment()

        assertEquals("s.name LIKE ?", where?.sql)
        assertEquals(listOf("%bolt"), where?.args)
    }

    @Test
    fun `test ILIKE operation`() {
        val nullAlign: String? = null
        val where = Where(WizardsTable) {
            WizardsTable.alignment ilike "neutral"
            nullable {
                WizardsTable.specialization ilike nullAlign
            }
        }.toFragment()

        assertEquals("w.alignment ILIKE ?", where?.sql)
        assertEquals(listOf("neutral"), where?.args)
    }

    @Test
    fun `test NOT LIKE operation`() {
        val nullElement: String? = null
        val where = Where(SpellsTable) {
            SpellsTable.element notLike "dark"
            nullable {
                SpellsTable.name notLike nullElement
            }
        }.toFragment()

        assertEquals("s.element NOT LIKE ?", where?.sql)
        assertEquals(listOf("dark"), where?.args)
    }

    @Test
    fun `test IN list`() {
        val nullElements: List<String>? = null
        val elements = listOf("fire", "water", "earth")
        val where = Where(SpellsTable) {
            SpellsTable.element inList elements
            nullable {
                SpellsTable.name inList nullElements
            }
        }.toFragment()

        assertEquals("s.element IN (?, ?, ?)", where?.sql)
        assertEquals(elements, where?.args)
    }

    @Test
    fun `test NOT IN list`() {
        val nullTiers: List<Int>? = null
        val tiers = listOf(1, 2)
        val where = Where(SpellsTable) {
            SpellsTable.tier notInList tiers
            nullable {
                SpellsTable.tier notInList nullTiers
            }
        }.toFragment()

        assertEquals("s.tier NOT IN (?, ?)", where?.sql)
        assertEquals(tiers, where?.args)
    }

    @Test
    fun `test BETWEEN with valid range`() {
        val nullRange: Pair<Int?, Int?>? = null
        val where = Where(WizardsTable) {
            WizardsTable.level between (1 to 10)
            nullable {
                WizardsTable.level between nullRange
            }
        }.toFragment()

        assertEquals("w.level BETWEEN ? AND ?", where?.sql)
        assertEquals(listOf(1, 10), where?.args)
    }

    @Test
    fun `test AND group combination`() {
        val nullGuild: String? = null
        val where = Where(WizardsTable) {
            WizardsTable.level gte 5
            and {
                WizardsTable.powerLevel gt 100.0f
                WizardsTable.manaCapacity gt 200.0f
                nullable {
                    WizardsTable.guild eq nullGuild
                }
            }
        }.toFragment()

        assertEquals("w.level >= ? AND (w.power_level > ? AND w.mana_capacity > ?)", where?.sql)
        assertEquals(listOf(5, 100.0f, 200.0f), where?.args)
    }

    @Test
    fun `test OR group combination`() {
        val nullElement: String? = null
        val where = Where(SpellsTable) {
            or {
                SpellsTable.element eq "fire"
                SpellsTable.element eq "water"
                nullable {
                    SpellsTable.name eq nullElement
                }
            }
        }.toFragment()

        assertEquals("(s.element = ? OR s.element = ?)", where?.sql)
        assertEquals(listOf("fire", "water"), where?.args)
    }

    @Test
    fun `test NOT conditions`() {
        val nullBanned: Boolean? = null
        val where = Where(SpellsTable) {
            not {
                SpellsTable.isBanned eq true
                nullable {
                    SpellsTable.isBanned eq nullBanned
                }
            }
        }.toFragment()

        assertEquals("NOT (s.is_banned = ?)", where?.sql)
        assertEquals(listOf(true), where?.args)
    }

    @Test
    fun `test GROUP conditions`() {
        val nullPower: Float? = null
        val where = Where(WizardsTable) {
            and {
                WizardsTable.level gt 5
                WizardsTable.powerLevel gt 100.0f
                nullable {
                    WizardsTable.manaCapacity gt nullPower
                }
            }
        }.toFragment()

        assertEquals("(w.level > ? AND w.power_level > ?)", where?.sql)
        assertEquals(listOf(5, 100.0f), where?.args)
    }

    @Test
    fun `test complex nested conditions`() {
        val nullGuild: String? = null
        val where = Where(WizardsTable) {
            WizardsTable.level gte 10
            or {
                WizardsTable.guild eq "Fire Mages"
                WizardsTable.guild eq "Water Mages"
                nullable {
                    WizardsTable.specialization eq nullGuild
                }
            }
            not {
                WizardsTable.alignment eq "Evil"
            }
        }.toFragment()

        assertEquals(
            "w.level >= ? AND (w.guild = ? OR w.guild = ?) AND NOT (w.alignment = ?)",
            where?.sql
        )
        assertEquals(listOf(10, "Fire Mages", "Water Mages", "Evil"), where?.args)
    }
}