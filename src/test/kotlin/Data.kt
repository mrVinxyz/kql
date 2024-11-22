import dialect.SQLiteDialect
import schema.Table

object WizardsTable : Table("wizards", SQLiteDialect()) {
    val id = integer("id").setPrimaryKey()
    val name = text("name")
    val level = integer("level")
    val powerLevel = float("power_level")
    val manaCapacity = float("mana_capacity")
    val experiencePoints = float("experience_points")
    val guild = text("guild")
    val specialization = text("specialization")
    val alignment = text("alignment")
    val lastLogin = dateText("last_login")
}

object SpellsTable : Table("spells", SQLiteDialect()) {
    val id = integer("id").setPrimaryKey()
    val name = text("name")
    val description = text("description")
    val manaCost = float("mana_cost")
    val castingTime = float("casting_time")
    val damage = integer("damage")
    val isAoe = boolean("is_aoe")
    val successRate = float("success_rate")
    val element = text("element")
    val range = integer("range")
    val cooldown = float("cooldown")
    val tier = integer("tier")
    val isBanned = boolean("is_banned")
}

object WizardSpellsTable : Table("wizard_spells", SQLiteDialect()) {
    val wizardId = integer("wizard_id")
    val spellId = integer("spell_id")
    val proficiency = integer("proficiency")
    val dateLearned = timestamp("date_learned")
    val timesCasted = integer("times_casted")
}
