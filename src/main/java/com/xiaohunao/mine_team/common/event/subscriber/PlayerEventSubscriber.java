package com.xiaohunao.mine_team.common.event.subscriber;

import com.xiaohunao.mine_team.MineTeam;
import com.xiaohunao.mine_team.common.attachment.TeamAttachment;
import com.xiaohunao.mine_team.common.init.MTAttachmentTypes;
import com.xiaohunao.mine_team.common.network.TeamAttachmentSyncPayload;
import com.xiaohunao.mine_team.common.team.TeamManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.UUID;

@EventBusSubscriber(modid = MineTeam.MODID)
public class PlayerEventSubscriber {
    @SubscribeEvent
    public static void onPlayerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel serverLevel = player.serverLevel();
        TeamManager teamManager = TeamManager.of(serverLevel);


        if (teamManager.isTeamEmpty()){
            Arrays.stream(DyeColor.values())
                    .forEach(dyeColor -> {
                        teamManager.createTeam(UUID.randomUUID(), dyeColor);
                    });
        }
        teamManager.setDirty();


        if (!player.hasData(MTAttachmentTypes.TEAM)) {
            TeamAttachment attachment = player.getData(MTAttachmentTypes.TEAM);
            attachment.setTeamUid(teamManager.getTeam(DyeColor.WHITE).getUid())
                    .setCanPvP(false);
            player.setData(MTAttachmentTypes.TEAM, attachment);
        }
        PacketDistributor.sendToPlayer(player, new TeamAttachmentSyncPayload(player.getId(),player.getData(MTAttachmentTypes.TEAM)));
        serverLevel.getDataStorage().save();
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.hasData(MTAttachmentTypes.TEAM)) {
            PacketDistributor.sendToPlayer(player, new TeamAttachmentSyncPayload(player.getId(),player.getData(MTAttachmentTypes.TEAM)));
        }
    }

}