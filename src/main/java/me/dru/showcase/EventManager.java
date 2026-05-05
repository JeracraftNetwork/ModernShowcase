package me.dru.showcase;

import me.dru.showcase.utils.ScheduleUtil;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;


import me.dru.showcase.block.Showcase;
import me.dru.showcase.utils.CoreProtectShowcaseAdapter;

public class EventManager implements Listener {
	ModernShowcase plugin;
	CoreProtectShowcaseAdapter coreProtect;
	public EventManager(ModernShowcase plugin) {
		this.plugin = plugin;
		coreProtect = plugin.getCoreProtect();
		Bukkit.getWorlds().forEach(w->w.getEntities().forEach(ent->{
			if(ent.getType()==EntityType.ITEM_DISPLAY&&Showcase.isShowcase(ent.getLocation()))
				Showcase.rotatesInstance.put(ent.getLocation().getBlock(),Showcase.get(ent.getLocation()));
		}));
	}
	
	@EventHandler
	public void onCraft(PrepareItemCraftEvent e) {
		if(e.getRecipe()!=null&&e.getRecipe() instanceof ShapedRecipe&&((ShapedRecipe)e.getRecipe()).getKey().getNamespace().equalsIgnoreCase("ModernShowcase")) {
			if(e.getViewers().get(0).hasPermission("modernshowcase.craft"))
				e.getInventory().setResult(plugin.getShowcaseItem(ModernShowcase.getLang((Player)e.getView().getPlayer()), e.getInventory().getResult().getType()));
			else
				e.getInventory().setResult(new ItemStack(Material.AIR));
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlace(BlockPlaceEvent e) {
		if(!e.isCancelled()&&e.getItemInHand()!=null&&Showcase.isShowcase(e.getItemInHand())) {
			if(!e.getPlayer().hasPermission("modernshowcase.place")) {
				e.getPlayer().sendMessage(ModernShowcase.getLang(e.getPlayer()).noPlacePerms);
				e.setCancelled(true);
				return;
			}
				
			int limit = ModernShowcase.Config().placedPerChunkLimit;
			if(limit>=0&&Showcase.getPlacedAmountOnChunk(e.getBlock().getChunk())>=limit) {
				e.getPlayer().sendMessage(ModernShowcase.getLang(e.getPlayer()).placedLimitReach);
				e.setCancelled(true);
				return;
			}
			Showcase.addPlacedAmountOnChunk(e.getBlock().getChunk(), 1);
			Location loc = e.getBlock().getLocation().subtract(e.getPlayer().getLocation());
			Showcase show = Showcase.get(e.getBlock().getLocation());
			show.spawnHolder(Math.abs(loc.getX())<Math.abs(loc.getZ()),new ItemStack(Material.ITEM_FRAME));
			show.setOwner(e.getPlayer().getUniqueId());
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBreak(BlockBreakEvent e) {
		if(!e.isCancelled()&&Showcase.isShowcase(e.getBlock().getLocation())) {
			Showcase showcase = Showcase.get(e.getBlock().getLocation());
			Showcase.addPlacedAmountOnChunk(e.getBlock().getChunk(), -1);
			showcase.despawn();
		}
	}
	
		
	PlayerInteractEvent dependCheck;
	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteract(PlayerInteractEvent e) {
		if (e != dependCheck
				&& e.getHand() != EquipmentSlot.OFF_HAND
				&& e.getAction() == Action.RIGHT_CLICK_BLOCK
				&& Showcase.isShowcase(e.getClickedBlock().getLocation())) {

			e.setCancelled(true);

			if (coreProtect != null && coreProtect.isInspecting(e.getPlayer()))
				return;

			Player player = e.getPlayer();
			Location loc = e.getClickedBlock().getLocation();
			Showcase showcase = Showcase.get(loc);

			// Use region-based PLAYER helper
			ScheduleUtil.PLAYER.runTask(plugin, player, () -> {

				boolean canEdit =
						player.hasPermission("modernshowcase.admin")
								|| (
								player.hasPermission("modernshowcase.edit")
										&& showcase.isOwner(player)
						);

				if (!player.isSneaking() || !canEdit) {
					ShowcaseUI.preview(player, showcase);
				} else {
					ShowcaseUI.open(player, showcase);
				}

			});
		}
	}

	

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityExplode(EntityExplodeEvent e) { 
		for(Block b : e.blockList())
			Showcase.get(b.getLocation()).despawn();	
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockExplode(BlockExplodeEvent e) { 
		for(Block b : e.blockList())
			Showcase.get(b.getLocation()).despawn();	
	}
	
	@EventHandler
	public void onPistonExtend(BlockPistonExtendEvent e) {
		for(Block b : e.getBlocks()) {
			Showcase.get(b.getLocation()).despawn();
		}
	}
	
	@EventHandler
	public void onPistonRetract(BlockPistonRetractEvent e) {
		for(Block b : e.getBlocks()) {
			Showcase.get(b.getLocation()).despawn();
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onClick(InventoryClickEvent e) {
		if(ShowcaseUI.isPreviewInventory(e.getInventory()))
			e.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onDrag(InventoryDragEvent e) {
		if(ShowcaseUI.isPreviewInventory(e.getInventory()))
			e.setCancelled(true);
	}
	
	@EventHandler
	public void onCloseInv(InventoryCloseEvent e) {
		if(ShowcaseUI.isPreviewInventory(e.getInventory()))
			ShowcaseUI.closePreviewInventory(e.getInventory());
	}
	
	@EventHandler
	public void onDeath(EntityDeathEvent e) {
		if(e.getEntityType()==EntityType.ITEM_DISPLAY&&Showcase.isShowcase(e.getEntity().getLocation()))
			Showcase.get(e.getEntity().getLocation()).despawn();
	}

	@EventHandler
	public void onLoad(EntitiesLoadEvent e) {
		for(Entity ent : e.getEntities()) {
			if(ent.getType()==EntityType.ITEM_DISPLAY&&Showcase.isShowcase(ent.getLocation())) {
				Showcase.get(ent.getLocation());
			}	
		}
		
	}
	@EventHandler
	public void onUnload(EntitiesUnloadEvent e) {
		HashSet<Block> unload = new HashSet<>();
		for(Entity ent : e.getEntities()) {
			if(ent.getType()==EntityType.ITEM_DISPLAY&&Showcase.isShowcase(ent.getLocation())) {
				unload.add(ent.getLocation().getBlock());
			}	
		}
		for(Block block : unload) {
			Showcase.rotatesInstance.remove(block);	
		}
		
		
	}
	
	public static int rotTimes =0;
	public static void rotate() {
		rotTimes+=1;
		Showcase.rotatesInstance.values().forEach(display->{
			if(display==null||display.getItemHolder()==null)
				return;
			float rot = ((float)rotTimes) /10f;
			ScheduleUtil.ENTITY.runTask(ModernShowcase.getInstance(), display.getItemHolder(), () -> {
				if(rotTimes%50==0) {
					int number = rotTimes/50;
					display.setItemDisplay(number);
				}
				ItemDisplay item = display.getItemHolder();
				Transformation transfom = item.getTransformation();
				transfom.getLeftRotation().set(new AxisAngle4f( (float)Math.PI*rot*display.getAutoRotateSpeed(), new Vector3f(0, 1, 0)));
				item.setTransformation(transfom);
				
			});
		});
	}
}
