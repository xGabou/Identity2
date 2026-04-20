package ember.qualitycommands.featurerenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ember.qualitycommands.util.EntityRenderStateModifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import ember.qualitycommands.util.EntityRenderStateModifier;
import net.minecraft.component.DataComponentTypes;
//@Environment(EnvType.CLIENT)
public class ExtraModelFeatureRenderer<S extends EntityRenderState, M extends EntityModel<S>> extends FeatureRenderer<S, M> {
	public ExtraModelFeatureRenderer(FeatureRendererContext<S, M> featureRendererContext) {
		super(featureRendererContext);
	}
    public final HashMap<String,ItemRenderState> test=new HashMap();
    public void render(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, S armedEntityRenderState, float f, float g) {
        Map<String,List<String>> targets=((EntityRenderStateModifier)armedEntityRenderState).getTargets();
        for(String key:targets.keySet()){
            if(this.test.containsKey(key)==false){
                this.test.put(key,new ItemRenderState());
            }
            this.test.get(key).clear();
    		for(String model:targets.get(key)){
                ItemStack stack=Items.DIRT.getDefaultStack();
                stack.set(DataComponentTypes.ITEM_MODEL,Identifier.of(model));
                MinecraftClient.getInstance().getItemModelManager().update(this.test.get(key), stack,ItemDisplayContext.GROUND,null, null,0);
            }
            this.renderItem(armedEntityRenderState, this.test.get(key), key, matrixStack, orderedRenderCommandQueue, i);
        }
		//this.renderItem(armedEntityRenderState, armedEntityRenderState.leftHandItemState, Arm.LEFT, matrixStack, orderedRenderCommandQueue, i);
	}
    //MinecraftClient.getInstance().getItemModelManager()
    //this.getContextModel().getRootPart().getChild(key)
	protected void renderItem(
		S entityState, ItemRenderState itemRenderState, String key, MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, int light
	) {
		if (!itemRenderState.isEmpty()) {
            if(this.getContextModel().getRootPart().hasChild(key)){
            ModelPart part=this.getContextModel().getRootPart().getChild(key);
			matrices.push();
			//this.getContextModel().setArmAngle(entityState, arm, matrices);
			//matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
			//matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
            
            matrices.translate(part.originX/16, part.originY/16, part.originZ/16);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(part.roll*57.29577951308232f));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(part.yaw*57.29577951308232f));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(part.pitch*57.29577951308232f));
            
			//matrices.translate((bl ? -1 : 1) / 16.0F, 0.125F, -0.625F);
            
            itemRenderState.render(matrices, orderedRenderCommandQueue, light, OverlayTexture.DEFAULT_UV, entityState.outlineColor);
			matrices.pop();
            }
		}
	}

}