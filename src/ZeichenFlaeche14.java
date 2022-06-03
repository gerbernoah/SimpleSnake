import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;


/**
 *
 * <p>Diese Klasse stellt eine einfache Zeichenfläche zur Verfügung.</p>
 * 
 * <p>Zunächst muss dazu eine Instanz dieser Klasse erzeugt werden:</p>
 * <p>ZeichenFlaeche meineZeichenFlaeche = new ZeichenFlaeche();</p>
 * 
 * <p>Dann muss ein Fenster mit leerem weißen Hintergrund erzeugt werden. Dazu dient die Methode
 * macheZeichenFlaecheSichtbar():</p>
 * <p>meineZeichenFlaeche.macheZeichenFlaecheSichtbar();</p>
 * 
 * <p></p>
 * <p>Alle Objekte, die gezeichnet werden sollen, müssen mit einer ID erzeugt werden.</p>
 * <p>Die ID ermöglicht das gezielte Löschen einzelner Objekte oder ganzer Gruppen von Objekten, falls diese die gleiche ID besitzen.</p>
 * <p>Außerdem werden die Objekte in der Reihenfolge der ID gezeichnet. Das heißt das Objekt mit der größeren ID wird ein Objekt mit der kleineren ID verdecken.</p>
 * <p></p>
 * 
 * <p></p>
 * <p>Außerdem ist es wichtig zu wissen, dass es zwei Möglichkeiten gibt, den Zeitpunkt des Zeichnens zu steuern.</p>
 * <p></p>
 * <p>1. Das automatische Zeichnen. In diesem Modus zeichnet die Zeichenfläche automatisch und der Benutzer</p>
 * <p>   muss sich nicht darum kümmern.</p>
 * <p>   Anfänger können diesen Modus benutzen, es ist der Standardmodus.</p>
 * <p>   Dieser Modus hat aber einen Nachteil. Wenn der Benutze ein Objekt auf der Zeichenfläche bewegen möchte,</p>
 * <p>   so muss er es erst vom alten Ort löschen und es dann am neuen Ort zeichnen. Wenn das automatische Zeichnen</p>
 * <p>   genau in dem Moment zeichnet, wenn das Objekt gelöscht ist, aber noch nicht neu gezeichnet wurde, so</p>
 * <p>   bleibt das Objekt bis zum nächsten Zeichnen unsichtbar. Der Zeitpunkt des nächsten Zeichnens ist in diesem</p>
 * <p>   Modus aber nicht von Benutzer steuerbar. Es kann in diesem Modus also zum Flickern von Objekten auf</p>
 * <p>   der Zeichenfläche kommen.</p>
 * <p>2. Das manuelle Zeichnen. Um in diesen Modus zu gelangen, muss die Methode</p>
 * <p></p>
 * <p>   stopAutomatischesZeichnen();</p>
 * <p></p>
 * <p>   aufgerufen werden. Danach zeichet die Zeichenfläche nur noch nach manuellem Aufruf des Benutzers.</p>
 * <p>   Das manuelle Zeichnen geschieht durch das Aufrufen der Methode</p>
 * <p></p>
 * <p>   manualPaint();</p>
 * <p></p>
 * <p></p>
 * <p>Um die genauen Funktionen der Methoden zu studieren, bitte weiter unten
 * bei den Methoden schauen.</p>
 * 
 * @author Jens Post
 * @version 14.0 (14.05.2014)
 *
 */

// This version includes output of Strings

// This version 3.0 includes mouse control and deletion of objects. Also the architecture is improved a lot.
// version 3.0 has a lot of additional functionality.
// Version 3.0 had a problem: The rendering is not done properly if objects are drawn directly into JPanel!!
// The Background is "forgotten" and pictures sometimes do not get rendered (for reasons I have not managed to find out).

// This version 4.0 draws with an entirely other strategy:
// 1. Render into an buffer image.
// 2. Draw into the JPanel nothing but the buffer image. The JVM normally renders this correctly (keep fingers crossed ;-)
// Also in this version 4.0 I encountered a treading problem.
// this version 4.0 is NOT YET 100% THREAD SAVE!!
// Added some synchronization to get rid of the most obvious problems (--> deleting objects while drawing them at the same time in different threads).

// This version 5.0 fixes a problem in the "loeschen()" method. Previously not all object with the given ID were deleted.

// This version 7.0 has a paintComponent without bufferImage (same as version 6.0).
// In addition it corrects a big fault from previous versions: The selective drawing does not work!!
// Previously ALL elements where only drawn if an element was deleted of the size of the zeichenfläche had changed.
// This DOES NOT WORK!! Always all elements have to be drawn!!
// --> removed esWurdeEinObjektGeloescht, alteBreite, alteHoehe

// Version 8.0 introduces the featur of layered objekts. This means object with lower ID will be drawn first,
// object with higher ID will be drawn later.
// Also in versoin 8.0 performance improvements are accompished by not doing repaint()-calls every time a new
// object is created.
//Another change in version 8.0 is to do a clone of the ArrayList of the objects and draw with the clone.
//In that way the rendering can be done without synchronization and object can be added while rendering is running.
//THIS BROUGHT AN ENORMOUS PERFORMANCE BOOST FOR VERY MANY OBJECTS ON THE ZEICHENFLAECHE!!

//In version 9.0 it is not any more possible to add objects without ID. This feature was not very useful but proved
//to confuse people a bit.

//In version 10.0 I do the rendering in a (hopefully) clever way in a separate Thread. This is no hard rendering as
//suggested in many books and articles, but a system in which a Thread prepares the paint area by drawing a picture
//of it in a BufferedImage so that the ZeichenFlaeche only has to draw (the readily rendered) picture to itself.
//Main reason for this change is the still occurring blinking of objects in the old version. So main goal for
//this version is to suppress blinking objects while maintaining performance (or even improving it?).

//In version 11:
//Backgroundcolor is not any more hardcoded in Constructor and render() Method but in a variable.
//RenderEngine is now NOT any more a Thread but the ScheduledThreadPoolExecutor class is used to
//implement a timer function which calls regularly the run() method of the RenderEngine.
//Some related smaller things fixed / changed.

// In version 12:
// Changed the manualPaint() to a real manual paint.

//In version 13:
//Changed the the rendering Thread to a ThreadPoolExecutor. This is to avoid Thread creation overhead for each call of
//manualPaint().

// In version 14:
// paintLock is out. If a user wants to set many object in a short time she should stop automatic rendering.
// The paintComponent() is overriden and inside render() is executed. This is due to the fact that Objects
// were sometimes painted (by the manualPaint()) but right afterwards deleted (probably by the system call
// to the inherited paintComponent() method).
// The method hinzufuegen() is now completely syncronized (has caused IndexOutOfBouds in rare cases...)


public class ZeichenFlaeche14 extends JPanel{
	
	private static final long serialVersionUID = 1L;
	
	private JFrame fenster;
	
	private volatile ArrayList<ZuZeichnendesObjekt> zuZeichnendeObjekteListe= new ArrayList<ZuZeichnendesObjekt>();
	
	private final String[] zulassigeFarbenString = {"BLACK", "BLUE", "CYAN", "DARK_GRAY", "GRAY",
													"GREEN", "LIGHT_GRAY", "MAGENTA", "ORANGE",
													"PINK", "RED", "WHITE", "YELLOW"};
	private Color[] zulassigeFarben;
	private final Color defaultFarbe = Color.BLACK;
	private Color aktuelleFarbe = defaultFarbe;
	private final Color defaultHintergrundfarbe = Color.WHITE;
	private Color aktuelleHintergrundfarbe = defaultHintergrundfarbe;

	
	static long gesamtRenderzeit = 0;
	static int anzahlRenderings = 0;
	
	private Renderer renderEngine = null;
	private ThreadPoolExecutor renderThread = null;

	private ScheduledThreadPoolExecutor timer = null;

	


	/**
	 * <p>Diese Methode muß aufgerufen werden, um die Zeichenfläche auf dem
	 * Bildschirm sichtbar zu machen. Nach dem Aufruf dieser Methode ist
	 * die Zeichenfläche leer.</p>
	 */
	public void macheZeichenFlaecheSichtbar(){
		
		initializeColors();				       		// Call own method to create an array of possible colors.
		
		fenster = new JFrame();
		fenster.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		fenster.setTitle("Zeichenfläche");
		fenster.setBackground(aktuelleHintergrundfarbe);
		
		this.setPreferredSize(new Dimension(800,600));
		this.setBackground(aktuelleHintergrundfarbe);
		this.setOpaque(true);
		fenster.setContentPane(this);
		
		fenster.pack();
		fenster.setVisible(true);
				
		this.requestFocusInWindow();				// Gets the key typing focus to this window so that KeyListener will get events.

		renderEngine = new Renderer(this);
		
		renderThread = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
												new SynchronousQueue<Runnable>(),
												new ThreadPoolExecutor.DiscardPolicy());
		renderThread.prestartAllCoreThreads();

		// Die Timer-Funktion für das zeichnen (auch wenn es nicht so aussieht...).
		timer = new ScheduledThreadPoolExecutor(1);
		timer.scheduleAtFixedRate(renderEngine, 50, 16, TimeUnit.MILLISECONDS);
	}

	/**
	 * <p>Diese Methode muß aufgerufen werden, um die Zeichenfläche auf dem
	 * Bildschirm sichtbar zu machen. Nach dem Aufruf dieser Methode ist
	 * die Zeichenfläche leer.</p>
	 */
	public void macheZeichenFlaecheSichtbar(int breite, int hoehe){
		
		initializeColors();				       		// Call own method to create an array of possible colors.
		
		fenster = new JFrame();
		fenster.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		fenster.setTitle("Zeichenfläche");
		fenster.setBackground(aktuelleHintergrundfarbe);
		
		this.setPreferredSize(new Dimension(breite,hoehe));
		this.setBackground(aktuelleHintergrundfarbe);
		this.setOpaque(true);
		fenster.setContentPane(this);
		
		fenster.pack();
		fenster.setVisible(true);
				
		this.requestFocusInWindow();				// Gets the key typing focus to this window so that KeyListener will get events.

		renderEngine = new Renderer(this);
		
		renderThread = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
												new SynchronousQueue<Runnable>(),
												new ThreadPoolExecutor.DiscardPolicy());
		renderThread.prestartAllCoreThreads();

		// Die Timer-Funktion für das zeichnen (auch wenn es nicht so aussieht...).
		timer = new ScheduledThreadPoolExecutor(1);
		timer.scheduleAtFixedRate(renderEngine, 50, 16, TimeUnit.MILLISECONDS);
	}
	
	private void initializeColors(){
		zulassigeFarben = new Color[13];
		zulassigeFarben[0] = Color.BLACK;
		zulassigeFarben[1] = Color.BLUE;
		zulassigeFarben[2] = Color.CYAN;
		zulassigeFarben[3] = Color.DARK_GRAY;
		zulassigeFarben[4] = Color.GRAY;
		zulassigeFarben[5] = Color.GREEN;
		zulassigeFarben[6] = Color.LIGHT_GRAY;
		zulassigeFarben[7] = Color.MAGENTA;
		zulassigeFarben[8] = Color.ORANGE;
		zulassigeFarben[9] = Color.PINK;
		zulassigeFarben[10] = Color.RED;
		zulassigeFarben[11] = Color.WHITE;
		zulassigeFarben[12] = Color.YELLOW;
	}

	
	/**
	 * <p>Setzen der Zeichenfarbe für die Zeichenfläche.</p>
	 * <p>Alles, was im folgenden auf der Zeichenfläche gezeichnet wird, wird in der hier gesetzten Farbe gezeichnet.</p>
	 * <p>Um auf die ursprüngliche Farbe (meist "BLACK") zurückzustellen, muss die resetFarbe() Methode aufgerufen werden.</p>
	 * <p>Zulässige Farbennamen können in der Java-API in der Klasse "Color" nachgesehen werden.</p>
	 * @param farbeString Der Farbenname als String (z.B. "YELLOW", "RED", usw.)
	 */
	// The use of this color system is NOT thread safe!
	// For thread safety use setzeXXXX Methods with method header including id and color!
	public void setzeFarbe(String farbeString){
		
		for(int i = 0; i < zulassigeFarbenString.length; i++){
			if(farbeString.equals(zulassigeFarbenString[i])){
				aktuelleFarbe = zulassigeFarben[i];
			}
		}
	}
	
	/**
	 * <p>Zurücksetzen der Zeichenfarbe für die Zeichenfläche auf den Standardwert.</p>
	 * <p>(Meist "BLACK")</p>
	 */
	public void resetFarbe(){
		
		aktuelleFarbe = defaultFarbe;
	}
	
	/**
	 * <p>Setzen eines Punktes auf der Zeichenfläche.</p>
	 * @param id Die ID des Punktes. Anhand der ID kann der Punkt später gelöscht werden.
	 * @param gegebenexKoordinate Die X-Koordinate, auf die der Punkt auf der Zeichenfläche gesetzt werden soll.
	 * @param gegebeneyKoordinate Die Y-Koordinate, auf die der Punkt auf der Zeichenfläche gesetzt werden soll.
	 */
	public void setzePunkt(int id, double gegebenexKoordinate, double gegebeneyKoordinate){
		
		Punkt neuerPunkt = new Punkt(id, gegebenexKoordinate, gegebeneyKoordinate, aktuelleFarbe);
		
		renderEngine.hinzufuegen(neuerPunkt);
	}
	/**
	 * <p>Setzen eines Punktes auf der Zeichenfläche.</p>
	 * <p>Der Punkt ist wieder löschbar. Löschen mit der loeschen(int id) Methode.</p>
	 * @param id Die ID des Punktes. Anhand der ID kann der Punkt später gelöscht werden.
	 * @param gegebenexKoordinate Die X-Koordinate, auf die der Punkt auf der Zeichenfläche gesetzt werden soll.
	 * @param gegebeneyKoordinate Die Y-Koordinate, auf die der Punkt auf der Zeichenfläche gesetzt werden soll.
	 * @param farbe Die Farbe, mit der der Punkt auf der Zeichenfläche gesetzt werden soll (z.B. Color.BLACK oder Color.RED).
	 */
	public void setzePunkt(int id, double gegebenexKoordinate, double gegebeneyKoordinate, Color farbe){
		
		Punkt neuerPunkt = new Punkt(id, gegebenexKoordinate, gegebeneyKoordinate, farbe);
		
		renderEngine.hinzufuegen(neuerPunkt);
	}

	/**
	 * <p>Setzen einer Linie auf der Zeichenfläche.</p>
	 * @param x1 Die X-Koordinate des Ausgangspunktes.
	 * @param y1 Die Y-Koordinate des Ausgangspunktes.
	 * @param x2 Die X-Koordinate des Zielpunktes.
	 * @param y2 Die Y-Koordinate des Zielpunktes.
	 */
	public void setzeLinie(int id, double x1, double y1, double x2, double y2){
		
		Linie neueLinie = new Linie (id, x1, y1, x2, y2, aktuelleFarbe);
		
		renderEngine.hinzufuegen(neueLinie);
	}
	/**
	 * <p>Setzen einer Linie auf der Zeichenfläche.</p>
	 * <p>Die Linie ist wieder löschbar. Löschen mit der loeschen(int id) Methode.</p>
	 * @param id Die ID der Linie. Anhand der ID kann die Linie später gelöscht werden.
	 * @param x1 Die X-Koordinate des Ausgangspunktes.
	 * @param y1 Die Y-Koordinate des Ausgangspunktes.
	 * @param x2 Die X-Koordinate des Zielpunktes.
	 * @param y2 Die Y-Koordinate des Zielpunktes.
	 * @param farbe Die Farbe, mit der die Linie auf der Zeichenfläche gesetzt werden soll (z.B. Color.BLACK oder Color.RED).
	 */
	public void setzeLinie(int id, double x1, double y1, double x2, double y2, Color farbe){
		
		Linie neueLinie = new Linie (id, x1, y1, x2, y2, farbe);
		
		renderEngine.hinzufuegen(neueLinie);
	}

	/**
	 * <p>Zeichnen einer Elipse auf der Zeichenfläche.</p>
	 * <p>Es wird eine Elipse gezeichnet, der genau in das Rechteck paßt, welches durch die Koordinaten
	 * definiert wird. Die Koordinaten geben die linke obere Ecke und die Breite und Höhe des Rechtecks an.</p>
	 * <p>Werden die Koordinaten so gesetzt, dass das Rechteck ein Quadrat ist, so wird ein Kreis gezeichnet.</p>
	 * @param x Die X-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param y Die Y-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param breite Die Breite des Rechtecks.
	 * @param hoehe Die Höhe des Rechtecks.
	 */
	public void setzeElipse(int id, double x, double y, double breite, double hoehe){
		
		Elipse neueElipse = new Elipse (id, x, y, breite, hoehe, aktuelleFarbe);
		
		renderEngine.hinzufuegen(neueElipse);
	}
	/**
	 * <p>Zeichnen einer Elipse auf der Zeichenfläche.</p>
	 * <p>Es wird eine Elipse gezeichnet, der genau in das Rechteck paßt, welches durch die Koordinaten
	 * definiert wird. Die Koordinaten geben die linke obere Ecke und die Breite und Höhe des Rechtecks an.</p>
	 * <p>Werden die Koordinaten so gesetzt, dass das Rechteck ein Quadrat ist, so wird ein Kreis gezeichnet.</p>
	 * <p>Die Elipse ist wieder löschbar. Löschen mit der loeschen(int id) Methode.</p>
	 * @param id Die ID der Elipse. Anhand der ID kann die Elipse später gelöscht werden.
	 * @param x Die X-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param y Die Y-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param breite Die Breite des Rechtecks.
	 * @param hoehe Die Höhe des Rechtecks.
	 * @param farbe Die Farbe, mit der die Linie auf der Zeichenfläche gesetzt werden soll (z.B. Color.BLACK oder Color.RED).
	 */
	public void setzeElipse(int id, double x, double y, double breite, double hoehe, Color farbe){
		
		Elipse neueElipse = new Elipse (id, x, y, breite, hoehe, farbe);
		
		renderEngine.hinzufuegen(neueElipse);
	}

	/**
	 * <p>Zeichnen einer ausgefüllten Elipse auf der Zeichenfläche.</p>
	 * <p>Es wird eine Elipse gezeichnet, der genau in das Rechteck paßt, welches durch die Koordinaten
	 * definiert wird. Die Koordinaten geben die linke obere Ecke und die Breite und Höhe des Rechtecks an.</p>
	 * <p>Werden die Koordinaten so gesetzt, dass das Rechteck ein Quadrat ist, so wird ein Kreis gezeichnet.</p>
	 * @param x Die X-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param y Die Y-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param breite Die Breite des Rechtecks.
	 * @param hoehe Die Höhe des Rechtecks.
	 */
	public void setzeGefuellteElipse(int id, double x, double y, double breite, double hoehe){
		
		GefuellteElipse neueGefuellteElipse = new GefuellteElipse (id, x, y, breite, hoehe, aktuelleFarbe);
		
		renderEngine.hinzufuegen(neueGefuellteElipse);
	}
	/**
	 * <p>Zeichnen einer ausgefüllten Elipse auf der Zeichenfläche.</p>
	 * <p>Es wird eine Elipse gezeichnet, der genau in das Rechteck paßt, welches durch die Koordinaten
	 * definiert wird. Die Koordinaten geben die linke obere Ecke und die Breite und Höhe des Rechtecks an.</p>
	 * <p>Werden die Koordinaten so gesetzt, dass das Rechteck ein Quadrat ist, so wird ein Kreis gezeichnet.</p>
	 * <p>Die Elipse ist wieder löschbar. Löschen mit der loeschen(int id) Methode.</p>
	 * @param id Die ID der Elipse. Anhand der ID kann die Elipse später gelöscht werden.
	 * @param x Die X-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param y Die Y-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param breite Die Breite des Rechtecks.
	 * @param hoehe Die Höhe des Rechtecks.
	 * @param farbe Die Farbe, mit der die Elipse auf der Zeichenfläche gesetzt werden soll (z.B. Color.BLACK oder Color.RED).
	 */
	public void setzeGefuellteElipse(int id, double x, double y, double breite, double hoehe, Color farbe){
		
		GefuellteElipse neueGefuellteElipse = new GefuellteElipse (id, x, y, breite, hoehe, farbe);
		
		renderEngine.hinzufuegen(neueGefuellteElipse);
	}
	
	/**
	 * <p>Zeichnen eines Rechtecks auf der Zeichenfläche.</p>
	 * <p>Es wird ein Rechteck gezeichnet. Die Koordinaten geben die linke obere Ecke des Rechtecks an.</p>
	 * @param x Die X-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param y Die Y-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param breite Die Breite des Rechtecks.
	 * @param hoehe Die Höhe des Rechtecks.
	 */
	public void setzeRechteck(int id, double x, double y, double breite, double hoehe){
		
		Rechteck neuesRechteck = new Rechteck (id, x, y, breite, hoehe, aktuelleFarbe);
		
		renderEngine.hinzufuegen(neuesRechteck);
	}
	/**
	 * <p>Zeichnen eines Rechtecks auf der Zeichenfläche.</p>
	 * <p>Es wird ein Rechteck gezeichnet. Die Koordinaten geben die linke obere Ecke des Rechtecks an.</p>
	 * <p>Das Rechteck ist wieder löschbar. Löschen mit der loeschen(int id) Methode.</p>
	 * @param id Die ID des Rechtecks. Anhand der ID kann das Rechteck später gelöscht werden.
	 * @param x Die X-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param y Die Y-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param breite Die Breite des Rechtecks.
	 * @param hoehe Die Höhe des Rechtecks.
	 * @param farbe Die Farbe, mit der das Rechteck auf der Zeichenfläche gesetzt werden soll (z.B. Color.BLACK oder Color.RED).
	 */
	public void setzeRechteck(int id, double x, double y, double breite, double hoehe, Color farbe){
		
		Rechteck neuesRechteck = new Rechteck (id, x, y, breite, hoehe, farbe);
		
		renderEngine.hinzufuegen(neuesRechteck);
	}

	/**
	 * <p>Zeichnen eines ausgefuellten Rechtecks auf der Zeichenfläche.</p>
	 * <p>Es wird ein Rechteck gezeichnet. Die Koordinaten geben die linke obere Ecke des Rechtecks an.</p>
	 * @param x Die X-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param y Die Y-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param breite Die Breite des Rechtecks.
	 * @param hoehe Die Höhe des Rechtecks.
	 */
	public void setzeGefuelltesRechteck(int id, double x, double y, double breite, double hoehe){
		
		GefuelltesRechteck neuesGefuelltesRechteck = new GefuelltesRechteck (id, x, y, breite, hoehe, aktuelleFarbe);
		
		renderEngine.hinzufuegen(neuesGefuelltesRechteck);
	}
	/**
	 * <p>Zeichnen eines ausgefuellten Rechtecks auf der Zeichenfläche.</p>
	 * <p>Es wird ein Rechteck gezeichnet. Die Koordinaten geben die linke obere Ecke des Rechtecks an.</p>
	 * <p>Das Rechteck ist wieder löschbar. Löschen mit der loeschen(int id) Methode.</p>
	 * @param id Die ID des Rechtecks. Anhand der ID kann das Rechteck später gelöscht werden.
	 * @param x Die X-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param y Die Y-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param breite Die Breite des Rechtecks.
	 * @param hoehe Die Höhe des Rechtecks.
	 * @param farbe Die Farbe, mit der das Rechteck auf der Zeichenfläche gesetzt werden soll (z.B. Color.BLACK oder Color.RED).
	 */
	public void setzeGefuelltesRechteck(int id, double x, double y, double breite, double hoehe, Color farbe){
		
		GefuelltesRechteck neuesGefuelltesRechteck = new GefuelltesRechteck (id, x, y, breite, hoehe, farbe);
		
		renderEngine.hinzufuegen(neuesGefuelltesRechteck);
	}

	/**
	 * <p>Zeichnen von Text auf der Zeichenfläche.</p>
	 * <p>Der Text ist wieder löschbar. Löschen mit der loeschen(int id) Methode.</p>
	 * @param id Die ID des Textes. Anhand der ID kann der Text später gelöscht werden.
	 * @param x Die X-Koordinate der linken Seite des Textes.
	 * @param y Die Y-Koordinate der Grundlinie des Textes.
	 * @param groesse Die Größe der Buchstaben.
	 */
	public void setzeText(int id, String text, double x, double y, int groesse){
		
		Text neuerText = new Text (id, text, x, y, groesse, aktuelleFarbe);
		
		renderEngine.hinzufuegen(neuerText);
	}
	/**
	 * <p>Zeichnen von Text auf der Zeichenfläche.</p>
	 * <p>Der Text ist wieder löschbar. Löschen mit der loeschen(int id) Methode.</p>
	 * @param id Die ID des Textes. Anhand der ID kann der Text später gelöscht werden.
	 * @param x Die X-Koordinate der linken Seite des Textes.
	 * @param y Die Y-Koordinate der Grundlinie des Textes.
	 * @param groesse Die Größe der Buchstaben.
	 * @param farbe Die Farbe, mit der der Text auf der Zeichenfläche gesetzt werden soll (z.B. Color.BLACK oder Color.RED).
	 */
	public void setzeText(int id, String text, double x, double y, double groesse, Color farbe){
		
		Text neuerText = new Text (id, text, x, y, groesse, farbe);
		
		renderEngine.hinzufuegen(neuerText);
	}
	
	/**
	 * <p>Zeichnen eines Bildes auf der Zeichenfläche.</p>
	 * <p>Es wird ein Bild gezeichnet. Die Koordinaten geben die linke obere Ecke des Bildes an.</p>
	 * <p>Das Bild wird so skaliert, dass es in die Vorgegebene Breite und Höhe passt.</p>
	 * <p>Das Bild ist wieder löschbar. Löschen mit der loeschen(int id) Methode.</p>
	 * @param id Die ID des Rechtecks. Anhand der ID kann das Rechteck später gelöscht werden.
	 * @param bild Das Bild, dass gezeichnet werden soll.
	 * @param x Die X-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param y Die Y-Koordinate der linken oberen Ecke des Rechtecks.
	 * @param breite Die Breite des Rechtecks.
	 * @param hoehe Die Höhe des Rechtecks.
	 */
	public void setzeBild(int id, Image bild, double x, double y, double breite, double hoehe){
		
		Bild neuesBild = new Bild (id, bild, x, y, breite, hoehe);
		
		renderEngine.hinzufuegen(neuesBild);
	}
	

	/**
	 * <p>Ändert die Position aller Objekte mit einer bestimmten ID auf der Zeichenfläche.</p>
	 * <p>Werden mehreren Objekten die gleiche ID zugewiesen, so ist es möglich diese Objekte als Gruppe auf einmal zu bewegen.</p>
	 * @param id Die ID des zu löschenden Objektes.
	 * @param deltaX Anzahl Pixel um die das Objekt verschoben wird.
	 * @param deltaY Anzahl Pixel um die das Objekt verschoben wird.
	 */
	public void verschieben(int id, double deltaX, double deltaY){
		
		renderEngine.verschieben(id, deltaX, deltaY);
	}
	
	/**
	 * <p>Löschen aller Objekte mit einer bestimmten ID von der Zeichenfläche.</p>
	 * <p>Werden mehreren Objekten die gleiche ID zugewiesen, so ist es möglich diese Objekte als Gruppe auf einmal zu löschen.</p>
	 * @param id Die ID des zu löschenden Objektes.
	 */
	public void loeschen(int id){
		
		renderEngine.loeschen(id);
	}

	public void manualPaint(){
	
		renderThread.execute(renderEngine);
	}
	
	@Override
	public void paintComponent(Graphics graphic){

		renderThread.execute(renderEngine);
	}
	
	
// Innere Klassen.
	
	class Renderer implements Runnable{
		
	// Attribute
		
		// Zeigt an, ob ein Rendering notwendig ist. Das ist z.B. der Fall, wenn neue Objekte zum Zeichnen zugeführt oder gelöscht
		// wurden.
		private boolean renderingNotwendig = true;
		
		private ZeichenFlaeche14 zeichenFlaechenPanel = null;
		
		public volatile BufferedImage bild = null;
		
		
	// Konstruktoren
		
		public Renderer(ZeichenFlaeche14 zeichenFlaechenPanel){
			this.zeichenFlaechenPanel = zeichenFlaechenPanel;
		}
	
		
	// Methoden
		public void run(){
			
			// Nur zeichnen, wenn zeichenFlaechenPanel existiert und eine Größe hat.
			if(zeichenFlaechenPanel != null && zeichenFlaechenPanel.getWidth() * zeichenFlaechenPanel.getHeight() != 0){

				if(renderingNotwendig){

					render();
				}
				
				Graphics grafik = zeichenFlaechenPanel.getGraphics();

				if (grafik != null){
					
					synchronized (this) {
						
						grafik.drawImage(renderEngine.bild, 0, 0, null);
					}
					java.awt.Toolkit.getDefaultToolkit().sync();
				}
			}
		}

		@SuppressWarnings("unchecked")
		private void render(){
			
			// Hier wird erst eine Kopie der Objektliste gemacht. Im Weiteren wird die Kopie zum Zeichnen verwendet
			// und die setzexxxx() Methoden können mit der Originalliste gleichzeitig weiterarbeiten.
			ArrayList<ZuZeichnendesObjekt> tempZuZeichnendeObjekte = null;
			synchronized (this) {
				tempZuZeichnendeObjekte = (ArrayList<ZuZeichnendesObjekt>) zuZeichnendeObjekteListe.clone();
			}
			
			int anzahlObjekte = tempZuZeichnendeObjekte.size();
			
			// Neue graphics erstellen um in ihr zu zeichnen.
			BufferedImage neuesBild = new BufferedImage(zeichenFlaechenPanel.getWidth(), zeichenFlaechenPanel.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D neueGraphics = neuesBild.createGraphics();
			
			neueGraphics.setColor(aktuelleHintergrundfarbe);
			neueGraphics.fillRect(0, 0, neuesBild.getWidth(), neuesBild.getHeight());

			// Zeichne alle Objekte in der Objekteliste.
			for (int objekteZahler = 0; objekteZahler < anzahlObjekte; objekteZahler = objekteZahler + 1){
				ZuZeichnendesObjekt aktuellesObjekt = tempZuZeichnendeObjekte.get(objekteZahler);
				aktuellesObjekt.zeichne(neueGraphics);
			}
			
			// Nach der Arbeit das alte Bild mit dem neuen Bild überschreiben.
			synchronized (this){

				bild = neuesBild;
			}
			
			renderingNotwendig = false;
		}

		public void setRendernIstNotwendig(){
			renderingNotwendig = true;
		}

		public void loeschen(int id){

			ZuZeichnendesObjekt objekt;
			int objektId = 0;
			int objektIndex = 0;
			
			synchronized (this) {
				
				objektIndex = sucheErstenIndexInListe(id);
				
				// Wenn die suchen-Methode -1 zurückgegeben hat, gibt es die gewünschte id nicht in der Liste.
				if(objektIndex == -1) return;

				
				while(objektIndex < zuZeichnendeObjekteListe.size()){
					objekt = zuZeichnendeObjekteListe.get(objektIndex);
					objektId = objekt.getId();
					
					// Beim Löschen ist zu beachten:
					// Wenn ein Objekt gelöscht wird, dann "rutschen" alle Objekte in der ArrayList einen Platz nach "oben".
					// Daher muss objektIndex auf dem selben Wert BLEIBEN.
					// Nur wenn KEIN Objekt gelöscht wird, dann muss objektIndex einen Schritt weiter gezählt werden.
					if(objektId == id){
						zuZeichnendeObjekteListe.remove(objektIndex);
					}else{
						objektIndex = objektIndex + 1;
					}
				}
			}
			
			renderingNotwendig = true;
		}
		
		public void verschieben(int id, double deltaX, double deltaY){
			
			synchronized (this) {

				int objektIndex = sucheErstenIndexInListe(id);
				
				// Wenn die suchen-Methode -1 zurückgegeben hat, gibt es die gewünschte id nicht in der Liste.
				if(objektIndex == -1) return;
				
				// Sonst verschieben.
				zuZeichnendeObjekteListe.get(objektIndex).verschieben(deltaX, deltaY);
			}
			
			renderingNotwendig = true;
		}


		// Ein Objekt wird an der richtigen Stelle in die ArrayList eingefügt.
		private synchronized void hinzufuegen(ZuZeichnendesObjekt zuZeichnendesObjekt){
			
			int objektID = zuZeichnendesObjekt.getId();

			int tokenPosition = 0;
			int untereGrenze = 0;
			int obereGrenze = zuZeichnendeObjekteListe.size() - 1;

			int linkesObjektID = 0;
			int rechtesObjektID = 0;

			boolean hinzugefuegt = false;

			// Falls Liste noch leer, dann einfach einfügen.
			if(obereGrenze < 0){
				zuZeichnendeObjekteListe.add(zuZeichnendesObjekt);
				hinzugefuegt = true;
			}

			// Falls nur ein Objekt in der Liste ist, dann einfaches vorher/nachher einfügen.
			if(!hinzugefuegt){
				if(obereGrenze == 0){
					if(objektID >= zuZeichnendeObjekteListe.get(0).getId()){

						zuZeichnendeObjekteListe.add(zuZeichnendesObjekt);

					}else {

						zuZeichnendeObjekteListe.add(0, zuZeichnendesObjekt);
					}
					hinzugefuegt = true;
				}
			}

			// Falls die Liste nicht leer ist, mit Teilmengenbildung (immer die Hälfte) einfügen.
			while(!hinzugefuegt && (untereGrenze < obereGrenze)){

				tokenPosition = ((obereGrenze - untereGrenze) / 2) + untereGrenze;

				linkesObjektID = zuZeichnendeObjekteListe.get(tokenPosition).getId();
				rechtesObjektID = zuZeichnendeObjekteListe.get(tokenPosition + 1).getId();

				if(linkesObjektID <= objektID){

					if(objektID < rechtesObjektID){

						zuZeichnendeObjekteListe.add(tokenPosition + 1, zuZeichnendesObjekt);
						hinzugefuegt = true;

					}else{
						untereGrenze = tokenPosition + 1;
					}

				} else {

					obereGrenze = tokenPosition;
				}
			}

			// Wenn in der obigen Schleife nichts gefunden wurde, sind hier obere und untere Grenze gleich.
			// Jetzt gibt es nur noch zwei Möglchkeiten, rechts oder links der Grenze.
			if(!hinzugefuegt){
				if(objektID >= zuZeichnendeObjekteListe.get(obereGrenze).getId()){

					zuZeichnendeObjekteListe.add(obereGrenze + 1, zuZeichnendesObjekt);

				} else {

					zuZeichnendeObjekteListe.add(obereGrenze, zuZeichnendesObjekt);

				}
			}

			// Rendern ist notwendig, wenn hinzugefügt wurde.
			renderingNotwendig = true;
		}
		
		// Gibt die Position des ERSTEN Objekts mit der angegebenen id zurück.
		// Gibt -1 zurück, wenn es kein Objekt mit der id in der Liste gibt.
		private int sucheErstenIndexInListe(int id){

			int objektId = 0;
			int objektIndex = 0;
			
			int anzahlObjekte = zuZeichnendeObjekteListe.size();
			
			// Wenn die Liste relativ kurz ist, dann linear von vorne suchen.
			if(anzahlObjekte < 20){
				while(objektIndex < anzahlObjekte){
					
					objektId = zuZeichnendeObjekteListe.get(objektIndex).getId();
					
					// Da die ArrayList sortiert ist, kann nach dem ersten Objekt mit einer 
					// größeren objektID abgebrochen werden. Die id kann noch nicht in der Liste sein.
					if(objektId > id){
						return -1;
					}
					
					if(objektId == id){
						return objektIndex;
					}

					objektIndex = objektIndex + 1;
				}

				return -1;
				
			}
			// Wenn die Liste länger ist, dann lieber mit dem Teilungsverfahren irgendeinen Eintrag finden
			// und danach von dort aus den linkesten Eintrag finden.
			else {
				
				objektIndex = sucheIrgendeinenIndexInListe(id);

				// Wenn bereits die vorherie Suche nichts gefunden hat, brauchen wir nicht weiter suchen.
				if(objektIndex == -1){
					return -1;
				}
				
				// Wenn id prinzipiell vorhanden ist, nach links gehen und weiter suchen.
				// Wenn wir ganz links angekommen sind, war unsere id der erste Eintrag.
				while(objektIndex > 0){
					
					// Links von uns prüfen.
					objektId = zuZeichnendeObjekteListe.get(objektIndex - 1).getId();
					
					// Wenn links von uns nicht die gleiche id hat, dann sind wir der erste Eintrag
					// da die Liste ja als sortiert vorausgesetzt wird.
					if(objektId != id){
						return objektIndex;
					}
					
					objektIndex = objektIndex - 1;
				}
				
				return 0;
			}
		}
		
		// Sucht nach der angegebenen id in der Liste. Gibt die Position des ersten Objekts zurück, das
		// gefunden wird. Die Position des Objekts in einer Gruppe von Objekten mit der gleichen id
		// wird nicht beachtet. Irgendein (zufällig) gefundenes Objekt mit der gegebenen id wird genommen. 
		private int sucheIrgendeinenIndexInListe(int id){
			
			int tokenPosition = 0;
			int untereGrenze = 0;
			int obereGrenze = zuZeichnendeObjekteListe.size() - 1;
			
			int linkesObjektID = 0;
			int rechtesObjektID = 0;

			// Falls Liste leer ist, dann -1 returnieren.
			if(obereGrenze < 0){
				return -1;
			}
			// Falls in der Liste nur 1 Objekt ist, dann prüfen ob es die selbe id hat,
			// wenn ja, fertig, wenn nein, dann gibt es die id nicht in der Liste.
			if(obereGrenze == 0){
				if(id == zuZeichnendeObjekteListe.get(0).getId()){
					return 0;
				} else {
					return -1;
				}
			}
			
			// Falls die Liste nicht leer ist, mit Teilmengenbildung (immer die Hälfte) suchen.
			while(untereGrenze != obereGrenze){

				tokenPosition = ((obereGrenze - untereGrenze) / 2) + untereGrenze;

				linkesObjektID = zuZeichnendeObjekteListe.get(tokenPosition).getId();
				rechtesObjektID = zuZeichnendeObjekteListe.get(tokenPosition + 1).getId();


				// Falls die id zur Linken oder zur Rechten stimmt, dann diese Position zurückgeben.
				if(id == linkesObjektID) return tokenPosition;
				if(id == rechtesObjektID) return (tokenPosition + 1);
				
				if(linkesObjektID < id){
					
					// Falls die objektID zwischen der Linken und der Rechten ist, dann kann sie nicht in
					// der Liste sein. Also -1 zurückgeben.
					if(id < rechtesObjektID){

						return -1;
						
					}else{
						
						untereGrenze = tokenPosition + 1;
					}
					
				} else {
					
					obereGrenze = tokenPosition;
				}
			}
			
			return -1;
		}
	}
		
	abstract class ZuZeichnendesObjekt{
		abstract void zeichne(Graphics g);
		abstract void verschieben(double deltaX, double deltaY);
		abstract int getId();
	}
	
	class Punkt extends ZuZeichnendesObjekt{
		
		// Instanzvariablen.
		int id = 0;
		double x = 0;
		double y = 0;
		Color farbe = null;
		
		// Konstruktoren.
		// Wenn die id -1 ist, dann ist das Objekt nicht löschbar.
		Punkt(int id, double x, double y, Color farbe){
			this.id = id;
			this.x = x;
			this.y = y;
			this.farbe = farbe;
		}
		
		// Zeichnet sich selbst.
		void zeichne(Graphics zeichenFlaechenGraphics){
			zeichenFlaechenGraphics.setColor(farbe);
			zeichenFlaechenGraphics.fillRect((int)x,
											 (int)y,
											 1,
											 1);
		}
		
		// Ändert seine Position.
		void verschieben(double deltaX, double deltaY){
			
			x = x + deltaX;
			y = x + deltaY;
		}
		
		// get-Methoden.
		int getId(){
			return this.id;
		}
	}
	
	class Linie extends ZuZeichnendesObjekt{
		
		// Instanzvariablen.
		int id = 0;
		double x1 = 0;
		double y1 = 0;
		double x2 = 0;
		double y2 = 0;
		Color farbe = null;
		
		// Konstruktoren.
		// Wenn die id -1 ist, dann ist das Objekt nicht löschbar.
		Linie(int id, double x1, double y1, double x2, double y2, Color farbe){
			this.id = id;
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			this.farbe = farbe;
		}

		// Zeichnet sich selbst.
		void zeichne(Graphics zeichenFlaechenGraphics){
			zeichenFlaechenGraphics.setColor(farbe);
			zeichenFlaechenGraphics.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
		}
		
		// Ändert seine Position.
		void verschieben(double deltaX, double deltaY){
			
			x1 = x1 + deltaX;
			y1 = y1 + deltaY;
			x2 = x2 + deltaX;
			y2 = y2 + deltaY;
		}

		// get-Methoden.
		int getId(){
			return this.id;
		}
	}
	
	class Elipse extends ZuZeichnendesObjekt{
		
		// Instanzvariablen.
		int id = 0;
		double x = 0;
		double y = 0;
		double breite = 0;
		double hoehe = 0;
		Color farbe = null;
		
		// Konstruktoren.
		// Wenn die id -1 ist, dann ist das Objekt nicht löschbar.
		Elipse(int id, double x, double y, double breite, double hoehe, Color farbe){
			this.id = id;
			this.x = x;
			this.y = y;
			this.breite = breite;
			this.hoehe = hoehe;
			this.farbe = farbe;
		}
		
		// Ändert seine Position.
		void verschieben(double deltaX, double deltaY){

			x = x + deltaX;
			y = y + deltaY;
		}

		// Zeichnet sich selbst.
		void zeichne(Graphics zeichenFlaechenGraphics){
			zeichenFlaechenGraphics.setColor(farbe);
			zeichenFlaechenGraphics.drawOval((int)x, (int)y, (int)breite, (int)hoehe);
		}
		
		// get-Methoden.
		int getId(){
			return this.id;
		}
	}

	class GefuellteElipse extends ZuZeichnendesObjekt{
		
		// Instanzvariablen.
		int id = 0;
		double x = 0;
		double y = 0;
		double breite = 0;
		double hoehe = 0;
		Color farbe = null;
		
		// Konstruktoren.
		// Wenn die id -1 ist, dann ist das Objekt nicht löschbar.
		GefuellteElipse(int id, double x, double y, double breite, double hoehe, Color farbe){
			this.id = id;
			this.x = x;
			this.y = y;
			this.breite = breite;
			this.hoehe = hoehe;
			this.farbe = farbe;
		}
		
		// Ändert seine Position.
		void verschieben(double deltaX, double deltaY){
			
			x = x + deltaX;
			y = y + deltaY;
		}

		// Zeichnet sich selbst.
		void zeichne(Graphics zeichenFlaechenGraphics){
			zeichenFlaechenGraphics.setColor(farbe);
			zeichenFlaechenGraphics.fillOval((int)x, (int)y, (int)breite, (int)hoehe);
		}
		
		// get-Methoden.
		int getId(){
			return this.id;
		}
	}

	class Rechteck extends ZuZeichnendesObjekt{
		
		// Instanzvariablen.
		int id = 0;
		double x = 0;
		double y = 0;
		double breite = 0;
		double hoehe = 0;
		Color farbe = null;
		
		// Konstruktoren.
		// Wenn die id -1 ist, dann ist das Objekt nicht löschbar.
		Rechteck(int id, double x, double y, double breite, double hoehe, Color farbe){
			this.id = id;
			this.x = x;
			this.y = y;
			this.breite = breite;
			this.hoehe = hoehe;
			this.farbe = farbe;
		}
		
		// Ändert seine Position.
		void verschieben(double deltaX, double deltaY){
			
			x = x + deltaX;
			y = y + deltaY;
		}

		// Zeichnet sich selbst.
		void zeichne(Graphics zeichenFlaechenGraphics){
			zeichenFlaechenGraphics.setColor(farbe);
			zeichenFlaechenGraphics.drawRect((int)x, (int)y, (int)breite, (int)hoehe);
		}
		
		// get-Methoden.
		int getId(){
			return this.id;
		}
	}

	class GefuelltesRechteck extends ZuZeichnendesObjekt{
		
		// Instanzvariablen.
		int id = 0;
		double x = 0;
		double y = 0;
		double breite = 0;
		double hoehe = 0;
		Color farbe = null;
		
		// Konstruktoren.
		// Wenn die id -1 ist, dann ist das Objekt nicht löschbar.
		GefuelltesRechteck(int id, double x, double y, double breite, double hoehe, Color farbe){
			this.id = id;
			this.x = x;
			this.y = y;
			this.breite = breite;
			this.hoehe = hoehe;
			this.farbe = farbe;
		}
		
		// Ändert seine Position.
		void verschieben(double deltaX, double deltaY){
			
			x = x + deltaX;
			y = y + deltaY;
		}

		// Zeichnet sich selbst.
		void zeichne(Graphics zeichenFlaechenGraphics){
			zeichenFlaechenGraphics.setColor(farbe);
			zeichenFlaechenGraphics.fillRect((int)x, (int)y, (int)breite, (int)hoehe);
		}
		
		// get-Methoden.
		int getId(){
			return this.id;
		}
	}


	class Text extends ZuZeichnendesObjekt{
		
		// Instanzvariablen.
		int id = 0;
		String text = null;
		double x = 0;
		double y = 0;
		double groesse = 0;
		Color farbe = null;
		
		// Konstruktoren.
		// Wenn die id -1 ist, dann ist das Objekt nicht löschbar.
		Text(int id, String text, double x, double y, double groesse, Color farbe){
			this.id = id;
			this.text = text;
			this.x = x;
			this.y = y;
			this.groesse = groesse;
			this.farbe = farbe;
		}
		
		// Ändert seine Position.
		void verschieben(double deltaX, double deltaY){
			
			x = x + deltaX;
			y = y + deltaY;
		}

		// Zeichnet sich selbst.
		void zeichne(Graphics zeichenFlaechenGraphics){
			zeichenFlaechenGraphics.setColor(farbe);
			
			Font altFont = zeichenFlaechenGraphics.getFont();
			Font font = new Font(altFont.getName(), altFont.getStyle(), (int)groesse);
			
			zeichenFlaechenGraphics.setFont(font);
			zeichenFlaechenGraphics.setColor(farbe);
			zeichenFlaechenGraphics.drawString(text, (int)x, (int)y);
		}
		
		// get-Methoden.
		int getId(){
			return this.id;
		}
	}
	
	class Bild extends ZuZeichnendesObjekt{
		
		// Instanzvariablen.
		int id = 0;
		double x = 0;
		double y = 0;
		Image bild = null;
		double breite = 0;
		double hoehe = 0;
		
		// Konstruktoren.
		// Wenn die id -1 ist, dann ist das Objekt nicht löschbar.
		Bild(int id, Image bild, double x, double y, double breite, double hoehe){
			this.id = id;
			this.bild = bild;
			this.x = x;
			this.y = y;
			this.breite = breite;
			this.hoehe = hoehe;
		}
		
		// Zeichnet sich selbst.
		void zeichne(Graphics zeichenFlaechenGraphics){
			zeichenFlaechenGraphics.drawImage(bild, (int)x, (int)y, (int)breite, (int)hoehe, null);
		}
		
		// Ändert seine Position.
		void verschieben(double deltaX, double deltaY){
			
			x = x + deltaX;
			y = y + deltaY;
		}

		// get-Methoden.
		int getId(){
			return this.id;
		}
	}
	
	
	/**
	 * <p>Einschalten des automatischen Zeichnens.</p>
	 * <p>Mit dieser Methode wird ausserdem das Zeichenintervall geändert, falls das automatische Zeichnen bereits eingeschaltet war.</p>
	 * <p></p>
	 * @param intervallInNanosekunden Zeit, nach der spätestens ein repaint() erfolgt in Nanosekunden. Hier kann man die minimale Frames pro Sekunde  einstellen.
	 */

	public void startAutomatischesZeichnen(int intervallInNanosekunden){
		
		// Die Timer-Funktion für das zeichnen (auch wenn es nicht so aussieht...).
		timer = new ScheduledThreadPoolExecutor(1);
		timer.scheduleAtFixedRate(renderEngine, 0, 16, TimeUnit.MILLISECONDS);
	}
	/**
	 * <p>Ausschalten des automatischen Zeichnens.</p>
	 * <p>Ist das automatische Zeichnen ausgeschaltet, muss das Zeichnen "manuell" durch aufrufen der Methode repaint() erfolgen.</p>
	 * <p></p>
	 */
	public void stopAutomatischesZeichnen(){
		timer.shutdown();
	}
}
