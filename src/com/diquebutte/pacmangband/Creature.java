 package com.diquebutte.pacmangband;

import java.awt.Color;

public class Creature {
	private World world;
	private char glyph;
	private Color color;
	private CreatureAi ai;
	private int maxHp;
	private int hp;
	private int attackValue;
	private int defenseValue;
	private String name;
	public int visionRadius;
	private Inventory inventory;
	private int maxFood;
	private int food;
	private Item weapon;
	private Item armour;
	private int xp;
	private int level;
	
	public int x;
	public int y;
	public int z;
	
	public Creature(World world, char glyph, Color color, int maxHp, int attackValue, int defenseValue, String name) {
		this.world = world;
		this.glyph = glyph;
		this.color = color;
		this.hp = maxHp;
		this.maxHp = maxHp;
		this.visionRadius = 9;
		this.attackValue = attackValue;
		this.defenseValue = defenseValue;
		this.name = name;
		this.inventory = new Inventory(20);
		this.maxFood = 1000;
		this.food = maxFood / 3 * 2;
	}
	
	public void gainMaxHp() {
		maxHp += 10;
		hp += 10;
		doAction("Health increased.");
	}
	
	public void gainAttackValue() {
		attackValue += 2;
		doAction("Attack increased.");
	}
	
	public void gainDefenseValue() {
		defenseValue += 2;
		doAction("Defense increased.");
	}
	
	public void gainVision() {
		visionRadius += 1;
		doAction("Perception increased.");
	}
	
	public void modifyXp(int amount) {
		xp += amount;
		notify("You %s %d xp.", amount < 0 ? "lose" : "gain", amount);
		while(xp > (int)(Math.pow(level, 1.5) * 20)) {
			level++;
			doAction("advance to level %d", level);
			ai.onGainLevel();
			modifyHp(level * 2);
		}
	}
	
	public void gainXp(Creature other) {
		int amount = other.maxHp
				+ other.attackValue()
				+ other.defenseValue()
				- level * 2;
		if (amount > 0) {
			modifyXp(amount);
		}
	}
	
	public int xp() {
		return xp;
	}
	
	public int level() {
		return level;
	}
	
	public void startingItem(Item item) {
		inventory.add(item);
		equip(item);
	}
	
	public void unequip(Item item) {
		if (item == null) {
			return;
		}
		if (item == armour) {
			doAction("remove a " + item.name());
			armour = null;
		} else if (item == weapon) {
			doAction("remove a " + item.name());
			weapon = null;
		}
	}
	
	public void equip(Item item) {
		if (item.type() != ItemType.ARMOUR && item.type() != ItemType.WEAPON && item.type() != ItemType.FOOD) {
			return;
		}
		
		if (item.type() == ItemType.WEAPON) {
			unequip(weapon);
			doAction("wield a " + item.name());
			weapon = item;
		} else if (item.type() == ItemType.ARMOUR) {
			unequip(armour);
			doAction("wear a " + item.name());
			armour = item;
		}
	}
	
	public Item weapon() {
		return weapon;
	}
	
	public Item armour() {
		return armour;
	}
	
	public int maxFood() {
		return maxFood;
	}
	
	public int food() {
		return food;
	}
	
	public void pickup() {
		Item item = world.item(x, y, z);
		if (inventory.isFull() || item == null) {
			doAction("grabs at the ground");
		} else {
			doAction("pickup %s", item.name());
			world.remove(x, y, z);
			inventory.add(item);
		}
	}
	
	public void drop(Item item) {
		doAction("drop a " + item.name());
		inventory.remove(item);
		unequip(item);
		world.addAtEmptySpace(item, x, y, z);
	}
	
	public Inventory inventory() {
		return inventory;
	}
	
	public boolean canSee(int wx, int wy, int wz) {
		return ai.canSee(wx, wy, wz);
	}
	
	public Tile tile(int wx, int wy, int wz) {
		return world.tile(wx, wy, wz);
	}
	
	public int visionRadius() {
		return visionRadius;
	}
	
	public String name() {
		return name;
	}
	
	public char glyph() {
		return glyph;
	}
	
	public Color color() {
		return color;
	}
	
	public void setCreatureAi(CreatureAi ai) {
		this.ai = ai;
	}

	public void moveBy(int mx, int my, int mz) {
        Tile tile = world.tile(x + mx, y + my, z + mz);
        if (mx == 0 && my == 0 && mz == 0) {
        	return;
        }
        if (mz == -1){
            if (tile == Tile.STAIRS_DOWN) {
                doAction("walk up the stairs to level %d", z + mz + 1);
            } else {
                doAction("try to go up but are stopped by the cave ceiling");
                return;
            }
        } else if (mz == 1){
            if (tile == Tile.STAIRS_UP) {
                doAction("walk down the stairs to level %d", z + mz + 1);
            } else {
                doAction("try to go down but are stopped by the cave floor");
                return;
            }
        }
     
        Creature other = world.creature(x + mx, y + my, z + mz);
     
        if (other == null) {
            ai.onEnter(x + mx, y + my, z + mz, tile);
        } else {
            attack(other);
        }
	}
	
	public Creature creature(int wx, int wy, int wz) {
		return world.creature(wx, wy, wz);
	}
	
	public void doAction(String message, Object ... params) {
		int r = 9;
		for (int ox = -r; ox < r + 1; ox++) {
			for (int oy = -r; oy < r + 1; oy++) {
				if (ox * ox + oy * oy > r * r) {
					continue;
				}
				Creature other = world.creature(x + ox, y + oy, z);
				
				if (other == null) {
					continue;
				}
				
				if (other == this) {
					other.notify("You " + message + ".", params);
				} else if (other.canSee(x, y, z)) {
					other.notify(String.format("The '%s' %s.", name, makeSecondPerson(message)), params);
				}
			}
		}
	}
	
	private String makeSecondPerson(String text) {
		String[] words = text.split(" ");
		words[0] = words[0] + "s";
		StringBuilder builder = new StringBuilder();
		for (String word : words) {
			builder.append(" ");
			builder.append(word);
		}
		return builder.toString().trim();
	}
	
	public void attack(Creature other) {
		int amount = Math.max(0,  attackValue() - other.defenseValue());
		amount = (int)(Math.random() * amount) + 1;
		other.modifyHp(-amount);
		notify("You attack the '%s' for %d damage.", other.name, amount);
		other.notify("The '%s' attacks you for %d damage.", name, amount);
		if (other.hp < 1) {
			gainXp(other);
		}
	}
	
	public void modifyHp(int amount) {
		hp += amount;
		if (hp < 1) {
			doAction("die");
			leaveCorpse();
			world.remove(this);
		}
	}
	
	private void leaveCorpse() {
		Item corpse = new Item('%', color, name + " corpse", ItemType.CORPSE);
		corpse.modifyFoodValue(maxHp * 3);
		world.addAtEmptySpace(corpse, x, y, z);
	}
	
	public void modifyFood(int amount) {
		food += amount;
		if (food > maxFood) {
			food = maxFood + 1;
			modifyHp(-1);
		} else if (food < 1 && isPlayer()) {
			modifyHp(-1000);
		}
	}
	
	private boolean isPlayer() {
		return glyph == '@';
	}
	
	public void eat(Item item) {
		if (item.foodValue() < 0) {
			notify("Ew.");
		}
		modifyFood(item.foodValue());
		inventory.remove(item);
		unequip(item);
	}
	
	public void update() {
		modifyFood(-1);
		ai.onUpdate();
	}
	
	public boolean canEnter(int wx, int wy, int wz) {
		return world.tile(wx, wy, wz).isGround() && world.creature(wx, wy, wz) == null;
	}
	
	public int hp() {
		return hp;
	}
	
	public int maxHp() {
		return maxHp;
	}
	
	public int attackValue() {
		return attackValue
				+ (weapon == null ? 0 : weapon.attackValue())
				+ (armour == null ? 0 : armour.attackValue());
	}
	
	public int defenseValue() {
		return defenseValue
				+ (weapon == null ? 0 : weapon.defenseValue())
				+ (armour == null ? 0 : armour.defenseValue());
	}
	
	public void notify(String message, Object ... params) {
		ai.onNotify(String.format(message, params));
	}
}
