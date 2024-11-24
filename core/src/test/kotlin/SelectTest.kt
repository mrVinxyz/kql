import expr.OrderDirection
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertContentEquals

class SelectTest {
    @Test
    fun `test select all columns`() {
        val select = Select(WizardsTable)
            .selectAll()
            .sqlArgs()

        val expectedSql = "SELECT w.id, w.name, w.level, w.power_level, w.mana_capacity, " +
                "w.experience_points, w.guild, w.specialization, w.alignment, w.last_login " +
                "FROM wizards w"

        assertEquals(expectedSql, select.sql)
        assertTrue(select.args.isEmpty())
    }

    @Test
    fun `test select specific columns`() {
        val select = Select(WizardsTable)
            .select(
                WizardsTable.name,
                WizardsTable.level,
                WizardsTable.powerLevel
            )
            .sqlArgs()

        val expectedSql = "SELECT w.name, w.level, w.power_level FROM wizards w"
        assertEquals(expectedSql, select.sql)
        assertTrue(select.args.isEmpty())
    }

    @Test
    fun `test select with primary key`() {
        val select = Select(WizardsTable)
            .selectPrimary(
                1,
                WizardsTable.name,
                WizardsTable.level
            )
            .sqlArgs()

        val expectedSql = "SELECT w.name, w.level FROM wizards w WHERE w.id = ?"
        assertEquals(expectedSql, select.sql)
        assertEquals(1, select.args[0])
    }

    @Test
    fun `test select all with column exclusion`() {
        val select = Select(WizardsTable)
            .selectAll(
                WizardsTable.lastLogin,
                WizardsTable.experiencePoints
            )
            .sqlArgs()

        val expectedSql = "SELECT w.id, w.name, w.level, w.power_level, w.mana_capacity, " +
                "w.guild, w.specialization, w.alignment FROM wizards w"

        assertEquals(expectedSql, select.sql)
        assertTrue(select.args.isEmpty())
    }

    @Test
    fun `test count`() {
        val select = Select(WizardsTable)
            .count()
            .sqlArgs()

        val expectedSql = "SELECT COUNT(*) FROM wizards w"
        assertEquals(expectedSql, select.sql)
        assertTrue(select.args.isEmpty())
    }

    @Test
    fun `test count with where clause`() {
        val select = Select(WizardsTable)
            .count {
                where { WizardsTable.level gte 20 }
            }
            .sqlArgs()

        val expectedSql = "SELECT COUNT(*) FROM wizards w WHERE w.level >= ?"
        assertEquals(expectedSql, select.sql)
        assertEquals(20, select.args[0])
    }

    @Test
    fun `test exists`() {
        val select = Select(WizardsTable)
            .exists {
                where {
                    WizardsTable.guild eq "White Council"
                }
            }
            .sqlArgs()

        val expectedSql = "SELECT EXISTS (SELECT 1 FROM wizards w WHERE w.guild = ?)"
        assertEquals(expectedSql, select.sql)
        assertEquals("White Council", select.args[0])
    }

    @Test
    fun `test select exists`() {
        val select = Select(WizardsTable)
            .exists {
                where {
                    SpellsTable.element eq "Fire"
                }
            }
            .sqlArgs()

        assertEquals(
            "SELECT EXISTS (SELECT 1 FROM wizards w WHERE s.element = ?)",
            select.sql
        )
        assertEquals(listOf("Fire"), select.args)
    }

    @Test
    fun `test empty select columns throws exception`() {
        val exception = assertThrows<IllegalArgumentException> {
            Select(WizardsTable)
                .select()
                .sqlArgs()
        }
        assertEquals("select columns can not be empty", exception.message)
    }

    @Test
    fun `test paginate with valid values`() {
        val select = Select(WizardsTable)
            .selectAll()
            .paginate(page = 2, size = 10)
            .sqlArgs()

        val expectedSql = "SELECT w.id, w.name, w.level, w.power_level, w.mana_capacity, " +
                "w.experience_points, w.guild, w.specialization, w.alignment, w.last_login " +
                "FROM wizards w LIMIT ? OFFSET ?"

        assertEquals(expectedSql, select.sql)
        assertEquals(10, select.args[0])
        assertEquals(10, select.args[1])
    }

    @Test
    fun `test paginate with invalid values throws exception`() {
        assertThrows<IllegalArgumentException> {
            Select(WizardsTable).paginate(page = 0, size = 10)
        }
        assertThrows<IllegalArgumentException> {
            Select(WizardsTable).paginate(page = 1, size = 0)
        }
    }

    @Test
    fun `test ordering with multiple columns`() {
        val select = Select(WizardsTable)
            .selectAll()
            .orderBy(
                WizardsTable.level to OrderDirection.DESC,
                WizardsTable.powerLevel to OrderDirection.ASC
            )
            .sqlArgs()

        val expectedSql = "SELECT w.id, w.name, w.level, w.power_level, w.mana_capacity, " +
                "w.experience_points, w.guild, w.specialization, w.alignment, w.last_login " +
                "FROM wizards w ORDER BY w.level DESC, w.power_level ASC"

        assertEquals(expectedSql, select.sql)
        assertTrue(select.args.isEmpty())
    }

    @Test
    fun `test comparison expressions`() {
        val select = Select(SpellsTable)
            .selectAll()
            .where {
                SpellsTable.name eq "Fireball"
                SpellsTable.element eq "Fire"
                SpellsTable.tier lte 3
                SpellsTable.isBanned eq false
                SpellsTable.manaCost lt 100.0f
                SpellsTable.damage gt 50
                SpellsTable.successRate gte 75.0f
                SpellsTable.cooldown neq 0.0f
            }
            .sqlArgs()

        val expectedSql = "SELECT s.id, s.name, s.description, s.mana_cost, s.casting_time, " +
                "s.damage, s.is_aoe, s.success_rate, s.element, s.range, s.cooldown, " +
                "s.tier, s.is_banned FROM spells s " +
                "WHERE s.name = ? AND s.element = ? AND s.tier <= ? AND s.is_banned = ? " +
                "AND s.mana_cost < ? AND s.damage > ? AND s.success_rate >= ? AND s.cooldown <> ?"

        assertEquals(expectedSql, select.sql)
        assertContentEquals(listOf("Fireball", "Fire", 3, false, 100.0f, 50, 75.0f, 0.0f), select.args)

    }

    @Test
    fun `test logical expressions`() {
        val select = Select(SpellsTable)
            .selectAll()
            .where {
                group {
                    or {
                        SpellsTable.element eq "Fire"
                        SpellsTable.element eq "Ice"
                    }
                }
                group {
                    and {
                        SpellsTable.tier lte 3
                        SpellsTable.manaCost lt 100.0f
                    }
                }
                not {
                    SpellsTable.isBanned eq true
                }
            }
            .sqlArgs()

        val expectedSql = "SELECT s.id, s.name, s.description, s.mana_cost, s.casting_time, " +
                "s.damage, s.is_aoe, s.success_rate, s.element, s.range, s.cooldown, " +
                "s.tier, s.is_banned FROM spells s " +
                "WHERE (s.element = ? OR s.element = ?) AND " +
                "(s.tier <= ? AND s.mana_cost < ?) AND " +
                "NOT (s.is_banned = ?)"

        assertEquals(expectedSql, select.sql)
        assertContentEquals(listOf("Fire", "Ice", 3, 100.0f, true), select.args)
    }

    @Test
    fun `test like expressions`() {
        val select = Select(SpellsTable)
            .selectAll()
            .where {
                SpellsTable.name like "%ball"
                SpellsTable.description likeContains "damage"
                SpellsTable.element likeStarts "Fire"
                SpellsTable.name likeEnds "bolt"
                SpellsTable.description ilike "%POWERFUL%"
                SpellsTable.element notLike "Dark%"
            }
            .sqlArgs()

        val expectedSql = "SELECT s.id, s.name, s.description, s.mana_cost, s.casting_time, " +
                "s.damage, s.is_aoe, s.success_rate, s.element, s.range, s.cooldown, " +
                "s.tier, s.is_banned FROM spells s " +
                "WHERE s.name LIKE ? AND s.description LIKE ? AND s.element LIKE ? " +
                "AND s.name LIKE ? AND s.description ILIKE ? AND s.element NOT LIKE ?"

        assertEquals(expectedSql, select.sql)
        assertContentEquals(listOf("%ball", "%damage%", "Fire%", "%bolt", "%POWERFUL%", "Dark%"), select.args)
    }

    @Test
    fun `test in list expressions`() {
        val select = Select(SpellsTable)
            .selectAll()
            .where {
                SpellsTable.element inList listOf("Fire", "Ice", "Lightning")
                SpellsTable.tier notInList listOf(0, 1)
                SpellsTable.range inList listOf(5, 10, 15)
            }
            .sqlArgs()

        val expectedSql = "SELECT s.id, s.name, s.description, s.mana_cost, s.casting_time, " +
                "s.damage, s.is_aoe, s.success_rate, s.element, s.range, s.cooldown, " +
                "s.tier, s.is_banned FROM spells s " +
                "WHERE s.element IN (?, ?, ?) AND s.tier NOT IN (?, ?) AND s.range IN (?, ?, ?)"

        assertEquals(expectedSql, select.sql)
        assertContentEquals(listOf(50.0f, 150.0f, 100, 500, 75.0f, 100.0f, 1, 10), select.args)
    }

    @Test
    fun `test between expressions`() {
        val select = Select(SpellsTable)
            .selectAll()
            .where {
                SpellsTable.manaCost between (50.0f to 150.0f)
                SpellsTable.damage between (100 to 500)
                SpellsTable.successRate between (75.0f to 100.0f)
                SpellsTable.range between (1 to 10)
            }
            .sqlArgs()

        val expectedSql = "SELECT s.id, s.name, s.description, s.mana_cost, s.casting_time, " +
                "s.damage, s.is_aoe, s.success_rate, s.element, s.range, s.cooldown, " +
                "s.tier, s.is_banned FROM spells s " +
                "WHERE s.mana_cost BETWEEN ? AND ? AND s.damage BETWEEN ? AND ? " +
                "AND s.success_rate BETWEEN ? AND ? AND s.range BETWEEN ? AND ?"

        assertEquals(expectedSql, select.sql)
        assertContentEquals(listOf(50, 1000.0f, 10), select.args)
    }

    @Test
    fun `test null check expressions`() {
        val select = Select(WizardsTable)
            .selectAll()
            .where {
                WizardsTable.lastLogin.isNull()
                WizardsTable.guild.isNotNull()
                WizardsTable.specialization.isNotNull()
            }
            .sqlArgs()

        val expectedSql = "SELECT w.id, w.name, w.level, w.power_level, w.mana_capacity, " +
                "w.experience_points, w.guild, w.specialization, w.alignment, w.last_login " +
                "FROM wizards w " +
                "WHERE w.last_login IS NULL AND w.guild IS NOT NULL AND w.specialization IS NOT NULL"

        assertEquals(expectedSql, select.sql)
        assertTrue(select.args.isEmpty())
    }

    @Test
    fun `test complex where clauses`() {
        val select = Select(WizardsTable)
            .selectAll()
            .where {
                withOr {
                    WizardsTable.guild eq "White Council"
                    withAnd {
                        WizardsTable.level gt 50
                        WizardsTable.powerLevel gt 500.0f
                    }
                }

                WizardsTable.lastLogin.isNotNull()
            }
            .sqlArgs()

        val expectedSql = "SELECT w.id, w.name, w.level, w.power_level, w.mana_capacity, " +
                "w.experience_points, w.guild, w.specialization, w.alignment, w.last_login " +
                "FROM wizards w " +
                "WHERE (w.guild = ? OR (w.level > ? AND w.power_level > ?)) " +
                "AND w.last_login IS NOT NULL"

        assertEquals(expectedSql, select.sql)
        assertEquals("White Council", select.args[0])
        assertEquals(50, select.args[1])
    }

    @Test
    fun `test multiple join combinations with different join types`() {
        val firstJoinChain = Select(WizardsTable)
            .select(
                WizardsTable.name,
                WizardsTable.level,
                SpellsTable.name,
                WizardSpellsTable.proficiency
            )
            .join(WizardSpellsTable.proficiency) {
                WizardsTable.id inner WizardSpellsTable.wizardId
            }
            .join(SpellsTable.name) {
                WizardSpellsTable.spellId right SpellsTable.id
            }
            .sqlArgs()

        assertEquals(
            "SELECT w.name, w.level, s.name, ws.proficiency " +
                    "FROM wizards w " +
                    "INNER JOIN wizard_spells ws ON w.id = ws.wizard_id " +
                    "RIGHT JOIN spells s ON ws.spell_id = s.id",
            firstJoinChain.sql
        )

        val secondJoinChain = Select(WizardsTable)
            .select(
                WizardsTable.name,
                SpellsTable.name,
                WizardSpellsTable.timesCasted
            )
            .join(WizardSpellsTable.timesCasted) {
                WizardsTable.id left WizardSpellsTable.wizardId
            }
            .join(SpellsTable.name) {
                WizardSpellsTable.spellId full SpellsTable.id
            }
            .sqlArgs()

        assertEquals(
            "SELECT w.name, s.name, ws.times_casted " +
                    "FROM wizards w " +
                    "LEFT JOIN wizard_spells ws ON w.id = ws.wizard_id " +
                    "FULL JOIN spells s ON ws.spell_id = s.id",
            secondJoinChain.sql
        )

        val thirdJoinChain = Select(SpellsTable)
            .select(
                SpellsTable.name,
                WizardsTable.name,
                WizardSpellsTable.proficiency
            )
            .join(WizardSpellsTable.proficiency) {
                SpellsTable.id right WizardSpellsTable.spellId
            }
            .join(WizardsTable.name) {
                WizardSpellsTable.wizardId outer WizardsTable.id
            }
            .sqlArgs()

        assertEquals(
            "SELECT s.name, w.name, ws.proficiency " +
                    "FROM spells s " +
                    "RIGHT JOIN wizard_spells ws ON s.id = ws.spell_id " +
                    "OUTER JOIN wizards w ON ws.wizard_id = w.id",
            thirdJoinChain.sql
        )
    }
}