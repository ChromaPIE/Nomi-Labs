package com.nomiceu.nomilabs.gregtech.multiblock;

import appeng.core.Api;
import com.nomiceu.nomilabs.gregtech.LabsRecipeMaps;
import com.nomiceu.nomilabs.gregtech.recipelogic.NaqRecipeLogic;
import gregtech.api.metatileentity.multiblock.FuelMultiblockController;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import gregicality.multiblocks.api.render.GCYMTextures;
import gregtech.client.renderer.texture.Textures;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregicality.multiblocks.common.block.GCYMMetaBlocks;
import gregicality.multiblocks.common.block.blocks.BlockLargeMultiblockCasing;
import gregtech.common.blocks.BlockGlassCasing;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import gregtech.api.util.TextFormattingUtil;
import gregtech.api.util.GTUtility;
import gregtech.api.GTValues;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import gregtech.client.utils.TooltipHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MetaTileEntityNaquadahReactor extends FuelMultiblockController {
    public final int numSpatial;
    public final IBlockState bottomFiller;
    public final IBlockState topFiller;
    public final int tier;
    public final int voltageTier;

    public static final int AMP = 3;

    public MetaTileEntityNaquadahReactor(ResourceLocation metaTileEntityId, int tier, int voltageTier, int numSpatial, IBlockState bottomFiller, IBlockState topFiller) {
        super(metaTileEntityId, LabsRecipeMaps.NAQUADAH_REACTOR_RECIPES.get(tier - 1), voltageTier);
        this.voltageTier = voltageTier;
        this.tier = tier;
        this.numSpatial = numSpatial;
        this.bottomFiller = bottomFiller;
        this.topFiller = topFiller;
        this.recipeMapWorkable = new NaqRecipeLogic(this);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityNaquadahReactor(metaTileEntityId, tier, voltageTier, numSpatial, bottomFiller, topFiller);
    }

    @Override
    @NotNull
    protected BlockPattern createStructurePattern() {
        String[] aisle1 = new String[this.numSpatial + 2];
        String[] aisle2 = new String[this.numSpatial + 2];
        String[] aisle3 = new String[this.numSpatial + 2];

        aisle1[0] = "CCC";
        aisle2[0] = "CCC";
        aisle3[0] = "CSC";

        // Loop for numSpatial times, starting from index 1, adding spatials to outside aisles
        for (int i = 1; i <= numSpatial; i++) {
            aisle1[i] = "PGP";
            aisle3[i] = "PGP";
        }
        // Loop for numSpatial - 1 times, starting from index 1, adding bottomFillers to inside aisle
        for (int i = 1; i < numSpatial; i++) {
            aisle2[i] = "GBG";
        }
        // Add top filler
        aisle2[numSpatial] = "GTG";

        aisle1[numSpatial + 1] = "CCC";
        aisle2[numSpatial + 1] = "CCC";
        aisle3[numSpatial + 1] = "CCC";

        return FactoryBlockPattern.start()
                .aisle(aisle1)
                .aisle(aisle2)
                .aisle(aisle3)
                .where('S', selfPredicate())
            .where('G', states(getCasingStateGlass()))
            .where('P', states(getCasingStateSpatial()))
            .where('T', states(this.topFiller))
            .where('B', states(this.bottomFiller))
            .where('C', states(getCasingStateMain()).setMinGlobalLimited(10)
                .or(abilities(MultiblockAbility.OUTPUT_ENERGY).setExactLimit(1))
                .or(autoAbilities(false, true, true, true, false, false, false)))
            .build();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return GCYMTextures.MIXER_CASING;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @NotNull
    protected ICubeRenderer getFrontOverlay() {
        return Textures.FUSION_REACTOR_OVERLAY;
    }

    protected IBlockState getCasingStateMain() {
        return GCYMMetaBlocks.LARGE_MULTIBLOCK_CASING.getState(BlockLargeMultiblockCasing.CasingType.MIXER_CASING);
    }

    protected IBlockState getCasingStateGlass() {
        return MetaBlocks.TRANSPARENT_CASING.getState(BlockGlassCasing.CasingType.FUSION_GLASS);
    }

    protected IBlockState getCasingStateSpatial() {
        if (Api.INSTANCE.definitions().blocks().spatialPylon().maybeBlock().isPresent()) {
            return Api.INSTANCE.definitions().blocks().spatialPylon().maybeBlock().get().getDefaultState();
        } else {
            assert Blocks.AIR != null;
            return Blocks.AIR.getDefaultState();
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addDisplayText(List<ITextComponent> textList) {
        if (!isStructureFormed()) {
            ITextComponent tooltip = new TextComponentTranslation("gregtech.multiblock.invalid_structure.tooltip");
            tooltip.setStyle(new Style().setColor(TextFormatting.GRAY));
            textList.add(new TextComponentTranslation("gregtech.multiblock.invalid_structure")
                    .setStyle(new Style().setColor(TextFormatting.RED)
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))));
        }
        else {
            long produces = GTValues.V[voltageTier] * AMP;
            String voltageName = GTValues.VNF[GTUtility.getFloorTierByVoltage(produces)] + TextFormatting.RESET;
            textList.add(new TextComponentTranslation("gregtech.multiblock.max_energy_per_tick", TextFormattingUtil.formatNumbers(produces), voltageName));

            if (!recipeMapWorkable.isWorkingEnabled()) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.work_paused"));
            } else if (recipeMapWorkable.isActive()) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.running"));
                int currentProgress = (int) (recipeMapWorkable.getProgressPercent() * 100);
                textList.add(new TextComponentTranslation("gregtech.multiblock.progress", currentProgress));
            } else {
                textList.add(new TextComponentTranslation("gregtech.multiblock.idling"));
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World player, @NotNull List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);

        tooltip.add(I18n.format("tooltip.nomilabs.naquadah_reactor.produces", AMP, GTValues.VNF[voltageTier] + TextFormatting.RESET));
        tooltip.add(TooltipHelper.RAINBOW_SLOW + I18n.format("gui.nomilabs.naquadah_reactor.overclock"));
    }
}
