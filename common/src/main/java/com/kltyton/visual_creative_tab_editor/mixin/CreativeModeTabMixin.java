package com.kltyton.visual_creative_tab_editor.mixin;

import com.kltyton.visual_creative_tab_editor.data.CreativeTabDefinition;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabType;
import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import java.util.Collection;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import com.kltyton.visual_creative_tab_editor.data.ResourceId;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Projects server-authoritative tab properties and contents into the native tab. */
@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin implements com.kltyton.visual_creative_tab_editor.runtime.CreativeTabContents {
    @Shadow
    private Collection<ItemStack> displayItems;

    @Shadow
    private Set<ItemStack> displayItemsSearchTab;

    @Override
    public void visualCreativeTabEditor$replaceContents(Collection<ItemStack> display, Collection<ItemStack> searchable) {
        Collection<ItemStack> newDisplay = new java.util.ArrayList<>(display);
        Set<ItemStack> newSearch = com.kltyton.visual_creative_tab_editor.platform.item.StackData.set();
        newSearch.addAll(searchable);
        this.displayItems = newDisplay;
        this.displayItemsSearchTab = newSearch;
    }

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$getDisplayName(CallbackInfoReturnable<Component> callback) {
        CreativeTabRuntime.definition((CreativeModeTab) (Object) this)
                .ifPresent(definition -> callback.setReturnValue(definition.title()));
    }

    @Inject(method = "getIconItem", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$getIcon(CallbackInfoReturnable<ItemStack> callback) {
        CreativeTabRuntime.definition((CreativeModeTab) (Object) this)
                .ifPresent(definition -> callback.setReturnValue(definition.icon()));
    }

    @Inject(method = "showTitle", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$showTitle(CallbackInfoReturnable<Boolean> callback) {
        CreativeTabRuntime.definition((CreativeModeTab) (Object) this)
                .ifPresent(definition -> callback.setReturnValue(definition.layout().showTitle()));
    }

    @Inject(method = "canScroll", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$canScroll(CallbackInfoReturnable<Boolean> callback) {
        CreativeTabRuntime.definition((CreativeModeTab) (Object) this)
                .ifPresent(definition -> callback.setReturnValue(definition.layout().canScroll()));
    }

    @Inject(method = "isAlignedRight", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$isAlignedRight(CallbackInfoReturnable<Boolean> callback) {
        CreativeTabRuntime.definition((CreativeModeTab) (Object) this)
                .ifPresent(definition -> callback.setReturnValue(definition.layout().alignedRight()));
    }

    @Inject(method = "getType", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$getType(CallbackInfoReturnable<CreativeModeTab.Type> callback) {
        CreativeTabRuntime.definition((CreativeModeTab) (Object) this)
                .ifPresent(definition -> callback.setReturnValue(definition.type().toVanilla()));
    }

    @Inject(method = "shouldDisplay", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$shouldDisplay(CallbackInfoReturnable<Boolean> callback) {
        CreativeTabRuntime.definition((CreativeModeTab) (Object) this).ifPresent(definition -> {
            if (CreativeTabRuntime.isForcedVisible((CreativeModeTab) (Object) this)) {
                callback.setReturnValue(true);
                return;
            }
            boolean hasContent = !isOperatorUtilities() || !this.displayItems.isEmpty();
            callback.setReturnValue(!definition.hidden() && hasContent);
        });
    }

    @Inject(method = "hasAnyItems", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$hasAnyItems(CallbackInfoReturnable<Boolean> callback) {
        CreativeTabRuntime.definition((CreativeModeTab) (Object) this).ifPresent(definition ->
                callback.setReturnValue(CreativeTabRuntime.isForcedVisible((CreativeModeTab) (Object) this)
                        || (!definition.hidden() && (!isOperatorUtilities() || !this.displayItems.isEmpty())))
        );
    }

    @Inject(method = "buildContents", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$buildRuntimeTabContents(
            CreativeModeTab.ItemDisplayParameters parameters,
            CallbackInfo callback
    ) {
        CreativeModeTab self = (CreativeModeTab) (Object) this;
        CreativeTabDefinition definition = CreativeTabRuntime.definition(self).orElse(null);
        if (definition == null
                || definition.type() != CreativeTabType.CATEGORY
                || BuiltInRegistries.CREATIVE_MODE_TAB.getKey(self) != null) {
            return;
        }
        applyDefinitionContents(definition, parameters);
        callback.cancel();
    }

    private void applyDefinitionContents(
            CreativeTabDefinition definition,
            CreativeModeTab.ItemDisplayParameters parameters
    ) {
        visualCreativeTabEditor$replaceContents(java.util.List.of(), java.util.List.of());
        if (isOperatorUtilities() && !parameters.hasPermissions()) {
            return;
        }
        definition.copyItemsTo(this.displayItems);
        definition.copySearchItemsTo(this.displayItemsSearchTab);
    }

    private boolean isOperatorUtilities() {
        return CreativeTabRuntime.id((CreativeModeTab) (Object) this)
                .filter(id -> id.equals(ResourceId.withDefaultNamespace("op_blocks")))
                .isPresent();
    }
    @Override
    public void visualCreativeTabEditor$applyRegisteredContents(CreativeModeTab.ItemDisplayParameters parameters) {
        CreativeModeTab self = (CreativeModeTab) (Object) this;
        CreativeTabDefinition definition = CreativeTabRuntime.definition(self).orElse(null);
        if (definition != null
                && definition.type() == CreativeTabType.CATEGORY
                && BuiltInRegistries.CREATIVE_MODE_TAB.getKey(self) != null) {
            applyDefinitionContents(definition, parameters);
        }
    
    }
}
