package dev.galar.rank;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: small textual radar snapshot displayed only in the graphical screen. */
public record RotomReportPayload(String report) implements CustomPacketPayload {
    public static final Type<RotomReportPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GalarRank.MOD_ID, "rotom_report"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RotomReportPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RotomReportPayload::report, RotomReportPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
