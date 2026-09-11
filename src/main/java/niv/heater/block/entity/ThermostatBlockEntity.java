package niv.heater.block.entity;

import static java.util.Objects.requireNonNull;
import static niv.heater.Heater.MOD_ID;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import niv.burning.api.BurningStorage;
import niv.burning.api.FuelVariant;
import niv.heater.block.ThermostatBlock;
import niv.heater.registry.HeaterBlockEntityTypes;
import niv.heater.screen.ThermostatMenu;

@NullMarked
public class ThermostatBlockEntity extends BlockEntity implements MenuProvider {

    public static final String CONTAINER_NAME;

    private static final Component CONTAINER_TITLE;

    private static final String TAG_FILTER;

    static {
        CONTAINER_NAME = "container." + MOD_ID + ".thermostat";
        CONTAINER_TITLE = Component.translatable(CONTAINER_NAME);
        TAG_FILTER = "filter";
    }

    @SuppressWarnings("null")
    private final ThreadLocal<Boolean> hasBeenExploredAlready = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private FuelVariant filter = FuelVariant.BLANK;

    public ThermostatBlockEntity(BlockPos pos, BlockState state) {
        super(HeaterBlockEntityTypes.THERMOSTAT, pos, state);
    }

    public boolean setFilter(@Nullable ItemStack stack) {
        if (stack == null)
            return false;

        var result = FuelVariant.of(stack);
        if (result.isBlank())
            return false;

        this.filter = result;
        return true;
    }

    public void unsetFilter() {
        this.filter = FuelVariant.BLANK;
    }

    private long tryInsert(@Nullable Direction side, @Nullable FuelVariant resource, long maxAmount,
            TransactionContext transaction) {
        if (resource == null)
            return 0L;

        StoragePreconditions.notBlankNotNegative(resource, maxAmount);

        var facing = getBlockState().getOptionalValue(DirectionalBlock.FACING).orElseThrow(IllegalStateException::new);
        if (facing.equals(side))
            return 0L;

        if (this.hasBeenExploredAlready.get().booleanValue()) {
            return 0L;
        } else {
            this.hasBeenExploredAlready.set(Boolean.TRUE);
            transaction.addOuterCloseCallback(result -> this.hasBeenExploredAlready.remove());
        }

        var inserted = this.getBlockState().getBlock() instanceof ThermostatBlock block
                ? block.getAge().ordinal() + 1
                : 0;
        if (inserted >= maxAmount)
            return maxAmount;

        var thisLevel = this.getLevel();
        if (thisLevel != null && (thisLevel.hasNeighborSignal(getBlockPos()) || this.filter.equals(resource))) {
            resource = this.filter.isBlank() ? resource : this.filter;

            var targetPos = getBlockPos().relative(facing);
            var storage = BurningStorage.SIDED.find(thisLevel, targetPos, facing.getOpposite());
            if (storage != null && storage.supportsInsertion())
                inserted += storage.insert(resource, maxAmount - inserted, transaction);
        }

        return inserted;
    }

    public InsertionOnlyStorage<FuelVariant> getBurningStorage(@Nullable Direction side) {
        return (resource, maxAmount, transaction) -> tryInsert(side, resource, maxAmount, requireNonNull(transaction));
    }

    @SuppressWarnings({ "null", "java:S2637" })
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.filter = input.read(TAG_FILTER, FuelVariant.CODEC).orElseGet(FuelVariant::blank);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable(TAG_FILTER, FuelVariant.CODEC, this.filter);
    }

    @SuppressWarnings("null")
    @Override
    protected void applyImplicitComponents(DataComponentGetter getter) {
        super.applyImplicitComponents(getter);
        this.filter = FuelVariant.of(getter
                .getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .copyOne());
    }

    @SuppressWarnings("null")
    @Override
    protected void collectImplicitComponents(Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(DataComponents.CONTAINER, this.filter.isBlank()
                ? ItemContainerContents.EMPTY
                : ItemContainerContents.fromItems(List.of(new ItemStack(this.filter.getObject()))));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard(TAG_FILTER);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        var proxy = new SimpleContainer(new ItemStack(this.filter.getFuel(), 1)) {
            @Override
            public void setChanged() {
                super.setChanged();
                var item = this.getItem(0);
                if (item.isEmpty()) {
                    ThermostatBlockEntity.this.unsetFilter();
                } else {
                    ThermostatBlockEntity.this.setFilter(item);
                }
            }
        };
        return new ThermostatMenu(containerId, inventory, proxy);
    }

    @Override
    public Component getDisplayName() {
        return CONTAINER_TITLE;
    }
}
