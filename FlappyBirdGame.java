import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;
import java.io.*;

public class FlappyBirdGame extends JPanel implements ActionListener, KeyListener {

    int boardWidth = 500;
    int boardHeight = 700;

    Image backgroundImg;
    Image birdImg;
    Image topPipeImg;
    Image bottomPipeImg;
    Image baseImg;
    Image startImg;

    int birdX = boardWidth / 5;
    int birdY = boardHeight / 2;
    int birdWidth = 40;
    int birdHeight = 28;

    class Bird {
        int x = birdX;
        int y = birdY;
        int width = birdWidth;
        int height = birdHeight;
        Image img;

        Bird(Image img) {
            this.img = img;
        }
    }

    class Pipe {
        int x;
        int y;
        int width = 64;
        int height = 320;
        Image img;
        boolean passed = false;

        Pipe(Image img, int x, int y) {
            this.img = img;
            this.x = x;
            this.y = y;
        }
    }

    Bird bird;
    ArrayList<Pipe> pipes;
    Random random = new Random();

    Timer gameLoop;
    Timer pipeTimer;

    boolean gameStarted = false;
    boolean gameOver = false;

    int velocityX = -4;
    double velocityY = 0;
    double gravity = 0.5;
    double flapStrength = -9;

    int score = 0;

    Clip wingSound, pointSound, hitSound;

    FlappyBirdGame() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);

        loadAssets();
        loadSounds();

        bird = new Bird(birdImg);
        pipes = new ArrayList<>();

        pipeTimer = new Timer(1600, e -> placePipe());
        gameLoop = new Timer(1000 / 60, this);  // 60 FPS
    }

    void loadAssets() {
        backgroundImg = new ImageIcon("images/background.png").getImage();
        birdImg = new ImageIcon("images/bird.png").getImage();
        topPipeImg = new ImageIcon("images/pipe.png").getImage();
        bottomPipeImg = new ImageIcon("images/pipe.png").getImage();
        baseImg = new ImageIcon("images/ground.png").getImage();
        startImg = new ImageIcon("images/start.png").getImage();
    }

    void loadSounds() {
        try {
            wingSound = loadSound("sound/wing.wav");
            pointSound = loadSound("sound/point.wav");
            hitSound = loadSound("sound/hit.wav");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Clip loadSound(String path) throws Exception {
        File soundFile = new File(path);
        AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
        Clip clip = AudioSystem.getClip();
        clip.open(audioIn);
        return clip;
    }

    void placePipe() {
        int gap = 150;
        int pipeTopY = -random.nextInt(200); // Less extreme Y
        int pipeBottomY = pipeTopY + 320 + gap;

        pipes.add(new Pipe(topPipeImg, boardWidth, pipeTopY));
        pipes.add(new Pipe(bottomPipeImg, boardWidth, pipeBottomY));
    }

    void resetGame() {
        bird.y = birdY;
        velocityY = 0;
        pipes.clear();
        score = 0;
        gameOver = false;
        gameStarted = false;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(backgroundImg, 0, 0, boardWidth, boardHeight, null);

        for (Pipe pipe : pipes) {
            if (pipe.y < 0) {
                // Draw top pipe flipped vertically
                g.drawImage(pipe.img,
                        pipe.x, pipe.y + pipe.height,
                        pipe.x + pipe.width, pipe.y,
                        0, 0,
                        pipe.img.getWidth(null), pipe.img.getHeight(null),
                        null);
            } else {
                // Draw bottom pipe normally
                g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
            }
        }

        g.drawImage(baseImg, 0, 630, boardWidth, 70, null);
        g.drawImage(bird.img, bird.x, bird.y, bird.width, bird.height, null);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 32));

        if (!gameStarted) {
            g.drawImage(startImg, (boardWidth - 200) / 2, 200, 200, 200, null);
        } else if (gameOver) {
            g.drawString("Game Over! Score: " + score, 100, 300);
            g.drawString("Press SPACE to restart", 90, 350);
        } else {
            g.drawString(String.valueOf(score), boardWidth / 2 - 10, 50);
        }
    }

    void move() {
        if (!gameStarted || gameOver) return;

        velocityY += gravity;
        bird.y += velocityY;

        for (Pipe pipe : pipes) {
            pipe.x += velocityX;

            if (!pipe.passed && pipe.x + pipe.width < bird.x) {
                score++;
                pipe.passed = true;
                playSound(pointSound);
            }

            if (checkCollision(bird, pipe)) {
                gameOver = true;
                playSound(hitSound);
                pipeTimer.stop();
                gameLoop.stop();
            }
        }

        if (bird.y > boardHeight - 70 || bird.y < 0) {
            gameOver = true;
            playSound(hitSound);
            pipeTimer.stop();
            gameLoop.stop();
        }
    }

    boolean checkCollision(Bird bird, Pipe pipe) {
        return bird.x + bird.width > pipe.x &&
               bird.x < pipe.x + pipe.width &&
               bird.y + bird.height > pipe.y &&
               bird.y < pipe.y + pipe.height;
    }

    void playSound(Clip clip) {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_UP) {
            if (!gameStarted) {
                gameStarted = true;
                pipeTimer.start();
                gameLoop.start();
            }

            if (!gameOver) {
                velocityY = flapStrength;
                playSound(wingSound);
            } else {
                resetGame();
            }
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Flappy Bird Java");
        FlappyBirdGame gamePanel = new FlappyBirdGame();
        frame.add(gamePanel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
