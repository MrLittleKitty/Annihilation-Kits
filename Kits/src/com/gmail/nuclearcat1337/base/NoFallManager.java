package com.gmail.nuclearcat1337.base;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

import com.gmail.nuclearcat1337.anniPro.main.AnnihilationMain;

public class NoFallManager implements Listener {

	public static ArrayList<Integer> noFall = new ArrayList<Integer>();
	private static boolean registered = false;

	public static void init()
	{
		Bukkit.getPluginManager().registerEvents(new NoFallManager(), AnnihilationMain.getInstance());
		registered = true;
	}

	public static void addNoFallDamageNextFall(Entity e, long maxTicks)
	{
		if (!registered)
			init();

		final int id = e.getEntityId();
		noFall.add(id);

		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				noFall.remove((Object) id);
			}
		}.runTaskLater(AnnihilationMain.getInstance(), maxTicks);
	}

	@EventHandler
	public void onDamage(EntityDamageEvent e)
	{
		if (e.getCause() == DamageCause.FALL)
			if (noFall.remove((Object) e.getEntity().getEntityId()))
				e.setCancelled(true);
	}
}
