import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Deterministically prepares the upstream Punchy 1.21.5 artifact for 1.21.4. */
public final class PunchyCompatPatcher {
    private static final String OPTIONAL_PARTICLES =
            "punchy/client/modelparts/ModelPartsParticleSystem.class";
    private static final Set<String> OPTIONAL_1215_RENDERERS = Set.of(
            "punchy/client/render/BoatInHandRenderer.class",
            "punchy/client/render/MinecartInHandRenderer.class",
            "punchy/client/render/CreatureBucketAnimator.class",
            "punchy/client/bedrockitems/BedrockItemRenderer.class");
    private static final List<String> REPORT = new ArrayList<>();

    private record Method(int access, String name, String descriptor,
                          String signature, String[] exceptions) {}

    public static void main(String[] args) throws Exception {
        if (args.length == 4 && args[0].equals("--audit-jar")) {
            Path temp = Files.createTempDirectory("punchy-linkage-");
            try (ZipFile zip = new ZipFile(Path.of(args[1]).toFile())) {
                for (ZipEntry entry : zip.stream().filter(e -> e.getName().startsWith("punchy/")
                        && e.getName().endsWith(".class")).toList()) {
                    Path target = temp.resolve(entry.getName());
                    Files.createDirectories(target.getParent());
                    try (InputStream in = zip.getInputStream(entry)) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            Set<String> errors = auditMinecraftLinkage(temp, Path.of(args[2]), false);
            Files.write(Path.of(args[3]), errors, StandardCharsets.UTF_8);
            if (!errors.isEmpty()) {
                throw new IOException("remapped Punchy links " + errors.size()
                        + " missing 1.21.4 member(s); see " + args[3]);
            }
            System.out.println("Remapped Punchy linkage audit passed");
            return;
        }
        if (args.length != 4 || !args[0].equals("--prepare")) {
            throw new IllegalArgumentException(
                    "usage: --prepare <upstream.jar> <output-dir> <minecraft-1.21.4.jar>");
        }
        Path source = Path.of(args[1]);
        Path output = Path.of(args[2]);
        extract(source, output);
        patchMixinConfig(output.resolve("punchy.mixins.json"));
        smoothDefaultHandInAnimation(output.resolve("resourcepacks/punchy/assets/minecraft/"
                + "punchy/animations/player.animation.json"));
        mergeBuiltinPack(output, output.resolve("resourcepacks/punchy"));
        write1214Bridge(output.resolve("punchy/compat/Punchy1214Bridge.class"));
        try (var files = Files.walk(output)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".class"))
                    .sorted(Comparator.naturalOrder()).toList()) {
                String relative = output.relativize(path).toString().replace('\\', '/');
                if (relative.equals(OPTIONAL_PARTICLES)
                        || OPTIONAL_1215_RENDERERS.contains(relative)) noOpOptionalRenderer(path, relative);
                else {
                    if (relative.equals("punchy/mixin/client/ItemInHandRendererMixin.class")) {
                        patchItemRendererMixinTarget(path);
                    }
                    if (relative.equals("punchy/fabric/PunchyFabricClient.class")) {
                        patchEmbeddedModContainer(path);
                    }
                    if (relative.equals("punchy/client/render/PunchyArmRenderer.class")) {
                        disable1215LayerTransformLookup(path);
                        addSmooth4dEquipRise(path);
                    }
                    if (relative.equals("punchy/config/PunchyConfig.class")) {
                        gatePunchyWithEnoughVisualsModule(path);
                    }
                    if (relative.equals("punchy/client/state/HandEquipStateMachine.class")) {
                        stabilizeEmptyToItemEquip(path);
                        useVanillaItemEquipTransition(path);
                    }
                    if (relative.equals("punchy/client/state/MiningStateMachine.class")) {
                        stabilizeMiningV2(path);
                    }
                    if (relative.equals("punchy/client/render/PendulumPhysics.class")) {
                        suspendPendulumDuringEquip(path);
                    }
                    if (relative.equals("punchy/client/state/MovementStateMachine.class")) {
                        disableProceduralIdleArmBob(path);
                    }
                    if (relative.equals("punchy/platform/FabricClientPlatform.class")) {
                        patchClientPlatformKeyBinding(path);
                    }
                    if (relative.equals("punchy/client/animation/ItemAnimationManager.class")
                            || relative.equals("punchy/client/bedrockitems/BedrockItemAnimationManager.class")) {
                        addEquipVisualReset(path, relative.substring(0, relative.length() - 6));
                    }
                    patch1214References(path, relative);
                }
            }
        }
        auditMinecraftLinkage(output, Path.of(args[3]), true);
        Files.writeString(output.resolve("punchy-1.21.4-compat-report.txt"),
                String.join(System.lineSeparator(), REPORT) + System.lineSeparator(),
                StandardCharsets.UTF_8);
        System.out.println("Prepared Punchy 2.6.2 for 1.21.4: " + REPORT.size() + " rewrites");
    }

    /**
     * Make the runtime gate part of Punchy's own bytecode. This remains valid
     * even before mixins finish applying and guarantees every renderer/state
     * machine observes the EnoughVisuals module toggle in the same frame.
     */
    private static void gatePunchyWithEnoughVisualsModule(Path path) throws IOException {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(Files.readAllBytes(path)).accept(node, 0);
        MethodNode target = null;
        for (MethodNode method : node.methods) {
            if (method.name.equals("isModEnabled") && method.desc.equals("()Z")) {
                target = method;
                break;
            }
        }
        if (target == null) {
            throw new IOException("PunchyConfig.isModEnabled() shape changed upstream");
        }
        int returns = 0;
        for (var insn = target.instructions.getFirst(); insn != null; ) {
            var next = insn.getNext();
            if (insn.getOpcode() == Opcodes.IRETURN) {
                org.objectweb.asm.tree.InsnList gate = new org.objectweb.asm.tree.InsnList();
                gate.add(new org.objectweb.asm.tree.MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "fun/enoughvisuals/systems/modules/modules/visual/BetterHands",
                        "isHoldMyItemsEnabled", "()Z", false));
                gate.add(new org.objectweb.asm.tree.InsnNode(Opcodes.IAND));
                target.instructions.insertBefore(insn, gate);
                returns++;
            }
            insn = next;
        }
        if (returns == 0) {
            throw new IOException("PunchyConfig.isModEnabled() has no boolean return");
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        Files.write(path, writer.toByteArray());
        REPORT.add("PunchyConfig: isModEnabled hard-gated by Hold My Items");
    }

    private static void patchEmbeddedModContainer(Path path) throws IOException {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(Files.readAllBytes(path)).accept(node, 0);
        boolean changed = false;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("onInitializeClient") || !method.desc.equals("()V")) continue;
            method.instructions.clear();
            if (method.tryCatchBlocks != null) method.tryCatchBlocks.clear();
            if (method.localVariables != null) method.localVariables.clear();
            method.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESTATIC,
                    "punchy/client/PunchyClient", "init", "()V", false));
            method.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESTATIC,
                    "punchy/client/command/PunchyCommands", "register", "()V", false));
            method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
            changed = true;
            break;
        }
        if (!changed) throw new IOException("Punchy Fabric entrypoint container lookup changed upstream");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        Files.write(path, writer.toByteArray());
        REPORT.add("PunchyFabricClient: merged animation assets used directly; recursive builtin-pack reload removed");
    }

    private static void mergeBuiltinPack(Path output, Path packRoot) throws IOException {
        if (!Files.isDirectory(packRoot)) {
            throw new IOException("Punchy builtin resource pack is missing: " + packRoot);
        }
        try (var files = Files.walk(packRoot)) {
            for (Path source : files.filter(Files::isRegularFile).toList()) {
                Path relative = packRoot.relativize(source);
                if (relative.toString().equalsIgnoreCase("pack.mcmeta")) continue;
                Path target = output.resolve(relative);
                Files.createDirectories(target.getParent());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        try (var files = Files.walk(packRoot)) {
            for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
        REPORT.add("Punchy resources: builtin pack merged into EnoughVisuals root");
    }

    private static void disable1215LayerTransformLookup(Path path) throws IOException {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(Files.readAllBytes(path)).accept(node, 0);
        boolean changed = false;
        for (MethodNode method : node.methods) {
            boolean layerLookup = method.name.equals("resolveItemTransform")
                    && method.desc.equals("(Lnet/minecraft/class_10444;)Lnet/minecraft/class_804;");
            boolean baselineLookup = method.name.equals("resolveBaselineUseTransform")
                    && method.desc.equals("(Lnet/minecraft/class_1799;Lnet/minecraft/class_811;"
                            + "Lnet/minecraft/class_1309;)Lnet/minecraft/class_804;");
            if (!layerLookup && !baselineLookup) continue;
            method.instructions.clear();
            if (method.tryCatchBlocks != null) method.tryCatchBlocks.clear();
            if (method.localVariables != null) method.localVariables.clear();
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            if (baselineLookup) {
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
                method.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESTATIC,
                        "punchy/compat/Punchy1214Bridge", "resolveBaseline",
                        "(Lnet/minecraft/class_1799;Lnet/minecraft/class_811;"
                                + "Lnet/minecraft/class_1309;)Lnet/minecraft/class_804;", false));
            } else {
                method.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "net/minecraft/class_10444", "method_65609",
                        "()Lnet/minecraft/class_804;", false));
            }
            method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ARETURN));
            method.maxStack = baselineLookup ? 3 : 1;
            changed = true;
        }
        if (!changed) throw new IOException("Punchy layer-transform lookup shape changed upstream");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        Files.write(path, writer.toByteArray());
        REPORT.add("PunchyArmRenderer: 1.21.5 layer accessors replaced with 1.21.4 ItemRenderState transform API");
    }

    /**
     * On 1.21.4 the empty-hand lowering clip and the incoming item's tuning
     * profile meet on the same frame.  That makes the main arm visibly snap
     * (and, with physics enabled, oscillate) when selecting any item from an
     * empty hotbar slot.  There is no outgoing item to lower in this case, so
     * enter the existing hand-in path directly.  Item-to-item and item-to-empty
     * transitions keep Punchy's original two-phase behavior.
     */
    private static void stabilizeEmptyToItemEquip(Path path) throws IOException {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(Files.readAllBytes(path)).accept(node, 0);
        final String owner = "punchy/client/state/HandEquipStateMachine";
        final String watchdogField = "enough$raiseTicks";
        final String loweringWatchdogField = "enough$lowerTicks";
        node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, watchdogField, "I", null, null));
        node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, loweringWatchdogField, "I", null, null));
        MethodNode target = null;
        MethodNode raising = null;
        MethodNode lowering = null;
        for (MethodNode method : node.methods) {
            if (method.name.equals("startLowering")
                    && method.desc.equals("(Lnet/minecraft/class_746;)V")) {
                target = method;
            } else if (method.name.equals("handleRaising")
                    && method.desc.equals("(Lnet/minecraft/class_310;Lnet/minecraft/class_746;"
                            + "Lnet/minecraft/class_1799;Lnet/minecraft/class_1799;Z)V")) {
                raising = method;
            } else if (method.name.equals("handleLowering")
                    && method.desc.equals("(Lnet/minecraft/class_310;Lnet/minecraft/class_746;"
                            + "Lnet/minecraft/class_1799;Lnet/minecraft/class_1799;Z)V")) {
                lowering = method;
            }
        }
        if (target == null || raising == null || lowering == null) {
            throw new IOException("Punchy HandEquipStateMachine equip method shape changed upstream");
        }

        org.objectweb.asm.tree.AbstractInsnNode insertionPoint = null;
        for (var insn = target.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof org.objectweb.asm.tree.FieldInsnNode field
                    && field.getOpcode() == Opcodes.PUTFIELD
                    && field.owner.equals("punchy/client/state/HandEquipStateMachine")
                    && field.name.equals("cycleOff")
                    && field.desc.equals("Z")) {
                insertionPoint = insn;
                break;
            }
        }
        if (insertionPoint == null) {
            throw new IOException("Punchy startLowering cycle initialization changed upstream");
        }

        org.objectweb.asm.tree.LabelNode normalLowering = new org.objectweb.asm.tree.LabelNode();
        org.objectweb.asm.tree.InsnList stable = new org.objectweb.asm.tree.InsnList();
        stable.add(new VarInsnNode(Opcodes.ALOAD, 0));
        stable.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                "punchy/client/state/HandEquipStateMachine", "cycleMain", "Z"));
        stable.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, normalLowering));
        stable.add(new VarInsnNode(Opcodes.ALOAD, 0));
        stable.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                "punchy/client/state/HandEquipStateMachine", "cycleOff", "Z"));
        stable.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFNE, normalLowering));
        stable.add(new VarInsnNode(Opcodes.ALOAD, 0));
        stable.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                "punchy/client/state/HandEquipStateMachine", "renderedMain",
                "Lnet/minecraft/class_1799;"));
        stable.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/class_1799", "method_7960", "()Z", false));
        stable.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, normalLowering));
        stable.add(new VarInsnNode(Opcodes.ALOAD, 0));
        stable.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                "punchy/client/state/HandEquipStateMachine", "pendingMain",
                "Lnet/minecraft/class_1799;"));
        stable.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFNULL, normalLowering));
        stable.add(new VarInsnNode(Opcodes.ALOAD, 0));
        stable.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                "punchy/client/state/HandEquipStateMachine", "pendingMain",
                "Lnet/minecraft/class_1799;"));
        stable.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/class_1799", "method_7960", "()Z", false));
        stable.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFNE, normalLowering));
        stable.add(new VarInsnNode(Opcodes.ALOAD, 0));
        stable.add(new VarInsnNode(Opcodes.ALOAD, 1));
        stable.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESPECIAL,
                "punchy/client/state/HandEquipStateMachine", "forceDirectHandInFromPending",
                "(Lnet/minecraft/class_746;)V", false));
        stable.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        stable.add(normalLowering);
        target.instructions.insert(insertionPoint, stable);

        org.objectweb.asm.tree.LabelNode keepRaising = new org.objectweb.asm.tree.LabelNode();
        org.objectweb.asm.tree.InsnList watchdog = new org.objectweb.asm.tree.InsnList();
        watchdog.add(new VarInsnNode(Opcodes.ALOAD, 0));
        watchdog.add(new org.objectweb.asm.tree.InsnNode(Opcodes.DUP));
        watchdog.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, watchdogField, "I"));
        watchdog.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_1));
        watchdog.add(new org.objectweb.asm.tree.InsnNode(Opcodes.IADD));
        watchdog.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTFIELD,
                owner, watchdogField, "I"));
        watchdog.add(new VarInsnNode(Opcodes.ALOAD, 0));
        watchdog.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, watchdogField, "I"));
        watchdog.add(new org.objectweb.asm.tree.IntInsnNode(Opcodes.BIPUSH, 12));
        watchdog.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IF_ICMPLE, keepRaising));
        watchdog.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC,
                "punchy/client/animation/PunchyAnimationManager", "POSE_HANDLER",
                "Lpunchy/client/animation/PoseHandler;"));
        // A timed-out equip clip must not leave its last sampled pose in the
        // blend-out buffer.  That made custom weapons remain half raised even
        // after the state machine had returned to IDLE.
        watchdog.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "punchy/client/animation/PoseHandler", "resetImmediate", "()V", false));
        watchdog.add(new VarInsnNode(Opcodes.ALOAD, 0));
        watchdog.add(new VarInsnNode(Opcodes.ALOAD, 2));
        watchdog.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESPECIAL,
                owner, "finishRaise", "(Lnet/minecraft/class_746;)V", false));
        watchdog.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        watchdog.add(keepRaising);
        raising.instructions.insert(watchdog);

        org.objectweb.asm.tree.LabelNode keepLowering = new org.objectweb.asm.tree.LabelNode();
        org.objectweb.asm.tree.InsnList loweringWatchdog = new org.objectweb.asm.tree.InsnList();
        loweringWatchdog.add(new VarInsnNode(Opcodes.ALOAD, 0));
        loweringWatchdog.add(new org.objectweb.asm.tree.InsnNode(Opcodes.DUP));
        loweringWatchdog.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, loweringWatchdogField, "I"));
        loweringWatchdog.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_1));
        loweringWatchdog.add(new org.objectweb.asm.tree.InsnNode(Opcodes.IADD));
        loweringWatchdog.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTFIELD,
                owner, loweringWatchdogField, "I"));
        loweringWatchdog.add(new VarInsnNode(Opcodes.ALOAD, 0));
        loweringWatchdog.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, loweringWatchdogField, "I"));
        loweringWatchdog.add(new org.objectweb.asm.tree.IntInsnNode(Opcodes.BIPUSH, 12));
        loweringWatchdog.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IF_ICMPLE, keepLowering));
        loweringWatchdog.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC,
                "punchy/client/animation/PunchyAnimationManager", "POSE_HANDLER",
                "Lpunchy/client/animation/PoseHandler;"));
        loweringWatchdog.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "punchy/client/animation/PoseHandler", "resetImmediate", "()V", false));
        loweringWatchdog.add(new VarInsnNode(Opcodes.ALOAD, 0));
        loweringWatchdog.add(new VarInsnNode(Opcodes.ALOAD, 2));
        loweringWatchdog.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESPECIAL,
                owner, "applyPending", "(Lnet/minecraft/class_746;)V", false));
        loweringWatchdog.add(new VarInsnNode(Opcodes.ALOAD, 0));
        loweringWatchdog.add(new VarInsnNode(Opcodes.ALOAD, 2));
        loweringWatchdog.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESPECIAL,
                owner, "startRaising", "(Lnet/minecraft/class_746;)V", false));
        loweringWatchdog.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        loweringWatchdog.add(keepLowering);
        lowering.instructions.insert(loweringWatchdog);

        for (MethodNode method : node.methods) {
            if ((method.name.equals("startRaising") || method.name.equals("finishRaise"))
                    && method.desc.equals("(Lnet/minecraft/class_746;)V")) {
                org.objectweb.asm.tree.InsnList resetWatchdog = new org.objectweb.asm.tree.InsnList();
                resetWatchdog.add(new VarInsnNode(Opcodes.ALOAD, 0));
                resetWatchdog.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_0));
                resetWatchdog.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTFIELD,
                        owner, watchdogField, "I"));
                method.instructions.insert(resetWatchdog);
            }
            // Do not reset the lowering watchdog in startLowering. Punchy can
            // restart the same hand-out clip every tick while the selected
            // stack is changing; resetting here made the LOWERING phase live
            // forever and left renderedMain on the empty stack until attack.
            if ((method.name.equals("startRaising")
                    || method.name.equals("finishRaise")
                    || method.name.equals("finishLoweringWithoutRaise")
                    || method.name.equals("resetEquipState"))
                    && method.desc.startsWith("(Lnet/minecraft/class_746;")) {
                org.objectweb.asm.tree.InsnList resetLoweringWatchdog = new org.objectweb.asm.tree.InsnList();
                resetLoweringWatchdog.add(new VarInsnNode(Opcodes.ALOAD, 0));
                resetLoweringWatchdog.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_0));
                resetLoweringWatchdog.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTFIELD,
                        owner, loweringWatchdogField, "I"));
                method.instructions.insert(resetLoweringWatchdog);
            }
            if ((method.name.equals("finishRaise")
                    || method.name.equals("finishLoweringWithoutRaise")
                    || method.name.equals("syncRenderedItems")
                    || method.name.equals("resetEquipState"))
                    && method.desc.startsWith("(Lnet/minecraft/class_746;")) {
                org.objectweb.asm.tree.InsnList clearVisuals = new org.objectweb.asm.tree.InsnList();
                clearVisuals.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESTATIC,
                        "punchy/client/animation/ItemAnimationManager", "enough$finishEquipVisuals",
                        "()V", false));
                clearVisuals.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESTATIC,
                        "punchy/client/bedrockitems/BedrockItemAnimationManager",
                        "enough$finishEquipVisuals", "()V", false));
                method.instructions.insert(clearVisuals);
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String left, String right) {
                return "java/lang/Object";
            }
        };
        node.accept(writer);
        Files.write(path, writer.toByteArray());
        REPORT.add("HandEquipStateMachine: direct empty-to-item hand-in plus lowering/raising watchdogs");
    }

    /**
     * Punchy's 1.21.5 hand-in/out renderer does not share Minecraft 1.21.4's
     * held-item equip interpolation.  Trying to replay those clips on 1.21.4
     * leaves weapon-specific item handlers at a partial pose.  Keep Punchy's
     * attacks/use/mining, but hand ordinary item changes to the vanilla held
     * item renderer for a short, deterministic window.
     */
    private static void useVanillaItemEquipTransition(Path path) throws IOException {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(Files.readAllBytes(path)).accept(node, 0);
        final String owner = "punchy/client/state/HandEquipStateMachine";
        final String mainRise = "enough$equipRiseMainTicks";
        final String offRise = "enough$equipRiseOffTicks";
        node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, mainRise, "I", null, null));
        node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, offRise, "I", null, null));
        MethodNode startLowering = null;
        MethodNode handleIdle = null;
        for (MethodNode method : node.methods) {
            if (method.name.equals("startLowering")
                    && method.desc.equals("(Lnet/minecraft/class_746;)V")) {
                startLowering = method;
            } else if (method.name.equals("handleIdle")
                    && method.desc.equals("(Lnet/minecraft/class_310;Lnet/minecraft/class_746;"
                            + "Lnet/minecraft/class_1799;Lnet/minecraft/class_1799;ZZ)V")) {
                handleIdle = method;
            }
        }
        if (startLowering == null || handleIdle == null) {
            throw new IOException("Punchy vanilla equip transition method shape changed upstream");
        }

        startLowering.instructions.clear();
        startLowering.tryCatchBlocks.clear();
        startLowering.localVariables = null;
        org.objectweb.asm.tree.InsnList body = startLowering.instructions;
        // Mining mutates the held ItemStack while the selected item itself is
        // unchanged (damage/components and transient pending snapshots).  The
        // 1.21.5 equip state machine interprets that mutation as a hand swap on
        // 1.21.4, resets the global pose, and arms the hand-in rise every tick.
        // Preserve the active mining loop when every changed hand still holds
        // the same Item instance.  A real sword -> pickaxe/block swap still
        // falls through to the normal equip transition below.
        org.objectweb.asm.tree.LabelNode normalEquipCheck = new org.objectweb.asm.tree.LabelNode();
        org.objectweb.asm.tree.LabelNode miningMainChecked = new org.objectweb.asm.tree.LabelNode();
        org.objectweb.asm.tree.LabelNode miningOffChecked = new org.objectweb.asm.tree.LabelNode();
        body.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESTATIC,
                "punchy/client/state/MiningActionTracker", "isMiningActive", "()Z", false));
        body.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, normalEquipCheck));
        body.add(new VarInsnNode(Opcodes.ALOAD, 1));
        body.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFNULL, normalEquipCheck));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, "pendingMainChange", "Z"));
        body.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, miningMainChecked));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, "renderedMain", "Lnet/minecraft/class_1799;"));
        body.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFNULL, normalEquipCheck));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, "renderedMain", "Lnet/minecraft/class_1799;"));
        body.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/class_1799", "method_7909", "()Lnet/minecraft/class_1792;", false));
        body.add(new VarInsnNode(Opcodes.ALOAD, 1));
        body.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/class_746", "method_6047", "()Lnet/minecraft/class_1799;", false));
        body.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/class_1799", "method_7909", "()Lnet/minecraft/class_1792;", false));
        body.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IF_ACMPNE, normalEquipCheck));
        body.add(miningMainChecked);
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, "pendingOffChange", "Z"));
        body.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, miningOffChecked));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, "renderedOff", "Lnet/minecraft/class_1799;"));
        body.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFNULL, normalEquipCheck));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, "renderedOff", "Lnet/minecraft/class_1799;"));
        body.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/class_1799", "method_7909", "()Lnet/minecraft/class_1792;", false));
        body.add(new VarInsnNode(Opcodes.ALOAD, 1));
        body.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/class_746", "method_6079", "()Lnet/minecraft/class_1799;", false));
        body.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/class_1799", "method_7909", "()Lnet/minecraft/class_1792;", false));
        body.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IF_ACMPNE, normalEquipCheck));
        body.add(miningOffChecked);
        body.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_0));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTSTATIC,
                owner, mainRise, "I"));
        body.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_0));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTSTATIC,
                owner, offRise, "I"));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new VarInsnNode(Opcodes.ALOAD, 1));
        body.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESPECIAL,
                owner, "resetToCurrent", "(Lnet/minecraft/class_746;)V", false));
        body.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        body.add(normalEquipCheck);
        // Forced action paths (notably continuous mining) also call
        // startLowering with pending*Change=true even though the selected stack
        // did not change.  Compare the actual rendered/pending stacks before
        // touching pose handlers or arming the visual rise.
        org.objectweb.asm.tree.LabelNode mainChecked = new org.objectweb.asm.tree.LabelNode();
        body.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_0));
        body.add(new VarInsnNode(Opcodes.ISTORE, 2));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, "pendingMainChange", "Z"));
        body.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, mainChecked));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, "renderedMain", "Lnet/minecraft/class_1799;"));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, "pendingMain", "Lnet/minecraft/class_1799;"));
        body.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESPECIAL,
                owner, "hasItemChanged", "(Lnet/minecraft/class_1799;Lnet/minecraft/class_1799;)Z", false));
        body.add(new VarInsnNode(Opcodes.ISTORE, 2));
        body.add(mainChecked);

        org.objectweb.asm.tree.LabelNode offChecked = new org.objectweb.asm.tree.LabelNode();
        body.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_0));
        body.add(new VarInsnNode(Opcodes.ISTORE, 3));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, "pendingOffChange", "Z"));
        body.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, offChecked));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, "renderedOff", "Lnet/minecraft/class_1799;"));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, "pendingOff", "Lnet/minecraft/class_1799;"));
        body.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESPECIAL,
                owner, "hasItemChanged", "(Lnet/minecraft/class_1799;Lnet/minecraft/class_1799;)Z", false));
        body.add(new VarInsnNode(Opcodes.ISTORE, 3));
        body.add(offChecked);

        org.objectweb.asm.tree.LabelNode realStackChange = new org.objectweb.asm.tree.LabelNode();
        body.add(new VarInsnNode(Opcodes.ILOAD, 2));
        body.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFNE, realStackChange));
        body.add(new VarInsnNode(Opcodes.ILOAD, 3));
        body.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFNE, realStackChange));
        // Clear only the stale equip bookkeeping. Do not reset POSE_HANDLER or
        // item handlers here: the currently playing mining/attack clip owns it.
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new VarInsnNode(Opcodes.ALOAD, 1));
        body.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESPECIAL,
                owner, "resetToCurrent", "(Lnet/minecraft/class_746;)V", false));
        body.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        body.add(realStackChange);
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new VarInsnNode(Opcodes.ALOAD, 1));
        body.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESPECIAL,
                owner, "applyPending", "(Lnet/minecraft/class_746;)V", false));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new VarInsnNode(Opcodes.ALOAD, 1));
        body.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESPECIAL,
                owner, "resetToCurrent", "(Lnet/minecraft/class_746;)V", false));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC,
                "punchy/client/animation/PunchyAnimationManager", "POSE_HANDLER",
                "Lpunchy/client/animation/PoseHandler;"));
        body.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "punchy/client/animation/PoseHandler", "resetImmediate", "()V", false));
        body.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESTATIC,
                "punchy/client/animation/ItemAnimationManager", "enough$finishEquipVisuals",
                "()V", false));
        body.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESTATIC,
                "punchy/client/bedrockitems/BedrockItemAnimationManager", "enough$finishEquipVisuals",
                "()V", false));
        org.objectweb.asm.tree.LabelNode noMain = new org.objectweb.asm.tree.LabelNode();
        body.add(new VarInsnNode(Opcodes.ILOAD, 2));
        body.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, noMain));
        body.add(new org.objectweb.asm.tree.IntInsnNode(Opcodes.BIPUSH, 10));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTSTATIC,
                owner, mainRise, "I"));
        body.add(noMain);
        org.objectweb.asm.tree.LabelNode noOff = new org.objectweb.asm.tree.LabelNode();
        body.add(new VarInsnNode(Opcodes.ILOAD, 3));
        body.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, noOff));
        body.add(new org.objectweb.asm.tree.IntInsnNode(Opcodes.BIPUSH, 10));
        body.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTSTATIC,
                owner, offRise, "I"));
        body.add(noOff);
        body.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));

        org.objectweb.asm.tree.InsnList decay = new org.objectweb.asm.tree.InsnList();
        for (String field : List.of(mainRise, offRise)) {
            org.objectweb.asm.tree.LabelNode done = new org.objectweb.asm.tree.LabelNode();
            decay.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC, owner, field, "I"));
            decay.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFLE, done));
            decay.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC, owner, field, "I"));
            decay.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_1));
            decay.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ISUB));
            decay.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTSTATIC, owner, field, "I"));
            decay.add(done);
        }
        handleIdle.instructions.insert(decay);

        MethodNode rise = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "enough$applyEquipRise", "(Lnet/minecraft/class_4587;Lnet/minecraft/class_742;"
                        + "Lnet/minecraft/class_1306;F)V", null, null);
        org.objectweb.asm.tree.LabelNode riseReturn = new org.objectweb.asm.tree.LabelNode();
        org.objectweb.asm.tree.LabelNode useOff = new org.objectweb.asm.tree.LabelNode();
        org.objectweb.asm.tree.LabelNode ticksReady = new org.objectweb.asm.tree.LabelNode();
        rise.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        rise.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFNULL, riseReturn));
        rise.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        rise.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFNULL, riseReturn));
        rise.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        rise.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFNULL, riseReturn));
        rise.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        rise.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/class_742", "method_6068", "()Lnet/minecraft/class_1306;", false));
        rise.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        rise.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IF_ACMPNE, useOff));
        rise.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC, owner, mainRise, "I"));
        rise.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.GOTO, ticksReady));
        rise.instructions.add(useOff);
        rise.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC, owner, offRise, "I"));
        rise.instructions.add(ticksReady);
        rise.instructions.add(new VarInsnNode(Opcodes.ISTORE, 4));
        rise.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
        rise.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFLE, riseReturn));
        // remaining = (ticks - partialTick) / 10; smoothstep keeps both ends soft.
        rise.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
        rise.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.I2F));
        rise.instructions.add(new VarInsnNode(Opcodes.FLOAD, 3));
        rise.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.FSUB));
        rise.instructions.add(new org.objectweb.asm.tree.LdcInsnNode(10.0f));
        rise.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.FDIV));
        rise.instructions.add(new VarInsnNode(Opcodes.FSTORE, 5));
        rise.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        rise.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.FCONST_0));
        rise.instructions.add(new VarInsnNode(Opcodes.FLOAD, 5));
        rise.instructions.add(new VarInsnNode(Opcodes.FLOAD, 5));
        rise.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.FMUL));
        rise.instructions.add(new org.objectweb.asm.tree.LdcInsnNode(3.0f));
        rise.instructions.add(new org.objectweb.asm.tree.LdcInsnNode(2.0f));
        rise.instructions.add(new VarInsnNode(Opcodes.FLOAD, 5));
        rise.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.FMUL));
        rise.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.FSUB));
        rise.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.FMUL));
        // Punchy's first-person root is rotated 180 degrees before arm/item
        // rendering, so positive local Y is visually below the resting pose.
        rise.instructions.add(new org.objectweb.asm.tree.LdcInsnNode(0.72f));
        rise.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.FMUL));
        rise.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.FCONST_0));
        rise.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/class_4587", "method_46416", "(FFF)V", false));
        rise.instructions.add(riseReturn);
        rise.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        node.methods.add(rise);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String left, String right) {
                return "java/lang/Object";
            }
        };
        node.accept(writer);
        Files.write(path, writer.toByteArray());
        REPORT.add("HandEquipStateMachine: smooth native 4D equip rise (Punchy attacks preserved)");
    }

    private static void addSmooth4dEquipRise(Path path) throws IOException {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(Files.readAllBytes(path)).accept(node, 0);
        int patched = 0;
        for (MethodNode method : node.methods) {
            int poseIndex;
            int playerIndex;
            int armIndex;
            int partialIndex;
            if (method.name.equals("renderArm")
                    && method.desc.equals("(Lnet/minecraft/class_591;Lnet/minecraft/class_742;"
                            + "Lnet/minecraft/class_1306;Lnet/minecraft/class_4587;"
                            + "Lnet/minecraft/class_4597;ILnet/minecraft/class_2960;ZF)V")) {
                playerIndex = 1; armIndex = 2; poseIndex = 3; partialIndex = 8;
            } else if (method.name.equals("renderItemInHand")
                    && method.desc.startsWith("(Lnet/minecraft/class_759;Lnet/minecraft/class_591;"
                            + "Lnet/minecraft/class_746;Lnet/minecraft/class_1306;Lnet/minecraft/class_4587;")) {
                playerIndex = 2; armIndex = 3; poseIndex = 4; partialIndex = 11;
            } else {
                continue;
            }
            org.objectweb.asm.tree.AbstractInsnNode push = null;
            for (var insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof org.objectweb.asm.tree.MethodInsnNode call
                        && call.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && call.owner.equals("net/minecraft/class_4587")
                        && call.name.equals("method_22903") && call.desc.equals("()V")) {
                    push = insn;
                    break;
                }
            }
            if (push == null) throw new IOException("Punchy " + method.name + " matrix push changed upstream");
            org.objectweb.asm.tree.InsnList apply = new org.objectweb.asm.tree.InsnList();
            apply.add(new VarInsnNode(Opcodes.ALOAD, poseIndex));
            apply.add(new VarInsnNode(Opcodes.ALOAD, playerIndex));
            apply.add(new VarInsnNode(Opcodes.ALOAD, armIndex));
            apply.add(new VarInsnNode(Opcodes.FLOAD, partialIndex));
            apply.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESTATIC,
                    "punchy/client/state/HandEquipStateMachine", "enough$applyEquipRise",
                    "(Lnet/minecraft/class_4587;Lnet/minecraft/class_742;Lnet/minecraft/class_1306;F)V", false));
            method.instructions.insert(push, apply);
            patched++;
        }
        if (patched != 2) throw new IOException("Punchy smooth 4D equip expected 2 render hooks, got " + patched);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override protected String getCommonSuperClass(String left, String right) { return "java/lang/Object"; }
        };
        node.accept(writer);
        Files.write(path, writer.toByteArray());
        REPORT.add("PunchyArmRenderer: eased 4D arm/item entrance without vanilla-model handoff");
    }

    private static void stabilizeMiningV2(Path path) throws IOException {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(Files.readAllBytes(path)).accept(node, 0);
        final String owner = "punchy/client/state/MiningStateMachine";
        MethodNode keepActive = null;
        MethodNode restart = null;
        MethodNode startSpeed = null;
        for (MethodNode method : node.methods) {
            if (method.name.equals("shouldKeepMiningActive")
                    && method.desc.equals("(ZLnet/minecraft/class_310;J)Z")) {
                keepActive = method;
            } else if (method.name.equals("restartMiningV2FromTrigger")
                    && method.desc.equals("(Lpunchy/client/animation/data/AnimationClip;)V")) {
                restart = method;
            } else if (method.name.equals("resolveMiningV2StartSpeed")
                    && method.desc.equals("(Lpunchy/client/animation/data/AnimationClip;)F")) {
                startSpeed = method;
            }
        }
        if (keepActive == null || restart == null || startSpeed == null) {
            throw new IOException("Punchy MiningStateMachine v2 method shape changed upstream");
        }

        // mining_v2 already animates arm + itemgrip through the global pose.
        // Do not also run the independent per-item mining timeline: applying
        // both makes a held click repeatedly drag the item through an entry-like
        // pose. The later legacy-mining branch deliberately stays unchanged.
        MethodNode tick = null;
        for (MethodNode method : node.methods) {
            if (method.name.equals("tick") && method.desc.equals("(Lnet/minecraft/class_310;)V")) {
                tick = method;
                break;
            }
        }
        if (tick == null) throw new IOException("Punchy MiningStateMachine.tick missing");
        int v2PersistentCalls = 0;
        for (org.objectweb.asm.tree.AbstractInsnNode insn = tick.instructions.getFirst();
             insn != null; insn = insn.getNext()) {
            if (!(insn instanceof org.objectweb.asm.tree.MethodInsnNode call)
                    || call.getOpcode() != Opcodes.INVOKESTATIC
                    || !call.owner.equals("punchy/client/animation/ItemAnimationManager")
                    || !call.name.equals("setPersistentType")
                    || !call.desc.equals("(Lnet/minecraft/class_1268;Ljava/lang/String;Ljava/lang/Boolean;Z)V")) {
                continue;
            }
            org.objectweb.asm.tree.AbstractInsnNode cursor = call.getNext();
            boolean followedByMiningV2 = false;
            for (int i = 0; cursor != null && i < 12; i++, cursor = cursor.getNext()) {
                if (cursor instanceof org.objectweb.asm.tree.MethodInsnNode next
                        && next.owner.equals(owner) && next.name.equals("tickMiningV2")) {
                    followedByMiningV2 = true;
                    break;
                }
            }
            if (!followedByMiningV2) continue;

            tick.instructions.insertBefore(call, new org.objectweb.asm.tree.InsnNode(Opcodes.POP));
            tick.instructions.insertBefore(call, new org.objectweb.asm.tree.InsnNode(Opcodes.POP));
            call.name = "clearPersistentType";
            call.desc = "(Lnet/minecraft/class_1268;Ljava/lang/String;)V";
            v2PersistentCalls++;
        }
        if (v2PersistentCalls != 1) {
            throw new IOException("Punchy mining_v2 expected one duplicate item handler, got " + v2PersistentCalls);
        }

        keepActive.instructions.clear();
        keepActive.tryCatchBlocks.clear();
        keepActive.localVariables = null;
        org.objectweb.asm.tree.LabelNode checkHeld = new org.objectweb.asm.tree.LabelNode();
        org.objectweb.asm.tree.LabelNode checkAim = new org.objectweb.asm.tree.LabelNode();
        org.objectweb.asm.tree.LabelNode checkRecentTarget = new org.objectweb.asm.tree.LabelNode();
        org.objectweb.asm.tree.LabelNode checkRecentChain = new org.objectweb.asm.tree.LabelNode();
        org.objectweb.asm.tree.LabelNode inactive = new org.objectweb.asm.tree.LabelNode();
        keepActive.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        keepActive.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, checkHeld));
        keepActive.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_1));
        keepActive.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.IRETURN));
        keepActive.instructions.add(checkHeld);
        keepActive.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        keepActive.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, "wasMining", "Z"));
        keepActive.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, inactive));
        // Preserve the minimum two-tick start window.
        keepActive.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        keepActive.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, "minStopTick", "J"));
        keepActive.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.LCONST_0));
        keepActive.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.LCMP));
        keepActive.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFLT, checkAim));
        keepActive.instructions.add(new VarInsnNode(Opcodes.LLOAD, 3));
        keepActive.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        keepActive.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD,
                owner, "minStopTick", "J"));
        keepActive.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.LCMP));
        keepActive.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFGE, checkAim));
        keepActive.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_1));
        keepActive.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.IRETURN));
        keepActive.instructions.add(checkAim);
        keepActive.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        keepActive.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        keepActive.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESPECIAL,
                owner, "isAttackDown", "(Lnet/minecraft/class_310;)Z", false));
        keepActive.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, inactive));
        keepActive.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        keepActive.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        keepActive.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESPECIAL,
                owner, "isAimingTrackableBlockNow", "(Lnet/minecraft/class_310;)Z", false));
        keepActive.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, checkRecentTarget));
        keepActive.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_1));
        keepActive.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.IRETURN));
        keepActive.instructions.add(checkRecentTarget);
        keepActive.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        keepActive.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESPECIAL,
                owner, "hasVeryRecentMiningTarget", "()Z", false));
        keepActive.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, checkRecentChain));
        keepActive.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_1));
        keepActive.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.IRETURN));
        keepActive.instructions.add(checkRecentChain);
        keepActive.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        keepActive.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESPECIAL,
                owner, "hasRecentMiningChain", "()Z", false));
        keepActive.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, inactive));
        keepActive.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_1));
        keepActive.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.IRETURN));
        keepActive.instructions.add(inactive);
        keepActive.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_0));
        keepActive.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.IRETURN));

        // Never accelerate the short start clip based on click frequency.
        startSpeed.instructions.clear();
        startSpeed.tryCatchBlocks.clear();
        startSpeed.localVariables = null;
        startSpeed.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        startSpeed.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESTATIC,
                "punchy/client/animation/PunchyAnimationManager", "clipSpeedMultiplier",
                "(Lpunchy/client/animation/data/AnimationClip;)F", false));
        startSpeed.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.FRETURN));

        // A rapid click during END used resetImmediate(), producing the visible
        // teleport. Blend the new START from the currently sampled pose.
        restart.instructions.clear();
        restart.tryCatchBlocks.clear();
        restart.localVariables = null;
        org.objectweb.asm.tree.LabelNode restartReturn = new org.objectweb.asm.tree.LabelNode();
        restart.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        restart.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFNULL, restartReturn));
        restart.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESTATIC,
                "punchy/client/animation/PunchyAnimationManager", "getMiningArm",
                "()Lnet/minecraft/class_1306;", false));
        restart.instructions.add(new VarInsnNode(Opcodes.ASTORE, 2));
        restart.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        restart.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        restart.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTFIELD,
                owner, "activeArm", "Lnet/minecraft/class_1306;"));
        restart.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        restart.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        restart.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTFIELD,
                owner, "activeClip", "Lpunchy/client/animation/data/AnimationClip;"));
        restart.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC,
                "punchy/client/animation/PunchyAnimationManager", "POSE_HANDLER",
                "Lpunchy/client/animation/PoseHandler;"));
        restart.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        restart.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "punchy/client/animation/PoseHandler", "setActiveArm", "(Lnet/minecraft/class_1306;)V", false));
        restart.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC,
                "punchy/client/animation/PunchyAnimationManager", "POSE_HANDLER",
                "Lpunchy/client/animation/PoseHandler;"));
        restart.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_0));
        restart.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "punchy/client/animation/PoseHandler", "setLooping", "(Z)V", false));
        restart.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC,
                "punchy/client/animation/PunchyAnimationManager", "POSE_HANDLER",
                "Lpunchy/client/animation/PoseHandler;"));
        restart.instructions.add(new org.objectweb.asm.tree.LdcInsnNode(0.10f));
        restart.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "punchy/client/animation/PoseHandler", "setBlendInDurationNext", "(F)V", false));
        restart.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC,
                "punchy/client/animation/PunchyAnimationManager", "POSE_HANDLER",
                "Lpunchy/client/animation/PoseHandler;"));
        restart.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC,
                "punchy/client/animation/PoseHandler$BlendInEasing", "SMOOTH",
                "Lpunchy/client/animation/PoseHandler$BlendInEasing;"));
        restart.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "punchy/client/animation/PoseHandler", "setBlendInEasingNext",
                "(Lpunchy/client/animation/PoseHandler$BlendInEasing;)V", false));
        restart.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC,
                "punchy/client/animation/PunchyAnimationManager", "POSE_HANDLER",
                "Lpunchy/client/animation/PoseHandler;"));
        restart.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        restart.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        restart.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESTATIC,
                "punchy/client/animation/PunchyAnimationManager", "clipSpeedMultiplier",
                "(Lpunchy/client/animation/data/AnimationClip;)F", false));
        restart.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_0));
        restart.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "punchy/client/animation/PoseHandler", "playWithBlendFromCurrentDynamic",
                "(Lpunchy/client/animation/data/AnimationClip;FZ)V", false));
        restart.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        restart.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC,
                "punchy/client/state/MiningStateMachine$MiningV2Phase", "START",
                "Lpunchy/client/state/MiningStateMachine$MiningV2Phase;"));
        restart.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTFIELD,
                owner, "miningV2Phase", "Lpunchy/client/state/MiningStateMachine$MiningV2Phase;"));
        restart.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        restart.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESPECIAL,
                owner, "currentTick", "()J", false));
        restart.instructions.add(new VarInsnNode(Opcodes.LSTORE, 3));
        restart.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        restart.instructions.add(new VarInsnNode(Opcodes.LLOAD, 3));
        restart.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTFIELD,
                owner, "lastTriggerTick", "J"));
        restart.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        restart.instructions.add(new VarInsnNode(Opcodes.LLOAD, 3));
        restart.instructions.add(new org.objectweb.asm.tree.LdcInsnNode(2L));
        restart.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.LADD));
        restart.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTFIELD,
                owner, "minStopTick", "J"));
        restart.instructions.add(restartReturn);
        restart.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override protected String getCommonSuperClass(String left, String right) { return "java/lang/Object"; }
        };
        node.accept(writer);
        Files.write(path, writer.toByteArray());
        REPORT.add("MiningStateMachine: continuous held loop, fixed-speed start, smooth rapid-click restart");
        REPORT.add("MiningStateMachine: duplicate per-item mining transform disabled for mining_v2");
    }

    private static void addEquipVisualReset(Path path, String owner) throws IOException {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(Files.readAllBytes(path)).accept(node, 0);
        for (MethodNode method : node.methods) {
            if (method.name.equals("enough$finishEquipVisuals")) {
                throw new IOException(owner + " already contains the compat reset method");
            }
        }
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "enough$finishEquipVisuals", "()V", null, null);
        emitEquipVisualResetForArm(method, owner, "field_6182");
        emitEquipVisualResetForArm(method, owner, "field_6183");
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        node.methods.add(method);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String left, String right) {
                return "java/lang/Object";
            }
        };
        node.accept(writer);
        Files.write(path, writer.toByteArray());
        REPORT.add(owner + ": explicit hand_in/hand_out visual-handler reset added");
    }

    private static void emitEquipVisualResetForArm(MethodNode method, String owner, String armField) {
        org.objectweb.asm.tree.LabelNode next = new org.objectweb.asm.tree.LabelNode();
        method.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC,
                owner, "HANDLERS", "Ljava/util/EnumMap;"));
        method.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC,
                "net/minecraft/class_1306", armField, "Lnet/minecraft/class_1306;"));
        method.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "java/util/EnumMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
        method.instructions.add(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.CHECKCAST,
                "punchy/client/animation/PoseHandler"));
        method.instructions.add(new VarInsnNode(Opcodes.ASTORE, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFNULL, next));
        // ACTIVE_TYPES is removed as soon as Punchy starts its very long item
        // blend-out (3 seconds), so filtering by that map misses the stale
        // handler that visibly holds the weapon half raised.  This hook only
        // runs when the equip state is finished/reset; clear the per-arm
        // handler unconditionally and let the normal idle action start cleanly
        // on the following frame.
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "punchy/client/animation/PoseHandler", "resetImmediate", "()V", false));
        for (String field : List.of("ACTIVE_CLIPS", "ACTIVE_TYPES", "ACTIVE_ENTRIES")) {
            method.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC,
                    owner, field, "Ljava/util/EnumMap;"));
            method.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC,
                    "net/minecraft/class_1306", armField, "Lnet/minecraft/class_1306;"));
            method.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                    "java/util/EnumMap", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
            method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.POP));
        }
        method.instructions.add(next);
    }

    private static void suspendPendulumDuringEquip(Path path) throws IOException {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(Files.readAllBytes(path)).accept(node, 0);
        MethodNode target = null;
        for (MethodNode method : node.methods) {
            if (method.name.equals("updateFrame")
                    && method.desc.equals("(Lnet/minecraft/class_746;F)V")) {
                target = method;
                break;
            }
        }
        if (target == null) throw new IOException("Punchy PendulumPhysics.updateFrame shape changed upstream");
        org.objectweb.asm.tree.LabelNode normal = new org.objectweb.asm.tree.LabelNode();
        org.objectweb.asm.tree.InsnList guard = new org.objectweb.asm.tree.InsnList();
        guard.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESTATIC,
                "punchy/client/state/HandEquipStateMachine", "isEquipInProgress", "()Z", false));
        guard.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, normal));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
        guard.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESPECIAL,
                "punchy/client/render/PendulumPhysics", "reset", "()V", false));
        guard.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        guard.add(normal);
        target.instructions.insert(guard);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String left, String right) {
                return "java/lang/Object";
            }
        };
        node.accept(writer);
        Files.write(path, writer.toByteArray());
        REPORT.add("PendulumPhysics: stale item inertia is cleared while an equip clip is active");
    }

    private static void disableProceduralIdleArmBob(Path path) throws IOException {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(Files.readAllBytes(path)).accept(node, 0);
        boolean changed = false;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("<clinit>")) continue;
            for (var insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof org.objectweb.asm.tree.FieldInsnNode field
                        && field.getOpcode() == Opcodes.PUTSTATIC
                        && field.owner.equals("punchy/client/state/MovementStateMachine")
                        && field.name.equals("IDLE_INTENSITY")
                        && field.desc.equals("F")) {
                    var value = insn.getPrevious();
                    if (value == null) throw new IOException("MovementStateMachine IDLE_INTENSITY has no initializer");
                    method.instructions.set(value, new org.objectweb.asm.tree.InsnNode(Opcodes.FCONST_0));
                    changed = true;
                    break;
                }
            }
        }
        if (!changed) throw new IOException("MovementStateMachine IDLE_INTENSITY initializer changed upstream");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        Files.write(path, writer.toByteArray());
        REPORT.add("MovementStateMachine: disabled perpetual first-person idle root bob after item equip");
    }

    private static void smoothDefaultHandInAnimation(Path path) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        String key = "\"hand_in\"";
        int keyStart = json.indexOf(key);
        if (keyStart < 0) throw new IOException("Punchy default hand_in animation is missing");
        int objectStart = json.indexOf('{', keyStart + key.length());
        if (objectStart < 0) throw new IOException("Punchy default hand_in animation is malformed");
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        int objectEnd = -1;
        for (int i = objectStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') inString = true;
            else if (c == '{') depth++;
            else if (c == '}' && --depth == 0) {
                objectEnd = i + 1;
                break;
            }
        }
        if (objectEnd < 0) throw new IOException("Punchy default hand_in object is unterminated");
        String replacement = "\"hand_in\": {\n"
                + "\t\t\t\"animation_length\": 0.32,\n"
                + "\t\t\t\"bones\": {\n"
                + "\t\t\t\t\"right_arm\": {\n"
                + "\t\t\t\t\t\"rotation\": {\"0.0\": [34, -4, -10], \"0.12\": [13, -1, 2], \"0.24\": [3, 0, 0], \"0.32\": [0, 0, 0]},\n"
                + "\t\t\t\t\t\"position\": {\"0.0\": [5, -9, -3], \"0.12\": [1.4, -3, 0], \"0.24\": [0.2, -0.5, 0], \"0.32\": [0, 0, 0]}\n"
                + "\t\t\t\t},\n"
                + "\t\t\t\t\"left_arm\": {\n"
                + "\t\t\t\t\t\"rotation\": {\"0.0\": [34, 4, 10], \"0.12\": [13, 1, -2], \"0.24\": [3, 0, 0], \"0.32\": [0, 0, 0]},\n"
                + "\t\t\t\t\t\"position\": {\"0.0\": [-5, -9, -3], \"0.12\": [-1.4, -3, 0], \"0.24\": [-0.2, -0.5, 0], \"0.32\": [0, 0, 0]}\n"
                + "\t\t\t\t}\n"
                + "\t\t\t}\n"
                + "\t\t}";
        json = json.substring(0, keyStart) + replacement + json.substring(objectEnd);
        Files.writeString(path, json, StandardCharsets.UTF_8);
        REPORT.add("player.animation.json: default hand_in replaced with a monotonic 0.32s equip curve");
    }

    private static void smoothMiningAnimationTimelines(Path path) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        Map<String, String> speeds = new java.util.LinkedHashMap<>();
        for (String family : List.of("new_mining_forward", "new_mining_digging",
                "new_mining_sideways", "new_mining_st", "full_block_mining")) {
            speeds.put(family + "_start", "1.0");
            speeds.put(family + "_loop", "1.35");
            speeds.put(family + "_end", "1.0");
        }
        int patched = 0;
        for (Map.Entry<String, String> entry : speeds.entrySet()) {
            String key = "\"" + entry.getKey() + "\"";
            int keyStart = json.indexOf(key);
            if (keyStart < 0) continue;
            int objectStart = json.indexOf('{', keyStart + key.length());
            int objectEnd = findJsonObjectEnd(json, objectStart);
            if (objectStart < 0 || objectEnd < 0) {
                throw new IOException("Punchy mining animation object malformed: " + entry.getKey());
            }
            String object = json.substring(objectStart, objectEnd);
            String replacement = object.replaceAll("anim_speed_[0-9]+(?:\\.[0-9]+)?",
                    "anim_speed_" + entry.getValue());
            if (!replacement.equals(object)) {
                json = json.substring(0, objectStart) + replacement + json.substring(objectEnd);
                patched++;
            }
        }
        if (patched < 12) {
            throw new IOException("Punchy mining smoothing expected at least 12 timelines, got " + patched);
        }
        Files.writeString(path, json, StandardCharsets.UTF_8);
        REPORT.add("player.animation.json: normalized mining_v2 start/end=1.0x and loop=1.35x");
    }

    private static int findJsonObjectEnd(String json, int objectStart) {
        if (objectStart < 0) return -1;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = objectStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') inString = true;
            else if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return i + 1;
        }
        return -1;
    }

    /**
     * Small intermediary bridge for the parameters added in 1.21.4. Loom remaps
     * this generated class together with the rest of Punchy.
     */
    private static void write1214Bridge(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        String bridge = "punchy/compat/Punchy1214Bridge";
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                bridge, null, "java/lang/Object", null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        init.visitCode(); init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN); init.visitMaxs(0, 0); init.visitEnd();

        MethodVisitor model = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "updateItemModel", "(Lnet/minecraft/class_10442;Lnet/minecraft/class_10444;"
                        + "Lnet/minecraft/class_1799;Lnet/minecraft/class_811;"
                        + "Lnet/minecraft/class_1309;)V", null, null);
        model.visitCode();
        model.visitVarInsn(Opcodes.ALOAD, 0); model.visitVarInsn(Opcodes.ALOAD, 1);
        model.visitVarInsn(Opcodes.ALOAD, 2); model.visitVarInsn(Opcodes.ALOAD, 3);
        emitLeftHand(model, 3);
        model.visitVarInsn(Opcodes.ALOAD, 4);
        model.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/class_10442", "method_65597",
                "(Lnet/minecraft/class_10444;Lnet/minecraft/class_1799;Lnet/minecraft/class_811;"
                        + "ZLnet/minecraft/class_1309;)V", false);
        model.visitInsn(Opcodes.RETURN); model.visitMaxs(0, 0); model.visitEnd();

        MethodVisitor held = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "renderHeldItem", "(Lnet/minecraft/class_759;Lnet/minecraft/class_1309;"
                        + "Lnet/minecraft/class_1799;Lnet/minecraft/class_811;"
                        + "Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V", null, null);
        held.visitCode();
        held.visitVarInsn(Opcodes.ALOAD, 0); held.visitVarInsn(Opcodes.ALOAD, 1);
        held.visitVarInsn(Opcodes.ALOAD, 2); held.visitVarInsn(Opcodes.ALOAD, 3);
        emitLeftHand(held, 3);
        held.visitVarInsn(Opcodes.ALOAD, 4); held.visitVarInsn(Opcodes.ALOAD, 5);
        held.visitVarInsn(Opcodes.ILOAD, 6);
        held.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/class_759", "method_3233",
                "(Lnet/minecraft/class_1309;Lnet/minecraft/class_1799;Lnet/minecraft/class_811;"
                        + "ZLnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V", false);
        held.visitInsn(Opcodes.RETURN); held.visitMaxs(0, 0); held.visitEnd();

        MethodVisitor block = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "renderBlockEntity", "(Lnet/minecraft/class_827;Lnet/minecraft/class_2586;F"
                        + "Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;II"
                        + "Lnet/minecraft/class_243;)V", null, null);
        block.visitCode();
        block.visitVarInsn(Opcodes.ALOAD, 0); block.visitVarInsn(Opcodes.ALOAD, 1);
        block.visitVarInsn(Opcodes.FLOAD, 2); block.visitVarInsn(Opcodes.ALOAD, 3);
        block.visitVarInsn(Opcodes.ALOAD, 4); block.visitVarInsn(Opcodes.ILOAD, 5);
        block.visitVarInsn(Opcodes.ILOAD, 6);
        block.visitMethodInsn(Opcodes.INVOKEINTERFACE, "net/minecraft/class_827", "method_3569",
                "(Lnet/minecraft/class_2586;FLnet/minecraft/class_4587;"
                        + "Lnet/minecraft/class_4597;II)V", true);
        block.visitInsn(Opcodes.RETURN); block.visitMaxs(0, 0); block.visitEnd();

        MethodVisitor baseline = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "resolveBaseline", "(Lnet/minecraft/class_1799;Lnet/minecraft/class_811;"
                        + "Lnet/minecraft/class_1309;)Lnet/minecraft/class_804;", null, null);
        org.objectweb.asm.Label noPack = new org.objectweb.asm.Label();
        org.objectweb.asm.Label unavailable = new org.objectweb.asm.Label();
        baseline.visitCode();
        baseline.visitVarInsn(Opcodes.ALOAD, 0); baseline.visitVarInsn(Opcodes.ALOAD, 1);
        baseline.visitMethodInsn(Opcodes.INVOKESTATIC,
                "punchy/client/render/ResourcePackItemTransformResolver", "resolve",
                "(Lnet/minecraft/class_1799;Lnet/minecraft/class_811;)Lnet/minecraft/class_804;", false);
        baseline.visitInsn(Opcodes.DUP); baseline.visitJumpInsn(Opcodes.IFNULL, noPack);
        baseline.visitInsn(Opcodes.ARETURN);
        baseline.visitLabel(noPack); baseline.visitInsn(Opcodes.POP);
        baseline.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/class_310", "method_1551",
                "()Lnet/minecraft/class_310;", false);
        baseline.visitVarInsn(Opcodes.ASTORE, 3);
        baseline.visitVarInsn(Opcodes.ALOAD, 3); baseline.visitJumpInsn(Opcodes.IFNULL, unavailable);
        baseline.visitVarInsn(Opcodes.ALOAD, 2); baseline.visitJumpInsn(Opcodes.IFNULL, unavailable);
        baseline.visitVarInsn(Opcodes.ALOAD, 3);
        baseline.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/class_310", "method_65386",
                "()Lnet/minecraft/class_10442;", false);
        baseline.visitVarInsn(Opcodes.ASTORE, 4);
        baseline.visitTypeInsn(Opcodes.NEW, "net/minecraft/class_10444"); baseline.visitInsn(Opcodes.DUP);
        baseline.visitMethodInsn(Opcodes.INVOKESPECIAL, "net/minecraft/class_10444", "<init>", "()V", false);
        baseline.visitVarInsn(Opcodes.ASTORE, 5);
        baseline.visitVarInsn(Opcodes.ALOAD, 4); baseline.visitVarInsn(Opcodes.ALOAD, 5);
        baseline.visitVarInsn(Opcodes.ALOAD, 0); baseline.visitVarInsn(Opcodes.ALOAD, 1);
        baseline.visitVarInsn(Opcodes.ALOAD, 2);
        baseline.visitMethodInsn(Opcodes.INVOKESTATIC, bridge, "updateItemModel",
                "(Lnet/minecraft/class_10442;Lnet/minecraft/class_10444;Lnet/minecraft/class_1799;"
                        + "Lnet/minecraft/class_811;Lnet/minecraft/class_1309;)V", false);
        baseline.visitVarInsn(Opcodes.ALOAD, 5);
        baseline.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/class_10444", "method_65609",
                "()Lnet/minecraft/class_804;", false);
        baseline.visitInsn(Opcodes.ARETURN);
        baseline.visitLabel(unavailable); baseline.visitInsn(Opcodes.ACONST_NULL);
        baseline.visitInsn(Opcodes.ARETURN); baseline.visitMaxs(0, 0); baseline.visitEnd();
        cw.visitEnd();
        Files.write(path, cw.toByteArray());
        REPORT.add("bridge: adapted 1.21.4 handed item-model, held-item and block-entity calls");
    }

    private static void emitLeftHand(MethodVisitor mv, int modeLocal) {
        org.objectweb.asm.Label left = new org.objectweb.asm.Label();
        org.objectweb.asm.Label done = new org.objectweb.asm.Label();
        mv.visitVarInsn(Opcodes.ALOAD, modeLocal);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/class_811", "field_4321",
                "Lnet/minecraft/class_811;");
        mv.visitJumpInsn(Opcodes.IF_ACMPEQ, left);
        mv.visitVarInsn(Opcodes.ALOAD, modeLocal);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/class_811", "field_4323",
                "Lnet/minecraft/class_811;");
        mv.visitJumpInsn(Opcodes.IF_ACMPEQ, left);
        mv.visitInsn(Opcodes.ICONST_0); mv.visitJumpInsn(Opcodes.GOTO, done);
        mv.visitLabel(left); mv.visitInsn(Opcodes.ICONST_1); mv.visitLabel(done);
    }

    private static void patchItemRendererMixinTarget(Path path) throws IOException {
        final String oldTarget = "method_3233(Lnet/minecraft/class_1309;Lnet/minecraft/class_1799;"
                + "Lnet/minecraft/class_811;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V";
        final String newTarget = "method_3233(Lnet/minecraft/class_1309;Lnet/minecraft/class_1799;"
                + "Lnet/minecraft/class_811;ZLnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V";
        final String oldHandler = "(Lnet/minecraft/class_1309;Lnet/minecraft/class_1799;"
                + "Lnet/minecraft/class_811;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I"
                + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V";
        final String newHandler = "(Lnet/minecraft/class_1309;Lnet/minecraft/class_1799;"
                + "Lnet/minecraft/class_811;ZLnet/minecraft/class_4587;Lnet/minecraft/class_4597;I"
                + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V";
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(Files.readAllBytes(path)).accept(node, 0);
        boolean changed = false;
        for (MethodNode method : node.methods) {
            rewriteInjectTargets(method.visibleAnnotations, oldTarget, newTarget);
            rewriteInjectTargets(method.invisibleAnnotations, oldTarget, newTarget);
            if (method.desc.equals(oldHandler)) {
                method.desc = newHandler;
                for (var insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof VarInsnNode variable && variable.var >= 4) variable.var++;
                    else if (insn instanceof IincInsnNode increment && increment.var >= 4) increment.var++;
                }
                if (method.localVariables != null) {
                    for (LocalVariableNode local : method.localVariables) if (local.index >= 4) local.index++;
                }
                method.maxLocals++;
            }
            changed = true;
        }
        if (!changed) throw new IOException("Punchy ItemInHandRendererMixin handler shapes changed upstream");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        Files.write(path, writer.toByteArray());
        REPORT.add("punchy/mixin/client/ItemInHandRendererMixin.class: strict renderItem target "
                + "adapted to the 1.21.4 leftHanded parameter");
    }

    private static void rewriteInjectTargets(List<AnnotationNode> annotations,
            String oldTarget, String newTarget) {
        if (annotations == null) return;
        for (AnnotationNode annotation : annotations) {
            if (!annotation.desc.startsWith("Lorg/spongepowered/asm/mixin/injection/")
                    || annotation.values == null) continue;
            for (int i = 1; i < annotation.values.size(); i += 2) {
                Object value = annotation.values.get(i);
                if (value instanceof List<?> list) {
                    @SuppressWarnings("unchecked") List<Object> mutable = (List<Object>) list;
                    for (int j = 0; j < mutable.size(); j++) {
                        if (oldTarget.equals(mutable.get(j))) mutable.set(j, newTarget);
                    }
                } else if (oldTarget.equals(value)) {
                    annotation.values.set(i, newTarget);
                }
            }
        }
    }

    private static void patchBedrockPipeline(Path path) throws IOException {
        final String pipeline = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;";
        ClassReader reader = new ClassReader(Files.readAllBytes(path));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public org.objectweb.asm.FieldVisitor visitField(int access, String name,
                    String descriptor, String signature, Object value) {
                if (descriptor.equals(pipeline)) descriptor = "Ljava/lang/Object;";
                return super.visitField(access, name, descriptor, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                if (name.equals("createEmissiveOverlayPipeline")
                        && descriptor.equals("(Z)" + pipeline)) {
                    MethodVisitor mv = super.visitMethod(access, name, "(Z)Ljava/lang/Object;",
                            null, exceptions);
                    mv.visitCode();
                    mv.visitInsn(Opcodes.ACONST_NULL);
                    mv.visitInsn(Opcodes.ARETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                    return null;
                }
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitFieldInsn(int opcode, String owner, String fieldName,
                            String fieldDescriptor) {
                        if (owner.equals("punchy/client/bedrockitems/BedrockItemRenderer")
                                && fieldDescriptor.equals(pipeline)) {
                            fieldDescriptor = "Ljava/lang/Object;";
                        }
                        super.visitFieldInsn(opcode, owner, fieldName, fieldDescriptor);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                            String methodDescriptor, boolean isInterface) {
                        if (owner.equals("punchy/client/bedrockitems/BedrockItemRenderer")
                                && methodName.equals("createEmissiveOverlayPipeline")
                                && methodDescriptor.equals("(Z)" + pipeline)) {
                            methodDescriptor = "(Z)Ljava/lang/Object;";
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }
                };
            }
        }, 0);
        Files.write(path, writer.toByteArray());
        REPORT.add("punchy/client/bedrockitems/BedrockItemRenderer.class: disabled only the "
                + "1.21.5 emissive RenderPipeline; base Bedrock rendering retained");
    }

    private record ClassApi(String parent, List<String> interfaces,
                            Set<String> fields, Set<String> methods) {}

    private static Set<String> auditMinecraftLinkage(Path output, Path minecraftJar,
            boolean preRemap) throws IOException {
        Map<String, ClassApi> classIndex = new HashMap<>();
        try (ZipFile zip = new ZipFile(minecraftJar.toFile())) {
            for (ZipEntry entry : zip.stream().filter(e -> e.getName().endsWith(".class")).toList()) {
                try (InputStream in = zip.getInputStream(entry)) {
                    ClassReader reader = new ClassReader(in);
                    Set<String> fields = new HashSet<>();
                    Set<String> methods = new HashSet<>();
                    String[] parent = {null};
                    List<String> interfaces = new ArrayList<>();
                    String[] name = {null};
                    reader.accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(int version, int access, String className, String signature,
                                String superName, String[] implemented) {
                            name[0] = className;
                            parent[0] = superName;
                            if (implemented != null) interfaces.addAll(List.of(implemented));
                        }

                        @Override
                        public org.objectweb.asm.FieldVisitor visitField(int access, String fieldName,
                                String descriptor, String signature, Object value) {
                            fields.add(fieldName + descriptor);
                            return null;
                        }

                        @Override
                        public MethodVisitor visitMethod(int access, String methodName,
                                String descriptor, String signature, String[] exceptions) {
                            methods.add(methodName + descriptor);
                            return null;
                        }
                    }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    classIndex.put(name[0], new ClassApi(parent[0], interfaces, fields, methods));
                }
            }
        }

        Set<String> missing = new java.util.TreeSet<>();
        try (var files = Files.walk(output)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                String source = output.relativize(path).toString().replace('\\', '/');
                if (source.equals("punchy/mixin/client/ItemModelResolverMixin.class")) continue;
                new ClassReader(Files.readAllBytes(path)).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor,
                            String signature, String[] exceptions) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitMethodInsn(int opcode, String owner, String methodName,
                                    String methodDescriptor, boolean isInterface) {
                                if (owner.startsWith("net/minecraft/")
                                        && !hasMember(classIndex, owner, methodName + methodDescriptor, false,
                                                new HashSet<>())) {
                                    missing.add(source + " -> method " + owner + "." + methodName
                                            + methodDescriptor);
                                }
                            }

                            @Override
                            public void visitFieldInsn(int opcode, String owner, String fieldName,
                                    String fieldDescriptor) {
                                if (owner.startsWith("net/minecraft/")
                                        && !hasMember(classIndex, owner, fieldName + fieldDescriptor, true,
                                                new HashSet<>())) {
                                    missing.add(source + " -> field " + owner + "." + fieldName
                                            + ":" + fieldDescriptor);
                                }
                            }
                        };
                    }
                }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        }
        if (!missing.isEmpty() && preRemap) {
            Files.write(output.resolve("punchy-1.21.4-linkage-errors.txt"), missing,
                    StandardCharsets.UTF_8);
            REPORT.add("pre-remap linkage: " + missing.size()
                    + " references require Loom remapping or explicit compatibility rewrites");
        } else if (missing.isEmpty()) {
            REPORT.add("linkage: every executable Minecraft member reference resolves against 1.21.4");
        }
        return missing;
    }

    private static boolean hasMember(Map<String, ClassApi> api, String owner, String member,
            boolean field, Set<String> seen) {
        if (!seen.add(owner)) return false;
        ClassApi type = api.get(owner);
        if (type == null) {
            if (field) return false;
            return member.equals("ordinal()I")
                    || member.equals("name()Ljava/lang/String;")
                    || member.equals("iterator()Ljava/util/Iterator;")
                    || member.equals("equals(Ljava/lang/Object;)Z")
                    || member.equals("hashCode()I")
                    || member.equals("toString()Ljava/lang/String;")
                    || member.equals("getClass()Ljava/lang/Class;");
        }
        if ((field ? type.fields() : type.methods()).contains(member)) return true;
        if (type.parent() != null && hasMember(api, type.parent(), member, field, seen)) return true;
        for (String iface : type.interfaces()) {
            if (hasMember(api, iface, member, field, seen)) return true;
        }
        return false;
    }

    private static void extract(Path source, Path output) throws IOException {
        try (ZipFile zip = new ZipFile(source.toFile())) {
            for (ZipEntry entry : zip.stream().sorted(Comparator.comparing(ZipEntry::getName)).toList()) {
                String name = entry.getName();
                if (entry.isDirectory() || !included(name)) continue;
                Path target = output.resolve(name).normalize();
                if (!target.startsWith(output.normalize())) {
                    throw new IOException("unsafe Punchy entry: " + name);
                }
                Files.createDirectories(target.getParent());
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static boolean included(String name) {
        if (name.equals("punchy/mixin/client/ItemModelResolverMixin.class")
                || name.equals("punchy/mixin/client/accessor/ItemStackRenderStateAccessor.class")
                || name.equals("punchy/mixin/client/accessor/ItemStackRenderStateLayerAccessor.class")) {
            return false;
        }
        return name.equals("LICENSE_Punchy")
                || name.equals("punchy.mixins.json")
                || name.equals("punchy.compat.mixins.json")
                || name.startsWith("punchy/")
                || name.startsWith("assets/punchy/")
                || name.equals("assets/minecraft/textures/misc/powder_snow_outline.png")
                || name.startsWith("resourcepacks/punchy/")
                || name.startsWith("META-INF/services/punchy.platform.services.");
    }

    private static void patchMixinConfig(Path path) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        json = json.replace("\n\t\t\"client.ItemModelResolverMixin\",", "")
                .replace("\n        \"client.ItemModelResolverMixin\",", "")
                .replace("\n\t\t\"client.accessor.ItemStackRenderStateLayerAccessor\",", "")
                .replace("\n        \"client.accessor.ItemStackRenderStateLayerAccessor\",", "")
                .replace("\n\t\t\"client.accessor.ItemStackRenderStateAccessor\",", "")
                .replace("\n        \"client.accessor.ItemStackRenderStateAccessor\",", "");
        if (!json.contains("\"required\": true") || !json.contains("\"defaultRequire\": 1")) {
            throw new IOException("main Punchy mixin config unexpectedly lost strict requirements");
        }
        Files.writeString(path, json, StandardCharsets.UTF_8);
        REPORT.add("mixin: removed two 1.21.5-only render-state mixins; strict requirements retained");
    }

    private static void patch1214References(Path path, String relative) throws IOException {
        byte[] original = Files.readAllBytes(path);
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        int[] rewrites = {0};
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                            String methodDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && owner.equals("net/minecraft/class_1661")
                                && methodName.equals("method_67532")
                                && methodDescriptor.equals("()I")) {
                            super.visitFieldInsn(Opcodes.GETFIELD, owner, "field_7545", "I");
                            rewrites[0]++;
                            return;
                        }
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && owner.equals("net/minecraft/class_1799")
                                && methodName.equals("method_58694")) {
                            methodName = "method_57381";
                            rewrites[0]++;
                        } else if (opcode == Opcodes.INVOKEVIRTUAL
                                && owner.equals("net/minecraft/class_777")
                                && methodName.equals("comp_3722")
                                && methodDescriptor.equals("()I")) {
                            methodName = "method_3359";
                            rewrites[0]++;
                        } else if (opcode == Opcodes.INVOKEVIRTUAL
                                && owner.equals("net/minecraft/class_777")
                                && methodName.equals("comp_3724")) {
                            methodName = "method_35788";
                            rewrites[0]++;
                        } else if (opcode == Opcodes.INVOKEVIRTUAL
                                && owner.equals("net/minecraft/class_804")
                                && methodName.startsWith("comp_374")
                                && methodDescriptor.equals("()Lorg/joml/Vector3fc;")) {
                            String field = switch (methodName) {
                                case "comp_3747" -> "field_4286";
                                case "comp_3748" -> "field_4287";
                                case "comp_3749" -> "field_4285";
                                default -> null;
                            };
                            if (field != null) {
                                super.visitFieldInsn(Opcodes.GETFIELD, owner, field,
                                        "Lorg/joml/Vector3f;");
                                rewrites[0]++;
                                return;
                            }
                        } else if (opcode == Opcodes.INVOKESPECIAL
                                && owner.equals("net/minecraft/class_804")
                                && methodName.equals("<init>")
                                && methodDescriptor.equals("(Lorg/joml/Vector3fc;Lorg/joml/Vector3fc;"
                                        + "Lorg/joml/Vector3fc;)V")) {
                            methodDescriptor = "(Lorg/joml/Vector3f;Lorg/joml/Vector3f;"
                                    + "Lorg/joml/Vector3f;)V";
                            rewrites[0]++;
                        } else if (opcode == Opcodes.INVOKEVIRTUAL
                                && owner.equals("net/minecraft/class_10442")
                                && methodName.equals("method_65597")
                                && methodDescriptor.equals("(Lnet/minecraft/class_10444;"
                                        + "Lnet/minecraft/class_1799;Lnet/minecraft/class_811;"
                                        + "Lnet/minecraft/class_1309;)V")) {
                            owner = "punchy/compat/Punchy1214Bridge";
                            methodName = "updateItemModel";
                            methodDescriptor = "(Lnet/minecraft/class_10442;Lnet/minecraft/class_10444;"
                                    + "Lnet/minecraft/class_1799;Lnet/minecraft/class_811;"
                                    + "Lnet/minecraft/class_1309;)V";
                            opcode = Opcodes.INVOKESTATIC; isInterface = false; rewrites[0]++;
                        } else if (opcode == Opcodes.INVOKEVIRTUAL
                                && owner.equals("net/minecraft/class_759")
                                && methodName.equals("method_3233")
                                && methodDescriptor.equals("(Lnet/minecraft/class_1309;"
                                        + "Lnet/minecraft/class_1799;Lnet/minecraft/class_811;"
                                        + "Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V")) {
                            owner = "punchy/compat/Punchy1214Bridge";
                            methodName = "renderHeldItem";
                            methodDescriptor = "(Lnet/minecraft/class_759;Lnet/minecraft/class_1309;"
                                    + "Lnet/minecraft/class_1799;Lnet/minecraft/class_811;"
                                    + "Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V";
                            opcode = Opcodes.INVOKESTATIC; isInterface = false; rewrites[0]++;
                        } else if (owner.equals("net/minecraft/class_827")
                                && methodName.equals("method_3569")
                                && methodDescriptor.endsWith("Lnet/minecraft/class_243;)V")) {
                            owner = "punchy/compat/Punchy1214Bridge";
                            methodName = "renderBlockEntity";
                            methodDescriptor = "(Lnet/minecraft/class_827;Lnet/minecraft/class_2586;F"
                                    + "Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;II"
                                    + "Lnet/minecraft/class_243;)V";
                            opcode = Opcodes.INVOKESTATIC; isInterface = false; rewrites[0]++;
                        } else if (opcode == Opcodes.INVOKEVIRTUAL
                                && owner.equals("net/minecraft/class_804")
                                && methodName.equals("method_23075")
                                && methodDescriptor.equals("(ZLnet/minecraft/class_4587$class_4665;)V")) {
                            // This call only derives a particle-root matrix. The 1.21.4 API accepts
                            // MatrixStack rather than Entry; discard the three operands and retain
                            // the already-captured base matrix instead of corrupting hand rendering.
                            super.visitInsn(Opcodes.POP);
                            super.visitInsn(Opcodes.POP);
                            super.visitInsn(Opcodes.POP);
                            rewrites[0]++;
                            return;
                        }
                        if (owner.equals("net/minecraft/class_4587")
                                && methodName.equals("method_22907")
                                && methodDescriptor.equals("(Lorg/joml/Quaternionfc;)V")) {
                            methodDescriptor = "(Lorg/joml/Quaternionf;)V";
                            rewrites[0]++;
                        } else if (owner.equals("net/minecraft/class_4587")
                                && methodName.equals("method_34425")
                                && methodDescriptor.equals("(Lorg/joml/Matrix4fc;)V")) {
                            methodDescriptor = "(Lorg/joml/Matrix4f;)V";
                            rewrites[0]++;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String fieldName,
                            String fieldDescriptor) {
                        if (opcode == Opcodes.GETFIELD
                                && (owner.equals("net/minecraft/class_746")
                                    || owner.equals("net/minecraft/class_1657"))
                                && fieldName.equals("field_6017")
                                && fieldDescriptor.equals("D")) {
                            super.visitFieldInsn(opcode, owner, fieldName, "F");
                            super.visitInsn(Opcodes.F2D);
                            rewrites[0]++;
                            return;
                        }
                        super.visitFieldInsn(opcode, owner, fieldName, fieldDescriptor);
                    }
                };
            }
        }, 0);
        if (rewrites[0] > 0) {
            Files.write(path, writer.toByteArray());
            REPORT.add(relative + ": " + rewrites[0] + " API rewrite(s)");
        }
    }

    private static void noOpOptionalRenderer(Path path, String relative) throws IOException {
        ClassReader reader = new ClassReader(Files.readAllBytes(path));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        List<Method> replaced = new ArrayList<>();
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                if (name.equals("<init>")) {
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
                replaced.add(new Method(access, name, descriptor, signature, exceptions));
                return null;
            }

            @Override
            public void visitEnd() {
                for (Method method : replaced) {
                    int access = method.access() & ~(Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE);
                    MethodVisitor mv = super.visitMethod(access, method.name(), method.descriptor(),
                            method.signature(), method.exceptions());
                    mv.visitCode();
                    Type result = Type.getReturnType(method.descriptor());
                    switch (result.getSort()) {
                        case Type.VOID -> mv.visitInsn(Opcodes.RETURN);
                        case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> {
                            mv.visitInsn(Opcodes.ICONST_0); mv.visitInsn(Opcodes.IRETURN);
                        }
                        case Type.LONG -> { mv.visitInsn(Opcodes.LCONST_0); mv.visitInsn(Opcodes.LRETURN); }
                        case Type.FLOAT -> { mv.visitInsn(Opcodes.FCONST_0); mv.visitInsn(Opcodes.FRETURN); }
                        case Type.DOUBLE -> { mv.visitInsn(Opcodes.DCONST_0); mv.visitInsn(Opcodes.DRETURN); }
                        default -> { mv.visitInsn(Opcodes.ACONST_NULL); mv.visitInsn(Opcodes.ARETURN); }
                    }
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }
                super.visitEnd();
            }
        }, 0);
        Files.write(path, writer.toByteArray());
        REPORT.add(relative + ": 1.21.5-only special renderer disabled; vanilla fallback retained");
    }

    /**
     * Custom Fabric Loader builds do not apply Fabric API's {@code @Accessor}
     * mixin to {@code KeyBinding.CATEGORY_MAP}, causing
     * {@code KeyBindingHelper.registerKeyBinding()} to throw
     * {@code AssertionError}.  Catch it and continue: the {@code KeyBinding}
     * constructor already auto-registers with the vanilla keybinding map, so
     * the keybinding still works without Fabric's wrapper.
     */
    private static void patchClientPlatformKeyBinding(Path path) throws IOException {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(Files.readAllBytes(path)).accept(node, 0);
        final String PLATFORM = "punchy/platform/FabricClientPlatform";
        boolean changed = false;

        // Add LOGGER field if absent
        boolean hasLogger = false;
        for (FieldNode field : node.fields) {
            if (field.name.equals("LOGGER") && field.desc.equals("Lorg/slf4j/Logger;")) {
                hasLogger = true;
                break;
            }
        }
        if (!hasLogger) {
            node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC
                    | Opcodes.ACC_FINAL, "LOGGER", "Lorg/slf4j/Logger;", null, null));
            // Find or create <clinit>
            MethodNode clinit = null;
            for (MethodNode m : node.methods) {
                if (m.name.equals("<clinit>")) {
                    clinit = m;
                    break;
                }
            }
            if (clinit == null) {
                clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                clinit.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
                node.methods.add(clinit);
            }
            // Prepend: LOGGER = LoggerFactory.getLogger("punchy/platform/FabricClientPlatform")
            org.objectweb.asm.tree.InsnList init = new org.objectweb.asm.tree.InsnList();
            init.add(new org.objectweb.asm.tree.LdcInsnNode(PLATFORM));
            init.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESTATIC,
                    "org/slf4j/LoggerFactory", "getLogger",
                    "(Ljava/lang/String;)Lorg/slf4j/Logger;", false));
            init.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTSTATIC, PLATFORM,
                    "LOGGER", "Lorg/slf4j/Logger;"));
            clinit.instructions.insert(init);
        }

        for (MethodNode method : node.methods) {
            if (!method.name.equals("registerKeyMapping")
                    || !method.desc.equals("(Lnet/minecraft/class_304;)V")) {
                continue;
            }
            method.instructions.clear();
            if (method.tryCatchBlocks != null) method.tryCatchBlocks.clear();
            if (method.localVariables != null) method.localVariables.clear();

            org.objectweb.asm.tree.LabelNode tryStart = new org.objectweb.asm.tree.LabelNode();
            org.objectweb.asm.tree.LabelNode tryEnd = new org.objectweb.asm.tree.LabelNode();
            org.objectweb.asm.tree.LabelNode catchStart = new org.objectweb.asm.tree.LabelNode();
            org.objectweb.asm.tree.LabelNode bodyEnd = new org.objectweb.asm.tree.LabelNode();

            method.tryCatchBlocks.add(new org.objectweb.asm.tree.TryCatchBlockNode(
                    tryStart, tryEnd, catchStart, "java/lang/AssertionError"));

            // try { KeyBindingHelper.registerKeyBinding(mapping); }
            method.instructions.add(tryStart);
            method.instructions.add(new org.objectweb.asm.tree.VarInsnNode(Opcodes.ALOAD, 1));
            method.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKESTATIC,
                    "net/fabricmc/fabric/api/client/keybinding/v1/KeyBindingHelper",
                    "registerKeyBinding",
                    "(Lnet/minecraft/class_304;)Lnet/minecraft/class_304;",
                    false));
            method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.POP));
            method.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.GOTO, bodyEnd));

            // } catch (AssertionError e) { LOGGER.warn(...); }
            method.instructions.add(tryEnd);
            method.instructions.add(catchStart);
            method.instructions.add(new org.objectweb.asm.tree.VarInsnNode(Opcodes.ASTORE, 2));
            method.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC, PLATFORM,
                    "LOGGER", "Lorg/slf4j/Logger;"));
            method.instructions.add(new org.objectweb.asm.tree.LdcInsnNode(
                    "EnoughVisuals: Fabric KeyBindingHelper is unavailable " +
                    "(custom loader). Keybinding may still work."));
            method.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(Opcodes.INVOKEINTERFACE,
                    "org/slf4j/Logger", "warn",
                    "(Ljava/lang/String;)V", true));

            // }
            method.instructions.add(bodyEnd);
            method.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
            changed = true;
            break;
        }
        if (!changed) {
            throw new IOException(
                    PLATFORM + ".registerKeyMapping() shape changed upstream");
        }
        ClassWriter writer = new ClassWriter(
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        Files.write(path, writer.toByteArray());
        REPORT.add(PLATFORM + ": registerKeyMapping wrapped in AssertionError try-catch");
    }
}
