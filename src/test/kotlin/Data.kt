import query.Executor
import query.Row
import query.exec
import query.schema.Table
import query.schema.createTable
import java.math.BigDecimal
import java.sql.Connection

class WizardsTable : Table("wizards") {
    val id = integer("id").setPrimaryKey()
    val name = text("name")
    val level = integer("level")
    val powerLevel = float("power_level")
    val manaCapacity = double("mana_capacity")
    val experiencePoints = double("experience_points")
}

class SpellsTable : Table("spells") {
    val id = integer("id").setPrimaryKey()
    val name = text("name")
    val description = text("description")
    val manaCost = double("mana_cost")
    val castingTime = double("casting_time")
    val damage = integer("damage")
    val isAoe = boolean("is_aoe")
    val successRate = double("success_rate")
}

class WizardSpellsTable : Table("wizard_spells") {
    val wizardId = integer("wizard_id")
    val spellId = integer("spell_id")
}
