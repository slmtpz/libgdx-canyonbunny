package game;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Input.Peripheral;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Rectangle;

import objects.BunnyHead;
import objects.BunnyHead.JUMP_STATE;
import objects.Feather;
import objects.GoldCoin;
import objects.Rock;
import screens.DirectedGame;
import screens.MenuScreen;
import screens.transitions.ScreenTransition;
import screens.transitions.ScreenTransitionSlice;
import screens.transitions.ScreenTransitionSlide;
import util.AudioManager;
import util.Constants;

public class WorldController extends InputAdapter {
	private static final String TAG = WorldController.class.getName();
	
	private DirectedGame game;
	
	public CameraHelper cameraHelper;
	public Level level;
	public int lives;
	public float livesVisual;
	public int score;
	public float scoreVisual;
	private float timeLeftGameOverDelay;
	private boolean goalReached;
	private boolean accelerometerAvailable;
	
	// Rectangles for collision detection
	private Rectangle r1 = new Rectangle();
	private Rectangle r2 = new Rectangle();
		
	public WorldController (DirectedGame game) {
		this.game = game;
		init();
	}
		
	public void init () {
		accelerometerAvailable = Gdx.input.isPeripheralAvailable(Peripheral.Accelerometer);
		cameraHelper = new CameraHelper();
		lives = Constants.LIVES_START;
		livesVisual = lives;
		timeLeftGameOverDelay = 0;
		goalReached = false;
		initLevel();
	}
	
	public void initLevel() {
		score = 0;
		scoreVisual = score;
		level = new Level(Constants.LEVEL_01);
		cameraHelper.setTarget(level.bunnyHead);
	}
	
	private void backToMenu () {
		// switch to menu screen
		ScreenTransition transition = ScreenTransitionSlide.init(0.75f, ScreenTransitionSlide.DOWN, false, Interpolation.bounceOut);
		game.setScreen(new MenuScreen(game), transition);
	}

	private void onCollisionBunnyHeadWithRock (Rock rock) {
		BunnyHead bunnyHead = level.bunnyHead;
		float heightDifference = Math.abs(bunnyHead.position.y - ( rock.position.y + rock.bounds.height));
		if(heightDifference > 0.25f) {
			boolean hitRightEdge = bunnyHead.position.x > rock.position.x + rock.bounds.width / 2.0f;
			if (hitRightEdge) {
				bunnyHead.position.x = rock.position.x + rock.bounds.width;
			} else {
				bunnyHead.position.x = rock.position.x - bunnyHead.bounds.width;
			}
			return;
		}
		switch (bunnyHead.jumpState) {
		case GROUNDED:
			break;
		case FALLING:
		case JUMP_FALLING:
			bunnyHead.position.y = rock.position.y + bunnyHead.bounds.height + bunnyHead.origin.y;
			bunnyHead.jumpState = JUMP_STATE.GROUNDED;
			break;
		case JUMP_RISING:
			bunnyHead.position.y = rock.position.y + bunnyHead.bounds.height + bunnyHead.origin.y;
			break;
		}
	}
	
	private void onCollisionBunnyWithGoldCoin (GoldCoin goldcoin) {
		goldcoin.collected = true;
		AudioManager.instance.play(Assets.instance.sounds.pickupCoin);
		score += goldcoin.getScore();
		Gdx.app.log(TAG, "Gold coin collected");
	}
	
	private void onCollisionBunnyWithFeather (Feather feather) {
		feather.collected = true;
		AudioManager.instance.play(Assets.instance.sounds.pickupFeather);
		score += feather.getScore();
		level.bunnyHead.setFeatherPowerup(true);
		Gdx.app.log(TAG, "Feather collected");
	}
	
	private void onCollisionBunnyWithGoal () {
		goalReached = true;
		Gdx.input.vibrate(500);
		backToMenu();
	}
	
	private void testCollisions () {
		r1.set(level.bunnyHead.position.x, level.bunnyHead.position.y, level.bunnyHead.bounds.width, level.bunnyHead.bounds.height);
		// Test collision: Bunny Head <-> Rocks
		for (Rock rock : level.rocks) {
			r2.set(rock.position.x, rock.position.y, rock.bounds.width, rock.bounds.height);
			if (!r1.overlaps(r2)) continue;
			onCollisionBunnyHeadWithRock(rock);
		}
		// Test collision: Bunny Head <-> Gold Coins
		for (GoldCoin goldcoin : level.goldcoins) {
			if (goldcoin.collected) continue;
			r2.set(goldcoin.position.x, goldcoin.position.y, goldcoin.bounds.width, goldcoin.bounds.height);
			if(!r1.overlaps(r2)) continue;
			onCollisionBunnyWithGoldCoin(goldcoin);
			break;
		}
		// Test collision: Bunny Head <-> Feathers
		for (Feather feather : level.feathers) {
			if (feather.collected) continue;
			r2.set(feather.position.x, feather.position.y, feather.bounds.width, feather.bounds.height);
			if(!r1.overlaps(r2)) continue;
			onCollisionBunnyWithFeather(feather);
			break;
		}
		if (!goalReached) {
			r2.set(level.goal.bounds);
			r2.x += level.goal.position.x;
			r2.y += level.goal.position.y;
			if (r1.overlaps(r2)) onCollisionBunnyWithGoal();
		}
	}
	
	public boolean isGameOver() {
		return lives < 0;
	}
	
	public boolean isPlayerInWater() {
		return level.bunnyHead.position.y < -5;
	}
	
	public void update (float deltaTime){
		handleDebugInput(deltaTime);
		if (isGameOver()) {
			timeLeftGameOverDelay -= deltaTime;
			if (timeLeftGameOverDelay < 0) backToMenu();
		} else {
			handleInputGame(deltaTime);
		}
		level.update(deltaTime);
		testCollisions();
		cameraHelper.update(deltaTime);
		if (!isGameOver() && isPlayerInWater()) {
			AudioManager.instance.play(Assets.instance.sounds.liveLost);
			lives--;
			if (isGameOver()) {
				timeLeftGameOverDelay = Constants.TIME_DELAY_GAME_OVER;
			} else {
				initLevel();
			}
		}
		level.mountains.updateScrollPosition(cameraHelper.getPosition());
		if (livesVisual >= lives)
			livesVisual = Math.max(lives, livesVisual - 1 * deltaTime);
		if (scoreVisual < score) 
			scoreVisual = Math.min(score, scoreVisual + 250 * deltaTime);
			
	}
	
	private void handleInputGame (float deltaTime) {
		if (cameraHelper.hasTarget(level.bunnyHead)) {
			
			// Player movement
			if (Gdx.input.isKeyPressed(Keys.LEFT)) {
				level.bunnyHead.velocity.x = -level.bunnyHead.terminalVelocity.x;
			} else if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
				level.bunnyHead.velocity.x = level.bunnyHead.terminalVelocity.x;
			} else {
				// Use accelerometer for movement if available
				if (accelerometerAvailable) {
					// Normalize accelerometer values from [-10, 10] to [-1, 1]
					// which translate to rotations of [-90, 90] degrees
					float amount = Gdx.input.getAccelerometerY() / 10.0f;
					amount *= 90.0f;
					// is angle of rotation inside dead zone ?
					if (Math.abs(amount) < Constants.ACCEL_ANGLE_DEAD_ZONE) {
						amount = 0;
					} else {
						// use the defined max angle of rotation instead of
						// the full 90 degrees for maximum velocity
						amount /= Constants.ACCEL_MAX_ANGLE_MAX_MOVEMENT;
					}
					level.bunnyHead.velocity.x = level.bunnyHead.terminalVelocity.x * amount;
				}
				// Execute auto-forward movement on non-desktop platform
				else if (Gdx.app.getType() != ApplicationType.Desktop) {
					level.bunnyHead.velocity.x = level.bunnyHead.terminalVelocity.x;
				}
			}
			
			// Bunny jump
			if (Gdx.input.isKeyPressed(Keys.SPACE) || (Gdx.input.isTouched())) {
				level.bunnyHead.setJumping(true);
			} else {
				level.bunnyHead.setJumping(false);
			}
		}
	}
	
	private void handleDebugInput (float deltaTime) {
		if (Gdx.app.getType() != ApplicationType.Desktop) return;
		
		if (!cameraHelper.hasTarget(level.bunnyHead)) {	
			// camera controls (move)
			float camMoveSpeed = 5 * deltaTime;
			float camMoveSpeedAccelerationFactor = 5;
			if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)) camMoveSpeed *= camMoveSpeedAccelerationFactor;
			if (Gdx.input.isKeyPressed(Keys.LEFT)) moveCamera(-camMoveSpeed, 0);
			if (Gdx.input.isKeyPressed(Keys.RIGHT)) moveCamera(camMoveSpeed, 0);
			if (Gdx.input.isKeyPressed(Keys.UP)) moveCamera(0, camMoveSpeed);
			if (Gdx.input.isKeyPressed(Keys.DOWN)) moveCamera(0, -camMoveSpeed);
			if (Gdx.input.isKeyPressed(Keys.BACKSPACE)) cameraHelper.setPosition(0, 0);
			
			// camera controls (zoom)
			float camZoomSpeed = 1 * deltaTime;
			float camZoomSpeedAccelerationFactor = 5;
			if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)) camZoomSpeed *= camZoomSpeedAccelerationFactor;
			if (Gdx.input.isKeyPressed(Keys.O)) cameraHelper.addZoom(camZoomSpeed);
			if (Gdx.input.isKeyPressed(Keys.P)) cameraHelper.addZoom(-camZoomSpeed);
			if (Gdx.input.isKeyPressed(Keys.L)) cameraHelper.setZoom(1);
		}
	}
	
	private void moveCamera (float x, float y) {
		x += cameraHelper.getPosition().x;
		y += cameraHelper.getPosition().y;
		cameraHelper.setPosition(x, y);
	}
	
	@Override
	public boolean keyUp (int keycode) {
		// reset game world
		if (keycode == Keys.R) {
			init();
			Gdx.app.debug(TAG, "Game world resetted");
		}
		// Toggle camera follow
		else if (keycode == Keys.ENTER) {
			cameraHelper.setTarget(cameraHelper.hasTarget() ? null : level.bunnyHead);
			Gdx.app.debug(TAG, "Camera follow eneabled: " + cameraHelper.hasTarget());
		}
		// Back to Menu
		else if (keycode == Keys.ESCAPE || keycode == Keys.BACK) {
			backToMenu();
		}
		
		return false;
	}
	
	
	
	
	
	
	
	
}
