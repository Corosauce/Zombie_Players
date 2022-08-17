package com.corosus.zombie_players.util;

import com.corosus.coroutil.util.CULog;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class UtilProfile implements Runnable {

    private static UtilProfile instance;
    private static Thread thread;

    private static GameProfile tempProfile;

    public static UtilProfile getInstance() {
        if (instance == null) {
            instance = new UtilProfile();
        }
        return instance;
    }

    //there were scenarios where GameProfiles .equals comparisons differed with same name, possibly due to some having a UUID in them, using name purely fixes this issue
    public ConcurrentLinkedQueue<String> listProfileRequests = new ConcurrentLinkedQueue<>();

    public ConcurrentHashMap<UUID, CachedPlayerData> lookupUUIDToCachedData = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, CachedPlayerData> lookupNameToCachedData = new ConcurrentHashMap<>();

    public class CachedPlayerData {
        //full profile
        private GameProfile profile;
        private ResourceLocation texture;
        private boolean isSlim = false;

        private MinecraftProfileTexture temp;

        public CachedPlayerData(GameProfile profile) {
            this.profile = profile;
        }

        public GameProfile getProfile() {
            return profile;
        }

        public void setProfile(GameProfile profile) {
            this.profile = profile;
        }

        public ResourceLocation getTexture() {
            return texture;
        }

        public void setTexture(ResourceLocation texture) {
            this.texture = texture;
        }

        public boolean isSlim() {
            return isSlim;
        }

        public void setSlim(boolean slim) {
            isSlim = slim;
        }

        public MinecraftProfileTexture getTemp() {
            return temp;
        }

        public void setTemp(MinecraftProfileTexture temp) {
            this.temp = temp;
        }
    }

    @Override
    public void run() {

        while (!listProfileRequests.isEmpty()) {
            //only fetch, do no remove from list yet
            String profile = listProfileRequests.peek();
            setupProfileData(new GameProfile(null, profile));
            //finally remove from list now that the lookup has the data the main thread will try to lookup first, to help concurrency consistency
            listProfileRequests.poll();

            //before code was improved to remove redundant lookups, api use throttling occurred, if issue, make use of this code again
            /*try {
                Thread.sleep(100);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {

            }*/
        }
    }

    public void tryToAddProfileToLookupQueue(GameProfile profile) {
        if (!listProfileRequests.contains(profile.getName())) {
            CULog.dbg("requesting data for: " + profile.getName());
            listProfileRequests.add(profile.getName());

            if (thread == null || thread.getState() == Thread.State.TERMINATED) {
                thread = new Thread(instance, "Player Profile Data Request Thread");
                thread.start();
            }
        }
    }

    public void setupProfileData(GameProfile profile) {
        try {
            //TileEntitySkull.updateGameprofile can change name to correct casing, this would break lookup
            String originalLookupName = profile.getName();

            this.tempProfile = null;
            //this does more than just get uuid, needs to run every time
            CULog.dbg("fetching profile for " + originalLookupName + " (" + profile.getName() + ")" + ", uuid: " + profile.getId());
            SkullBlockEntity.updateGameprofile(profile, (p_155747_) -> {
                this.tempProfile = p_155747_;
            });
            //keep our threaded design, working around their async, maybe redesign mine later to work in line with theirs
            for (int timeout = 0; timeout < 50 && this.tempProfile == null; timeout++) {
                Thread.sleep(100);
            }
            CULog.dbg("got updated profile for " + originalLookupName + " (" + profile.getName() + ")" + ", uuid: " + profile.getId());

            //make sure network or cache got what it needed
            if (this.tempProfile != null) {

                CachedPlayerData data = new CachedPlayerData(this.tempProfile);

                Minecraft minecraft = Minecraft.getInstance();
                Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = minecraft.getSkinManager().getInsecureSkinInformation(this.tempProfile);
                MinecraftProfileTexture.Type type = MinecraftProfileTexture.Type.SKIN;
                if (map.containsKey(type)){
                    CULog.dbg("set temp data to load from gl context");
                    data.setTemp(map.get(type));
                } else {
                    //happens if a bad name is used, eg one with spaces
                    CULog.dbg("error getting profile texture map data for name " + originalLookupName);
                }

                //add either way to mark it was tried
                lookupNameToCachedData.put(originalLookupName, data);
                lookupUUIDToCachedData.put(this.tempProfile.getId(), data);
            } else {
                CULog.dbg("error2 getting profile texture map data for name " + originalLookupName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
