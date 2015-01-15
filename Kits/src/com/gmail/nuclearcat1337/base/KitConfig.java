package com.gmail.nuclearcat1337.base;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.gmail.nuclearcat1337.anniPro.anniGame.AnniPlayer;
import com.gmail.nuclearcat1337.anniPro.itemMenus.MenuItem;
import com.gmail.nuclearcat1337.anniPro.kits.Kit;
import com.gmail.nuclearcat1337.anniPro.main.AnniArgument;
import com.gmail.nuclearcat1337.anniPro.main.AnniCommand;
import com.gmail.nuclearcat1337.anniPro.main.AnnihilationMain;
import com.gmail.nuclearcat1337.anniPro.utils.IDTools;
import com.gmail.nuclearcat1337.anniPro.xp.AsyncLogQuery;
import com.gmail.nuclearcat1337.anniPro.xp.AsyncQuery;
import com.gmail.nuclearcat1337.anniPro.xp.Database;
import com.gmail.nuclearcat1337.anniPro.xp.XPSystem;
import com.google.common.base.Predicate;

public class KitConfig
{
	private static KitConfig instance;
	public static KitConfig getInstance()
	{
		if(instance == null)
			instance = new KitConfig();
		return instance;
	}
	
	private YamlConfiguration kitConfig;
	private boolean useAllKits;
	private boolean useDatabase;
	private Database database;
	//private final ChatColor aqua = ChatColor.AQUA;
	private KitConfig()
	{
		File f = new File(AnnihilationMain.getInstance().getDataFolder().getAbsolutePath(),"StarterKitsConfig.yml");
		boolean b = !f.exists();
		try
		{
			if(b)
				f.createNewFile();
			kitConfig = YamlConfiguration.loadConfiguration(f);
			if(b)
			{
				ConfigurationSection section = kitConfig.createSection("StarterKits");
				section.set("EnableAllKits", true);
				section.set("UseDatabase", false);
				kitConfig.save(f);
			}
			ConfigurationSection section = kitConfig.getConfigurationSection("StarterKits");
			useAllKits = section.getBoolean("EnableAllKits");
			useDatabase = section.getBoolean("UseDatabase");
			if(useDatabase)
			{
				database = XPSystem.getDatabase();
				if(database != null)
				{
					database.updateSQL("CREATE TABLE IF NOT EXISTS tbl_player_kits (ID VARCHAR(40), Kit VARCHAR(20))");
					for(AnniPlayer p : AnniPlayer.getPlayers())
					{
						loadKits(p);
					}
					Bukkit.getPluginManager().registerEvents(new Listener(){
						@EventHandler(priority=EventPriority.MONITOR)
						public void kitLoader(PlayerJoinEvent event)
						{
							AnniPlayer p = AnniPlayer.getPlayer(event.getPlayer().getUniqueId());
							if(p != null)
								loadKits(p);
						}
						
						@EventHandler(priority=EventPriority.MONITOR)
						public void kitLoader(PlayerTeleportEvent event)
						{
							AnniPlayer p = AnniPlayer.getPlayer(event.getPlayer().getUniqueId());
							if(p != null)
								loadKits(p);
						}
					}, AnnihilationMain.getInstance());
					
					//Bukkit.getLogger().info("[Annihilation] Registering the Argument");
					AnniCommand.registerArgument(new AnniArgument(){
						@Override
						public void executeCommand(final CommandSender sender, String label, final String[] args)
						{
							if(args != null && args.length > 2 && useDatabase)
							{
								IDTools.getUUID(args[2], new Predicate<UUID>(){
									@Override
									public boolean apply(UUID id)
									{
										if(id != null)
										{
											Kit kit = Kit.getKit(args[1]);
											if(kit != null)
											{
												if(args[0].equalsIgnoreCase("add"))
												{
													sender.sendMessage(ChatColor.GREEN+"Kit added.");
													addKit(kit.getName(), id);
												}
												else if(args[0].equalsIgnoreCase("remove"))
												{
													sender.sendMessage(ChatColor.RED+"Kit removed.");
													removeKit(kit.getName(), id);
												}
												else 
													sender.sendMessage(ChatColor.RED+"Operation "+ChatColor.GOLD+args[0]+ChatColor.RED+" is not supported.");
											}
											else 
												sender.sendMessage(ChatColor.RED+"Could not locate the kit you specified.");
										}
										else 
											sender.sendMessage(ChatColor.RED+"Could not locate the player you specified.");
										return false;
									}});			
							}
						}
	
						@Override
						public String getArgumentName()
						{
							return "Kit";
						}
	
						@Override
						public String getHelp()
						{			
							return ChatColor.LIGHT_PURPLE+"Kit [add,remove] <kit> <player>--"+ChatColor.GREEN+"adds or removes a kit from a player.";
						}
	
						@Override
						public MenuItem getMenuItem()
						{
							return null;
						}
	
						@Override
						public String getPermission()
						{
							return null;
						}
	
						@Override
						public boolean useByPlayerOnly()
						{
							return false;
						}});
				}
				else 
					useDatabase = false;
			}
//			else
//			{
//				//this means we need to register permissions for each of the kits
//			}
		}
		catch (IOException | ClassNotFoundException | SQLException e)
		{
			e.printStackTrace();
		}
	}

	public Player getPlayerInSightTest(Player player, int distance)
	{
		@SuppressWarnings("deprecation")
		Block[] bs = player.getLineOfSight(null, distance).toArray(new Block[0]);
		List<Entity> near = player.getNearbyEntities(distance, distance, distance);
		Player insight = null;
		for (Block b : bs)
		{
			for (Entity e : near)
			{
				if(e.getType() == EntityType.PLAYER)
				{
					if (e.getLocation().distance(b.getLocation()) < 1)
					{
						if(insight == null || insight.getLocation().distanceSquared(player.getLocation()) > e.getLocation().distanceSquared(player.getLocation()))
							insight = (Player)e;
					}
				}
			}
		}
		return insight;
	}
	
	public Player getPlayerInSight(Player player, int distance)
	{
        Location playerLoc = player.getLocation();
        Vector3D playerDirection = new Vector3D(playerLoc.getDirection());
        Vector3D start = new Vector3D(playerLoc);
        Vector3D end = start.add(playerDirection.multiply(distance));
        Player inSight = null;
        for(Entity nearbyEntity : player.getNearbyEntities(distance, distance, distance ))
        {
        	if(nearbyEntity.getType() == EntityType.PLAYER)
        	{
	            Vector3D nearbyLoc = new Vector3D(nearbyEntity.getLocation());
	 
	            //Bounding box
	            Vector3D min = nearbyLoc.subtract(0.5D, 1.6D, 0.5D);
	            Vector3D max = nearbyLoc.add(0.5D, 0.3D, 0.5D);
	 
	            if(hasIntersection(start, end, min, max))
	            {
	                if(inSight == null || inSight.getLocation().distanceSquared(playerLoc) > nearbyEntity.getLocation().distanceSquared(playerLoc))
	                {
	                    inSight = (Player)nearbyEntity;
	                    return inSight;
	                }
	            }
        	}
        }
 
        return inSight;
    }
 
    private boolean hasIntersection(Vector3D start, Vector3D end, Vector3D min, Vector3D max) 
    {
        final double epsilon = 0.0001f;
 
        Vector3D d = end.subtract(start).multiply(0.5);
        Vector3D e = max.subtract(min).multiply(0.5);
        Vector3D c = start.add(d).subtract(min.add(max).multiply(0.5));
        Vector3D ad = d.abs();
 
        if(Math.abs(c.getX()) > e.getX() + ad.getX()){
            return false;
        }
 
        if(Math.abs(c.getY()) > e.getY() + ad.getY()){
            return false;
        }
 
        if(Math.abs(c.getZ()) > e.getX() + ad.getZ()){
            return false;
        }
 
        if(Math.abs(d.getY() * c.getZ() - d.getZ() * c.getY()) > e.getY() * ad.getZ() + e.getZ() * ad.getY() + epsilon){
            return false;
        }
 
        if(Math.abs(d.getZ() * c.getX() - d.getX() * c.getZ()) > e.getZ() * ad.getX() + e.getX() * ad.getZ() + epsilon){
            return false;
        }
 
        if(Math.abs(d.getX() * c.getY() - d.getY() * c.getX()) > e.getX() * ad.getY() + e.getY() * ad.getX() + epsilon){
            return false;
        }
 
        return true;
    }
	
	public boolean useAllKits()
	{
		return this.useAllKits;
	}
	
	public boolean useDefaultPermissions()
	{
		return !this.useDatabase;
	}
	
	public void saveConfig()
	{
		try
		{
			kitConfig.save(new File(AnnihilationMain.getInstance().getDataFolder().getAbsolutePath(),"StarterKitsConfig.yml"));
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void removeKit(final String kit, final UUID player)
	{
		AnniPlayer p = AnniPlayer.getPlayer(player);
		if(p != null)
		{
			Object obj = p.getData("Kits");
			if(obj != null && obj instanceof List)
			{
				List<String> str = (List<String>)obj;
				str.remove(kit.toLowerCase());
			}
		}
		database.addNewAsyncLogQuery(new AsyncLogQuery(){
			@Override
			public String getQuery()
			{
				//return "INSERT INTO tbl_player_kits (ID,Kit) VALUES ('"+player.toString()+"','"+kit+"');";
				return "DELETE FROM tbl_player_kits WHERE ID='"+player+"' AND Kit='"+kit+"';";
			}});
	}
	
	@SuppressWarnings("unchecked")
	public void addKit(final String kit, final UUID player)
	{
		AnniPlayer p = AnniPlayer.getPlayer(player);
		if(p != null)
		{
			Object obj = p.getData("Kits");
			if(obj != null && obj instanceof List)
			{
				((List<String>)obj).add(kit.toLowerCase());
			}
		}
		database.addNewAsyncLogQuery(new AsyncLogQuery(){
			@Override
			public String getQuery()
			{
				return "INSERT INTO tbl_player_kits (ID,Kit) VALUES ('"+player.toString()+"','"+kit+"');";
			}});
	}
	
	public void loadKits(final AnniPlayer player)
	{
		Object o = player.getData("KitsLoaded");
		if(o == null || (boolean)o == false)
		{
			database.addNewAsyncQuery(new KitLoader(player));
			player.setData("KitsLoaded", true);
		}
	}
	
	private class KitLoader implements AsyncQuery
	{
		private final AnniPlayer player;
		private List<String> kits;
		public KitLoader(AnniPlayer p)
		{
			this.player = p;
		}
		
		
		@Override
		public void run()
		{
			player.setData("Kits", kits);
		}

		@Override
		public String getQuerey() 
		{	
			return "SELECT * FROM tbl_player_kits WHERE ID='"+player.getID()+"';";
		}

		@Override
		public boolean isCallback() 
		{
			return true;
		}

		@Override
		public void setResult(ResultSet result) 
		{
			kits = new ArrayList<String>();
			try
			{
				while(result.next())
				{
					kits.add(result.getString("Kit").toLowerCase());
				}
			}
			catch(SQLException e)
			{
				
			}
		}
	}
	
	public ConfigurationSection createKitSection(String kitname)
	{
		return kitConfig.getConfigurationSection("StarterKits").createSection(kitname);
	}
	
	public ConfigurationSection getKitSection(String kit)
	{
		try
		{
			return kitConfig.getConfigurationSection("StarterKits").getConfigurationSection(kit);
		}
		catch(Exception e)
		{
			return null;
		}
	}
}
