import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.gl2.GLUT;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Base implementation for JOGL including even handling for mouse events
 * Program allows for user to load .m file of their choice from absolute file directory.
 * Note this project is a gradle project and JOGL library must be located in the project directory for it to function,
 * and named 'jogl' -> See build.gradle
 * JOGL documentation -> http://jogamp.org/jogl/www/
 * Program is based on JOGL 2.3.
 * http://math.hws.edu/eck/cs424/notes2013/07_GLUT_and_JOGL.html
 */
class WindowViewer extends JPanel implements
        GLEventListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener, ActionListener {

    static Pattern VERTEX_PATTERN = Pattern.compile("Vertex\\s(\\d+)\\s+(-?\\d*\\.?\\d*e?-?\\d*)\\s(-?\\d*\\.?\\d*e?-?\\d*)\\s(-?\\d*\\.?\\d*e?-?\\d*)\\s\\{normal=\\((-?\\d*\\.?\\d*e?-?\\d*)\\s(-?\\d*\\.?\\d*e?-?\\d*)\\s(-?\\d*\\.?\\d*e?-?\\d*)\\)\\}");
    static Pattern FACE_PATTERN = Pattern.compile("Face\\s(\\d+)\\s+(\\d+)\\s(\\d+)\\s(\\d+)");

    private int TRANSFORM_ROTATE = 1;
    private int TRANSFORM_SCALE = 2;
    private int TRANSFORM_TRANSLATE = 3;

    private int OBJ_WIREFRAME = 0;
    private int OBJ_SOLID = 1;
    private int OBJ_EDGE = 2;


    private float x_dist = 0;
    private float y_dist = 0;

    private float press_x, press_y;

    private float x_angle = 0;
    private float y_angle = 0;

    private float scale_size = 1;
    private int proj_mode = 0;

    private int PROJ_PERSPECTIVE = 1;

    static int obj_mode = 0;
    private static int xform_mode = 0;

    static Vector<Vertex> vertices = new Vector<>();
    static Vector<Face> faces = new Vector<>();

    public static void main(String[] args) throws FileNotFoundException {
        String model = Popup.prompt().getFileDir();
        readFile(model);
        JFrame window = new JFrame("Model Viewer ");
        WindowViewer panel = new WindowViewer();
        window.setContentPane(panel);
        window.setJMenuBar(panel.createMenuBar());
        window.pack();
        window.setLocation(50, 50);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
    }

    private GLJPanel display;


    public WindowViewer() {
        GLCapabilities caps = new GLCapabilities(null);
        display = new GLJPanel(caps);
        display.setPreferredSize(new Dimension(800, 800));
        display.addGLEventListener(this);
        setLayout(new BorderLayout());
        add(display, BorderLayout.CENTER);
        requestFocusInWindow();

        display.addKeyListener(this);

        display.addMouseListener(this);
        display.addMouseMotionListener(this);
        display.addMouseWheelListener(this);


    }

    // ---------------  Methods of the GLEventListener interface -----------

    /**
     * This method is called when the OpenGL display needs to be redrawn.
     */
    public void display(GLAutoDrawable drawable) {
        // called when the panel needs to be drawn

        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        gl.glMatrixMode(GL2.GL_PROJECTION);  // TODO: Fix projection view
        gl.glLoadIdentity();
        if (proj_mode == PROJ_PERSPECTIVE) {
            gl.glFrustum(-10, 10, -10, 0, .1, 1000);
        } else {
            gl.glOrtho(-2, 2, -2, 2, 0.1, 100);
        }
        gl.glMatrixMode(GL2.GL_PROJECTION);

        gl.glLoadIdentity();
        gl.glViewport(50,50,700,700);

//        http://3dengine.org/Draw_a_grid
        gl.glBegin(gl.GL_LINES);
        for (int i = -10; i <= 10; i++) {
            if (i == 0) {
                gl.glColor3f((float) .6, (float) .3, (float) .3);
            } else {
                gl.glColor3f((float) .25, (float) .25, (float) .25);
            }
            gl.glVertex3f(i, 0, -10);
            gl.glVertex3f(i, 0, 10);
            if (i == 0) {
                gl.glColor3f((float) .3, (float) .3, (float) .6);
            } else {
                gl.glColor3f((float) .25, (float) .25, (float) .25);
            }
            gl.glVertex3f(-10, 0, i);
            gl.glVertex3f(10, 0, i);
        }
        gl.glEnd();

        gl.glLoadIdentity();
//        Z-Axis
        gl.glBegin(gl.GL_LINES);
        gl.glColor3f((float) .3, (float) .6, (float) .3);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(0, 10, 0);
        gl.glEnd();

        gl.glMatrixMode(GL2.GL_MODELVIEW);

        gl.glLoadIdentity();

//        ModelView transformations
        gl.glTranslatef(x_dist, -y_dist, 0);
        gl.glRotatef(x_angle, 0, 1, 0);
        gl.glRotatef(y_angle, 1, 0, 0);
        gl.glScalef(scale_size, scale_size, scale_size);


//        drawBoundingBox(gl);

        if (obj_mode == OBJ_WIREFRAME) {
            for (int i = 0; i < faces.size(); i++) {

                Face face = faces.get(i);

                Vertex v1 = vertices.get(face.v1);
                Vertex v2 = vertices.get(face.v2);
                Vertex v3 = vertices.get(face.v3);


                gl.glBegin(gl.GL_LINES);
                gl.glColor3f(1, 0, 0);

                gl.glVertex3f(v1.x, v1.y, v1.z);
                gl.glVertex3f(v2.x, v2.y, v2.z);

                gl.glVertex3f(v2.x, v2.y, v2.z);
                gl.glVertex3f(v3.x, v3.y, v3.z);

                gl.glVertex3f(v3.x, v3.y, v3.z);
                gl.glVertex3f(v1.x, v1.y, v1.z);

                gl.glEnd();
            }
        } else if (obj_mode == OBJ_SOLID) {
            gl.glDisable(GL2.GL_LIGHTING);        // Enable lighting.
            gl.glDisable(GL2.GL_LIGHT0);          // Turn on a light.  By default, shines from direction of viewer.
            gl.glDisable(GL2.GL_NORMALIZE);       // OpenGL will make all normal vectors into unit normals
            gl.glDisable(GL2.GL_COLOR_MATERIAL);

            gl.glEnable(gl.GL_BLEND);
            gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
            gl.glPolygonMode(gl.GL_FRONT, gl.GL_FILL);
            gl.glEnable(gl.GL_POLYGON_SMOOTH);
            gl.glHint(gl.GL_POLYGON_SMOOTH_HINT, gl.GL_NICEST);

            for (int i = 0; i < faces.size(); i++) {

                Face face = faces.get(i);

                Vertex v1 = vertices.get(face.v1);
                Vertex v2 = vertices.get(face.v2);
                Vertex v3 = vertices.get(face.v3);


                gl.glBegin(gl.GL_TRIANGLE_STRIP);
                gl.glColor3f(0, 1, 0);

                gl.glNormal3f(v1.nx, v1.ny, v1.nz);
                gl.glVertex3f(v1.x, v1.y, v1.z);

                gl.glNormal3f(v2.nx, v2.ny, v2.nz);
                gl.glVertex3f(v2.x, v2.y, v2.z);

                gl.glNormal3f(v3.nx, v3.ny, v3.nz);
                gl.glVertex3f(v3.x, v3.y, v3.z);


                gl.glEnd();

            }

        } else if (obj_mode == OBJ_EDGE) {
            gl.glEnable(GL2.GL_LIGHTING);        // Enable lighting.
            gl.glEnable(GL2.GL_LIGHT0);          // Turn on a light.  By default, shines from direction of viewer.
            gl.glEnable(GL2.GL_NORMALIZE);       // OpenGL will make all normal vectors into unit normals
            gl.glEnable(GL2.GL_COLOR_MATERIAL);

            gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
            gl.glShadeModel(gl.GL_FLAT);
//            gl.glPolygonMode(gl.GL_FRONT, gl.GL_FILL);
            for (int i = 0; i < faces.size(); i++) {

                Face face = faces.get(i);

                Vertex v1 = vertices.get(face.v1);
                Vertex v2 = vertices.get(face.v2);
                Vertex v3 = vertices.get(face.v3);


                gl.glBegin(gl.GL_TRIANGLES);
                gl.glColor3f(0, 0, 1);

                gl.glNormal3f(v1.nx, v1.ny, v1.nz);
                gl.glVertex3f(v1.x, v1.y, v1.z);

                gl.glNormal3f(v2.nx, v2.ny, v2.nz);
                gl.glVertex3f(v2.x, v2.y, v2.z);

                gl.glNormal3f(v3.nx, v3.ny, v3.nz);
                gl.glVertex3f(v3.x, v3.y, v3.z);


                gl.glEnd();
            }
        }
//            Bounding Box
        {
            double max_x = (-1e100);
            double max_y = (-1e100);
            double max_z = (-1e100);
            double min_x = (1e100);
            double min_y = (1e100);
            double min_z = (1e100);

            for (int i = 0; i < vertices.size(); i++) {
                Vertex v = vertices.get(i);

                if (v.x < min_x)
                    min_x = v.x;
                if (v.y < min_y)
                    min_y = v.y;
                if (v.z < min_z)
                    min_z = v.z;

                if (v.x > max_x)
                    max_x = v.x;
                if (v.y > max_y)
                    max_y = v.y;
                if (v.z > max_z)
                    max_z = v.z;
            }
            float maxX = (float) max_x;
            float maxY = (float) max_y;
            float maxZ = (float) max_z;
            float minX = (float) min_x;
            float minY = (float) min_y;
            float minZ = (float) min_z;

            gl.glColor3f(.6f, .3f, .6f);
            gl.glBegin(gl.GL_LINE_LOOP);
//            Front
            gl.glVertex3f(minX, minY, minZ);
            gl.glVertex3f(minX, maxY, minZ);
            gl.glVertex3f(maxX, maxY, minZ);
            gl.glVertex3f(maxX, minY, minZ);
            gl.glVertex3f(minX, minY, minZ);
//            Right
            gl.glVertex3f(minX, maxY, minZ);
            gl.glVertex3f(minX, maxY, maxZ);
            gl.glVertex3f(maxX, maxY, maxZ);
            gl.glVertex3f(maxX, maxY, minZ);
            gl.glVertex3f(minX, maxY, minZ);
//  `         Back

            gl.glVertex3f(minX, maxY, maxZ);
            gl.glVertex3f(minX, minY, maxZ);
            gl.glVertex3f(maxX, minY, maxZ);
            gl.glVertex3f(maxX, maxY, maxZ);
            gl.glVertex3f(minX, maxY, maxZ);


//            Left
            gl.glVertex3f(minX, minY, maxZ);
            gl.glVertex3f(maxX, minY, maxZ);
            gl.glVertex3f(maxX, minY, minZ);
            gl.glVertex3f(minX, minY, minZ);
            gl.glVertex3f(minX, minY, maxZ);

//            gl.glVertex3f(0,coord,coord);
            gl.glEnd();
        }

    }

//        https://www.opengl.org/discussion_boards/showthread.php/164180-Draw-a-checker-floor


    /**
     * This is called when the GLJPanel is first created.  It can be used to initialize
     * the OpenGL drawing context.
     */
    public void init(GLAutoDrawable drawable) {
        // called when the panel is created
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0.3F, 0.3F, 0.3F, 1.0F);

        gl.glEnable(GL2.GL_DEPTH_TEST);

    }

    GLUT glut = new GLUT();

    public static void readFile(String dir) throws FileNotFoundException {
        faces.clear();
        vertices.clear();
        BufferedReader br = new BufferedReader(new FileReader(dir));
        try {
            String line = br.readLine();
            while (line != null) {
                Matcher vertex = VERTEX_PATTERN.matcher(line);
                Matcher face = FACE_PATTERN.matcher(line);
                if (vertex.find()) {
                    System.out.println(vertex.toString());
                    int num = Integer.parseInt(vertex.group(1));
                    double x = Double.parseDouble(vertex.group(2));
                    double y = Double.parseDouble(vertex.group(3));
                    double z = Double.parseDouble(vertex.group(4));
                    double nx = Double.parseDouble(vertex.group(5));
                    double ny = Double.parseDouble(vertex.group(6));
                    double nz = Double.parseDouble(vertex.group(7));
                    // Add to vector
                    Vertex v = new Vertex(x, y, z, nx, ny, nz);
                    vertices.add(num - 1, v);
                    line = br.readLine();
                } else if (face.find()) {
                    int num = Integer.parseInt(face.group(1));
                    int first = Integer.parseInt(face.group(2));
                    int second = Integer.parseInt(face.group(3));
                    int third = Integer.parseInt(face.group(4));
                    Face f = new Face(first - 1, second - 1, third - 1);
                    faces.add(num - 1, f);
                    line = br.readLine();
                } else {
                    System.out.println("Did not match: " + line);
                    line = br.readLine();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Called when the size of the GLJPanel changes.  Note:  glViewport(x,y,width,height)
     * has already been called before this method is called!
     */
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        // TODO: Add any code required to respond to the size of the display area.
        //             (Not usually needed.)
    }

    /**
     * This is called before the GLJPanel is destroyed.  It can be used to release OpenGL resources.
     */
    public void dispose(GLAutoDrawable drawable) {
    }


    // ------------ Support for a menu -----------------------

    public JMenuBar createMenuBar() {
        JMenuBar menubar = new JMenuBar();

        MenuHandler menuHandler = new MenuHandler(); // An object to respond to menu commands.

        JMenu menu = new JMenu("Projection"); // Create a menu and add it to the menu bar
        JMenuItem item1 = new JMenuItem("Orthogonal");
        JMenuItem item2 = new JMenuItem("Perspective");
        JMenuItem item3 = new JMenuItem("New Model");

        menubar.add(menu);

        JMenuItem item = new JMenuItem("Quit");  // Create a menu command.
        item.addActionListener(menuHandler);  // Set up handling for this command.
        item1.addActionListener(menuHandler);
        item2.addActionListener(menuHandler);
        item3.addActionListener(menuHandler);
        // Add the command to the menu.
        menu.add(item1);
        menu.add(item2);
        menu.add(item3);
        menu.add(item);
        JMenu drawMenu = new JMenu("Display Options");
        JMenuItem drawItem = new JMenuItem("Wireframe");
        JMenuItem drawItem2 = new JMenuItem("Flat");
        JMenuItem drawItem3 = new JMenuItem("Smooth");

        drawMenu.add(drawItem);
        drawMenu.add(drawItem2);
        drawMenu.add(drawItem3);

        drawItem.addActionListener(menuHandler);  // Set up handling for this command.
        drawItem2.addActionListener(menuHandler);
        drawItem3.addActionListener(menuHandler);

        menubar.add(drawMenu);

        return menubar;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        System.out.println("Key Pressed: " + actionEvent.getActionCommand());
    }


    @Override
    public void keyTyped(KeyEvent keyEvent) {
        System.out.println("Key Typed: " + keyEvent.getKeyCode());
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        System.out.println("Key Pressed: " + keyEvent.getKeyCode());
    }


    @Override
    public void keyReleased(KeyEvent keyEvent) {
        System.out.println("Key Pressed: " + keyEvent.getKeyCode());
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        xform_mode = mouseEvent.getButton();
        System.out.println("Mouse button: " + mouseEvent.getButton());
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        xform_mode = mouseEvent.getButton();
        press_x = mouseEvent.getX();
        press_y = mouseEvent.getY();
        System.out.println("X: " + press_x + " Y: " + press_y + " Coordiantes pressed");

    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        myMotion(mouseEvent.getX(), mouseEvent.getY());
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {

    }


    public void myMotion(float x, float y) {
        if (xform_mode == TRANSFORM_ROTATE) {
            x_angle += (x - press_x) / 3.0;

            if (x_angle > 180)
                x_angle -= 360;
            else if (x_angle < -180)
                x_angle += 360;

            press_x = (int) x;

            y_angle += (y - press_y) / 5.0;

            if (y_angle > 180)
                y_angle -= 360;
            else if (y_angle < -180)
                y_angle += 360;

            press_y = (int) y;
        } else if (xform_mode == TRANSFORM_SCALE) {

            float old_size = scale_size;

            scale_size *= (1 + (y - press_y) / 60.0);

            if (scale_size < 0)
                scale_size = old_size;
            press_y = y;
        } else if (xform_mode == TRANSFORM_TRANSLATE) {
            x_dist = x/400;
            y_dist = y/400;
        }

        display.repaint();
    }


    /**
     * A class to define the ActionListener object that will respond to menu commands.
     */
    private class MenuHandler implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
            String command = evt.getActionCommand();  // The text of the command.
            if (command.equals("Perspective")) {
                proj_mode = PROJ_PERSPECTIVE;
            } else if (command.equals("Orthogonal")) {
                proj_mode = 0;
            } else if (command.equals("Wireframe")) {
                obj_mode = OBJ_WIREFRAME;
            } else if (command.equals("Flat")) {
                obj_mode = OBJ_EDGE;
            } else if (command.equals("Smooth")) {
                obj_mode = OBJ_SOLID;
            } else if (command.equals("Quit")) {
                System.exit(0);
            } else if (command.equals("New Model")) {
                String model = Popup.prompt().getFileDir();
                try {
                    readFile(model);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            display.repaint();
            System.out.println("Projection mode: " + proj_mode);
            System.out.println("Object mode: " + obj_mode);
        }
    }
}