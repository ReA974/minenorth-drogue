package fr.minenorth.drogue.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.minenorth.drogue.DrugItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Animations de prise, côté joueur : le joint monte à la bouche (on fume), le miroir se lève devant le visage et se déplace le long des lignes (on sniffe),
 * la seringue part vers l'avant-bras (piqûre). Le modèle de l'objet change aussi pendant la prise (prédicats snort / inject, voir ClientSetup).
 */
public final class DrugClient implements IClientItemExtensions {
    /** Main près de la bouche (joint, miroir). */
    public static final HumanoidModel.ArmPose MOUTH = HumanoidModel.ArmPose.create("minenorthdrogue_mouth", false, (model, entity, arm) -> {
        ModelPart p = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        p.xRot = -1.45f;
        p.yRot = arm == HumanoidArm.RIGHT ? -0.45f : 0.45f;
        p.zRot = 0f;
    });
    /** Bras replié devant soi (piqûre à l'avant-bras). */
    public static final HumanoidModel.ArmPose INJECT = HumanoidModel.ArmPose.create("minenorthdrogue_inject", false, (model, entity, arm) -> {
        ModelPart p = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        p.xRot = -0.8f;
        p.yRot = arm == HumanoidArm.RIGHT ? 0.75f : -0.75f;
        p.zRot = 0f;
    });

    /** Une seule instance : Forge appelle initializeClient dans le constructeur de l'objet, avant que son type de prise soit connu ; on le lit donc dans l'objet au moment de l'animation. */
    public static final DrugClient INSTANCE = new DrugClient();

    private DrugClient() {}

    private static DrugItem.Kind kindOf(ItemStack stack) { return stack.getItem() instanceof DrugItem d ? d.kind() : null; }

    private boolean using(LivingEntity e, ItemStack st) { return e.isUsingItem() && e.getUseItem().getItem() == st.getItem(); }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (using(entity, stack) && entity.getUsedItemHand() == hand) return kindOf(stack) == DrugItem.Kind.METH ? INJECT : MOUTH;
        return null;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack ps, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick, float equipProcess, float swingProcess) {
        DrugItem.Kind kind = kindOf(stack);
        if (kind == null || !using(player, stack)) return false;
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float used = player.getTicksUsingItem() + partialTick;
        float t = Math.min(1f, used / 8f);
        float lift = t * t * (3f - 2f * t);
        ps.translate(side * 0.56f, -0.52f + equipProcess * -0.6f, -0.72f);
        switch (kind) {
            case JOINT -> {
                float drag = (float) Math.sin(used * 0.35f) * 0.012f;
                ps.translate(side * -0.30f * lift, (0.46f + drag) * lift, 0.30f * lift);
                ps.mulPose(Axis.XP.rotationDegrees(-58f * lift));
                ps.mulPose(Axis.YP.rotationDegrees(side * -22f * lift));
            }
            case COKE -> {
                // le miroir monte devant le visage, puis glisse le long des deux lignes
                float run = Math.max(0f, Math.min(1f, (used - 10f) / 30f));
                ps.translate(side * (-0.42f + 0.25f * run) * lift, 0.30f * lift, 0.42f * lift);
                ps.mulPose(Axis.XP.rotationDegrees(-72f * lift));
                ps.mulPose(Axis.ZP.rotationDegrees(side * 8f * lift * (float) Math.sin(run * Math.PI * 2)));
            }
            case METH -> {
                // la seringue part vers l'avant-bras opposé, puis le piston s'enfonce (modèle)
                float prick = Math.max(0f, Math.min(1f, (used - 12f) / 10f));
                ps.translate(side * -0.64f * lift, (0.18f - 0.04f * prick) * lift, (0.20f + 0.05f * prick) * lift);
                ps.mulPose(Axis.ZP.rotationDegrees(side * -32f * lift));
                ps.mulPose(Axis.YP.rotationDegrees(side * 42f * lift));
            }
        }
        return true;
    }
}
