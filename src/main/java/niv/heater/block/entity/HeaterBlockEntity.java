package niv.heater.block.entity;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import niv.burning.api.Burning;
import niv.burning.api.BurningContext;
import niv.burning.api.BurningPropagator;
import niv.burning.api.BurningStorage;
import niv.burning.api.base.BurningStorageBlockEntity;
import niv.heater.block.HeaterBlock;
import niv.heater.registry.HeaterBlockEntityTypes;
import niv.heater.screen.HeaterMenu;
import niv.heater.util.HeaterBurningStorage;
import niv.heater.util.HeaterContainer;

public class HeaterBlockEntity extends BlockEntity implements MenuProvider, Nameable {

    public static final String CONTAINER_NAME = "container.heater";

    private final HeaterContainer container = new HeaterContainer() {
        @Override
        protected boolean isFuel(ItemStack itemStack) {
            return HeaterBlockEntity.this.level.fuelValues().isFuel(itemStack);
        }

        @Override
        public void setChanged() {
            HeaterBlockEntity.this.setChanged();
        }
    };

    private final HeaterBurningStorage burningStorage = new HeaterBurningStorage() {
        @Override
        protected void onFinalCommit() {
            HeaterBlockEntity.this.setChanged();
        }
    };

    private final ContainerData burningData = new ContainerData() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0:
                    return HeaterBlockEntity.this.burningStorage.getCurrentBurning();
                case 1:
                    return HeaterBlockEntity.this.burningStorage.getMaxBurning();
                default:
                    throw new IndexOutOfBoundsException(index);
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0:
                    HeaterBlockEntity.this.burningStorage.setCurrentBurning(value);
                    break;
                case 1:
                    HeaterBlockEntity.this.burningStorage.setMaxBurning(value);
                    break;
                default:
                    throw new IndexOutOfBoundsException(index);
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    private LockCode lock;

    @Nullable
    private Component name;

    public HeaterBlockEntity(BlockPos pos, BlockState state) {
        super(HeaterBlockEntityTypes.HEATER, pos, state);
        this.lock = LockCode.NO_LOCK;
        this.name = null;
    }

    public boolean isBurning() {
        return this.burningStorage.getCurrentBurning() > 0;
    }

    private void consumeFuel(BurningContext context, Transaction transaction) {
        var fuelStack = this.container.getItem(0);
        if (!this.isBurning() && !fuelStack.isEmpty()) {
            var fuelItem = fuelStack.getItem();
            var burning = Burning.of(fuelItem, context);
            if (burning != null) {
                fuelStack.shrink(1);
                if (fuelStack.isEmpty()) {
                    var bucketItem = fuelItem.getCraftingRemainder();
                    this.container.setItem(0, bucketItem == null ? ItemStack.EMPTY : bucketItem);
                }
                this.burningStorage.insert(burning.one(), context, transaction);
            }
        }
    }

    public BurningStorage getBurningStorage() {
        return this.burningStorage;
    }

    public Storage<ItemVariant> getItemStorage(@Nullable Direction side) {
        return this.container.getItemStorage(side);
    }

    // BlockEntity

    @Override
    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        this.lock = LockCode.fromTag(valueInput);
        this.name = valueInput.read("CustomName", ComponentSerialization.CODEC).orElse(null);
        ContainerHelper.loadAllItems(valueInput, this.container.items);
        this.burningStorage.load(valueInput);
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        this.lock.addToTag(valueOutput);
        valueOutput.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
        ContainerHelper.saveAllItems(valueOutput, this.container.items);
        this.burningStorage.save(valueOutput);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter getter) {
        super.applyImplicitComponents(getter);
        this.name = getter.get(DataComponents.CUSTOM_NAME);
        this.lock = getter.getOrDefault(DataComponents.LOCK, LockCode.NO_LOCK);
        getter.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.container.getItems());
    }

    @Override
    protected void collectImplicitComponents(Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(DataComponents.CUSTOM_NAME, this.name);
        if (!this.lock.equals(LockCode.NO_LOCK)) {
            builder.set(DataComponents.LOCK, this.lock);
        }
        builder.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.container.getItems()));
    }

    @Override
    public void setChanged() {
        BurningStorageBlockEntity.tryUpdateLitProperty(this, this.burningStorage);
        super.setChanged();
    }

    @Override
    public void preRemoveSideEffects(BlockPos blockPos, BlockState blockState) {
        Containers.dropContents(this.level, blockPos, this.container);
    }

    // MenuProvider

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
        if (BaseContainerBlockEntity.canUnlock(player, this.lock, this.getDisplayName())) {
            return new HeaterMenu(syncId, inventory, container, burningData);
        } else {
            return null;
        }
    }

    // Nameable

    @Override
    public Component getName() {
        return name == null ? Component.translatable(CONTAINER_NAME) : name;
    }

    @Override
    public Component getCustomName() {
        return name;
    }

    public void setCustomName(Component name) {
        this.name = name;
    }

    // Static

    public static void tick(Level level, BlockPos pos, BlockState state, HeaterBlockEntity heater) {
        var context = BurningContext.worldlyContext(level);
        try (var transaction = Transaction.openOuter()) {
            if (heater.isBurning())
                statelessPropagation(level, pos, context, transaction);

            if (heater.isBurning() && state.getBlock() instanceof HeaterBlock block)
                heater.burningStorage.extract(
                        heater.burningStorage.getBurning(context).withValue(block.getAge().ordinal() + 1, context),
                        context,
                        transaction);

            heater.consumeFuel(context, transaction);
            transaction.commit();
        }
    }

    private static void statelessPropagation(
            Level level, BlockPos zero, BurningContext context, Transaction transaction) {
        var storages = new BurningStorage[] { null, null };

        searchBurningStorages(level, zero, (pos, storage) -> {
            if (zero.equals(pos)) {
                storages[0] = storage;
            } else if (storage instanceof HeaterBurningStorage) {
                // ignore
            } else if (storage.supportsInsertion() && storage.getBurning(context).getPercent() <= .9) {
                storages[1] = storage;
                return true;
            }
            return false;
        });

        BurningStorage.transfer(storages[0], storages[1], storages[0].getBurning(context), context, transaction);
    }

    private static final void searchBurningStorages(Level level, BlockPos start,
            BiPredicate<BlockPos, BurningStorage> shallReturn) {
        var open = new LinkedList<Triple<Direction, BlockPos, Integer>>();
        var closed = HashSet.newHashSet(64);
        closed.add(start);

        for (var elem = Triple.of((Direction) null, start, 64); elem != null; elem = open.poll()) {
            var from = elem.getLeft();
            var pos = elem.getMiddle();
            var storage = BurningStorage.SIDED.find(level, pos, from);
            if (storage != null && shallReturn.test(pos, storage))
                return;

            BurningPropagator propagator = null;
            int hops = elem.getRight() - 1;
            if (hops > 0) {
                propagator = BurningPropagator.SIDED.find(level, pos, from);
            }

            if (propagator == null)
                continue;

            var dirs = propagator.evalPropagationTargets(level, pos).toArray(Direction[]::new);

            for (int i = dirs.length - 1; i > 0; --i) {
                int j = level.random.nextInt(i + 1);
                var d = dirs[j];
                dirs[j] = dirs[i];
                dirs[i] = d;
            }

            for (var dir : dirs) {
                var relative = pos.relative(dir);
                if (closed.add(relative))
                    open.addFirst(Triple.of(dir.getOpposite(), relative, hops));
            }
        }
    }
}
