import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class Main {

    // === 벡터 연산 유틸 === //
    static double[] add(double[] a, double[] b) {
        if (a == null || b == null) return null;
        return new double[]{a[0] + b[0], a[1] + b[1]};
    }
    static double[] sub(double[] a, double[] b) {
        if (a == null || b == null) return null;
        return new double[]{a[0] - b[0], a[1] - b[1]};
    }
    static double[] mul(double[] a, double s) {
        if (a == null) return null;
        return new double[]{a[0] * s, a[1] * s};
    }
    static double dot(double[] a, double[] b) {
        if (a == null || b == null) return 0.0;
        return a[0] * b[0] + a[1] * b[1];
    }
    static double cross(double[] a, double[] b) {
        if (a == null || b == null) return 0.0;
        return a[0] * b[1] - a[1] * b[0];
    }
    static double length(double[] a) {
        if (a == null) return 0.0;
        return Math.sqrt(a[0] * a[0] + a[1] * a[1]);
    }
    static double[] normalize(double[] a) {
        if (a == null) return new double[]{0, 0};
        double len = length(a);
        return len == 0 ? new double[]{0, 0} : new double[]{a[0] / len, a[1] / len};
    }
    static double[] rotate(double[] a, double angle) {
        if (a == null) return null;
        double c = Math.cos(angle), s = Math.sin(angle);
        return new double[]{c * a[0] - s * a[1], s * a[0] + c * a[1]};
    }
    static double[] perp(double[] a) {
        if (a == null) return null;
        return new double[]{-a[1], a[0]};
    }

    enum ShapeType { RECT, TRIANGLE }

    static abstract class RigidBody {
        double[] position;
        double angle;
        double[] velocity;
        double angularVelocity;
        double mass, invMass;
        double inertia, invInertia;
        Color color;
        boolean isStatic;
        ShapeType shapeType;
        double[] oldPosition; // new double[2]

        RigidBody(double[] pos, double angle, double density, Color color, boolean isStatic, ShapeType shapeType) {
            this.position = pos.clone();
            this.angle = angle;
            this.velocity = new double[]{0, 0};
            this.angularVelocity = 0;
            this.color = color;
            this.isStatic = isStatic;
            this.shapeType = shapeType;
            this.oldPosition = pos.clone();
        }

        abstract double[][] getVertices();
        abstract boolean contains(double[] p);
        abstract void draw(Graphics2D g2d, boolean highlight);
    }

    static class RectBody extends RigidBody {
        int width, height;

        RectBody(double[] pos, double angle, int width, int height, double density, Color color, boolean isStatic) {
            super(pos, angle, density, color, isStatic, ShapeType.RECT);
            this.width = width;
            this.height = height;
            this.mass = width * height * density;
            this.invMass = isStatic ? 0 : 1.0 / this.mass;
            this.inertia = isStatic ? 0 : (1.0 / 12.0) * this.mass * (width * width + height * height);
            this.invInertia = isStatic ? 0 : 1.0 / this.inertia;
        }

        double[] toWorld(double[] local) {
            return add(position, rotate(local, angle));
        }

        @Override
        double[][] getVertices() {
            double hw = width / 2.0, hh = height / 2.0;
            double[][] local = {{-hw, -hh}, {hw, -hh}, {hw, hh}, {-hw, hh}};
            double[][] verts = new double[4][2];
            for (int i = 0; i < 4; i++) verts[i] = toWorld(local[i]);
            return verts;
        }

        @Override
        boolean contains(double[] p) {
            return pointInPolygon(p, getVertices());
        }

        @Override
        void draw(Graphics2D g2d, boolean highlight) {
            double[][] v = getVertices();
            int[] xs = new int[4], ys = new int[4];
            for (int i = 0; i < 4; i++) {
                xs[i] = (int) v[i][0];
                ys[i] = (int) v[i][1];
            }
            g2d.setColor(color);
            g2d.fillPolygon(xs, ys, 4);
            if (highlight) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawPolygon(xs, ys, 4);
                g2d.setStroke(new BasicStroke(1));
            } else {
                g2d.setColor(Color.BLACK);
                g2d.drawPolygon(xs, ys, 4);
            }
        }
    }

    static class TriangleBody extends RigidBody {
        int size;

        TriangleBody(double[] pos, double angle, int size, double density, Color color, boolean isStatic) {
            super(pos, angle, density, color, isStatic, ShapeType.TRIANGLE);
            this.size = size;
            this.mass = (Math.sqrt(3) / 4) * size * size * density;
            this.invMass = isStatic ? 0 : 1.0 / this.mass;
            this.inertia = isStatic ? 0 : (this.mass * size * size) / 36;
            this.invInertia = isStatic ? 0 : 1.0 / this.inertia;
        }

        double[] toWorld(double[] local) {
            return add(position, rotate(local, angle));
        }

        @Override
        double[][] getVertices() {
            double h = size * Math.sqrt(3) / 2;
            double[][] local = {
                    {0, -h / 3},
                    {-size / 2.0, h * 2 / 3 / 2},
                    {size / 2.0, h * 2 / 3 / 2}
            };
            double[][] verts = new double[3][2];
            for (int i = 0; i < 3; i++) verts[i] = toWorld(local[i]);
            return verts;
        }

        @Override
        boolean contains(double[] p) {
            return pointInPolygon(p, getVertices());
        }

        @Override
        void draw(Graphics2D g2d, boolean highlight) {
            double[][] v = getVertices();
            int[] xs = new int[3], ys = new int[3];
            for (int i = 0; i < 3; i++) {
                xs[i] = (int) v[i][0];
                ys[i] = (int) v[i][1];
            }
            g2d.setColor(color);
            g2d.fillPolygon(xs, ys, 3);
            if (highlight) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawPolygon(xs, ys, 3);
                g2d.setStroke(new BasicStroke(1));
            } else {
                g2d.setColor(Color.BLACK);
                g2d.drawPolygon(xs, ys, 3);
            }
        }
    }

    // ====== 이하 유틸, 충돌, 메인 ======

    static boolean pointInPolygon(double[] p, double[][] poly) {
        if(p == null || poly == null) return false;
        boolean inside = false;
        for (int i = 0, j = poly.length - 1; i < poly.length; j = i++) {
            if(poly[i] == null || poly[j] == null) continue;
            if (((poly[i][1] > p[1]) != (poly[j][1] > p[1])) &&
                    (p[0] < (poly[j][0] - poly[i][0]) * (p[1] - poly[i][1]) / (poly[j][1] - poly[i][1]) + poly[i][0]))
                inside = !inside;
        }
        return inside;
    }

    static class Manifold {
        RigidBody A, B;
        double[] normal;
        double penetration;
        List<double[]> contacts;

        Manifold(RigidBody A, RigidBody B, double[] n, double p, List<double[]> contacts) {
            this.A = A;
            this.B = B;
            this.normal = n;
            this.penetration = p;
            this.contacts = contacts;
        }
    }

    static Manifold collide(RigidBody A, RigidBody B) {
        return collidePolyPoly(A, B);
    }

    // 폴리곤-폴리곤 충돌(SAT 기반)
    static Manifold collidePolyPoly(RigidBody A, RigidBody B) {
        double[][] vertsA = A.getVertices();
        double[][] vertsB = B.getVertices();
        double minOverlap = Double.POSITIVE_INFINITY;
        double[] smallestAxis = null;
        boolean shouldFlip = false;

        for (int shape = 0; shape < 2; shape++) {
            double[][] verts = (shape == 0) ? vertsA : vertsB;
            for (int i = 0; i < verts.length; i++) {
                int j = (i + 1) % verts.length;
                double[] edge = sub(verts[j], verts[i]);
                double[] axis = normalize(perp(edge));
                if(axis == null) continue;
                double[] projA = projectOntoAxis(vertsA, axis);
                double[] projB = projectOntoAxis(vertsB, axis);
                double overlap = Math.min(projA[1], projB[1]) - Math.max(projA[0], projB[0]);
                if (overlap < 0) return null;
                if (overlap < minOverlap) {
                    minOverlap = overlap;
                    smallestAxis = axis;
                    shouldFlip = shape == 1;
                }
            }
        }
        double[] dir = normalize(sub(B.position, A.position));
        double[] normal = (shouldFlip) ? mul(smallestAxis, -1) : smallestAxis;
        if (dot(dir, normal) < 0) normal = mul(normal, -1);

        List<double[]> contacts = new ArrayList<>();
        for (double[] va : vertsA) if (pointInPolygon(va, vertsB)) contacts.add(va);
        for (double[] vb : vertsB) if (pointInPolygon(vb, vertsA)) contacts.add(vb);
        if (contacts.isEmpty()) {
            double[] cp = closestPoint(vertsA, vertsB);
            if(cp != null) contacts.add(cp);
        }
        if(contacts.isEmpty()) return null;
        return new Manifold(A, B, normal, minOverlap, contacts);
    }
    static double[] projectOntoAxis(double[][] verts, double[] axis) {
        if(axis == null || verts == null) return new double[]{0,0};
        double min = dot(verts[0], axis), max = min;
        for (int i = 1; i < verts.length; i++) {
            if(verts[i] == null) continue;
            double p = dot(verts[i], axis);
            if (p < min) min = p;
            if (p > max) max = p;
        }
        return new double[]{min, max};
    }
    static double[] closestPoint(double[][] vertsA, double[][] vertsB) {
        if(vertsA == null || vertsB == null) return null;
        double minDist = Double.POSITIVE_INFINITY;
        double[] minP = null;
        for (double[] a : vertsA)
            for (double[] b : vertsB) {
                if(a == null || b == null) continue;
                double d = length(sub(a, b));
                if (d < minDist) {
                    minDist = d;
                    minP = new double[]{(a[0] + b[0]) / 2, (a[1] + b[1]) / 2};
                }
            }
        return minP;
    }

    private static void setFullWidth(JComponent comp) {
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, comp.getPreferredSize().height));
    }

    // ========== MAIN ==========
    public static void main(String[] args) {
        JFrame frame = new JFrame("PhysicsSimulator2D");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 700);

        final double[] gravity = {800};
        final double[] restitution = {0.95};
        final double[] friction = {0.5};
        final int dt_ms = 16;
        final boolean[] isPaused = {false};
        final boolean[] wasPausedBeforeRightDrag = {false};

        final RigidBody[] selectedBody = {null};
        final RigidBody[] draggedBody = {null};
        final double[][] dragOffset = {{0, 0}};
        final boolean[] isDragging = {false};
        final boolean[] isRightDragging = {false};
        final RigidBody[] rightDraggedBody = {null};
        final double[][] rightDragStart = {new double[2]};
        final double[][] rightDragCurrent = {new double[2]};

        List<RigidBody> bodies = new ArrayList<>();
        List<RigidBody> walls = new ArrayList<>();

        JPanel simulationPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(new Color(240, 240, 240));
                g2d.fillRect(0, 0, getWidth(), getHeight());

                for (RigidBody b : walls) {
                    if (b instanceof RectBody) ((RectBody) b).draw(g2d, false);
                }
                for (RigidBody b : bodies) {
                    b.draw(g2d, b == selectedBody[0]);
                }
                // --- 우클릭 드래그 방향 벡터 시각화 ---
                if (isRightDragging[0] && rightDraggedBody[0] != null) {
                    int x1 = (int) rightDragStart[0][0];
                    int y1 = (int) rightDragStart[0][1];
                    int x2 = (int) rightDragCurrent[0][0];
                    int y2 = (int) rightDragCurrent[0][1];
                    g2d.setColor(Color.RED);
                    g2d.setStroke(new BasicStroke(2.5f));
                    g2d.drawLine(x1, y1, x2, y2);
                    // 화살촉
                    double angle = Math.atan2(y2 - y1, x2 - x1);
                    int arrLen = 20;
                    int xA = x2 - (int) (arrLen * Math.cos(angle - Math.PI / 8));
                    int yA = y2 - (int) (arrLen * Math.sin(angle - Math.PI / 8));
                    int xB = x2 - (int) (arrLen * Math.cos(angle + Math.PI / 8));
                    int yB = y2 - (int) (arrLen * Math.sin(angle + Math.PI / 8));
                    g2d.drawLine(x2, y2, xA, yA);
                    g2d.drawLine(x2, y2, xB, yB);
                    g2d.setStroke(new BasicStroke(1));
                }
                if (isPaused[0]) {
                    g2d.setColor(new Color(255, 0, 0, 100));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    g2d.setColor(Color.RED);
                    g2d.setFont(new Font("맑은 고딕", Font.BOLD, 48));
                    FontMetrics fm = g2d.getFontMetrics();
                    String pauseText = "PAUSED";
                    int x = (getWidth() - fm.stringWidth(pauseText)) / 2;
                    int y = getHeight() / 2;
                    g2d.drawString(pauseText, x, y);
                }
            }
        };

        Runnable updateWalls = () -> {
            int wallThickness = 20;
            int W = simulationPanel.getWidth();
            int H = simulationPanel.getHeight();
            walls.clear();
            walls.add(new RectBody(new double[]{W / 2.0, H - wallThickness / 2.0}, 0, W - wallThickness, wallThickness, 0.01, new Color(40, 40, 40), true));
            walls.add(new RectBody(new double[]{W / 2.0, wallThickness / 2.0}, 0, W - wallThickness, wallThickness, 0.01, new Color(40, 40, 40), true));
            walls.add(new RectBody(new double[]{wallThickness / 2.0, H / 2.0}, 0, wallThickness, H, 0.01, new Color(30, 30, 30), true));
            walls.add(new RectBody(new double[]{W - wallThickness / 2.0, H / 2.0}, 0, wallThickness, H, 0.01, new Color(30, 30, 30), true));
        };

        // (2) infoPanel 생성 및 조립
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createTitledBorder("정보 및 안내"));
        infoPanel.setPreferredSize(new Dimension(260, 360));

        JLabel boxCountLabel = new JLabel();
        JLabel selectedBoxLabel = new JLabel("선택된 도형 정보 없음");
        JLabel positionLabel = new JLabel();
        JLabel velocityLabel = new JLabel();
        JLabel angleLabel = new JLabel();
        JLabel angularVelocityLabel = new JLabel();

        setFullWidth(boxCountLabel);
        setFullWidth(selectedBoxLabel);
        setFullWidth(positionLabel);
        setFullWidth(velocityLabel);
        setFullWidth(angleLabel);
        setFullWidth(angularVelocityLabel);

        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(boxCountLabel);
        infoPanel.add(Box.createVerticalStrut(20));
        infoPanel.add(selectedBoxLabel);
        infoPanel.add(positionLabel);
        infoPanel.add(velocityLabel);
        infoPanel.add(angleLabel);
        infoPanel.add(angularVelocityLabel);
        infoPanel.add(Box.createVerticalGlue());

        Runnable updateInfoPanel = () -> {
            boxCountLabel.setText("도형 개수: " + bodies.size());
            if (selectedBody[0] != null) {
                selectedBoxLabel.setText("선택된 도형 정보:");
                positionLabel.setText(String.format("위치: (%.0f, %.0f)", selectedBody[0].position[0], selectedBody[0].position[1]));
                velocityLabel.setText(String.format("속도: (%.1f, %.1f)", selectedBody[0].velocity[0], selectedBody[0].velocity[1]));
                angleLabel.setText(String.format("각도: %.1f°", Math.toDegrees(selectedBody[0].angle)));
                angularVelocityLabel.setText(String.format("각속도: %.2f", selectedBody[0].angularVelocity));
            } else {
                selectedBoxLabel.setText("선택된 도형 정보 없음");
                positionLabel.setText("");
                velocityLabel.setText("");
                angleLabel.setText("");
                angularVelocityLabel.setText("");
            }
        };

        // (3) controlPanel 생성 및 조립
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createTitledBorder("제어 패널"));
        controlPanel.setPreferredSize(new Dimension(260, 380));

        JLabel gravityLabel = new JLabel("중력: " + (int) gravity[0] + "  (←/→)");
        JSlider gravitySlider = new JSlider(0, 2000, (int) gravity[0]);
        gravitySlider.addChangeListener(e -> {
            gravity[0] = gravitySlider.getValue();
            gravityLabel.setText("중력: " + (int) gravity[0] + "  (←/→)");
            simulationPanel.requestFocusInWindow();
        });

        JLabel restitutionLabel = new JLabel("반발계수: " + String.format("%.2f", restitution[0]) + "  (↑/↓)");
        JSlider restitutionSlider = new JSlider(0, 100, (int) (restitution[0] * 100));
        restitutionSlider.addChangeListener(e -> {
            restitution[0] = restitutionSlider.getValue() / 100.0;
            restitutionLabel.setText("반발계수: " + String.format("%.2f", restitution[0]) + "  (↑/↓)");
            simulationPanel.requestFocusInWindow();
        });

        JLabel frictionLabel = new JLabel("마찰계수: " + String.format("%.2f", friction[0]) + "  (PgUp/PgDn)");
        JSlider frictionSlider = new JSlider(0, 100, (int) (friction[0] * 100));
        frictionSlider.addChangeListener(e -> {
            friction[0] = frictionSlider.getValue() / 100.0;
            frictionLabel.setText("마찰계수: " + String.format("%.2f", friction[0]) + "  (PgUp/PgDn)");
            simulationPanel.requestFocusInWindow();
        });

        JLabel shapeLabel = new JLabel("도형 선택: (1:사각형, 2:삼각형)");
        JRadioButton rectBtn = new JRadioButton("사각형 (1)", true);
        JRadioButton triBtn = new JRadioButton("삼각형 (2)");
        ButtonGroup shapeGroup = new ButtonGroup();
        shapeGroup.add(rectBtn);
        shapeGroup.add(triBtn);

        JPanel shapePanel = new JPanel();
        shapePanel.setLayout(new GridLayout(1, 2));
        shapePanel.add(rectBtn);
        shapePanel.add(triBtn);
        shapePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, shapePanel.getPreferredSize().height));

        ShapeType[] currentShape = {ShapeType.RECT};
        rectBtn.addActionListener(e -> {
            currentShape[0] = ShapeType.RECT;
            simulationPanel.requestFocusInWindow();
        });
        triBtn.addActionListener(e -> {
            currentShape[0] = ShapeType.TRIANGLE;
            simulationPanel.requestFocusInWindow();
        });

        JButton pauseButton = new JButton("일시정지 (F5)");
        pauseButton.addActionListener(e -> {
            isPaused[0] = !isPaused[0];
            pauseButton.setText(isPaused[0] ? "재생 (F5)" : "일시정지 (F5)");
            simulationPanel.repaint();
            updateInfoPanel.run();
            simulationPanel.requestFocusInWindow();
        });

        JButton resetButton = new JButton("리셋 (Ctrl+Shift+F5)");
        resetButton.addActionListener(e -> {
            bodies.clear();
            bodies.add(new RectBody(new double[]{250, 200}, 0, 60, 60, 0.01, Color.BLUE, false));
            bodies.add(new TriangleBody(new double[]{350, 100}, Math.toRadians(22), 60, 0.01, Color.GREEN, false));
            selectedBody[0] = null;
            draggedBody[0] = null;
            isDragging[0] = false;
            isPaused[0] = false;
            pauseButton.setText("일시정지 (F5)");
            simulationPanel.repaint();
            updateInfoPanel.run();
            simulationPanel.requestFocusInWindow();
        });

        JButton clearButton = new JButton("모든 도형 삭제 (Ctrl+Delete)");
        clearButton.addActionListener(e -> {
            bodies.clear();
            selectedBody[0] = null;
            draggedBody[0] = null;
            isDragging[0] = false;
            simulationPanel.repaint();
            updateInfoPanel.run();
            simulationPanel.requestFocusInWindow();
        });

        setFullWidth(frictionLabel);
        setFullWidth(frictionSlider);
        setFullWidth(gravityLabel);
        setFullWidth(gravitySlider);
        setFullWidth(restitutionLabel);
        setFullWidth(restitutionSlider);
        setFullWidth(shapeLabel);
        setFullWidth(rectBtn);
        setFullWidth(triBtn);
        setFullWidth(shapePanel);
        setFullWidth(pauseButton);
        setFullWidth(resetButton);
        setFullWidth(clearButton);

        JLabel deleteHintLabel = new JLabel("선택 도형 삭제: Delete");
        setFullWidth(deleteHintLabel);
        JLabel mouseHintLabel = new JLabel(
                "<html>마우스 사용법<br>"
                        + "· 좌클릭: 추가/선택<br>"
                        + "· 드래그: 이동<br>"
                        + "· 우클릭: 삭제/사출<br>"
                        + "· 우클릭드래그: 사출 방향 지정</html>");
        setFullWidth(mouseHintLabel);

        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(gravityLabel);
        controlPanel.add(gravitySlider);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(restitutionLabel);
        controlPanel.add(restitutionSlider);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(frictionLabel);
        controlPanel.add(frictionSlider);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(shapeLabel);
        controlPanel.add(shapePanel);
        controlPanel.add(Box.createVerticalStrut(20));
        controlPanel.add(pauseButton);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(resetButton);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(clearButton);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(deleteHintLabel);
        controlPanel.add(Box.createVerticalStrut(4));
        controlPanel.add(mouseHintLabel);
        controlPanel.add(Box.createVerticalGlue());

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.add(infoPanel, BorderLayout.NORTH);
        rightPanel.add(controlPanel, BorderLayout.CENTER);

        simulationPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                double[] mousePos = {e.getX(), e.getY()};
                RigidBody clickedBody = null;
                for (int i = bodies.size() - 1; i >= 0; i--) {
                    if (bodies.get(i).contains(mousePos)) {
                        clickedBody = bodies.get(i);
                        break;
                    }
                }
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (clickedBody != null) {
                        selectedBody[0] = clickedBody;
                        draggedBody[0] = clickedBody;
                        dragOffset[0][0] = mousePos[0] - clickedBody.position[0];
                        dragOffset[0][1] = mousePos[1] - clickedBody.position[1];
                        isDragging[0] = true;
                        clickedBody.velocity[0] = 0;
                        clickedBody.velocity[1] = 0;
                        clickedBody.angularVelocity = 0;
                    } else {
                        selectedBody[0] = null;
                        int wallThickness = 20;
                        int W = simulationPanel.getWidth(), H = simulationPanel.getHeight();
                        int shapeMargin = wallThickness + 35;
                        if (e.getX() > shapeMargin && e.getX() < W - shapeMargin &&
                                e.getY() > shapeMargin && e.getY() < H - shapeMargin) {
                            Color[] colors = {Color.ORANGE, Color.PINK, Color.CYAN, Color.YELLOW, Color.MAGENTA};
                            Color color = colors[(int) (Math.random() * colors.length)];
                            RigidBody newBody = null;
                            if (currentShape[0] == ShapeType.RECT)
                                newBody = new RectBody(new double[]{e.getX(), e.getY()}, Math.random() * Math.PI, 60, 60, 0.01, color, false);
                            else if (currentShape[0] == ShapeType.TRIANGLE)
                                newBody = new TriangleBody(new double[]{e.getX(), e.getY()}, Math.random() * Math.PI, 60, 0.01, color, false);
                            bodies.add(newBody);
                            selectedBody[0] = newBody;
                        }
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    if (clickedBody != null) {
                        wasPausedBeforeRightDrag[0] = isPaused[0];
                        isPaused[0] = true;
                        pauseButton.setText("재생 (F5)");
                        isRightDragging[0] = true;
                        rightDraggedBody[0] = clickedBody;
                        rightDragStart[0][0] = e.getX();
                        rightDragStart[0][1] = e.getY();
                        rightDragCurrent[0][0] = e.getX();
                        rightDragCurrent[0][1] = e.getY();
                    } else {
                        isRightDragging[0] = false;
                        rightDraggedBody[0] = null;
                    }
                }
                simulationPanel.repaint();
                updateInfoPanel.run();
                simulationPanel.requestFocusInWindow();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && isDragging[0]) {
                    isDragging[0] = false;
                    draggedBody[0] = null;
                }
                if (SwingUtilities.isRightMouseButton(e) && isRightDragging[0] && rightDraggedBody[0] != null) {
                    double[] start = rightDragStart[0];
                    double[] end = rightDragCurrent[0];
                    double[] launchVec = sub(end, start);
                    double forceScale = 5.0;
                    rightDraggedBody[0].velocity[0] = launchVec[0] * forceScale;
                    rightDraggedBody[0].velocity[1] = launchVec[1] * forceScale;
                    rightDraggedBody[0].angularVelocity = 0;
                    isRightDragging[0] = false;
                    rightDraggedBody[0] = null;
                    isPaused[0] = wasPausedBeforeRightDrag[0];
                    pauseButton.setText(isPaused[0] ? "재생 (F5)" : "일시정지 (F5)");
                }
                simulationPanel.repaint();
                updateInfoPanel.run();
                simulationPanel.requestFocusInWindow();
            }
        });

        simulationPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging[0] && draggedBody[0] != null) {
                    int panelWidth = simulationPanel.getWidth();
                    int panelHeight = simulationPanel.getHeight();
                    int wallThickness = 20;
                    double newX = e.getX() - dragOffset[0][0];
                    double newY = e.getY() - dragOffset[0][1];
                    if (draggedBody[0].shapeType == ShapeType.RECT) {
                        RectBody rb = (RectBody) draggedBody[0];
                        int halfW = rb.width / 2;
                        int halfH = rb.height / 2;
                        if (newX < halfW) newX = halfW;
                        if (newX > panelWidth - halfW) newX = panelWidth - halfW;
                        if (newY < halfH) newY = halfH;
                        if (newY > panelHeight - halfH) newY = panelHeight - halfH;
                    } else if (draggedBody[0].shapeType == ShapeType.TRIANGLE) {
                        TriangleBody tb = (TriangleBody) draggedBody[0];
                        double r = tb.size / Math.sqrt(3);
                        if (newX < r) newX = r;
                        if (newX > panelWidth - r) newX = panelWidth - r;
                        if (newY < r) newY = r;
                        if (newY > panelHeight - r) newY = panelHeight - r;
                    }
                    draggedBody[0].position[0] = newX;
                    draggedBody[0].position[1] = newY;
                    draggedBody[0].velocity[0] = 0;
                    draggedBody[0].velocity[1] = 0;
                    draggedBody[0].angularVelocity = 0;
                }
                // --- Angry Birds 스타일: 우클릭 드래그 시 끝점 갱신 ---
                if (isRightDragging[0] && rightDraggedBody[0] != null) {
                    rightDragCurrent[0][0] = e.getX();
                    rightDragCurrent[0][1] = e.getY();
                }
                simulationPanel.repaint();
                updateInfoPanel.run();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                double[] mousePos = {e.getX(), e.getY()};
                RigidBody bodyUnderMouse = null;
                for (int i = bodies.size() - 1; i >= 0; i--) {
                    if (bodies.get(i).contains(mousePos)) {
                        bodyUnderMouse = bodies.get(i);
                        break;
                    }
                }
                if (bodyUnderMouse != null) {
                    simulationPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    simulationPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        // ---- 키보드 단축키 지원 ----
        KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                boolean ctrl = (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0;
                boolean shift = (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_F5:
                        if (ctrl && shift) {
                            bodies.clear();
                            bodies.add(new RectBody(new double[]{250, 200}, 0, 60, 60, 0.01, Color.BLUE, false));
                            bodies.add(new TriangleBody(new double[]{350, 100}, Math.toRadians(22), 60, 0.01, Color.GREEN, false));
                            selectedBody[0] = null;
                            draggedBody[0] = null;
                            isDragging[0] = false;
                            isPaused[0] = false;
                            pauseButton.setText("일시정지 (F5)");
                            simulationPanel.repaint();
                            updateInfoPanel.run();
                        } else if (!ctrl && !shift) {
                            isPaused[0] = !isPaused[0];
                            pauseButton.setText(isPaused[0] ? "재생 (F5)" : "일시정지 (F5)");
                            simulationPanel.repaint();
                            updateInfoPanel.run();
                        }
                        break;
                    case KeyEvent.VK_DELETE:
                        if (ctrl) {
                            bodies.clear();
                            selectedBody[0] = null;
                            draggedBody[0] = null;
                            isDragging[0] = false;
                            simulationPanel.repaint();
                            updateInfoPanel.run();
                        } else {
                            if (selectedBody[0] != null) {
                                bodies.remove(selectedBody[0]);
                                selectedBody[0] = null;
                                draggedBody[0] = null;
                                isDragging[0] = false;
                                simulationPanel.repaint();
                                updateInfoPanel.run();
                            }
                        }
                        break;
                    case KeyEvent.VK_1:
                        currentShape[0] = ShapeType.RECT;
                        rectBtn.setSelected(true);
                        break;
                    case KeyEvent.VK_2:
                        currentShape[0] = ShapeType.TRIANGLE;
                        triBtn.setSelected(true);
                        break;
                    case KeyEvent.VK_LEFT:
                        gravity[0] = Math.max(0, gravity[0] - 50);
                        gravitySlider.setValue((int) gravity[0]);
                        gravityLabel.setText("중력: " + (int) gravity[0] + "  (←/→)");
                        break;
                    case KeyEvent.VK_RIGHT:
                        gravity[0] = Math.min(2000, gravity[0] + 50);
                        gravitySlider.setValue((int) gravity[0]);
                        gravityLabel.setText("중력: " + (int) gravity[0] + "  (←/→)");
                        break;
                    case KeyEvent.VK_UP:
                        restitution[0] = Math.min(1.0, restitution[0] + 0.05);
                        restitutionSlider.setValue((int) (restitution[0] * 100));
                        restitutionLabel.setText("반발계수: " + String.format("%.2f", restitution[0]) + "  (↑/↓)");
                        break;
                    case KeyEvent.VK_DOWN:
                        restitution[0] = Math.max(0.0, restitution[0] - 0.05);
                        restitutionSlider.setValue((int) (restitution[0] * 100));
                        restitutionLabel.setText("반발계수: " + String.format("%.2f", restitution[0]) + "  (↑/↓)");
                        break;
                    case KeyEvent.VK_PAGE_UP:
                        friction[0] = Math.min(1.0, friction[0] + 0.05);
                        frictionSlider.setValue((int) (friction[0] * 100));
                        frictionLabel.setText("마찰계수: " + String.format("%.2f", friction[0]) + "  (PgUp/PgDn)");
                        break;
                    case KeyEvent.VK_PAGE_DOWN:
                        friction[0] = Math.max(0.0, friction[0] - 0.05);
                        frictionSlider.setValue((int) (friction[0] * 100));
                        frictionLabel.setText("마찰계수: " + String.format("%.2f", friction[0]) + "  (PgUp/PgDn)");
                        break;
                }
            }
        };
        frame.addKeyListener(keyAdapter);
        simulationPanel.addKeyListener(keyAdapter);
        simulationPanel.setFocusable(true);
        simulationPanel.requestFocusInWindow();

        Timer timer = new Timer(dt_ms, e -> {
            if (isPaused[0]) return;
            updateWalls.run();
            int sweepSteps = 6;
            double sweepDt = (dt_ms / 1000.0) / sweepSteps;
            for (RigidBody b : bodies) {
                b.oldPosition[0] = b.position[0];
                b.oldPosition[1] = b.position[1];
            }
            for (int step = 0; step < sweepSteps; step++) {
                for (RigidBody b : bodies) {
                    if (b.isStatic || b == draggedBody[0]) continue;
                    b.velocity[1] += gravity[0] * sweepDt;
                    b.position[0] += b.velocity[0] * sweepDt;
                    b.position[1] += b.velocity[1] * sweepDt;
                    b.angle += b.angularVelocity * sweepDt;
                }
                List<Manifold> contacts = new ArrayList<>();
                int N = bodies.size();
                for (int i = 0; i < N; i++) {
                    for (int j = i + 1; j < N; j++) {
                        RigidBody bodyA = bodies.get(i);
                        RigidBody bodyB = bodies.get(j);
                        if (bodyA == draggedBody[0] && bodyB == draggedBody[0]) continue;
                        Manifold m = collide(bodyA, bodyB);
                        if (m != null && m.contacts != null && !m.contacts.isEmpty()) contacts.add(m);
                    }
                }
                for (RigidBody box : bodies) {
                    for (RigidBody wall : walls) {
                        Manifold m = collide(box, wall);
                        if (m != null && m.contacts != null && !m.contacts.isEmpty()) contacts.add(m);
                    }
                }
                for (Manifold m : contacts) {
                    for (double[] contact : m.contacts) {
                        if (contact == null) continue;
                        RigidBody A = m.A, B = m.B;
                        boolean AisDragged = (A == draggedBody[0]);
                        boolean BisDragged = (B == draggedBody[0]);
                        double[] ra = sub(contact, A.position);
                        double[] rb = sub(contact, B.position);
                        double[] va = add(A.velocity, mul(perp(ra), A.angularVelocity));
                        double[] vb = add(B.velocity, mul(perp(rb), B.angularVelocity));
                        double[] rv = sub(vb, va);
                        double velAlongNormal = dot(rv, m.normal);
                        if (velAlongNormal > 0) continue;
                        double thisRestitution = restitution[0];
                        if (Math.abs(velAlongNormal) < 0.01)
                            thisRestitution = 0.0;
                        double raCrossN = ra[0] * m.normal[1] - ra[1] * m.normal[0];
                        double rbCrossN = rb[0] * m.normal[1] - rb[1] * m.normal[0];
                        double invMassA = AisDragged ? 0 : A.invMass;
                        double invMassB = BisDragged ? 0 : B.invMass;
                        double invMassSum = invMassA + invMassB
                                + (raCrossN * raCrossN) * (AisDragged ? 0 : A.invInertia)
                                + (rbCrossN * rbCrossN) * (BisDragged ? 0 : B.invInertia);
                        if (invMassSum == 0) continue;
                        double j = -(1 + thisRestitution) * velAlongNormal / invMassSum;
                        j /= m.contacts.size();
                        double[] impulse = mul(m.normal, j);
                        if (!AisDragged) {
                            A.velocity = sub(A.velocity, mul(impulse, A.invMass));
                            A.angularVelocity -= cross(ra, impulse) * A.invInertia;
                        }
                        if (!BisDragged) {
                            B.velocity = add(B.velocity, mul(impulse, B.invMass));
                            B.angularVelocity += cross(rb, impulse) * B.invInertia;
                        }
                        // 마찰력 (friction[0] 사용)
                        double[] tangent = {-m.normal[1], m.normal[0]};
                        va = add(A.velocity, mul(perp(ra), A.angularVelocity));
                        vb = add(B.velocity, mul(perp(rb), B.angularVelocity));
                        rv = sub(vb, va);
                        double vt = dot(rv, tangent);
                        double mu = friction[0];
                        double jt = -vt / invMassSum;
                        jt /= m.contacts.size();
                        double maxFriction = mu * Math.abs(j);
                        double frictionImpulseMag = Math.max(-maxFriction, Math.min(jt, maxFriction));
                        double[] frictionImpulse = mul(tangent, frictionImpulseMag);
                        if (!AisDragged) {
                            A.velocity = sub(A.velocity, mul(frictionImpulse, A.invMass));
                            A.angularVelocity -= cross(ra, frictionImpulse) * A.invInertia;
                        }
                        if (!BisDragged) {
                            B.velocity = add(B.velocity, mul(frictionImpulse, B.invMass));
                            B.angularVelocity += cross(rb, frictionImpulse) * B.invInertia;
                        }
                        double percent = 0.13;
                        double slop = 0.5;
                        double maxCorrection = 2.5;
                        double invMassSum2 = (AisDragged ? 0 : A.invMass) + (BisDragged ? 0 : B.invMass);
                        double corr = Math.min(Math.max(m.penetration - slop, 0) / (invMassSum2 > 0 ? invMassSum2 : 1.0) * percent, maxCorrection);
                        if (invMassSum2 > 0) {
                            if (!AisDragged && A.invMass > 0)
                                A.position = sub(A.position, mul(m.normal, corr * A.invMass / invMassSum2));
                            if (!BisDragged && B.invMass > 0)
                                B.position = add(B.position, mul(m.normal, corr * B.invMass / invMassSum2));
                        }
                    }
                }
            }
            for (RigidBody b : bodies) {
                if (b.isStatic || b == draggedBody[0]) continue;
                if (Math.abs(b.velocity[0]) < 1.0) b.velocity[0] = 0;
                if (Math.abs(b.velocity[1]) < 1.0) b.velocity[1] = 0;
                if (Math.abs(b.angularVelocity) < 0.02) b.angularVelocity = 0;
            }
            simulationPanel.repaint();
            updateInfoPanel.run();
        });
        timer.start();

        frame.setLayout(new BorderLayout());
        frame.add(simulationPanel, BorderLayout.CENTER);
        frame.add(rightPanel, BorderLayout.EAST);
        frame.setVisible(true);

        SwingUtilities.invokeLater(() -> {
            updateWalls.run();
            updateInfoPanel.run();
        });
    }
}