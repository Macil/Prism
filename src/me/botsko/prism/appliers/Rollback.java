package me.botsko.prism.appliers;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actions.Action;
import me.botsko.prism.actions.ActionType;
import me.botsko.prism.changers.WorldChangeQueue;
import me.botsko.prism.events.PrismProcessType;
import me.botsko.prism.utils.BlockUtils;
import me.botsko.prism.utils.EntityUtils;

public class Rollback extends Preview {
	
	
	/**
	 * 
	 * @param plugin
	 * @return 
	 */
	public Rollback( Prism plugin, Player player, List<Action> results, QueryParameters parameters ) {
		this.plugin = plugin;
		this.player = player;
		this.results = results;
		this.parameters = parameters;
	}
	
	
	/**
	 * Set preview move and then do a rollback
	 * @return
	 */
	public ApplierResult preview(){
		is_preview = true;
		return apply();
	}
	
	
	/**
	 * 
	 */
	public ApplierResult apply(){
		
		// Remove any fire at this location
		if(plugin.getConfig().getBoolean("prism.appliers.remove-fire-on-burn-rollback") && parameters.getActionTypes().contains(ActionType.BLOCK_BURN)){
			int fires_ext = BlockUtils.extinguish(player.getLocation(),parameters.getRadius());
			if(fires_ext > 0){
				player.sendMessage( plugin.playerHeaderMsg("Extinguishing fire!" + ChatColor.GRAY + " Like a boss.") );
			}
		}
		
		// Remove item drops in this radius
		if(plugin.getConfig().getBoolean("prism.appliers.remove-drops-on-explode-rollback") && (parameters.getActionTypes().contains(ActionType.TNT_EXPLODE) || parameters.getActionTypes().contains(ActionType.CREEPER_EXPLODE)) ){
			int removed = EntityUtils.removeNearbyItemDrops(player, parameters.getRadius());
			if(removed > 0){
				player.sendMessage( plugin.playerHeaderMsg("Removed " + removed + " drops in affected area." + ChatColor.GRAY + " Like a boss.") );
			}
		}
		
//		// Remove any liquid at this location
//		if(plugin.getConfig().getBoolean("prism.appliers.remove-liquid-on-flow-rollback") && ( parameters.getActionTypes().contains(ActionType.WATER_FLOW) || parameters.getActionTypes().contains(ActionType.LAVA_FLOW)) ){
//			int fires_ext = BlockUtils.drain(player.getLocation(),parameters.getRadius());
//			if(fires_ext > 0){
//				responses.add( plugin.playerHeaderMsg("Draining liquid first!" + ChatColor.GRAY + " Like a boss.") );
//			}
//		}
		
		// @todo can't really work here. doesn't return a proper result, etc
		// Remove any lava blocks when doing a lava bucket rollback
		if(parameters.getActionTypes().contains(ActionType.LAVA_BUCKET) || parameters.getActionTypes().contains(ActionType.LAVA_FLOW)){
			BlockUtils.drainlava(parameters.getPlayerLocation(), parameters.getRadius());
		}
		if(parameters.getActionTypes().contains(ActionType.WATER_BUCKET) || parameters.getActionTypes().contains(ActionType.WATER_FLOW)){
			BlockUtils.drainwater(parameters.getPlayerLocation(), parameters.getRadius());
		}
	
			
		// Give the results to the changequeue
		WorldChangeQueue changeQueue = new WorldChangeQueue( plugin, PrismProcessType.ROLLBACK, results, player, is_preview, parameters );
		ApplierResult changesApplied = changeQueue.apply();
		
		if(changesApplied == null){
			player.sendMessage( plugin.playerError( ChatColor.GRAY + "No actions found that match the criteria." ) );
			return null;
		}
		
		// Build the results message
		if(!is_preview){
			
			String msg = changesApplied.getChanges_applied() + " reversals.";
			if(changesApplied.getChanges_skipped() > 0){
				msg += " " + changesApplied.getChanges_skipped() + " skipped.";
			}
			if(changesApplied.getChanges_applied() > 0){
				msg += ChatColor.GRAY + " It's like it never happened.";
			}
			player.sendMessage( plugin.playerHeaderMsg( msg ) );
			
		} else {
		
			// Build the results message
			String msg = changesApplied.getChanges_applied() + " planned reversals.";
			if(changesApplied.getChanges_skipped() > 0){
				msg += " " + changesApplied.getChanges_skipped() + " skipped.";
			}
			if(changesApplied.getChanges_applied() > 0){
				msg += ChatColor.GRAY + " Use /prism preview apply to confirm this rollback.";
			}
			player.sendMessage( plugin.playerHeaderMsg( msg ) );
			
			// Let me know there's no need to cancel/apply
			if(changesApplied.getChanges_applied() == 0){
				player.sendMessage( plugin.playerHeaderMsg( ChatColor.GRAY + "Nothing to rollback, preview canceled for you." ) );
			}
		}
		return changesApplied;
	}
}