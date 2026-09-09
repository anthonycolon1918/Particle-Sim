// Import necessary Java Swing and AWT classes for GUI creation and graphics rendering
import javax.swing.*;
import java.awt.*;

// Main class extends JPanel so it acts as a graphical canvas we can draw on
public class ParticleSimulation extends JPanel {

    // ==========================================
    // PARTICLE DATA STRUCTURE
    // ==========================================
    // Inner class representing a single particle. 
    // This perfectly mimics the C struct from your original code.
    static class Particle {
        float x, y;       // Current screen position (Java coordinates)
        float vx, vy;     // Velocity (speed and direction along axes)
        int type;         // Determines the color of the particle
        int mass;         // Mass used for calculating collision momentum

        // Constructor to easily instantiate a new particle with all properties
        public Particle(float x, float y, float vx, float vy, int type, int mass) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.type = type;
            this.mass = mass;
        }
    }

    // ==========================================
    // CONSTANTS & CONFIGURATION
    // ==========================================
    // Strictly limited to 2 particles based on your request
    private static final int NUM_PARTICLES = 2;
    
    // The visual radius/half-width of the particle 
    private static final float PARTICLE_SIZE = 3.0f; 
    
    // Window dimensions in pixels
    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 800;

    // Defining exact colors using RGB Hex values (similar to Raylib's Color struct)
    private final Color pal_ocean_blue = new Color(0x27, 0x7D, 0xA1);
    private final Color pal_light_green = new Color(0x90, 0xBE, 0x6D);
    private final Color pal_yellow = new Color(0xF9, 0xC7, 0x4F);
    private final Color pal_red_orange = new Color(0xF9, 0x41, 0x44);

    // Array to hold the color choices, accessed via the particle's 'type' integer
    private final Color[] particleColors = {
            pal_ocean_blue, pal_light_green, pal_yellow, pal_red_orange
    };

    // The fixed-size array storing our two active particles
    private final Particle[] particles = new Particle[NUM_PARTICLES];
    
    // Variables used strictly for tracking and drawing the Frames Per Second (FPS)
    private long lastTime = System.currentTimeMillis();
    private int fps = 0;
    private int framesCount = 0;

    // ==========================================
    // CONSTRUCTOR & INITIALIZATION
    // ==========================================
    public ParticleSimulation() {
        // Configure the size and background color of the drawing panel
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(Color.BLACK);

        // --- CARTESIAN CONFIGURATION BLOCK ---
        // (0,0) is the absolute center of the window.
        // +X is right, -X is left.
        // +Y is up, -Y is down.

        // Particle 1 (Blue, Mass 5)
        // Starts on the left side of the screen, moving straight to the right
        particles[0] = createCartesianParticle(
            -200.0f,  // Cartesian X: 200 units left of center
               0.0f,  // Cartesian Y: exactly on the center horizontal line
               3.0f,  // X Velocity: moving right at 3 pixels per frame
               0.0f,  // Y Velocity: no vertical movement
                  0,  // Color Type: 0 (Ocean Blue)
                  5   // Mass: 5
        );

        // Particle 2 (Red Orange, Mass 2)
        // Starts on the right side of the screen, moving straight to the left
        particles[1] = createCartesianParticle(
             200.0f,  // Cartesian X: 200 units right of center
               0.0f,  // Cartesian Y: exactly on the center horizontal line
              -2.0f,  // X Velocity: moving left at -2 pixels per frame
               0.0f,  // Y Velocity: no vertical movement
                  3,  // Color Type: 3 (Red Orange)
                  2   // Mass: 2
        );

        // Set up the 60 FPS Game Loop using a Java Swing Timer.
        // 1000ms / 60 frames = ~16ms delay between loop ticks.
        Timer timer = new Timer(1000 / 60, e -> {
            updateLogic(); // Step 1: Calculate physics, velocities, and collisions
            repaint();     // Step 2: Tell Java to wipe and re-draw the screen
        });
        
        // Start the repeating timer immediately
        timer.start();
    }

    // ==========================================
    // HELPER: CARTESIAN TO SCREEN TRANSLATOR
    // ==========================================
    /**
     * Maps standard mathematical Cartesian coordinates (where Y goes up and 0,0 is center) 
     * into Java Screen coordinates (where Y goes down and 0,0 is top-left).
     */
    private Particle createCartesianParticle(float cartX, float cartY, float cartVx, float cartVy, int type, int mass) {
        // Shift X so that 0 becomes the middle of the screen (e.g., 500)
        float screenX = (WINDOW_WIDTH / 2.0f) + cartX;
        
        // Shift Y to the middle, but INVERT it. 
        // Java draws from top-to-bottom, so a positive Cartesian Y needs to go UP the screen (lower Y value).
        float screenY = (WINDOW_HEIGHT / 2.0f) - cartY;
        
        // X velocity stays the same (moving right is positive in both systems)
        float screenVx = cartVx;
        
        // Y velocity is inverted to ensure positive inputs move the particle UP the screen visually
        float screenVy = -cartVy;

        // Return a fully constructed particle using the mapped screen values
        return new Particle(screenX, screenY, screenVx, screenVy, type, mass);
    }

    // ==========================================
    // PHYSICS & LOGIC ENGINE
    // ==========================================
    private void updateLogic() {
        // --- Pass 1: MOVEMENT ---
        // Apply current velocities to update the X and Y positions of both particles
        for (int i = 0; i < NUM_PARTICLES; i++) {
            Particle p = particles[i];
            p.x += p.vx;
            p.y += p.vy;
        }

        // --- Pass 2: COLLISIONS ---
        for (int i = 0; i < NUM_PARTICLES; i++) {
            Particle p = particles[i];

            // Compare against the other particle (j = i + 1 prevents double-checking)
            for (int j = i + 1; j < NUM_PARTICLES; j++) {
                Particle p2 = particles[j];

                // Broad-phase proximity check to save processing power
                float closeDistance = PARTICLE_SIZE + 10.0f; 
                boolean isClose = (Math.abs(p.x - p2.x) < closeDistance) &&
                                  (Math.abs(p.y - p2.y) < closeDistance);

                if (isClose) {
                    // Narrow-phase precise collision check using a bounding box (AABB)
                    boolean isCollision = (Math.abs(p.x - p2.x) < PARTICLE_SIZE) &&
                                          (Math.abs(p.y - p2.y) < PARTICLE_SIZE);
                    
                    if (isCollision) {
                        // --- COEFFICIENT OF RESTITUTION (ELASTICITY) ---
                        // e = 1.0f -> Perfectly Elastic (momentum & kinetic energy conserved, bouncy)
                        // e = 0.5f -> Partially Inelastic (some energy lost to heat/sound)
                        // e = 0.0f -> Perfectly Inelastic (particles stick together like clay)
                        float e = 1.0f; // Change this value to adjust bounce springiness!

                        // Temporarily store initial velocities
                        float v1x = p.vx;
                        float v2x = p2.vx;
                        float v1y = p.vy;
                        float v2y = p2.vy;

                        // Store masses in local variables for cleaner math
                        float m1 = p.mass;
                        float m2 = p2.mass;

                        // Apply generalized restitution formula for the X axis
                        p.vx =  (m1 * v1x + m2 * v2x + m2 * e * (v2x - v1x)) / (m1 + m2);
                        p2.vx = (m1 * v1x + m2 * v2x + m1 * e * (v1x - v2x)) / (m1 + m2);

                        // Apply generalized restitution formula for the Y axis
                        p.vy =  (m1 * v1y + m2 * v2y + m2 * e * (v2y - v1y)) / (m1 + m2);
                        p2.vy = (m1 * v1y + m2 * v2y + m1 * e * (v1y - v2y)) / (m1 + m2);
                        
                        // Anti-Sticking measure: Push them apart slightly so they don't get trapped 
                        // inside each other's hitboxes on the next frame, especially on low 'e' values.
                        p.x += (p.x > p2.x) ? 1.0f : -1.0f;
                        p2.x += (p2.x > p.x) ? 1.0f : -1.0f;
                    }
                }
            }

            // --- Pass 3: WALL BOUNDARIES ---
            // Left Wall
            if (p.x < PARTICLE_SIZE) { 
                p.x += PARTICLE_SIZE; // Nudge out of the wall
                p.vx *= -1;           // Reverse horizontal direction
            }
            // Right Wall
            if (p.x > WINDOW_WIDTH - PARTICLE_SIZE) { 
                p.x -= PARTICLE_SIZE; 
                p.vx *= -1; 
            }
            // Top Wall
            if (p.y < PARTICLE_SIZE) { 
                p.y += PARTICLE_SIZE; 
                p.vy *= -1;           // Reverse vertical direction
            }
            // Bottom Wall
            if (p.y > WINDOW_HEIGHT - PARTICLE_SIZE) { 
                p.y -= PARTICLE_SIZE; 
                p.vy *= -1; 
            }
        }
    }

    // ==========================================
    // RENDER PASS (GRAPHICS)
    // ==========================================
    @Override
    protected void paintComponent(Graphics g) {
        // Always call super first to wipe the previous frame clean (prevents smearing)
        super.paintComponent(g);
        
        // Cast Graphics to Graphics2D for access to modern drawing tools
        Graphics2D g2d = (Graphics2D) g;

        // --- FPS COUNTER ---
        framesCount++;
        long currentTime = System.currentTimeMillis();
        // If one second has passed, update the locked-in FPS number
        if (currentTime - lastTime >= 1000) {
            fps = framesCount;
            framesCount = 0;
            lastTime = currentTime;
        }
        // Draw the FPS text in the top left corner
        g2d.setColor(Color.GREEN);
        g2d.drawString("FPS: " + fps, 20, 20);

        // --- CARTESIAN AXES ---
        // Draw faint gray guide lines intersecting at (0,0) center screen
        g2d.setColor(new Color(50, 50, 50));
        g2d.drawLine(0, WINDOW_HEIGHT/2, WINDOW_WIDTH, WINDOW_HEIGHT/2); // Horizontal X Axis
        g2d.drawLine(WINDOW_WIDTH/2, 0, WINDOW_WIDTH/2, WINDOW_HEIGHT);  // Vertical Y Axis

        // --- DRAW PARTICLES ---
        for (int i = 0; i < NUM_PARTICLES; i++) {
            Particle p = particles[i];
            
            // Set brush color based on particle type
            g2d.setColor(particleColors[p.type]);
            
            // Draw a rectangle. Java requires integers, so cast floats to (int).
            // Width and Height are calculated as Diameter (2 * Radius)
            g2d.fillRect((int) p.x, (int) p.y, (int) (2 * PARTICLE_SIZE), (int) (2 * PARTICLE_SIZE));
        }
    }

    // ==========================================
    // MAIN ENTRY POINT
    // ==========================================
    public static void main(String[] args) {
        // GUI creation MUST occur on the Event Dispatch Thread in Java Swing
        SwingUtilities.invokeLater(() -> {
            // Create the main window frame
            JFrame frame = new JFrame("Two Particle Collision Simulation");
            
            // Ensure the program terminates when the user clicks 'X'
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // Lock window size so our hardcoded wall boundaries don't break
            frame.setResizable(false);
            
            // Instantiate our simulation panel and add it to the window
            ParticleSimulation simulation = new ParticleSimulation();
            frame.add(simulation);
            
            // Pack shrinks the JFrame to exactly fit the JPanel's PreferredSize
            frame.pack();
            
            // Centering the window on the user's monitor
            frame.setLocationRelativeTo(null); 
            
            // Reveal the window
            frame.setVisible(true);
        });
    }
}
