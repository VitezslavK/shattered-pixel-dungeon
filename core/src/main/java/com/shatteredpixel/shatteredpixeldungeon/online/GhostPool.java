/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import com.watabou.utils.Random;

import java.io.IOException;
import java.util.ArrayList;

//Ghosts available to invade this player's runs. Remote ghosts come from other players when the
// online backend is configured, and the player's own past heroes are always kept locally too.
public class GhostPool {

	private static final String LOCAL_FILE = "ghosts.dat";
	private static final int MAX_LOCAL = 20;

	private static final ArrayList<GhostSnapshot> local = new ArrayList<>();
	private static final ArrayList<GhostSnapshot> remote = new ArrayList<>();
	private static boolean localLoaded = false;

	private static final ArrayList<OnlineExtras.BonesEntry> remoteBones = new ArrayList<>();

	//picks remote bones left near the given depth, or null
	public static OnlineExtras.BonesEntry pickBones( int depth ){
		ArrayList<OnlineExtras.BonesEntry> candidates = new ArrayList<>();
		for (OnlineExtras.BonesEntry b : remoteBones){
			if (Math.abs(b.depth - depth) <= 1) candidates.add(b);
		}
		if (candidates.isEmpty()) return null;
		OnlineExtras.BonesEntry b = Random.element(candidates);
		remoteBones.remove(b);
		return b;
	}

	//called when a new run starts
	public static void refresh(){
		loadLocal();
		OnlineExtras.fetchBones(new OnlineExtras.BonesCallback() {
			@Override
			public void onResult(ArrayList<OnlineExtras.BonesEntry> bones) {
				remoteBones.clear();
				remoteBones.addAll(bones);
			}
		});
		OnlineService.fetchRecent(new OnlineService.Callback() {
			@Override
			public void onResult(ArrayList<GhostSnapshot> ghosts) {
				remote.clear();
				String mine = OnlineService.friendCode();
				for (GhostSnapshot g : ghosts){
					if (!mine.equals(g.friendCode)) remote.add(g);
				}
			}
		});
	}

	//called when a run ends, records the hero locally and uploads it if online
	public static void onRunEnd( boolean won ){
		if (Dungeon.hero == null) return;
		GhostSnapshot s = GhostSnapshot.fromHero(Dungeon.hero, won);
		loadLocal();
		local.add(0, s);
		while (local.size() > MAX_LOCAL) local.remove(local.size()-1);
		saveLocal();
		OnlineService.uploadGhost(s);
		if (!won) OnlineExtras.uploadBones(Dungeon.hero);
	}

	//picks a ghost that reached at least close to the given depth, preferring other players
	public static GhostSnapshot pick( int depth ){
		loadLocal();
		ArrayList<GhostSnapshot> candidates = new ArrayList<>();
		for (GhostSnapshot g : remote){
			if (g.depth >= depth - 2) candidates.add(g);
		}
		if (candidates.isEmpty()){
			for (GhostSnapshot g : local){
				if (g.depth >= depth - 2) candidates.add(g);
			}
		}
		return candidates.isEmpty() ? null : Random.element(candidates);
	}

	public static boolean hasAny(){
		loadLocal();
		return !local.isEmpty() || !remote.isEmpty();
	}

	private static void loadLocal(){
		if (localLoaded) return;
		localLoaded = true;
		local.clear();
		try {
			Bundle bundle = FileUtils.bundleFromFile(LOCAL_FILE);
			for (Bundlable b : bundle.getCollection("ghosts")){
				if (b instanceof GhostSnapshot) local.add((GhostSnapshot) b);
			}
		} catch (IOException e){
			//no local ghosts yet
		}
	}

	private static void saveLocal(){
		Bundle bundle = new Bundle();
		bundle.put("ghosts", local);
		try {
			FileUtils.bundleToFile(LOCAL_FILE, bundle);
		} catch (IOException e){
			ShatteredPixelDungeon.reportException(e);
		}
	}
}
