// Dev-only (ci/mixin-audit/run.sh): never part of the mod.
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.spongepowered.asm.mixin.MixinEnvironment;

/** Dev-only: loads every mixin target (applying all mixins, ours and Fabric API's) before the game starts, then exits. */
public class MixinAudit implements PreLaunchEntrypoint {
    @Override public void onPreLaunch() {
        int code = 0;
        String[] targets = {"net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer", "net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState",
            "net.minecraft.client.renderer.special.SpecialModelRenderers", "net.minecraft.client.gui.GuiGraphicsExtractor",
            "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen", "dev.whizzle.shulker.client.render.BoxExtras",
            "dev.whizzle.shulker.client.render.Labels", "dev.whizzle.shulker.client.ui.IconSlot",
            "dev.whizzle.shulker.client.render.Patterns", "dev.whizzle.shulker.client.render.PatternedShulkerBoxRenderer"};
        for (String t : targets) {
            try { Class.forName(t, true, Thread.currentThread().getContextClassLoader()); System.out.println("AUDIT loaded " + t); }
            catch (Throwable e) { code = 1; System.out.println("AUDIT FAILED " + t + ": " + e); e.printStackTrace(System.out); }
        }
        try {
            Class<?> s = Class.forName("net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState", true, Thread.currentThread().getContextClassLoader());
            Class<?> h = Class.forName("dev.whizzle.shulker.client.render.BoxExtras", true, Thread.currentThread().getContextClassLoader());
            System.out.println("AUDIT render state implements BoxExtras: " + h.isAssignableFrom(s));
            if (!h.isAssignableFrom(s)) code = 1;
        } catch (Throwable e) { code = 1; e.printStackTrace(System.out); }
        try {
            Class<?> a = Class.forName("net.minecraft.client.gui.screens.inventory.AbstractContainerScreen", true, Thread.currentThread().getContextClassLoader());
            Class<?> acc = Class.forName("dev.whizzle.shulker.client.mixin.ContainerScreenAccessor", false, Thread.currentThread().getContextClassLoader());
            System.out.println("AUDIT container screen has the accessor: " + acc.isAssignableFrom(a));
            if (!acc.isAssignableFrom(a)) code = 1;
        } catch (Throwable e) { code = 1; e.printStackTrace(System.out); }
        try { MixinEnvironment.getCurrentEnvironment().audit(); System.out.println("AUDIT mixin audit done"); }
        catch (Throwable e) { code = 1; System.out.println("AUDIT mixin audit FAILED: " + e); e.printStackTrace(System.out); }
        System.out.println("AUDIT RESULT " + (code == 0 ? "OK" : "FAILED"));
        Runtime.getRuntime().halt(code);
    }
}
