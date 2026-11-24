package com.hana.gear_forge.menu;

import com.hana.gear_forge.entity.ysm.NatsuCompanionEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 队友小夏的装备 + 背包菜单。
 *
 * <p>槽位布局：
 * <ul>
 *   <li>0-5：队友装备槽（头/胸/腿/脚/主手/副手），直接读写实体的 EquipmentSlot</li>
 *   <li>6-23：队友背包 18 格（9x2）</li>
 *   <li>24-50：玩家背包 27 格</li>
 *   <li>51-59：玩家快捷栏 9 格</li>
 * </ul>
 */
public class CompanionMenu extends AbstractContainerMenu {

    private static final EquipmentSlot[] EQUIPMENT_ORDER = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    private final NatsuCompanionEntity companion;

    /** 客户端/服务端通用构造：从 extraData 读实体 ID，在当前 level 中查找队友。 */
    public CompanionMenu(int containerId, Inventory playerInventory,
                         net.minecraft.network.RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, findCompanion(playerInventory, extraData.readInt()));
    }

    private static NatsuCompanionEntity findCompanion(Inventory playerInventory, int entityId) {
        net.minecraft.world.entity.Entity e = playerInventory.player.level().getEntity(entityId);
        return e instanceof NatsuCompanionEntity c ? c : null;
    }

    public CompanionMenu(int containerId, Inventory playerInventory, NatsuCompanionEntity companion) {
        super(ModMenus.COMPANION.get(), containerId);
        this.companion = companion;

        // 槽位以 x=8+col*18 的统一网格排布（原版玩家背包就是 x=8 起，之前用 x=84 导致错位）。
        // 布局（9 列 × 7 行）：
        //   第 0 行(y=18)     ：队友装备 6 槽（头/胸/腿/脚/主手/副手）
        //   第 1-2 行(y=36,54)：队友背包 18 格
        //   第 3-5 行(y=72..) ：玩家背包 27 格
        //   第 6 行(y=126)    ：玩家快捷栏 9 格
        int gridX = 8;
        int gridStep = 18;

        // 队友装备槽 0-5：第 0 行，x=8/26/44/62/80/98
        Container equipmentFallback = new net.minecraft.world.SimpleContainer(1);
        for (int i = 0; i < EQUIPMENT_ORDER.length; i++) {
            final EquipmentSlot slot = EQUIPMENT_ORDER[i];
            this.addSlot(new Slot(companion != null ? new EquipmentContainer(companion, slot) : equipmentFallback,
                    0, gridX + i * gridStep, 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) return true;
                    return stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor
                            && armor.getEquipmentSlot() == slot;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        // 队友背包 18 格（9x2）：第 1-2 行
        Container inv = companion != null ? companion.getInventory() : new net.minecraft.world.SimpleContainer(18);
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9, gridX + col * gridStep, 36 + row * gridStep));
            }
        }

        // 玩家背包 27 格（9x3）：第 3-5 行
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        gridX + col * gridStep, 72 + row * gridStep));
            }
        }
        // 玩家快捷栏 9 格：第 6 行
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, gridX + col * gridStep, 126));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();
        int invStart = 6;   // 队友背包起点
        int playerStart = 24; // 玩家背包起点

        if (index < 6) {
            // 装备槽 -> 玩家背包
            if (!this.moveItemStackTo(stack, playerStart, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (index < playerStart) {
            // 队友背包 -> 玩家背包
            if (!this.moveItemStackTo(stack, playerStart, this.slots.size(), false)) return ItemStack.EMPTY;
        } else {
            // 玩家背包/快捷栏 -> 队友背包；满了则尝试装备槽
            if (!this.moveItemStackTo(stack, invStart, playerStart, false)
                    && !this.moveItemStackTo(stack, 0, 6, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return companion != null && companion.isAlive() && companion.distanceTo(player) < 8.0;
    }

    /** 把单个 EquipmentSlot 包装成 Container，供 Slot 读写。 */
    private static final class EquipmentContainer implements Container {
        private final NatsuCompanionEntity entity;
        private final EquipmentSlot slot;

        EquipmentContainer(NatsuCompanionEntity entity, EquipmentSlot slot) {
            this.entity = entity;
            this.slot = slot;
        }

        @Override public int getContainerSize() { return 1; }
        @Override public boolean isEmpty() { return entity.getItemBySlot(slot).isEmpty(); }
        @Override public ItemStack getItem(int index) { return entity.getItemBySlot(slot); }
        @Override public ItemStack removeItem(int index, int count) {
            ItemStack cur = entity.getItemBySlot(slot);
            if (cur.isEmpty()) return ItemStack.EMPTY;
            ItemStack split = cur.split(count);
            entity.setItemSlot(slot, cur);
            return split;
        }
        @Override public ItemStack removeItemNoUpdate(int index) {
            ItemStack cur = entity.getItemBySlot(slot);
            entity.setItemSlot(slot, ItemStack.EMPTY);
            return cur;
        }
        @Override public void setItem(int index, ItemStack stack) {
            entity.setItemSlot(slot, stack);
        }
        @Override public void setChanged() {}
        @Override public boolean stillValid(Player player) { return entity.isAlive(); }
        @Override public void clearContent() { entity.setItemSlot(slot, ItemStack.EMPTY); }
    }
}
