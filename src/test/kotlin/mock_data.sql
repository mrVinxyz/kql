INSERT INTO wizards (id, name, level, power_level, mana_capacity, experience_points)
VALUES (1, 'Gandalf', 20, 150.5, 1000.0, 5000.0),
       (2, 'Merlin', 18, 140.2, 950.0, 4600.0),
       (3, 'Radagast', 15, 120.3, 850.0, 4300.0),
       (4, 'Saruman', 25, 160.7, 1100.0, 5500.0),
       (5, 'Albus', 22, 145.0, 980.0, 4800.0);

INSERT INTO spells (id, name, description, mana_cost, casting_time, damage, is_aoe, success_rate)
VALUES (1, 'Fireball', 'A powerful fire attack causing high damage', 50.0, 1.5, 200, false, 0.95),
       (2, 'Lightning Strike', 'A quick lightning bolt attack', 40.0, 1.0, 180, false, 0.98),
       (3, 'Healing Wave', 'A healing spell that restores health to allies', 30.0, 2.0, 0, true, 0.9),
       (4, 'Ice Storm', 'A freezing storm that causes damage and slows enemies', 60.0, 2.5, 220, true, 0.85),
       (5, 'Arcane Blast', 'A burst of magical energy with a moderate damage output', 45.0, 1.8, 150, false, 0.92),
       (6, 'Wind Gust', 'A blast of wind to knock back enemies', 35.0, 1.2, 100, true, 0.96),
       (7, 'Teleport', 'Instantly teleports the caster to a location', 25.0, 0.5, 0, false, 1.0),
       (8, 'Shadow Bolt', 'A bolt of dark energy that damages and weakens the target', 55.0, 1.7, 210, false, 0.89),
       (9, 'Earthquake', 'A spell that causes the ground to tremble and damage enemies', 70.0, 3.0, 250, true, 0.87),
       (10, 'Mana Shield', 'A protective shield that consumes mana to absorb damage', 40.0, 2.0, 0, false, 0.95);

INSERT INTO wizard_spells (wizard_id, spell_id)
VALUES (1, 1),
       (1, 2),
       (2, 3),
       (2, 4),
       (3, 5),
       (3, 6),
       (4, 7),
       (4, 8),
       (5, 9),
       (5, 10);