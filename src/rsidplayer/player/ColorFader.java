package rsidplayer.player;

import java.awt.Color;

class ColorFader {
	float r, g, b;
	float rt, gt, bt;
	float rd, gd, bd;
	float speed;
	
	void newRandom() {
		rt = (float)Math.random();
		gt = (float)Math.random();
		bt = (float)Math.random();
		
		rd = (rt - r) / speed;
		gd = (gt - g) / speed;
		bd = (bt - b) / speed;
	}
	
	void step() {
		r += rd;
		g += gd;
		b += bd;
		
		if(r > 1.0f)
			r = 1.0f;
		if(g > 1.0f)
			g = 1.0f;
		if(b > 1.0f)
			b = 1.0f;
		
		if(r < 0.0f)
			r = 0.0f;
		if(g < 0.0f)
			g = 0.0f;
		if(b < 0.0f)
			b = 0.0f;

		if(Math.abs(rt - r) < 0.1 && Math.abs(gt - g) < 0.1 && Math.abs(bt - b) < 0.1)
			newRandom();
	}
	
	Color getColor() {
		return new Color(r, g, b, 1.0f);
	}
	
	ColorFader(int speed) {
		this.speed = speed;
		r = 0.5f;
		g = 0.5f;
		b = 0.5f;
		newRandom();
	}
	
}
