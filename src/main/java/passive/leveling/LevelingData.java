package passive.leveling;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record LevelingData(int level, int xp, int prestige) {
    public static final LevelingData DEFAULT = new LevelingData(1, 0, 0);

    public static final Codec<LevelingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(LevelingData::level),
            Codec.INT.fieldOf("xp").forGetter(LevelingData::xp),
            Codec.INT.fieldOf("prestige").forGetter(LevelingData::prestige)
    ).apply(instance, LevelingData::new));

    public static final PacketCodec<ByteBuf, LevelingData> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, LevelingData::level,
            PacketCodecs.INTEGER, LevelingData::xp,
            PacketCodecs.INTEGER, LevelingData::prestige,
            LevelingData::new
    );
}