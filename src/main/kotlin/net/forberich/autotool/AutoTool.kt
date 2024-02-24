package net.forberich.autotool

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.minecraft.block.Block
import net.minecraft.block.Material
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.*
import net.minecraft.text.LiteralText
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import kotlin.reflect.KClass


object AutoTool : ModInitializer {

	private var debugMode = false
	override fun onInitialize() {
		AttackBlockCallback.EVENT.register(AttackBlockCallback { player, world, _, pos, _ ->
			val state = world.getBlockState(pos)
			val tool = getIdealToolForBlock(state.block) ?: return@AttackBlockCallback ActionResult.PASS
			if (debugMode)
				player.sendMessage(LiteralText("").append(state.block.name).append(" -> ").append(tool.simpleName).formatted(Formatting.GREEN), false)

			if (!isItemOfType(player.inventory.getStack(player.inventory.selectedSlot).item, tool)) {
				val bestAxe = findHighestValueItem(player, tool)
				if (bestAxe != -1) {
					if (bestAxe <= 9){
						player.inventory.selectedSlot = bestAxe
					} else {
						switchItems(player, bestAxe, player.inventory.selectedSlot)
					}
				}
			}

			ActionResult.PASS
		})
		AttackEntityCallback.EVENT.register(AttackEntityCallback { player, _, _, _, _ ->
			if (!isItemOfType(player.inventory.getStack(player.inventory.selectedSlot).item, SwordItem::class)){
				val bestSword = findHighestValueItem(player, SwordItem::class)
				if (bestSword <= 9){
					player.inventory.selectedSlot = bestSword
				} else {
					switchItems(player, bestSword, player.inventory.selectedSlot)
				}
			}
			ActionResult.PASS
		})

		ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("autotool_debug").executes { _ ->
			debugMode = !debugMode
			1
		})
	}

	private fun isItemOfType(item: Item, kClass: KClass<out Item>): Boolean {
		return item::class == kClass
	}

	private fun findHighestValueItem(player: PlayerEntity, itemKClass: KClass<out Item>): Int {
		var slot = -1
		var durability = 0
		for (slotIndex in 0 until player.inventory.size()) {
			val stack = player.inventory.getStack(slotIndex)
			if (stack.item::class != itemKClass) continue

			if (stack.item !is ToolItem) continue

			val tool = stack.item as ToolItem
			tool.material.durability
			if (tool.material.durability > durability) {
				slot = slotIndex
				durability = tool.material.durability
			}
		}
		return slot
	}

	private fun switchItems(player: PlayerEntity, firstSlot: Int, secondSlot: Int) {
		val stack = player.inventory.getStack(firstSlot)
		val hotbarStack = player.inventory.getStack(secondSlot)

		player.inventory.setStack(firstSlot, hotbarStack)
		player.inventory.setStack(secondSlot, stack)
	}

	private fun getIdealToolForBlock(block: Block): KClass<out Item>? {
		val material = block.defaultState.material

		return when (material) {
			Material.WOOD -> AxeItem::class
			Material.STONE, Material.METAL -> PickaxeItem::class
			Material.SOIL, Material.PLANT, Material.SOLID_ORGANIC, Material.SNOW_BLOCK, Material.SNOW_LAYER -> ShovelItem::class
			else -> null
		}
	}
}