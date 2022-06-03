import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.*;

public class Game extends JPanel implements ActionListener
{
    private final Color HEAD = Color.YELLOW, TAIL = Color.RED, APPLE = Color.GREEN;
    private final int fieldWidth = 800, fieldHeight = 800, squareSize = 40, delay = 140;
    private final int[] x = new int[fieldWidth * fieldHeight / squareSize], y = new int[fieldHeight * fieldHeight / squareSize];;
    private int foodX, foodY, length;
    private Direction direction = Direction.RIGHT;
    private boolean inGame = true;
    private Timer timer;
    public Game()
    {
        addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                switch (e.getKeyCode())
                {
                    case KeyEvent.VK_LEFT:
                        direction = direction.changeDirection(Direction.LEFT);
                        break;
                    case KeyEvent.VK_RIGHT:
                        direction = direction.changeDirection(Direction.RIGHT);
                        break;
                    case KeyEvent.VK_UP:
                        direction = direction.changeDirection(Direction.UP);
                        break;
                    case KeyEvent.VK_DOWN:
                        direction = direction.changeDirection(Direction.DOWN);
                        break;
                }
            }
        });
        setBackground(Color.black);
        setFocusable(true);

        setPreferredSize(new Dimension(fieldWidth, fieldHeight));
        initGame();
    }

    private void initGame()
    {
        length = 3;

        for (int z = 0; z < length; z++)
        {
            x[z] = (length + 2) * squareSize - z * squareSize;
            y[z] = 5 * squareSize;
        }

        spawnFood();

        timer = new Timer(delay, this);
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (inGame)
        {
            g.setColor(APPLE);
            g.drawOval(foodX - squareSize / 2, foodY - squareSize / 2, squareSize, squareSize);

            for (int z = 0; z < length; z++)
            {
                if (z == 0)
                    g.setColor(HEAD);
                else
                    g.setColor(TAIL);
                g.fillRect(x[z] - squareSize / 2, y[z] - squareSize / 2, squareSize, squareSize);
            }
            Toolkit.getDefaultToolkit().sync();
        } else
            gameOver(g);
    }

    private void gameOver(Graphics g)
    {
        String msg = "Spiel zu Ende. " + length + " Elemente erreicht.";
        g.setColor(Color.white);
        g.setFont(new Font("ARIAL", Font.BOLD, fieldWidth / 20));
        g.drawString(msg, (fieldWidth - getFontMetrics(g.getFont()).stringWidth(msg)) / 2, fieldHeight / 2);
    }

    private void move()
    {

        for (int z = length; z > 0; z--)
        {
            x[z] = x[(z - 1)];
            y[z] = y[(z - 1)];
        }

        switch (direction)
        {
            case LEFT:
                x[0] -= squareSize;
                break;
            case RIGHT:
                x[0] += squareSize;
                break;
            case UP:
                y[0] -= squareSize;
                break;
            case DOWN:
                y[0] += squareSize;
                break;
        }
    }

    private void spawnFood()
    {
        int fieldNumbersX = fieldWidth / squareSize;
        int fieldNumbersY = fieldHeight / squareSize;

        //placing apples not in the border
        int appleXField = (int) (Math.random() * (fieldNumbersX - 2) + 1);
        int appleYField = (int) (Math.random() * (fieldNumbersY - 2) + 1);

        foodX = appleXField * squareSize;
        foodY = appleYField * squareSize;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (inGame)
        {
            //apple eaten
            if ((x[0] == foodX) && (y[0] == foodY))
            {
                length++;
                spawnFood();
            }

            //self collision detection
            for (int z = length; z > 0; z--)
                if ((z > 4) && x[0] == x[z] && y[0] == y[z])
                {
                    inGame = false;
                    break;
                }

            //out of border detection
            if (y[0] >= fieldHeight || x[0] >= fieldWidth || x[0] < 0 || y[0] < 0)
            {
                inGame = false;
                timer.stop();
            }

            if (inGame)
                move();
        }
        repaint();
    }

    public static void main(String[] args)
    {
        EventQueue.invokeLater(() ->
        {
            JFrame jFrame = new JFrame();    //replaceable with "JFrame jFrame = new JFrame();"
            jFrame.setVisible(true);
            jFrame.add(new Game());
            jFrame.setResizable(false);
            jFrame.pack();
            jFrame.setTitle("Snake");
            jFrame.setLocationRelativeTo(null);
            jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        });
    }
}