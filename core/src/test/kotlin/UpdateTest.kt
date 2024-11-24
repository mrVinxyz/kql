import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.to

class UpdateTest {
    @Test
    fun `test simple update with single column`() {
        val update = Update(WizardsTable)
            .update {
                WizardsTable.level to 5
            }
            .sqlArgs()

        val expectedSql = "UPDATE wizards SET level = ?"
        assertEquals(expectedSql, update.sql)
        assertContentEquals(listOf(5), update.args)
    }

    @Test
    fun `test update with multiple columns`() {
        val update = Update(WizardsTable)
            .update {
                WizardsTable.level to 10
                WizardsTable.powerLevel to 150.0f
                WizardsTable.guild to "Fire Mages"
            }
            .sqlArgs()

        val expectedSql = "UPDATE wizards SET level = ?, power_level = ?, guild = ?"
        assertEquals(expectedSql, update.sql)
        assertContentEquals(listOf(10, 150.0f, "Fire Mages"), update.args)
    }

    @Test
    fun `test update with where clause`() {
        val update = Update(WizardsTable)
            .update {
                WizardsTable.level to 15
                WizardsTable.experiencePoints to 1000.0f
            }
            .where {
                WizardsTable.guild eq "Novice"
                WizardsTable.level lt 10
            }
            .sqlArgs()

        val expectedSql = "UPDATE wizards SET level = ?, experience_points = ? WHERE guild = ? AND level < ?"
        assertEquals(expectedSql, update.sql)
        assertContentEquals(listOf(15, 1000.0f, "Novice", 10), update.args)
    }

    @Test
    fun `test update with primary key`() {
        val update = Update(WizardsTable)
            .updatePrimary(1) {
                WizardsTable.level to 20
                WizardsTable.manaCapacity to 200.0f
            }
            .sqlArgs()

        val expectedSql = "UPDATE wizards SET level = ?, mana_capacity = ? WHERE id = ?"
        assertEquals(expectedSql, update.sql)
        assertContentEquals(listOf(20, 200.0f, 1), update.args)
    }

    @Test
    fun `test update with complex where conditions`() {
        val update = Update(WizardsTable)
            .update {
                WizardsTable.level to 25
                WizardsTable.powerLevel to 300.0f
            }
            .where {
                group {
                    WizardsTable.level lt 20
                    withOr {
                        WizardsTable.guild eq "Apprentice"
                        WizardsTable.experiencePoints lt 500.0f
                    }
                }
                not {
                    WizardsTable.lastLogin.isNull()
                }
            }
            .sqlArgs()

        val expectedSql = "UPDATE wizards SET level = ?, power_level = ? " +
                "WHERE (level < ? AND (guild = ? OR experience_points < ?)) " +
                "AND NOT (last_login IS NULL)"
        assertEquals(expectedSql, update.sql)
        assertContentEquals(listOf(25, 300.0f, 20, "Apprentice", 500.0f), update.args)
    }

    @Test
    fun `test update with null values`() {
        val update = Update(WizardsTable)
            .update {
                WizardsTable.guild to null
                WizardsTable.specialization to null
            }
            .where {
                WizardsTable.lastLogin.isNull()
            }
            .sqlArgs()

        val expectedSql =
            "UPDATE wizards SET guild = COALESCE(?, guild), specialization = COALESCE(?, specialization) " +
                    "WHERE last_login IS NULL"
        assertEquals(expectedSql, update.sql)
        assertContentEquals(listOf(null, null), update.args)
    }

    @Test
    fun `test update with both setter styles`() {
        val update = Update(WizardsTable)
            .update {
                set(WizardsTable.level, 30)
                WizardsTable.powerLevel to 400.0f
            }
            .sqlArgs()

        val expectedSql = "UPDATE wizards SET level = ?, power_level = ?"
        assertEquals(expectedSql, update.sql)
        assertContentEquals(listOf(30, 400.0f), update.args)
    }

    @Test
    fun `test update with no columns throws exception`() {
        val exception = assertThrows<IllegalArgumentException> {
            Update(WizardsTable)
                .update { }
                .sqlArgs()
        }
        assertEquals("No columns specified for update", exception.message)
    }

    @Test
    fun `test update with like conditions`() {
        val update = Update(WizardsTable)
            .update {
                WizardsTable.level to 35
            }
            .where {
                WizardsTable.name likeStarts "Dark"
                WizardsTable.guild likeEnds "Council"
            }
            .sqlArgs()

        val expectedSql = "UPDATE wizards SET level = ? WHERE name LIKE ? AND guild LIKE ?"
        assertEquals(expectedSql, update.sql)
        assertContentEquals(listOf(35, "Dark%", "%Council"), update.args)
    }

    @Test
    fun `test update with in list condition`() {
        val update = Update(WizardsTable)
            .update {
                WizardsTable.level to 40
                WizardsTable.powerLevel to 500.0f
            }
            .where {
                WizardsTable.guild inList listOf("Fire Mages", "Ice Mages", "Storm Mages")
            }
            .sqlArgs()

        val expectedSql = "UPDATE wizards SET level = ?, power_level = ? WHERE guild IN (?, ?, ?)"
        assertEquals(expectedSql, update.sql)
        assertContentEquals(listOf(40, 500.0f, "Fire Mages", "Ice Mages", "Storm Mages"), update.args)
    }

    @Test
    fun `test update with between condition`() {
        val update = Update(WizardsTable)
            .update {
                WizardsTable.level to 45
            }
            .where {
                WizardsTable.powerLevel between (200.0f to 400.0f)
                WizardsTable.experiencePoints between (1000.0f to 2000.0f)
            }
            .sqlArgs()

        val expectedSql =
            "UPDATE wizards SET level = ? WHERE power_level BETWEEN ? AND ? AND experience_points BETWEEN ? AND ?"
        assertEquals(expectedSql, update.sql)
        assertContentEquals(listOf(45, 200.0f, 400.0f, 1000.0f, 2000.0f), update.args)
    }
}