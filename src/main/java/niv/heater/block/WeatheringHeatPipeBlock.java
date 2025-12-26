package niv.heater.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringHeatPipeBlock extends HeatPipeBlock implements WeatheringCopper {

    private final WeatherState weatherState;

    public WeatheringHeatPipeBlock(WeatherState weatherState, Properties settings) {
        super(settings);
        this.weatherState = weatherState;
    }

    @Override
    public WeatherState getAge() {
        return this.weatherState;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        this.applyChangeOverTime(state, level, pos, random);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return WeatheringCopper.getNext(state.getBlock()).isPresent();
    }
}
