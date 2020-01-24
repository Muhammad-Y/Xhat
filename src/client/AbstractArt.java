package client;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.Random;

/**
 * Genererar abstrakt konst.
 */
public class AbstractArt {
	private Graphics2D g2;
	private int maxWidth, maxHeight;
	private Random rand = new Random();
	
	public AbstractArt(Graphics2D g2, int maxWidth, int maxHeight) {
		this.g2 = g2;
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
	}

	/**
	 * Ritar <code>nbrOfLines</code> antal linjer med slumpmässig längd,
	 * tjocklek, färg och placering.
	 * 
	 * @param nbrOfLines Antal linjer som ska ritas.
	 */
	public void drawRandomLines(int nbrOfLines) {
		int width = 1, x1, y1, x2, y2;
		for (int i = 0; i < nbrOfLines; i++) {
			// width = rand.nextInt(16) + 4;
			x1 = rand.nextInt(maxWidth);
			y1 = rand.nextInt(maxHeight);
			x2 = rand.nextInt(maxWidth);
			y2 = rand.nextInt(maxHeight);
			g2.setPaint(getRandomColor()); // ritfärg
			g2.setStroke(new BasicStroke(width)); // bredd
			g2.draw(new Line2D.Double(x1, y1, x2, y2)); // från - till
		}
	}

	/**
	 * Fyller hela fönstret med en slumpmässig färg.
	 */
	public void drawRandomColorBackground() {
		g2.setColor(getRandomColor());
		g2.fillRect(0, 0, maxWidth, maxHeight);
	}

	/**
	 * Fyller hela fönstret med slumpmässiga färgpixlar.
	 */
	public void drawColorNoiseBackground() {
		for (int i = 0; i < maxWidth; i++) {
			for (int j = 0; j < maxHeight; j++) {
				g2.setPaint(getRandomColor());
				g2.drawRect(i, j, 1, 1);
			}
		}
	}

	/**
	 * Genererar en slumpmässig färg.
	 * @return En slumpmässig färg.
	 */
	private Color getRandomColor() {
		return new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
	}

}
