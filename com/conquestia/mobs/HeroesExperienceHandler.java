package com.conquestia.mobs;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.ExperienceChangeEvent;
import com.herocraftonline.heroes.api.events.HeroKillCharacterEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass.ExperienceType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Handles Heroes plugin experience for custom mob kills
 * @author ferrago
 */
public class HeroesExperienceHandler implements Listener {

    //User Settings
    private final double xpScale; //xp factor used to increase xp for higher level mobs.
    private final boolean maEnabled;
    private final double maScale;
    private final boolean moneyDrops;
    private final boolean debug;
    double levelCost = 0.1;
    
    //Plugin Instances
    Plugin CqMobs; //Instance of Cq Mobs
    private Economy econ; //Instance of the servers economy
    private final Heroes heroes;// Instance of Heroes
    
    //Data Maps
    private final HashMap<String, LivingEntity> mobKillMap = new HashMap<>(); //Used to help compute accurate solo/party xp
    private final HashMap<EntityType, Double> typeCost = new HashMap<>(); //Initial xp drops for mob types
    public static HashMap<String, LivingEntity> mobArenaKillMap = new HashMap(); //Used to calculate modified xp for arena mobs
    


    /**
     * Instantiate variables, Register services
     * 
     * @param CqMobs Calling plugin
     * @param experienceScale Config option expScale
     * @param maEnabled Is Ma Enabled?
     * @param maScale Mob Arena modifier factor
     */
    public HeroesExperienceHandler(Plugin CqMobs, double experienceScale, boolean maEnabled, double maScale, boolean moneyDrops, boolean debug) {
        
        //Registration
        Bukkit.getServer().getPluginManager().registerEvents(this, CqMobs);
        
        //Instantiation
        this.xpScale = experienceScale;
        this.CqMobs = CqMobs;
        this.maEnabled = maEnabled;
        this.maScale = maScale;
        this.moneyDrops = moneyDrops;
        this.debug = debug;
        heroes = (Heroes) Bukkit.getPluginManager().getPlugin("Heroes");
        
        //Fill money map
        if (moneyDrops) {
            buildMoneyDrops();
        }
        
    }

    /**
     * Handles what happens when a player kills a mob
     * 
     * @param event Triggering event
     */
    @EventHandler
    public void onHeroMobDeath(HeroKillCharacterEvent event) {
        
        if (!ConquestiaMobs.getEnabledWorlds().contains(event.getDefender().getEntity().getWorld())) {
            return;
        }
        
        //If the killer is not a player, or if the mob was spawned from a mob spawner, we don't calculate the exp here
        if (!(event.getDefender().getEntity() instanceof Player) && !event.getDefender().getEntity().hasMetadata("Spawner")) { 
            
            boolean showDeath = true; //Always start off by setting this to true
            
            //String entityName = ChatColor.stripColor(event.getDefender().getEntity().getCustomName()); //We need the name without fancy colors to compute the level
            
            //If the defender is a monster && they have a level
            if (event.getDefender().getEntity() instanceof Monster) {
                mobKillMap.put(event.getAttacker().getPlayer().getUniqueId().toString(), event.getDefender().getEntity()); // Put the hero and the mob into our data map
                double maxMoneyDrop = 0; //Do we want there to be a money cap?
                
                int level = 1;
                
                if (debug) {
                    long start = System.currentTimeMillis();
                    level = MobSpawnHandler.getMobLevel(event.getDefender().getEntity());
                    ConquestiaMobs.debug(ChatColor.BLUE + "Looking Up Level required: " + ChatColor.YELLOW + "" + ((System.currentTimeMillis() - start)) + ChatColor.BLUE + " miliseconds with " + MobSpawnHandler.getLevelMap().keySet().size() + " mobs");
                } else {
                    level = getMobLevel(event.getDefender().getEntity());
                }
                
                //Calculate random money drop if the user wants money drops
                double moneyDrop = 0; // Initial declaration.
                if (moneyDrops && typeCost.containsKey(event.getDefender().getEntity().getType())) {
                    Random rand = new Random();
                    maxMoneyDrop = ((double)level * levelCost * typeCost.get(event.getDefender().getEntity().getType())) + typeCost.get(event.getDefender().getEntity().getType());
                    moneyDrop = maxMoneyDrop * rand.nextDouble();
                }
                
                if (event.getAttacker().hasParty()) {
                    moneyDrop = ((moneyDrop * 1.5) / event.getAttacker().getParty().getMembers().size());
                }
                
                //Mob Arena experience drops
                if (maEnabled && ConquestiaMobs.getMobArena() != null && ((com.garbagemule.MobArena.MobArena)ConquestiaMobs.getMobArena()).getArenaMaster().getArenaAtLocation(event.getDefender().getEntity().getLocation()) != null) {
                    mobArenaKillMap.put(event.getAttacker().getPlayer().getUniqueId().toString(), event.getDefender().getEntity());
                    com.garbagemule.MobArena.framework.Arena arena = ((com.garbagemule.MobArena.MobArena)ConquestiaMobs.getMobArena()).getArenaMaster().getArenaWithPlayer(event.getAttacker().getPlayer());
                    moneyDrop = 0;
                    showDeath = false;
                    if (event.getAttacker().hasParty()) {
                        for (Hero hero : event.getAttacker().getParty().getMembers()) {

                            if (((com.garbagemule.MobArena.MobArena)ConquestiaMobs.getMobArena()).getArenaMaster().getArenaWithPlayer(hero.getPlayer()) != null && ((com.garbagemule.MobArena.MobArena)ConquestiaMobs.getMobArena()).getArenaMaster().getArenaWithPlayer(hero.getPlayer()) == arena) {
                                mobArenaKillMap.put(hero.getPlayer().getUniqueId().toString(), event.getDefender().getEntity());
                            }

                        }
                    }
                }
                
                //Display xp/money holograms
                if (showDeath) {
                    double xp =  heroes.getCharacterManager().getMonster(event.getDefender().getEntity()).getExperience() * level * xpScale;
                    
                    //@Research
                    xp *= -3; //Don't know why this works...Research later
                    xp += 3;
                    
                    //If Hero has party send xp to all players
                    if (event.getAttacker().hasParty()) {
                        xp = (xp * 2) / event.getAttacker().getParty().getMembers().size();
                        ArrayList<Player> party = new ArrayList<>();
                        for (Hero hero : event.getAttacker().getParty().getMembers()) {
                            party.add(hero.getPlayer());
                        }
                    }
                    
                    ConquestiaMobs.getDisplay().DisplayStuffHeroes(event.getDefender().getEntity().getLocation(), xp, moneyDrop, event.getAttacker().getPlayer());
                }

            }
        }
    }

    /**
     * Sets default money drops for base level mob
     * Will make this configurable in next patch.
     */
    public void buildMoneyDrops() {
        typeCost.put(EntityType.CAVE_SPIDER, 0.1);
        typeCost.put(EntityType.BLAZE, 0.1);
        typeCost.put(EntityType.CREEPER, 0.1);
        typeCost.put(EntityType.GIANT, 0.1);
        typeCost.put(EntityType.IRON_GOLEM, 0.1);
        typeCost.put(EntityType.MAGMA_CUBE, 0.1);
        typeCost.put(EntityType.ZOMBIE, 0.1);
        typeCost.put(EntityType.WITCH, 0.1);
        typeCost.put(EntityType.SPIDER, 0.1);
        typeCost.put(EntityType.PIG_ZOMBIE, 0.1);
        typeCost.put(EntityType.SKELETON, 0.1);
        typeCost.put(EntityType.SLIME, 0.05);
            
    }
    
    /**
     * Retrieve the monsters level.
     * 
     * @param ent The entity. 
     * @return returns the monster's level
     */
    private int getMobLevel(LivingEntity ent) {
        return MobSpawnHandler.getMobLevel(ent);
    }

    /**
     * When the experience is calculated for the player, modify
     * it to reflect the appropriate experience.
     * 
     * @param event Triggering event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onHeroExpChange(ExperienceChangeEvent event) {
        if (!ConquestiaMobs.getEnabledWorlds().contains(event.getHero().getPlayer().getWorld())) {
            return;
        }
        if (event.getSource() == ExperienceType.KILLING && mobKillMap.containsKey(event.getHero().getPlayer().getUniqueId().toString())) {
            LivingEntity ent = mobKillMap.get(event.getHero().getPlayer().getUniqueId().toString());
            int level = getMobLevel(ent);
            if (mobArenaKillMap.containsKey(event.getHero().getPlayer().getUniqueId().toString())) {
                event.setExpGain(event.getExpChange() + (level * xpScale * maScale * event.getExpChange()));
            } else {
                event.setExpGain(event.getExpChange() + (level * xpScale * event.getExpChange()));
            }
            
            
            MobSpawnHandler.getMobLevel(ent);
            mobKillMap.remove(event.getHero().getPlayer().getUniqueId().toString());
            
        }
    }

}
