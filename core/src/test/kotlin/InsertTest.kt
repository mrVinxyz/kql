import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import org.junit.jupiter.api.assertThrows
import query.Insert
import query.insert
import kotlin.test.Test

class InsertTest {
    @Test
    fun `test insert with infix to operator`() {
        val insert = Insert(WizardsTable)
            .insert {
                WizardsTable.name to "Merlin"
                WizardsTable.level to 50
                WizardsTable.powerLevel to 999.9f
                WizardsTable.guild to "White Council"
            }
            .sqlArgs()

        val expectedSql = "INSERT INTO wizards (name, level, power_level, guild) VALUES (?, ?, ?, ?)"
        assertEquals(expectedSql, insert.sql)
        assertContentEquals(listOf("Merlin", 50, 999.9f, "White Council"), insert.args)
    }

    @Test
    fun `test insert using extension function`() {
        val insert = WizardsTable.insert {
            WizardsTable.name to "Gandalf"
            WizardsTable.level to 100
            WizardsTable.manaCapacity to 1000.0f
        }.sqlArgs()

        val expectedSql = "INSERT INTO wizards (name, level, mana_capacity) VALUES (?, ?, ?)"
        assertEquals(expectedSql, insert.sql)
        assertContentEquals(listOf("Gandalf", 100, 1000.0f), insert.args)
    }

    @Test
    fun `test insert with empty columns throws exception`() {
        val exception = assertThrows<IllegalArgumentException> {
            Insert(WizardsTable)
                .insert { }
                .sqlArgs()
        }
        assertEquals("No columns specified for insert", exception.message)
    }

    @Test
    fun `test insert with null values are ignored`() {
        val insert = WizardsTable.insert {
            WizardsTable.name to "Harry"
            WizardsTable.level to 5
            WizardsTable.guild to null
            WizardsTable.specialization to null
        }.sqlArgs()

        val expectedSql = "INSERT INTO wizards (name, level) VALUES (?, ?)"
        assertEquals(expectedSql, insert.sql)
        assertContentEquals(listOf("Harry", 5), insert.args)
    }
}