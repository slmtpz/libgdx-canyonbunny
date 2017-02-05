package objects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import game.Assets;

public class GoldCoin extends AbstractGameObject {
	
	private TextureRegion regGoldCoin;
	private static float TIME_DIMENSION_CHANGE = 0.5f;
	private float timeLeftDimensionChange;
	private boolean gettingBigger;
	public boolean collected;
	
	
	public GoldCoin() {
		init();
	}
	
	private void init() {
		dimension.set(0.5f, 0.5f);
		
		regGoldCoin = Assets.instance.goldCoin.goldCoin;
		
		// Set bounding box for collision detection
		bounds.set(0, 0, dimension.x, dimension.y);
		
		timeLeftDimensionChange = TIME_DIMENSION_CHANGE;
		gettingBigger = false;
		collected = false;
	}
	
	public void update (float deltaTime) {
		super.update(deltaTime);
		if (timeLeftDimensionChange < 0) {
			gettingBigger = !gettingBigger;
			timeLeftDimensionChange = TIME_DIMENSION_CHANGE;
		} else {
			timeLeftDimensionChange -= deltaTime;
			if (gettingBigger)
				scale.add(0.01f, 0.01f);
			else
				scale.add(-0.01f, -0.01f);
		}
	}
	
	public void render (SpriteBatch batch) {
		if (collected) return;
		
		TextureRegion reg = null;
		reg = regGoldCoin;
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
				false,
				false
				);
	}
	
	public int getScore() {
		return 100;
	}
}
