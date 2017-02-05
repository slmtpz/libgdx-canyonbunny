package game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Interpolation;

import screens.DirectedGame;
import screens.MenuScreen;
import screens.transitions.ScreenTransition;
import screens.transitions.ScreenTransitionSlice;
import util.AudioManager;
import util.GamePreferences;

public class CanyonBunnyMain extends DirectedGame {

	@Override
	public void create() {
		// Set LibGdx log level
		Gdx.app.setLogLevel(Application.LOG_DEBUG);
		// Load assets
		Assets.instance.init(new AssetManager());
		// Load preferences for audio settings and start playing music
		GamePreferences.instance.load();
		AudioManager.instance.play(Assets.instance.music.song01);
		// Start game at menu screen
		ScreenTransition transition = ScreenTransitionSlice.init(2, ScreenTransitionSlice.UP_DOWN, 10, Interpolation.pow5Out);
		setScreen(new MenuScreen(this), transition);
	}
	
}