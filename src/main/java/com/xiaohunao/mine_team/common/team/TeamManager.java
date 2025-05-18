package com.xiaohunao.mine_team.common.team;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.xiaohunao.mine_team.MineTeam;
import com.xiaohunao.mine_team.common.compat.LoadedCompat;
import com.xiaohunao.mine_team.common.init.MTAttachmentTypes;
import com.xiaohunao.mine_team.common.mixed.TeamManagerContainer;
import com.xiaohunao.mine_team.common.network.TeamManagerSyncPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;


public class TeamManager extends SavedData {

    private static final String NAME = MineTeam.MODID + "_manager";
    private static final Logger LOGGING = LoggerFactory.getLogger(TeamManager.class);

    private final Map<UUID, Team> taems = Maps.newHashMap();
    private final BiMap<DyeColor,Team> dyeColorTeam = HashBiMap.create();

    private TeamManager clientMonger;
    private Level level;

    public static TeamManager of(Level level) {
        TeamManagerContainer container = (TeamManagerContainer) level;
        TeamManager manager = container.mine_team$getTeamManager();
        if (level instanceof ServerLevel serverLevel) {
            if (manager == null) {
                manager = serverLevel.getDataStorage().computeIfAbsent(new Factory<>(TeamManager::new,
                        (CompoundTag compoundTag, HolderLookup.Provider tag) -> load(serverLevel,compoundTag)), NAME);
                container.mine_team$setTeamManager(manager);
                manager.level = serverLevel;
            }
        } else {
            if (manager == null) {
                manager = new TeamManager();
                container.mine_team$setTeamManager(manager);
            }
            manager.level = level;
        }

        return manager;
    }

    @Override
    public  CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        CompoundTag taems = new CompoundTag();
        this.taems.forEach((uuid, team) -> {
            taems.put(uuid.toString(), team.serializeNBT());
        });
        CompoundTag dyeColorTeam = new CompoundTag();
//        this.dyeColorTeam.forEach((dyeColor, team) -> {
//            dyeColorTeam.put(dyeColor.toString(), team.serializeNBT());
//        });
        compoundTag.put("taems", taems);
//        compoundTag.put("dyeColorTeam", dyeColorTeam);

        if (!level.isClientSide){
            PacketDistributor.sendToAllPlayers(new TeamManagerSyncPayload(compoundTag));
        }
        return compoundTag;
    }

    public static TeamManager load(Level level,CompoundTag compoundTag) {
        TeamManager manager = new TeamManager();
        manager.deserializeNBT(compoundTag);
        if (!level.isClientSide){
            PacketDistributor.sendToAllPlayers(new TeamManagerSyncPayload(compoundTag));
        }
        return manager;
    }

    public void deserializeNBT(CompoundTag compoundTag) {
        for (String uid : compoundTag.getCompound("taems").getAllKeys()) {
            UUID uuid = UUID.fromString(uid);
            Team team = new Team().deserializeNBT(compoundTag.getCompound("taems").getCompound(uid));
            this.taems.put(uuid, team);

            DyeColor dyeColor = DyeColor.byFireworkColor(team.getColor());
            if (dyeColor != null) {
                this.dyeColorTeam.put(dyeColor, team);

            }
        }

//            if (!LoadedCompat.FTB_TEAMS){

//            }
    }


    public boolean isTeamEmpty() {
        return taems.isEmpty();
    }

    public Team createTeam(UUID uuid,int textureDiffuseColor) {
        if (taems.containsKey(uuid)) {
            LOGGING.warn("Team with UUID {} already exists", uuid);
            return null;
        }
        Team team = new Team(uuid, textureDiffuseColor);
        setDirty();
        taems.put(uuid, team);
        return team;
    }

    public Team createTeam(UUID uuid, DyeColor dyeColor) {
        Team team = createTeam(uuid, dyeColor.getFireworkColor());
        dyeColorTeam.put(dyeColor, team);
        setDirty();
        return team;
    }

    public Team getTeam(UUID uuid) {
        return taems.get(uuid);
    }

    public Team getTeam(DyeColor dyeColor) {
        return dyeColorTeam.get(dyeColor);
    }
    public DyeColor getDyeColor(Team team) {
        return dyeColorTeam.inverse().get(team);
    }

    public static Team getTeam(Entity entity) {
        TeamManager manager = TeamManager.of(entity.level());
        if (entity.hasData(MTAttachmentTypes.TEAM)) {
            return manager.taems.get(entity.getData(MTAttachmentTypes.TEAM).getTeamUid());
        }
        return null;
    }
}
