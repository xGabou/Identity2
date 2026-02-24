package net.Gabou.identity2.items;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

public final class IdentityBookItem extends Item {
    public IdentityBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        held.set(DataComponents.WRITTEN_BOOK_CONTENT, createGuideBookContent());
        player.openItemGui(held, hand);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }

    private static WrittenBookContent createGuideBookContent() {
        List<Filterable<Component>> pages = List.of(
            page(
                "Identity 2 Guide\n\n"
                    + "Kill mobs to unlock morphs.\n"
                    + "Morph into unlocked forms and use their abilities.\n\n"
                    + "This book is a quick reference."
            ),
            page(
                "Controls\n\n"
                    + "G: Identity Menu\n"
                    + "V: Primary Ability\n"
                    + "B: Secondary Ability\n"
                    + "F6/F7/F8: Morph Favorite\n"
                    + "F9/F10/F11: Save Favorite"
            ),
            page(
                "Core Behavior\n\n"
                    + "- Morphs can be configured to require unlocks.\n"
                    + "- Some morphs can fly.\n"
                    + "- Hostile/passive mob reactions depend on config.\n"
                    + "- Death penalties depend on Soulbound/progression settings."
            ),
            page(
                "Progression Systems\n\n"
                    + "- Charges (optional)\n"
                    + "- Soul Jars (optional)\n"
                    + "- Permanent Jar Morphs (optional)\n"
                    + "- Soul Absorption (optional endgame)\n\n"
                    + "See /identity progression ..."
            ),
            page(
                "Useful Commands\n\n"
                    + "/identity list\n"
                    + "/identity morph <namespace:id>\n"
                    + "/identity clear\n"
                    + "/identity ability current\n\n"
                    + "Admin:\n"
                    + "/identity config list"
            ),
            page(
                "Progression Commands (Admin)\n\n"
                    + "/identity progression ui\n"
                    + "/identity progression charges get <id>\n"
                    + "/identity progression jar list\n"
                    + "/identity progression jar create <jar> <tier>\n"
                    + "/identity progression jar store <jar> <id>"
            )
        );

        return new WrittenBookContent(Filterable.passThrough("Identity 2 Guide"), "Identity2", 0, pages, true);
    }

    private static Filterable<Component> page(String text) {
        return Filterable.passThrough(Component.literal(text));
    }
}
