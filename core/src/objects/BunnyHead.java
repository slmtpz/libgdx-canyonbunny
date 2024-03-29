package objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

import game.Assets;
import util.AudioManager;
import util.CharacterSkin;
import util.Constants;
import util.GamePreferences;

public class BunnyHead extends AbstractGameObject {

	public static final String TAG = BunnyHead.class.getName();

	private final float JUMP_TIME_MAX = 0.3f;
	private final float JUMP_TIME_MIN = 0.1f;
	private final float JUMP_TIME_OFFSET_FLYING = JUMP_TIME_MAX - 0.018f;
	
	public ParticleEffect dustParticles = new ParticleEffect();

	public enum VIEW_DIRECTION {
		LEFT, RIGHT
	}

	public enum JUMP_STATE {
		GROUNDED, FALLING, JUMP_RISING, JUMP_FALLING
	}

	private TextureRegion regHead;

	public VIEW_DIRECTION viewDirection;
	public float timeJumping;
	public JUMP_STATE jumpState;
	public boolean hasFeatherPowerup;
	public float timeLeftFeatherPowerup;

	public BunnyHead() {
		init();
	}

	public void init() {
		dimension.set(1, 1);
		regHead = Assets.instance.bunny.head;
		// Center image on game object
		origin.set(dimension.x / 2, dimension.y / 2);
		// Bounding box for collision detection
		bounds.set(0, 0, dimension.x, dimension.y);
		// Set physics values
		terminalVelocity.set(3.0f, 4.0f);
		friction.set(12.0f, 0.0f);
		acceleration.set(0.0f, -25.0f);
		// View direction
		viewDirection = VIEW_DIRECTION.RIGHT;
		// Jump state
		jumpState = JUMP_STATE.FALLING;
		timeJumping = 0;
		// Power-ups
		hasFeatherPowerup = false;
		timeLeftFeatherPowerup = 0;
		// Particles
		dustParticles.load(Gdx.files.internal("particles/dust.pfx"), Gdx.files.internal("particles"));
	}

	public void setJumping(boolean jumpKeyPressed) {
		switch (jumpState) {
		case GROUNDED: // Character is standing on a platform
			if (jumpKeyPressed) {
				AudioManager.instance.play(Assets.instance.sounds.jump);
				timeJumping = 0;
				jumpState = JUMP_STATE.JUMP_RISING;
			}
			break;
		case JUMP_RISING: // Rising in the air
			if (!jumpKeyPressed) {
				jumpState = JUMP_STATE.JUMP_FALLING;
			}
			break;
		case FALLING: // Falling down
		case JUMP_FALLING: // Falling down after jump
			if (jumpKeyPressed && hasFeatherPowerup) {
				AudioManager.instance.play(Assets.instance.sounds.jumpWithFeather, 1, MathUtils.random(1.0f, 1.1f));
				timeJumping = JUMP_TIME_OFFSET_FLYING;
				jumpState = JUMP_STATE.JUMP_RISING;
			}
		}
	}

	public void setFeatherPowerup(boolean pickedUp) {
		hasFeatherPowerup = pickedUp;
		if (pickedUp) {
			timeLeftFeatherPowerup = Constants.ITEM_FEATHER_POWERUP_DURATION;
		}
	}

	public boolean hasFeatherPowerup() {
		return hasFeatherPowerup && timeLeftFeatherPowerup > 0;
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		if (velocity.x != 0) {
			viewDirection = velocity.x < 0 ? VIEW_DIRECTION.LEFT : VIEW_DIRECTION.RIGHT;
		}
		if (timeLeftFeatherPowerup > 0) {
			timeLeftFeatherPowerup -= deltaTime;
			if (timeLeftFeatherPowerup < 0) {
				timeLeftFeatherPowerup = 0;
				setFeatherPowerup(false);
			}
		}
		dustParticles.update(deltaTime);
	}

	@Override
	protected void updateMotionY(float deltaTime) {
		switch (jumpState) {
		case GROUNDED:
			jumpState = JUMP_STATE.FALLING;
			if (velocity.x != 0) {
				dustParticles.setPosition(position.x + dimension.x / 2, position.y);
				dustParticles.start();
			}
			break;
		case JUMP_RISING:
			timeJumping += deltaTime;
			if (timeJumping <= JUMP_TIME_MAX) {
				velocity.y = terminalVelocity.y;
			}
			break;
		case FALLING:
			break;
		case JUMP_FALLING:
			timeJumping += deltaTime;
			if (timeJumping > 0 && timeJumping <= JUMP_TIME_MIN) {
				velocity.y = terminalVelocity.y;
			}
		}
		if (jumpState != JUMP_STATE.GROUNDED) {
			dustParticles.allowCompletion();
			super.updateMotionY(deltaTime);
		}
	}

	@Override
	public void render(SpriteBatch batch) {
		TextureRegion reg = null;
		
		// Draw particles
		dustParticles.draw(batch);
		
		// Apply Skin Color
		batch.setColor(CharacterSkin.values()[GamePreferences.instance.charSkin].getColor());
		
		// Set special color when game object has a feather power-up
		if (hasFeatherPowerup) {
			batch.setColor(1.0f, 0.8f, 0.0f, 1.0f);
		}
		reg = regHead;
		batch.draw(reg.getTexture(),
				position.x,
				position.y,
				origin.x,
				origin.y,
				dimension.x,
				dimension.y,
				scale.x,
				scale.y,
				rotation,
				reg.getRegionX(),
				reg.getRegionY(), 
				reg.getRegionWidth(),
				reg.getRegionHeight(),
				viewDirection == VIEW_DIRECTION.LEFT, // Character always lookvin the direction it is moving
				false
				);
		batch.setColor(1, 1, 1, 1);
	}
}
