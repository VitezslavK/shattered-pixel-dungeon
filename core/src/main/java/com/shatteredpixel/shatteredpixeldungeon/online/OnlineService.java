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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.watabou.noosa.Game;
import com.watabou.utils.Random;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Properties;

//Talks to the optional online backend (a Supabase project, see docs/online). If no backend is
// configured in assets/online.properties, every online feature quietly stays offline.
public class OnlineService {

	private static String url = null;
	private static String key = null;
	private static boolean configLoaded = false;

	public interface Callback {
		void onResult( ArrayList<GhostSnapshot> ghosts );
	}

	private static void loadConfig(){
		if (configLoaded) return;
		configLoaded = true;
		try {
			FileHandle file = Gdx.files.internal("online.properties");
			if (!file.exists()) return;
			Properties props = new Properties();
			Reader reader = file.reader("UTF-8");
			props.load(reader);
			reader.close();
			String u = props.getProperty("supabase_url", "").trim();
			String k = props.getProperty("supabase_anon_key", "").trim();
			if (u.startsWith("https://") && !k.isEmpty()){
				url = u.endsWith("/") ? u.substring(0, u.length()-1) : u;
				key = k;
			}
		} catch (Exception e){
			url = key = null;
		}
	}

	public static boolean enabled(){
		loadConfig();
		return url != null && key != null;
	}

	//the player's identity, generated once and kept in the game settings
	private static final String KEY_CODE = "online_code";
	private static final String KEY_NAME = "online_name";
	private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

	public static String friendCode(){
		String code = SPDSettings.getString(KEY_CODE, "", 8);
		if (code.length() != 6){
			StringBuilder b = new StringBuilder();
			for (int i = 0; i < 6; i++) b.append(CODE_CHARS.charAt(Random.Int(CODE_CHARS.length())));
			code = b.toString();
			SPDSettings.put(KEY_CODE, code);
		}
		return code;
	}

	public static String playerName(){
		return SPDSettings.getString(KEY_NAME, "Hero", 16);
	}

	public static void playerName( String name ){
		name = name.replaceAll("[^\\p{L}\\p{N} _-]", "").trim();
		if (name.length() > 16) name = name.substring(0, 16);
		if (!name.isEmpty()) SPDSettings.put(KEY_NAME, name);
	}

	public static String friendTarget(){
		return SPDSettings.getString("online_friend", "", 8);
	}

	public static void friendTarget( String code ){
		code = code.toUpperCase().replaceAll("[^A-Z0-9]", "");
		if (code.length() > 6) code = code.substring(0, 6);
		SPDSettings.put("online_friend", code);
	}

	private static Net.HttpRequest request( String method, String path ){
		Net.HttpRequest req = new Net.HttpRequest(method);
		req.setUrl(url + "/rest/v1/" + path);
		req.setHeader("apikey", key);
		req.setHeader("Authorization", "Bearer " + key);
		req.setHeader("Content-Type", "application/json");
		req.setTimeOut(8000);
		return req;
	}

	public static void uploadGhost( GhostSnapshot snapshot ){
		if (!enabled()) return;
		Net.HttpRequest req = request(Net.HttpMethods.POST, "ghosts");
		req.setHeader("Prefer", "return=minimal");
		req.setContent(snapshot.toJson());
		Gdx.net.sendHttpRequest(req, new Net.HttpResponseListener() {
			@Override public void handleHttpResponse(Net.HttpResponse r) { }
			@Override public void failed(Throwable t) { }
			@Override public void cancelled() { }
		});
	}

	public static void fetchRecent( Callback callback ){
		fetch("ghosts?select=*&order=created_at.desc&limit=40", callback);
	}

	public static void fetchFriend( String code, Callback callback ){
		if (!code.matches("[A-Z0-9]{6}")){
			callback.onResult(new ArrayList<>());
			return;
		}
		fetch("ghosts?select=*&friend_code=eq." + code + "&order=created_at.desc&limit=1", callback);
	}

	private static void fetch( String path, Callback callback ){
		if (!enabled()){
			callback.onResult(new ArrayList<>());
			return;
		}
		Gdx.net.sendHttpRequest(request(Net.HttpMethods.GET, path), new Net.HttpResponseListener() {
			@Override
			public void handleHttpResponse(Net.HttpResponse r) {
				ArrayList<GhostSnapshot> result = new ArrayList<>();
				try {
					if (r.getStatus().getStatusCode() == 200) {
						JsonValue arr = new JsonReader().parse(r.getResultAsString());
						for (JsonValue v = arr.child; v != null; v = v.next) {
							GhostSnapshot s = GhostSnapshot.fromJson(v);
							if (s != null) result.add(s);
						}
					}
				} catch (Exception e){
					result.clear();
				}
				deliver(callback, result);
			}

			@Override
			public void failed(Throwable t) {
				deliver(callback, new ArrayList<>());
			}

			@Override
			public void cancelled() {
				deliver(callback, new ArrayList<>());
			}
		});
	}

	//results are always handed back on the game thread
	private static void deliver( Callback callback, ArrayList<GhostSnapshot> result ){
		Game.runOnRenderThread(new com.watabou.utils.Callback() {
			@Override
			public void call() {
				callback.onResult(result);
			}
		});
	}
}
