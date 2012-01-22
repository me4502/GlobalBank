package me.samkio.globalbank.methods;

import java.util.ArrayList;

import me.samkio.globalbank.GlobalBank;
import me.samkio.globalbank.PlayerState;
import me.samkio.globalbank.delayedTasks.DelayedBank;
import me.samkio.globalbank.delayedTasks.DelayedSlot;
import me.samkio.globalbank.util.SqliteDB;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SimpleMethods {

	public static void openSlot(GlobalBank b, Player p, int i) {
		b.getServer().getScheduler()
				.scheduleSyncDelayedTask(b, new DelayedSlot(p, i, b), 1);
	}

	public static void openBank(GlobalBank b, Player p) {
		b.getServer().getScheduler()
				.scheduleSyncDelayedTask(b, new DelayedBank(p, b), 1);
	}

	public static boolean handleSlot(ItemStack i, Player p, Inventory inv,
			GlobalBank b) {
		if (i.getType() == Material.CHEST) {
			SlotDataMethods.saveBank(p, inv.getContents().clone(), PlayerState
					.getPlayerState(p).getSlot());
			SimpleMethods.openBank(b, p);
			return true;
		} else if (i.getType() == Material.PAPER) {
			ItemStack[] is = b.sort.sortItemStack(inv.getContents().clone(), 2,inv.getSize());
			inv.setContents(is);

			p.sendMessage(ChatColor.BLUE + "[B]" + ChatColor.WHITE
					+ " Slot " + PlayerState.getPlayerState(p).getSlot()
					+ " Sorted.");

			SlotDataMethods.saveBank(p, inv.getContents().clone(), PlayerState
					.getPlayerState(p).getSlot());
			SimpleMethods.openSlot(b, p, PlayerState.getPlayerState(p)
					.getSlot() - 1);
					
			return true;
		}
		return false;
	}

	public static boolean handleBank(GlobalBank b, Player p, int slot) {
		if (b.economy == null) {
			SimpleMethods.openSlot(b, p, slot);
			return true;
		}
		ArrayList<Integer> ids = new ArrayList<Integer>();
		if (SqliteDB.containsSlotIds(p.getName())) {
			ids = SqliteDB.getAvailiableSlots(p.getName());
		} else {
			ids.add(1);
			SqliteDB.newRowSlotIdList(p.getName(), ids);
		}
		if (ids.contains((slot + 1))) {
			SimpleMethods.openSlot(b, p, slot);
			PlayerState.getPlayerState(p).setBuyingSlot(0);
			return true;
		} else if (PlayerState.getPlayerState(p).getBuyingSlot() == (slot + 1)) {
			EconomyResponse r = b.economy.withdrawPlayer(p.getName(), SimpleMethods.costOfSlot(slot));
			if (r.transactionSuccess()) {
				p.sendMessage(ChatColor.BLUE + "[B]" + ChatColor.WHITE
						+ " Slot Purchased!");
				ids.add(slot + 1);
				SqliteDB.updateSlotList(p.getName(), ids);
				SimpleMethods.openSlot(b, p, slot);
				PlayerState.getPlayerState(p).setBuyingSlot(0);
				return true;
			} else {
				PlayerState.getPlayerState(p).setBuyingSlot(0);
				p.sendMessage(ChatColor.BLUE + "[B]" + ChatColor.WHITE
						+ " Error occured: " + r.errorMessage);
				return true;
			}
		} else {
			PlayerState.getPlayerState(p).setBuyingSlot(slot + 1);
			p.sendMessage(ChatColor.BLUE + "[B]" + ChatColor.WHITE
					+ " Click Slot again to purchase.");
			p.sendMessage(ChatColor.BLUE + "[B]" + ChatColor.WHITE
					+ " Cost: "+SimpleMethods.costOfSlot(slot));
			return true;
		}
	}
	public static double costOfSlot(int slot){
		return (GlobalBank.plugin.s.costPerSlot*(GlobalBank.plugin.s.multiplier*(slot)));
	}
}