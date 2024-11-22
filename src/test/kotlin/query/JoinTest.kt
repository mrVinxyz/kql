package query

import SpellsTable
import WizardsTable
import dialect.SQLiteDialect
import kotlin.test.Test
import org.junit.jupiter.api.Assertions.assertEquals

class JoinTest {
    private val dialect = SQLiteDialect()

    private fun Join.toFragment(useTableAlias: Boolean = true) =
        currentJoin?.accept(dialect, useTableAlias)

    @Test
    fun `test INNER JOIN operation`() {
        val join = Join(WizardSpellsTable, null, WizardsTable.name, SpellsTable.name) {
            WizardSpellsTable.wizardId inner WizardsTable.id
        }

        val sqlFragment = join?.toFragment()
        assertEquals(
            "INNER JOIN wizards w ON ws.wizard_id = w.id",
            sqlFragment?.sql
        )
    }

    @Test
    fun `test LEFT JOIN operation`() {
        val join = Join(
            WizardSpellsTable,
            null,
            WizardsTable.name,
            WizardsTable.level,
            SpellsTable.name,
            SpellsTable.manaCost
        ) {
            WizardSpellsTable.spellId left SpellsTable.id
        }

        val sqlFragment = join?.toFragment()
        assertEquals(
            "LEFT JOIN spells s ON ws.spell_id = s.id",
            sqlFragment?.sql
        )
    }

    @Test
    fun `test FULL JOIN operation`() {
        val join = Join(
            SpellsTable,
            null,
            WizardsTable.name,
            SpellsTable.name,
            WizardSpellsTable.proficiency
        ) {
            SpellsTable.id full WizardSpellsTable.spellId
        }

        val sqlFragment = join?.toFragment()
        assertEquals(
            "FULL JOIN wizard_spells ws ON s.id = ws.spell_id",
            sqlFragment?.sql
        )
    }

    @Test
    fun `test RIGHT JOIN operation`() {
        val join = Join(
            WizardSpellsTable,
            null,
            WizardsTable.name,
            WizardsTable.level,
            WizardSpellsTable.proficiency,
            WizardSpellsTable.timesCasted
        ) {
            WizardsTable.id right WizardSpellsTable.wizardId
        }

        val sqlFragment = join?.toFragment()
        assertEquals(
            "RIGHT JOIN wizards w ON w.id = ws.wizard_id",
            sqlFragment?.sql
        )
    }

    @Test
    fun `test INNER JOIN operation with multiple columns`() {
        val join = Join(
            WizardSpellsTable,
            null,
            WizardsTable.name,
            WizardsTable.level,
            WizardsTable.guild,
            SpellsTable.name,
            SpellsTable.manaCost,
            SpellsTable.damage,
            WizardSpellsTable.proficiency,
            WizardSpellsTable.timesCasted
        ) {
            WizardSpellsTable.spellId inner SpellsTable.id
        }

        val sqlFragment = join?.toFragment()
        assertEquals(
            "INNER JOIN spells s ON ws.spell_id = s.id",
            sqlFragment?.sql
        )
    }

    @Test
    fun `test INNER JOIN operation without table alias`() {
        val join = Join(
            SpellsTable,
            null,
            SpellsTable.name,
            SpellsTable.manaCost,
            SpellsTable.damage,
            WizardSpellsTable.timesCasted
        ) {
            SpellsTable.id inner WizardSpellsTable.spellId
        }

        val sqlFragment = join?.toFragment(useTableAlias = false)
        assertEquals(
            "INNER JOIN wizard_spells ON id = spell_id",
            sqlFragment?.sql
        )
    }

    @Test
    fun `test LEFT JOIN operation with null check potential`() {
        val join = Join(
            SpellsTable,
            null,
            SpellsTable.name,
            SpellsTable.description,
            SpellsTable.manaCost,
            WizardSpellsTable.wizardId
        ) {
            SpellsTable.id left WizardSpellsTable.spellId
        }

        val sqlFragment = join?.toFragment()
        assertEquals(
            "LEFT JOIN wizard_spells ws ON s.id = ws.spell_id",
            sqlFragment?.sql
        )
    }

    @Test
    fun `test INNER JOIN operation with single column`() {
        val join = Join(WizardSpellsTable, null, WizardsTable.name) {
            WizardsTable.id inner WizardSpellsTable.wizardId
        }

        val sqlFragment = join?.toFragment()
        assertEquals(
            "INNER JOIN wizards w ON w.id = ws.wizard_id",
            sqlFragment?.sql
        )
    }

    @Test
    fun `test INNER JOIN operation with all spell columns`() {
        val join = Join(
            SpellsTable,
            null,
            SpellsTable.name,
            SpellsTable.description,
            SpellsTable.manaCost,
            SpellsTable.damage,
            SpellsTable.tier,
            SpellsTable.element
        ) {
            SpellsTable.id inner WizardSpellsTable.spellId
        }

        val sqlFragment = join?.toFragment()
        assertEquals(
            "INNER JOIN wizard_spells ws ON s.id = ws.spell_id",
            sqlFragment?.sql
        )
    }
}