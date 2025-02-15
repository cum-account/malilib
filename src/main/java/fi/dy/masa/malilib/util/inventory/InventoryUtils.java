package fi.dy.masa.malilib.util.inventory;

import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.data.IntRange;
import fi.dy.masa.malilib.util.data.ItemType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class InventoryUtils
{
    /**
     * Check whether the stacks are identical otherwise, but ignoring the stack size
     */
    public static boolean areStacksEqual(ItemStack stack1, ItemStack stack2)
    {
        return ItemStack.areItemsEqual(stack1, stack2) && ItemStack.areItemStackTagsEqual(stack1, stack2);
    }

    /**
     * Check whether the stacks are identical otherwise, but ignoring the stack size,
     * and optionally ignoring the NBT data
     */
    public static boolean areStacksEqual(ItemStack stack1, ItemStack stack2, boolean ignoreNbt)
    {
        return ItemStack.areItemsEqual(stack1, stack2) && (ignoreNbt || ItemStack.areItemStackTagsEqual(stack1, stack2));
    }

    /**
     * Checks whether the given stacks are identical, ignoring the stack size and the durability of damageable items.
     */
    public static boolean areStacksEqualIgnoreDurability(ItemStack stack1, ItemStack stack2)
    {
        return ItemStack.areItemsEqualIgnoreDurability(stack1, stack2) && ItemStack.areItemStackTagsEqual(stack1, stack2);
    }

    /**
     * Checks whether the given stacks are identical, ignoring the stack size and the durability of damageable items.
     * Also optionally ignores the NBT data.
     */
    public static boolean areStacksEqualIgnoreDurability(ItemStack stack1, ItemStack stack2, boolean ignoreNbt)
    {
        return ItemStack.areItemsEqualIgnoreDurability(stack1, stack2) && (ignoreNbt || ItemStack.areItemStackTagsEqual(stack1, stack2));
    }

    /**
     * Swaps the stack from the slot <b>slotNum</b> to the given hotbar slot <b>hotbarSlot</b>
     */
    public static void swapSlots(Container container, int slotNum, int hotbarSlot)
    {
        Minecraft mc = Minecraft.getMinecraft();
        mc.playerController.windowClick(container.windowId, slotNum, hotbarSlot, ClickType.SWAP, mc.player);
    }

    /**
     * Assuming that the slot is from the ContainerPlayer container,
     * returns whether the given slot number is one of the regular inventory slots.
     * This means that the crafting slots and armor slots are not valid.
     */
    public static boolean isRegularInventorySlot(int slotNumber, boolean allowOffhand)
    {
        return slotNumber > 8 && (slotNumber < 45 || (allowOffhand && slotNumber == 45));
    }

    /**
     * Finds an empty slot in the player inventory. Armor slots are not valid for the return value of this method.
     * Whether or not the offhand slot is valid, depends on the <b>allowOffhand</b> argument.
     * @return the slot number, or -1 if none were found
     */
    public static int findEmptySlotInPlayerInventory(Container containerPlayer, boolean allowOffhand, boolean reverse)
    {
        final int startSlot = reverse ? containerPlayer.inventorySlots.size() - 1 : 0;
        final int endSlot = reverse ? -1 : containerPlayer.inventorySlots.size();
        final int increment = reverse ? -1 : 1;

        for (int slotNum = startSlot; slotNum != endSlot; slotNum += increment)
        {
            Slot slot = containerPlayer.inventorySlots.get(slotNum);
            ItemStack stackSlot = slot.getStack();

            // Inventory crafting, armor and offhand slots are not valid
            if (stackSlot.isEmpty() && isRegularInventorySlot(slot.slotNumber, allowOffhand))
            {
                return slot.slotNumber;
            }
        }

        return -1;
    }

    /**
     * Finds a slot with an identical item to <b>stackReference</b> from the regular player inventory,
     * ignoring the durability of damageable items.
     * Does not allow crafting or armor slots or the off hand slot.
     * @param reverse if true, then the slots are iterated in reverse order
     * @return the slot number, or -1 if none were found
     */
    public static int findPlayerInventorySlotWithItem(Container container, ItemStack stackReference, boolean reverse)
    {
        return findPlayerInventorySlotWithItem(container, stackReference, false, reverse);
    }

    /**
     * Finds a slot with an identical item to <b>stackReference</b> from the regular player inventory,
     * ignoring the durability of damageable items and optionally ignoring NBT data.
     * Does not allow crafting or armor slots or the off hand slot.
     * @param reverse if true, then the slots are iterated in reverse order
     * @return the slot number, or -1 if none were found
     */
    public static int findPlayerInventorySlotWithItem(Container container, ItemStack stackReference, boolean ignoreNbt, boolean reverse)
    {
        if ((container instanceof ContainerPlayer) == false)
        {
            return -1;
        }

        final int startSlot = reverse ? container.inventorySlots.size() - 1 : 0;
        final int endSlot = reverse ? -1 : container.inventorySlots.size();
        final int increment = reverse ? -1 : 1;

        for (int slotNum = startSlot; slotNum != endSlot; slotNum += increment)
        {
            Slot slot = container.inventorySlots.get(slotNum);

            if (isRegularInventorySlot(slot.slotNumber, false) &&
                areStacksEqualIgnoreDurability(slot.getStack(), stackReference, ignoreNbt))
            {
                return slot.slotNumber;
            }
        }

        return -1;
    }

    /**
     * Tries to find a slot with the given item for pick-blocking.
     * Prefers the hotbar to the rest of the inventory.
     */
    public static int findSlotWithItemToPickBlock(Container container, ItemStack stackReference, boolean ignoreNbt)
    {
        if ((container instanceof ContainerPlayer) == false)
        {
            return -1;
        }

        // Hotbar
        int slot = findSlotWithItem(container, stackReference, IntRange.of(36, 9), true, ignoreNbt);

        if (slot != -1)
        {
            return slot;
        }

        // Regular player inventory and offhand
        return findSlotWithItem(container, stackReference, IntRange.of(9, 27 + 1), true, ignoreNbt);
    }

    public static int findSlotWithItem(Container container, ItemStack stackReference,
                                       IntRange intRange, boolean ignoreDurability, boolean ignoreNbt)
    {
        final int startSlot = intRange.getFirst();
        final int endSlot = intRange.getLast();
        List<Slot> slots = container.inventorySlots;

        if (startSlot < 0 || endSlot >= slots.size())
        {
            return -1;
        }

        for (int slotNum = startSlot; slotNum <= endSlot; ++slotNum)
        {
            Slot slot = slots.get(slotNum);

            if ((ignoreDurability          && areStacksEqualIgnoreDurability(slot.getStack(), stackReference, ignoreNbt)) ||
                (ignoreDurability == false && areStacksEqual(slot.getStack(), stackReference, ignoreNbt)))
            {
                return slot.slotNumber;
            }
        }

        return -1;
    }

    /**
     * Swap the given item to the player's main hand, if that item is found in the player's inventory.
     * @return true if an item was swapped to the main hand, false if it was already in the hand, or was not found in the inventory
     */
    public static boolean swapItemToMainHand(ItemStack stackReference)
    {
        return swapItemToMainHand(stackReference, false);
    }

    /**
     * Swap the given item to the player's main hand, if that item is found in the player's inventory.
     * @return true if an item was swapped to the main hand, false if it was already in the hand, or was not found in the inventory
     */
    public static boolean swapItemToMainHand(ItemStack stackReference, boolean ignoreNbt)
    {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        boolean isCreative = player.capabilities.isCreativeMode;

        // Already holding the requested item
        if (areStacksEqual(stackReference, player.getHeldItemMainhand(), ignoreNbt))
        {
            return false;
        }

        if (isCreative)
        {
            player.inventory.setPickedItemStack(stackReference.copy());
            mc.playerController.sendSlotPacket(stackReference.copy(), 36 + player.inventory.currentItem);
            return true;
        }
        else
        {
            int slot = findPlayerInventorySlotWithItem(player.inventoryContainer, stackReference, true);

            if (slot != -1)
            {
                int currentHotbarSlot = player.inventory.currentItem;
                mc.playerController.windowClick(player.inventoryContainer.windowId, slot, currentHotbarSlot, ClickType.SWAP, mc.player);
                return true;
            }
        }

        return false;
    }

    /**
     * Re-stocks more items to the stack in the player's current hotbar slot.
     * @param threshold the number of items at or below which the re-stocking will happen
     * @param allowHotbar whether or not to allow taking items from other hotbar slots
     */
    public static void preRestockHand(EntityPlayer player, EnumHand hand, int threshold, boolean allowHotbar)
    {
        final ItemStack stackHand = player.getHeldItem(hand);
        final int count = stackHand.getCount();
        final int max = stackHand.getMaxStackSize();

        if (stackHand.isEmpty() == false &&
            player.openContainer == player.inventoryContainer &&
            player.inventory.getItemStack().isEmpty() &&
            (count <= threshold && count < max))
        {
            Minecraft mc = Minecraft.getMinecraft();
            Container container = player.inventoryContainer;
            int endSlot = allowHotbar ? 44 : 35;
            int currentMainHandSlot = player.inventory.currentItem + 36;
            int currentSlot = hand == EnumHand.MAIN_HAND ? currentMainHandSlot : 45;

            for (int slotNum = 9; slotNum <= endSlot; ++slotNum)
            {
                if (slotNum == currentMainHandSlot)
                {
                    continue;
                }

                Slot slot = container.inventorySlots.get(slotNum);
                ItemStack stackSlot = slot.getStack();

                if (areStacksEqual(stackSlot, stackHand))
                {
                    // If all the items from the found slot can fit into the current
                    // stack in hand, then left click, otherwise right click to split the stack
                    int button = stackSlot.getCount() + count <= max ? 0 : 1;

                    mc.playerController.windowClick(container.windowId, slot.slotNumber, button, ClickType.PICKUP, player);
                    mc.playerController.windowClick(container.windowId, currentSlot, 0, ClickType.PICKUP, player);

                    break;
                }
            }
        }

    }

    /**
     * Checks if the given Shulker Box (or other storage item with the
     * same NBT data structure) currently contains any items.
     */
    public static boolean shulkerBoxHasItems(ItemStack stack)
    {
        NBTTagCompound nbt = stack.getTagCompound();

        if (nbt != null && nbt.hasKey("BlockEntityTag", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound tag = nbt.getCompoundTag("BlockEntityTag");

            if (tag.hasKey("Items", Constants.NBT.TAG_LIST))
            {
                NBTTagList tagList = tag.getTagList("Items", Constants.NBT.TAG_COMPOUND);
                return tagList.tagCount() > 0;
            }
        }

        return false;
    }

    /**
     * Returns the list of items currently stored in the given Shulker Box
     * (or other storage item with the same NBT data structure).
     * Does not keep empty slots.
     */
    public static NonNullList<ItemStack> getStoredItemsNonEmpty(ItemStack stackIn)
    {
        NBTTagCompound nbt = stackIn.getTagCompound();

        if (nbt != null && nbt.hasKey("BlockEntityTag", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound tagBlockEntity = nbt.getCompoundTag("BlockEntityTag");

            if (tagBlockEntity.hasKey("Items", Constants.NBT.TAG_LIST))
            {
                NonNullList<ItemStack> items = NonNullList.create();
                NBTTagList tagList = tagBlockEntity.getTagList("Items", Constants.NBT.TAG_COMPOUND);
                final int count = tagList.tagCount();

                for (int i = 0; i < count; ++i)
                {
                    ItemStack stack = new ItemStack(tagList.getCompoundTagAt(i));

                    if (stack.isEmpty() == false)
                    {
                        items.add(stack);
                    }
                }

                return items;
            }
        }

        return NonNullList.create();
    }

    public static InventoryView getExactStoredItemsView(ItemStack stackIn)
    {
        ListBackedInventoryView inv = new ListBackedInventoryView();
        readStoredItems(stackIn, (pair) -> inv.setStackInSlot(pair.getKey(), pair.getValue()));
        return inv;
    }

    public static InventoryView getNonEmptyStoredItemsView(ItemStack stackIn)
    {
        ListBackedInventoryView inv = new ListBackedInventoryView();
        readStoredItems(stackIn, (pair) -> inv.addStack(pair.getValue()));
        return inv;
    }

    /* TODO
    public static InventoryView getCompactedStoredItemsView(ItemStack stackIn)
    {
    }
    */

    public static void readStoredItems(ItemStack containerStack, Consumer<Pair<Integer, ItemStack>> consumer)
    {
        NBTTagCompound nbt = containerStack.getTagCompound();

        if (nbt != null && nbt.hasKey("BlockEntityTag", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound tagBlockEntity = nbt.getCompoundTag("BlockEntityTag");

            if (tagBlockEntity.hasKey("Items", Constants.NBT.TAG_LIST))
            {
                NBTTagList tagList = tagBlockEntity.getTagList("Items", Constants.NBT.TAG_COMPOUND);
                final int count = tagList.tagCount();

                for (int i = 0; i < count; ++i)
                {
                    NBTTagCompound tag = tagList.getCompoundTagAt(i);
                    ItemStack stack = new ItemStack(tag);
                    int slot = tag.getByte("Slot");

                    if (slot >= 0 && stack.isEmpty() == false)
                    {
                        consumer.accept(Pair.of(slot, stack));
                    }
                }
            }
        }
    }

    /**
     * @return a combined view of both inventories appended after each other.
     * inventory1 will be first, and inventory2 will be appended after it.
     */
    public static InventoryView getCombinedInventoryView(InventoryView inventory1, InventoryView inventory2)
    {
        return new CombinedInventoryView(inventory1, inventory2);
    }

    /**
     * Returns a map of the stored item counts in the given Shulker Box
     * (or other storage item with the same NBT data structure).
     */
    public static Object2IntOpenHashMap<ItemType> getStoredItemCounts(ItemStack stackIn)
    {
        Object2IntOpenHashMap<ItemType> map = new Object2IntOpenHashMap<>();
        NonNullList<ItemStack> items = getStoredItemsNonEmpty(stackIn);

        for (ItemStack stack : items)
        {
            if (stack.isEmpty() == false)
            {
                map.addTo(new ItemType(stack, false, true), stack.getCount());
            }
        }

        return map;
    }

    /**
     * Returns a map of the stored item counts in the given inventory.
     * This also counts the contents of any Shulker Boxes
     * (or other storage item with the same NBT data structure).
     */
    public static Object2IntOpenHashMap<ItemType> getInventoryItemCounts(IInventory inv)
    {
        Object2IntOpenHashMap<ItemType> map = new Object2IntOpenHashMap<>();
        final int slots = inv.getSizeInventory();

        for (int slot = 0; slot < slots; ++slot)
        {
            ItemStack stack = inv.getStackInSlot(slot);

            if (stack.isEmpty() == false)
            {
                map.addTo(new ItemType(stack, false, true), stack.getCount());

                if (shulkerBoxHasItems(stack))
                {
                    Object2IntOpenHashMap<ItemType> boxCounts = getStoredItemCounts(stack);

                    for (ItemType type : boxCounts.keySet())
                    {
                        map.addTo(type, boxCounts.getInt(type));
                    }
                }
            }
        }

        return map;
    }
}
