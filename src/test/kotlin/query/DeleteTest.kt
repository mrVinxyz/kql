package query

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class DeleteTest {
    @Test
    fun `test delete with primary key`() {
        val delete = Delete(WizardsTable)
            .deletePrimary(1)
            .sqlArgs()

        val expectedSql = "DELETE FROM wizards WHERE id = ?"
        assertEquals(expectedSql, delete.sql)
        assertContentEquals(listOf(1), delete.args)
    }

    @Test
    fun `test delete with where condition`() {
        val delete = Delete(WizardsTable)
            .deleteWhere {
                WizardsTable.level lt 10
                WizardsTable.lastLogin.isNull()
            }
            .sqlArgs()

        val expectedSql = "DELETE FROM wizards WHERE level < ? AND last_login IS NULL"
        assertEquals(expectedSql, delete.sql)
        assertContentEquals(listOf(10), delete.args)
    }

    @Test
    fun `test delete with complex where conditions`() {
        val delete = Delete(WizardsTable)
            .deleteWhere {
                group {
                    WizardsTable.level lt 5
                    withOr {
                        WizardsTable.experiencePoints lt 100.0f
                        WizardsTable.lastLogin.isNull()
                    }
                }
                not {
                    WizardsTable.guild eq "Fire Mages"
                }
            }
            .sqlArgs()

        val expectedSql = "DELETE FROM wizards WHERE (level < ? AND " +
                "(experience_points < ? OR last_login IS NULL)) AND " +
                "NOT (guild = ?)"
        assertEquals(expectedSql, delete.sql)
        assertContentEquals(listOf(5, 100.0f, "Fire Mages"), delete.args)
    }
}