import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * UI 클래스는 2D 물리 시뮬레이션의 전체 사용자 인터페이스를 담당합니다.
 * 그래픽 창, 시뮬레이션 패널, 도형 제어, 마우스/키보드 이벤트 처리 등
 * 물리를 몰라도 작동 방식을 이해할 수 있도록 설계되어 있습니다.
 *
 * 참고:
 * - Java Swing GUI 공식 문서: https://docs.oracle.com/javase/tutorial/uiswing/
 * - 2D 벡터와 인터랙션: https://en.wikipedia.org/wiki/Vector_(mathematics_and_physics)
 */
public final class UI {
    /**
     * 실제 물리 시뮬레이션이 이루어지는 World 객체
     */
    private final World world;
    private JFrame frame;
    private JPanel simulationPanel;
    private Timer timer;

    /**
     * UI의 각종 상태 변수들 (배열로 선언하여 내부 클래스에서도 접근 가능)
     */
    private final boolean[] isPaused = {false};
    private final RigidBody[] selectedBody = {null};
    private final RigidBody[] draggedBody = {null};
    private final Vector2D[] dragOffset = {new Vector2D(0, 0)};
    private final boolean[] isDragging = {false};
    private final boolean[] isRightDragging = {false};
    private final RigidBody[] rightDraggedBody = {null};
    private final Vector2D[] rightDragStart = {new Vector2D(0, 0)};
    private final Vector2D[] rightDragCurrent = {new Vector2D(0, 0)};
    private final boolean[] wasPausedBeforeRightDrag = {false};
    private final ShapeType[] currentShape = {ShapeType.RECT};

    /**
     * UI 생성자
     * @param world 물리 시뮬레이션을 관리하는 World 객체
     */
    public UI(final World world) {
        this.world = world;
    }

    /**
     * UI 창 및 시뮬레이션 환경, 각종 패널, 이벤트 리스너 등을 모두 세팅합니다.
     * 메인 진입점에 해당합니다.
     */
    public void show() {
        this.frame = new JFrame("PhysicsSimulator2D");
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setSize(1280, 768);

        /**
         * 시뮬레이션을 실제로 그리는 메인 패널입니다.
         * 도형, 벽, 마우스 인터랙션 등 모든 시각적 요소를 그림.
         */
        this.simulationPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 패널 배경색
                g2d.setColor(new Color(240, 240, 240));
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // 벽(경계) 그리기
                for (RigidBody b : UI.this.world.getWalls()) {
                    b.draw(g2d, false);
                }

                // 모든 도형 그리기 (선택된 도형은 강조)
                for (RigidBody b : UI.this.world.getBodies()) {
                    b.draw(g2d, b == UI.this.selectedBody[0]);
                }

                // 우클릭 드래그 화살표 시각화
                if (UI.this.isRightDragging[0] && UI.this.rightDraggedBody[0] != null) {
                    final int x1 = (int) UI.this.rightDragStart[0].getX();
                    final int y1 = (int) UI.this.rightDragStart[0].getY();

                    final int x2 = (int) UI.this.rightDragCurrent[0].getX();
                    final int y2 = (int) UI.this.rightDragCurrent[0].getY();

                    g2d.setColor(Color.RED);
                    g2d.setStroke(new BasicStroke(2.5f));
                    g2d.drawLine(x1, y1, x2, y2);

                    // 화살표 머리
                    double angle = Math.atan2(y2 - y1, x2 - x1);

                    final int arrLen = 20;
                    final int xA = x2 - (int) (arrLen * Math.cos(angle - Math.PI / 8));
                    final int yA = y2 - (int) (arrLen * Math.sin(angle - Math.PI / 8));

                    final int xB = x2 - (int) (arrLen * Math.cos(angle + Math.PI / 8));
                    final int yB = y2 - (int) (arrLen * Math.sin(angle + Math.PI / 8));

                    g2d.drawLine(x2, y2, xA, yA);
                    g2d.drawLine(x2, y2, xB, yB);

                    g2d.setStroke(new BasicStroke(1));
                }

                // 일시정지 오버레이
                if (UI.this.isPaused[0]) {
                    g2d.setColor(new Color(255, 0, 0, 100));
                    g2d.fillRect(0, 0, getWidth(), getHeight());

                    g2d.setColor(Color.RED);
                    g2d.setFont(new Font("맑은 고딕", Font.BOLD, 48));

                    FontMetrics fm = g2d.getFontMetrics();
                    String pauseText = "PAUSED";

                    final int x = (getWidth() - fm.stringWidth(pauseText)) / 2;
                    final int y = getHeight() / 2;

                    g2d.drawString(pauseText, x, y);
                }
            }
        };

        /**
         * 벽(경계) 자동 생성.
         * 화면 사이즈에 맞추어 4방향에 벽을 재생성.
         * 벽은 항상 정적인 RigidBody(직사각형)로 추가됨.
         */
        Runnable updateWalls = () -> {
            final int wallThickness = 20;

            final int W = UI.this.simulationPanel.getWidth();
            final int H = UI.this.simulationPanel.getHeight();

            UI.this.world.clearWalls();
            UI.this.world.addWall(new Rectangle(new Vector2D(W / 2.0, H - wallThickness / 2.0), 0, W - wallThickness, wallThickness, 0.01, new Color(40, 40, 40), true));
            UI.this.world.addWall(new Rectangle(new Vector2D(W / 2.0, wallThickness / 2.0), 0, W - wallThickness, wallThickness, 0.01, new Color(40, 40, 40), true));
            UI.this.world.addWall(new Rectangle(new Vector2D(wallThickness / 2.0, H / 2.0), 0, wallThickness, H, 0.01, new Color(30, 30, 30), true));
            UI.this.world.addWall(new Rectangle(new Vector2D(W - wallThickness / 2.0, H / 2.0), 0, wallThickness, H, 0.01, new Color(30, 30, 30), true));
        };

        /**
         * infoPanel: 현재 도형 개수, 선택된 도형의 정보(위치, 속도, 각도, 각속도) 등 실시간 표시
         * BoxLayout을 사용해 위에서 아래로 쌓임.
         */
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createTitledBorder("정보 및 안내"));
        infoPanel.setPreferredSize(new Dimension(260, 180));
        infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel boxCountLabel = new JLabel();
        JLabel selectedBoxLabel = new JLabel();
        JLabel positionLabel = new JLabel();
        JLabel velocityLabel = new JLabel();
        JLabel angleLabel = new JLabel();
        JLabel angularVelocityLabel = new JLabel();

        boxCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectedBoxLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        positionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        velocityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        angleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        angularVelocityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(boxCountLabel);
        infoPanel.add(selectedBoxLabel);
        infoPanel.add(positionLabel);
        infoPanel.add(velocityLabel);
        infoPanel.add(angleLabel);
        infoPanel.add(angularVelocityLabel);

        /**
         * infoPanel을 새로 갱신하는 함수
         * 도형 정보, 선택된 도형 정보(위치/속도/각도 등) 갱신
         */
        Runnable updateInfoPanel = () -> {
            boxCountLabel.setText("도형 개수: " + this.world.getBodies().size());
            if (this.selectedBody[0] != null) {
                selectedBoxLabel.setText("선택된 도형: " + this.selectedBody[0].getClass().getSimpleName());
                positionLabel.setText(String.format("위치: (%.0f, %.0f)", this.selectedBody[0].getPosition().getX(), this.selectedBody[0].getPosition().getY()));
                velocityLabel.setText(String.format("속도: (%.1f, %.1f)", this.selectedBody[0].getVelocity().getX(), this.selectedBody[0].getVelocity().getY()));
                angleLabel.setText(String.format("각도: %.1f°", Math.toDegrees(this.selectedBody[0].getAngle())));
                angularVelocityLabel.setText(String.format("각속도: %.2f", this.selectedBody[0].getAngularVelocity()));
            } else {
                selectedBoxLabel.setText("선택된 도형 없음");
                positionLabel.setText("");
                velocityLabel.setText("");
                angleLabel.setText("");
                angularVelocityLabel.setText("");
            }
        };

        /**
         * controlPanel: 중력, 반발계수, 마찰계수 슬라이더, 도형 선택(사각형/삼각형), 색상 선택 등
         * BoxLayout을 사용해 위에서 아래로 쌓임.
         */
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createTitledBorder("제어 패널"));
        controlPanel.setPreferredSize(new Dimension(260, 380));
        controlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel gravityLabel = new JLabel("중력: " + (int) this.world.getGravity() + "  (←/→)");
        gravityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSlider gravitySlider = new JSlider(0, 2000, (int) this.world.getGravity());
        gravitySlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        gravitySlider.addChangeListener(e -> {
            this.world.setGravity(gravitySlider.getValue());
            gravityLabel.setText("중력: " + gravitySlider.getValue() + "  (←/→)");
            this.simulationPanel.requestFocusInWindow();
        });

        JLabel restitutionLabel = new JLabel("반발계수: " + String.format("%.2f", this.world.getRestitution()) + "  (↑/↓)");
        restitutionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSlider restitutionSlider = new JSlider(0, 100, (int) (this.world.getRestitution() * 100));
        restitutionSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        restitutionSlider.addChangeListener(e -> {
            double value = restitutionSlider.getValue() / 100.0;
            this.world.setRestitution(value);
            restitutionLabel.setText("반발계수: " + String.format("%.2f", value) + "  (↑/↓)");
            this.simulationPanel.requestFocusInWindow();
        });

        JLabel frictionLabel = new JLabel("마찰계수: " + String.format("%.2f", this.world.getFriction()) + "  (PgUp/PgDn)");
        frictionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSlider frictionSlider = new JSlider(0, 100, (int) (this.world.getFriction() * 100));
        frictionSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        frictionSlider.addChangeListener(e -> {
            double value = frictionSlider.getValue() / 100.0;
            this.world.setFriction(value);
            frictionLabel.setText("마찰계수: " + String.format("%.2f", value) + "  (PgUp/PgDn)");
            this.simulationPanel.requestFocusInWindow();
        });

        JLabel shapeLabel = new JLabel("도형 선택: (1:사각형, 2:삼각형)");
        shapeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JRadioButton rectBtn = new JRadioButton("사각형 (1)", true);
        rectBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        rectBtn.addActionListener(e -> {
            this.currentShape[0] = ShapeType.RECT;
            this.simulationPanel.requestFocusInWindow();
        });

        JRadioButton triBtn = new JRadioButton("삼각형 (2)");
        triBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        triBtn.addActionListener(e -> {
            this.currentShape[0] = ShapeType.TRIANGLE;
            this.simulationPanel.requestFocusInWindow();
        });

        ButtonGroup shapeGroup = new ButtonGroup();
        shapeGroup.add(rectBtn);
        shapeGroup.add(triBtn);

        JPanel shapePanel = new JPanel();
        shapePanel.setLayout(new GridLayout(1, 2));
        shapePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        shapePanel.add(rectBtn);
        shapePanel.add(triBtn);

        // 도형 색상 선택 (RGB 직접 입력 + 랜덤 색상)
        JPanel colorPanel = new JPanel();
        colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.Y_AXIS));
        colorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel colorLabel = new JLabel("도형 색상 선택:");
        colorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel rgbPanel = new JPanel();
        rgbPanel.setLayout(new BoxLayout(rgbPanel, BoxLayout.X_AXIS));
        rgbPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel rLabel = new JLabel("R");
        rLabel.setAlignmentY(Component.CENTER_ALIGNMENT);

        JTextField rField = new JTextField("255", 3);
        rField.setAlignmentY(Component.CENTER_ALIGNMENT);

        JLabel gLabel = new JLabel("G");
        gLabel.setAlignmentY(Component.CENTER_ALIGNMENT);

        JTextField gField = new JTextField("165", 3);
        gField.setAlignmentY(Component.CENTER_ALIGNMENT);

        JLabel bLabel = new JLabel("B");
        bLabel.setAlignmentY(Component.CENTER_ALIGNMENT);

        JTextField bField = new JTextField("0", 3);
        bField.setAlignmentY(Component.CENTER_ALIGNMENT);

        rgbPanel.add(rLabel);
        rgbPanel.add(rField);
        rgbPanel.add(Box.createHorizontalStrut(4));
        rgbPanel.add(gLabel);
        rgbPanel.add(gField);
        rgbPanel.add(Box.createHorizontalStrut(4));
        rgbPanel.add(bLabel);
        rgbPanel.add(bField);

        rgbPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JCheckBox randomColorCheck = new JCheckBox("랜덤 색상 생성", true);
        randomColorCheck.setAlignmentX(Component.LEFT_ALIGNMENT);

        rField.setEnabled(false);
        gField.setEnabled(false);
        bField.setEnabled(false);

        randomColorCheck.addActionListener(e -> {
            boolean random = randomColorCheck.isSelected();
            rField.setEnabled(!random);
            gField.setEnabled(!random);
            bField.setEnabled(!random);
        });

        colorPanel.add(colorLabel);
        colorPanel.add(rgbPanel);
        colorPanel.add(Box.createVerticalStrut(4));
        colorPanel.add(randomColorCheck);

        colorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, colorPanel.getPreferredSize().height));

        // 제어 패널에 요소들 추가
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
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(colorPanel);
        controlPanel.add(Box.createVerticalStrut(16));

        /**
         * 각종 제어 버튼(일시정지, 리셋, 전체삭제, 선택삭제) 및 안내 문구 추가
         */
        JButton pauseButton = new JButton("일시정지 (F5)");
        pauseButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        pauseButton.addActionListener(e -> {
            this.isPaused[0] = !this.isPaused[0];
            pauseButton.setText(this.isPaused[0] ? "재생 (F5)" : "일시정지 (F5)");
            this.simulationPanel.repaint();
            updateInfoPanel.run();
            this.simulationPanel.requestFocusInWindow();
        });

        JButton resetButton = new JButton("리셋 (Ctrl+Shift+F5)");
        resetButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetButton.addActionListener(e -> {
            this.world.clearBodies();
            this.world.addBody(new Rectangle(new Vector2D(250, 200), 0, 60, 60, 0.01, Color.BLUE, false));
            this.world.addBody(new Triangle(new Vector2D(350, 100), Math.toRadians(22), 60, 0.01, Color.GREEN, false));
            this.selectedBody[0] = null;
            this.draggedBody[0] = null;
            this.isDragging[0] = false;
            this.isPaused[0] = false;
            pauseButton.setText("일시정지 (F5)");
            this.simulationPanel.repaint();
            updateInfoPanel.run();
            this.simulationPanel.requestFocusInWindow();
        });

        JButton clearButton = new JButton("모든 도형 삭제 (Ctrl+Delete)");
        clearButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        clearButton.addActionListener(e -> {
            this.world.clearBodies();
            this.selectedBody[0] = null;
            this.draggedBody[0] = null;
            this.isDragging[0] = false;
            this.simulationPanel.repaint();
            updateInfoPanel.run();
            this.simulationPanel.requestFocusInWindow();
        });

        JLabel deleteHintLabel = new JLabel("선택 도형 삭제: Delete");
        deleteHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel mouseHintLabel = new JLabel(
                "<html>마우스 사용법<br>"
                        + "· 좌클릭: 추가/선택<br>"
                        + "· 드래그: 이동<br>"
                        + "· 우클릭: 사출 도형 선택<br>"
                        + "· 우클릭드래그: 사출 방향 지정</html>");
        mouseHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        controlPanel.add(pauseButton);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(resetButton);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(clearButton);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(deleteHintLabel);
        controlPanel.add(Box.createVerticalStrut(4));
        controlPanel.add(mouseHintLabel);

        // 우측(좌측) 패널 통합
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(infoPanel, BorderLayout.NORTH);
        rightPanel.add(controlPanel, BorderLayout.CENTER);

        this.frame.setLayout(new BorderLayout());
        this.frame.add(this.simulationPanel, BorderLayout.CENTER);
        this.frame.add(rightPanel, BorderLayout.EAST);

        /**
         * 마우스 클릭/드래그 이벤트 핸들링
         * - 좌클릭: 도형 선택/생성, 드래그 준비
         * - 우클릭: 도형 삭제/사출, 우클릭 드래그 준비
         * - 드래그 시 마우스 위치에 맞게 도형 위치 실시간 보정
         */
        this.simulationPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 마우스가 눌린 위치를 2D 벡터로 저장
                Vector2D mousePos = new Vector2D(e.getX(), e.getY());
                RigidBody clickedBody = null;

                /*
                 * (1) 마우스가 현재 어떤 도형 위에 있는지 확인
                 * - 리스트의 마지막(가장 위에 그려진 도형)부터 검사 → 겹칠 때 상위 도형이 선택됨
                 * - 도형의 contains()는 해당 점이 내부에 포함되는지 검사
                 */
                for (int i = UI.this.world.getBodies().size() - 1; i >= 0; i--) {
                    if (UI.this.world.getBodies().get(i).contains(mousePos)) {
                        clickedBody = UI.this.world.getBodies().get(i);
                        break;
                    }
                }

                // ----------- 좌클릭(왼쪽 마우스 버튼)
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (clickedBody != null) {
                        /*
                         * (2-1) 도형 위에서 좌클릭:
                         * - 해당 도형을 선택 및 드래그 준비 상태로 변경
                         * - 마우스와 도형 중심 사이 거리(dragOffset)를 기록하여
                         *   드래그 시 도형이 순간이동하지 않고 자연스럽게 이동하도록 함
                         * - 드래그 시작 시 속도/각속도를 0으로 초기화(불안정한 동작 방지)
                         *
                         * 참고: 마우스로 물체를 드래그해서 옮기는 인터랙션 구현
                         * 출처: https://en.wikipedia.org/wiki/Drag_and_drop
                         */
                        UI.this.selectedBody[0] = clickedBody;
                        UI.this.draggedBody[0] = clickedBody;
                        UI.this.dragOffset[0] = mousePos.subtract(clickedBody.getPosition());
                        UI.this.isDragging[0] = true;

                        clickedBody.setVelocity(new Vector2D(0, 0));
                        clickedBody.setAngularVelocity(0);
                    } else {
                        /*
                         * (2-2) 빈 공간에서 좌클릭:
                         * - 현재 선택 해제 후, 클릭 위치가 벽 경계 안쪽이면 도형을 새로 추가
                         * - RGB 직접 입력 또는 랜덤 색상 여부는 체크박스로 결정
                         * - 도형 종류(사각형/삼각형)는 라디오버튼/단축키에 따라 다름
                         */
                        UI.this.selectedBody[0] = null;
                        int wallThickness = 20;

                        int W = UI.this.simulationPanel.getWidth(), H = UI.this.simulationPanel.getHeight();
                        int shapeMargin = wallThickness + 35;

                        // 경계(벽)에서 너무 가까운 위치에는 도형이 생성되지 않도록 체크
                        if (e.getX() > shapeMargin && e.getX() < W - shapeMargin && e.getY() > shapeMargin && e.getY() < H - shapeMargin) {
                            Color color;
                            if (randomColorCheck.isSelected()) {
                                // (2-2-a) 랜덤 색상 생성
                                Color[] colors = {Color.ORANGE, Color.PINK, Color.CYAN, Color.YELLOW, Color.MAGENTA};
                                color = colors[(int) (Math.random() * colors.length)];
                            } else {
                                // (2-2-b) RGB 값 직접 입력
                                try {
                                    int r = Integer.parseInt(rField.getText().trim());
                                    int g = Integer.parseInt(gField.getText().trim());
                                    int b = Integer.parseInt(bField.getText().trim());

                                    r = Math.max(0, Math.min(r, 255));
                                    g = Math.max(0, Math.min(g, 255));
                                    b = Math.max(0, Math.min(b, 255));

                                    color = new Color(r, g, b);
                                } catch (NumberFormatException ex) {
                                    color = Color.GRAY; // 입력값 오류시 기본색
                                }
                            }

                            // (2-2-c) 도형 종류(사각형/삼각형) 선택 후 생성
                            RigidBody newBody;
                            if (UI.this.currentShape[0] == ShapeType.RECT) {
                                // 회전각 랜덤, 크기 60x60, 밀도 0.01, 정적아님
                                newBody = new Rectangle(mousePos, Math.random() * Math.PI, 60, 60, 0.01, color, false);
                            } else {
                                newBody = new Triangle(mousePos, Math.random() * Math.PI, 60, 0.01, color, false);
                            }

                            // 생성된 도형을 시뮬레이션에 추가, 선택 상태로 만듦
                            UI.this.world.addBody(newBody);
                            UI.this.selectedBody[0] = newBody;
                        }
                    }
                }

                // ----------- 우클릭(오른쪽 마우스 버튼)
                else if (SwingUtilities.isRightMouseButton(e)) {
                    if (clickedBody != null) {
                        /*
                         * (3-1) 도형 위에서 우클릭:
                         * - 우클릭 드래그 시작 상태로 전환(사출 힘/방향 지정)
                         * - 기존 일시정지 여부를 기억, 일시정지 상태로 변경(드래그 후 마우스 놓을 때 복원)
                         * - 드래그 시작 위치/현재 위치 모두 저장
                         *
                         * 참고: 마우스로 힘(velocity)을 전달하는 원리
                         * 출처: https://en.wikipedia.org/wiki/Newton%27s_laws_of_motion
                         */
                        UI.this.wasPausedBeforeRightDrag[0] = UI.this.isPaused[0];
                        UI.this.isPaused[0] = true;

                        pauseButton.setText("재생 (F5)");

                        UI.this.isRightDragging[0] = true;
                        UI.this.rightDraggedBody[0] = clickedBody;
                        UI.this.rightDragStart[0] = mousePos;
                        UI.this.rightDragCurrent[0] = mousePos;
                    } else {
                        /*
                         * (3-2) 빈 공간에서 우클릭:
                         * - 우클릭 드래그 관련 상태 모두 해제(실행중이던 사출 취소)
                         */
                        UI.this.isRightDragging[0] = false;
                        UI.this.rightDraggedBody[0] = null;
                    }
                }

                // 시뮬레이션 패널 다시 그리기 & 정보 패널 갱신 & 포커스 재설정
                UI.this.simulationPanel.repaint();
                updateInfoPanel.run();
                UI.this.simulationPanel.requestFocusInWindow();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                /*
                 * (4) 좌클릭 드래그 상태라면 드래그 해제
                 */
                if (SwingUtilities.isLeftMouseButton(e) && UI.this.isDragging[0]) {
                    UI.this.isDragging[0] = false;
                    UI.this.draggedBody[0] = null;
                }

                /*
                 * (5) 우클릭 드래그 후 마우스 놓을 때:
                 * - 사출(발사) 벡터 = 드래그 방향, 길이만큼 도형에 속도로 전달
                 * - forceScale은 사출 강도 계수(크면 더 빨리 날아감)
                 * - 각속도는 0으로 초기화
                 * - 일시정지 상태는 이전 값으로 복구
                 * - 사출 관련 상태 모두 해제
                 *
                 * 참고: "힘=질량x가속도, 속도=방향벡터x계수" (F=ma, v=F/m)
                 */
                if (SwingUtilities.isRightMouseButton(e) && UI.this.isRightDragging[0] && UI.this.rightDraggedBody[0] != null) {
                    Vector2D start = UI.this.rightDragStart[0];
                    Vector2D end = UI.this.rightDragCurrent[0];

                    Vector2D launchVec = end.subtract(start);     // 드래그 방향 벡터

                    double forceScale = 5.0;                      // 발사 속도 계수(실험적으로 결정)

                    UI.this.rightDraggedBody[0].setVelocity(launchVec.multiply(forceScale));
                    UI.this.rightDraggedBody[0].setAngularVelocity(0);
                    UI.this.isRightDragging[0] = false;
                    UI.this.rightDraggedBody[0] = null;

                    UI.this.isPaused[0] = UI.this.wasPausedBeforeRightDrag[0];

                    pauseButton.setText(UI.this.isPaused[0] ? "재생 (F5)" : "일시정지 (F5)");
                }

                // 시뮬레이션 패널 다시 그리기 & 정보 패널 갱신 & 포커스 재설정
                UI.this.simulationPanel.repaint();
                updateInfoPanel.run();
                UI.this.simulationPanel.requestFocusInWindow();
            }
        });

        this.simulationPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // (1) 마우스가 드래그되는 동안의 좌표를 2D 벡터로 구함
                Vector2D mousePos = new Vector2D(e.getX(), e.getY());

                // --- [A] 도형을 드래그 중인 경우 ---
                if (UI.this.isDragging[0] && UI.this.draggedBody[0] != null) {
                    // 패널 크기 및 벽 두께 정보
                    int panelWidth = UI.this.simulationPanel.getWidth();
                    int panelHeight = UI.this.simulationPanel.getHeight();
                    int wallThickness = 20;

                    /*
                     * (1-A) 새 도형 위치 = 마우스 위치 - 처음 잡을 때의 오프셋(중심까지 거리)
                     * 이렇게 하면 마우스가 도형 어느 부분에서 시작하더라도 자연스럽게 이동함
                     */
                    double newX = mousePos.getX() - UI.this.dragOffset[0].getX();
                    double newY = mousePos.getY() - UI.this.dragOffset[0].getY();
                    RigidBody body = UI.this.draggedBody[0];

                    // 사각형의 경우, 경계 바깥으로 안 나가게 위치 제한
                    if (body instanceof Rectangle) {
                        Rectangle rb = (Rectangle) body;

                        int halfW = rb.getWidth() / 2;
                        int halfH = rb.getHeight() / 2;

                        // 왼쪽/오른쪽/위/아래 벽 충돌 방지 (좌표가 벽 안에 머물게 제한)
                        if (newX < halfW) {
                            newX = halfW;
                        }
                        if (newX > panelWidth - halfW) {
                            newX = panelWidth - halfW;
                        }
                        if (newY < halfH) {
                            newY = halfH;
                        }
                        if (newY > panelHeight - halfH) {
                            newY = panelHeight - halfH;
                        }
                    }
                    // 삼각형도 동일하게 경계 바깥으로 안 나가게 제한
                    else if (body instanceof Triangle) {
                        Triangle tb = (Triangle) body;

                        // (정삼각형 외접원의 반지름에 해당하는 값)
                        double r = tb.getSize() / Math.sqrt(3);

                        if (newX < r) {
                            newX = r;
                        }
                        if (newX > panelWidth - r) {
                            newX = panelWidth - r;
                        }
                        if (newY < r) {
                            newY = r;
                        }
                        if (newY > panelHeight - r) {
                            newY = panelHeight - r;
                        }
                    }

                    // (1-B) 계산된 위치로 도형 이동. 드래그 중에는 속도/각속도를 0으로(정지)
                    body.setPosition(new Vector2D(newX, newY));
                    body.setVelocity(new Vector2D(0, 0));
                    body.setAngularVelocity(0);
                }

                // --- [B] 우클릭 드래그(사출 방향 설정) 중일 때 ---
                if (UI.this.isRightDragging[0] && UI.this.rightDraggedBody[0] != null) {
                    // 현재 마우스 위치를 저장(화살표 시각화 및 사출 방향 계산에 사용)
                    UI.this.rightDragCurrent[0] = mousePos;
                }

                // 시뮬레이션/정보 패널 다시 그림
                UI.this.simulationPanel.repaint();
                updateInfoPanel.run();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                // (2) 마우스가 움직일 때마다 해당 위치에 도형이 있는지 확인
                Vector2D mousePos = new Vector2D(e.getX(), e.getY());
                RigidBody bodyUnderMouse = null;

                // 리스트의 마지막(위에 그려진 도형)부터 검사 → 겹칠 때 최상단 도형이 선택됨
                for (int i = UI.this.world.getBodies().size() - 1; i >= 0; i--) {
                    if (UI.this.world.getBodies().get(i).contains(mousePos)) {
                        bodyUnderMouse = UI.this.world.getBodies().get(i);
                        break;
                    }
                }

                /*
                 * (2-A) 마우스가 도형 위에 있으면 커서를 손 모양으로 바꿈(사용자 친화성)
                 * (2-B) 빈 공간이면 기본 커서로 변경
                 */
                if (bodyUnderMouse != null) {
                    UI.this.simulationPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    UI.this.simulationPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        /**
         * 키보드 입력(단축키) 처리
         * - F5: 일시정지/재생
         * - Ctrl+Shift+F5: 리셋
         * - Delete: 선택 도형 삭제
         * - Ctrl+Delete: 전체 도형 삭제
         * - 1/2: 도형 종류 선택
         * - ←/→/↑/↓/PgUp/PgDn: 물리 파라미터 조정
         */
        KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                boolean ctrl = (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0;
                boolean shift = (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0;

                /*
                 * 키보드 입력 이벤트 처리
                 * - e.getKeyCode(): 어떤 키를 눌렀는지 판별
                 * - ctrl/shift 플래그는 조합키(단축키) 구현에 사용
                 *
                 * (참고: 자바 단축키 구현 공식 가이드
                 * https://docs.oracle.com/javase/tutorial/uiswing/events/keylistener.html)
                 */
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_F5:
                        // F5: 일시정지/재생, Ctrl+Shift+F5: 리셋(기본 도형으로 초기화)
                        if (ctrl && shift) {
                            /*
                             * Ctrl+Shift+F5
                             * - 모든 도형 삭제 후 예시 도형 2개 추가(초기상태 복원)
                             * - 선택/드래그/일시정지 상태도 전부 초기화
                             */
                            UI.this.world.clearBodies();

                            UI.this.world.addBody(new Rectangle(new Vector2D(250, 200), 0, 60, 60, 0.01, Color.BLUE, false));
                            UI.this.world.addBody(new Triangle(new Vector2D(350, 100), Math.toRadians(22), 60, 0.01, Color.GREEN, false));

                            UI.this.selectedBody[0] = null;
                            UI.this.draggedBody[0] = null;
                            UI.this.isDragging[0] = false;
                            UI.this.isPaused[0] = false;

                            pauseButton.setText("일시정지 (F5)");

                            UI.this.simulationPanel.repaint();
                            updateInfoPanel.run();
                        } else if (!ctrl && !shift) {
                            /*
                             * F5만 단독 입력
                             * - 일시정지 ↔ 재생 전환
                             * - 버튼 텍스트도 동적으로 변경
                             */
                            UI.this.isPaused[0] = !UI.this.isPaused[0];

                            pauseButton.setText(UI.this.isPaused[0] ? "재생 (F5)" : "일시정지 (F5)");

                            UI.this.simulationPanel.repaint();
                            updateInfoPanel.run();
                        }
                        break;

                    case KeyEvent.VK_DELETE:
                        // Delete: 선택 도형 삭제, Ctrl+Delete: 전체 삭제(리셋 아님)
                        if (ctrl) {
                            /*
                             * Ctrl+Delete
                             * - 전체 도형 삭제
                             * - 선택/드래그 상태 초기화
                             */
                            UI.this.world.clearBodies();

                            UI.this.selectedBody[0] = null;
                            UI.this.draggedBody[0] = null;
                            UI.this.isDragging[0] = false;

                            UI.this.simulationPanel.repaint();
                            updateInfoPanel.run();
                        } else {
                            /*
                             * Delete만 단독 입력
                             * - 현재 선택된 도형만 삭제
                             * - 삭제 후 선택/드래그 상태 초기화
                             */
                            if (UI.this.selectedBody[0] != null) {
                                UI.this.world.removeBody(UI.this.selectedBody[0]);

                                UI.this.selectedBody[0] = null;
                                UI.this.draggedBody[0] = null;
                                UI.this.isDragging[0] = false;

                                UI.this.simulationPanel.repaint();
                                updateInfoPanel.run();
                            }
                        }
                        break;

                    case KeyEvent.VK_1:
                        /*
                         * 1번 키: 도형 추가/생성시 '사각형'으로 선택
                         * - 라디오 버튼 UI도 연동하여 선택됨을 표시
                         */
                        UI.this.currentShape[0] = ShapeType.RECT;
                        rectBtn.setSelected(true);
                        break;

                    case KeyEvent.VK_2:
                        /*
                         * 2번 키: 도형 추가/생성시 '삼각형'으로 선택
                         * - 라디오 버튼 UI도 연동하여 선택됨을 표시
                         */
                        UI.this.currentShape[0] = ShapeType.TRIANGLE;
                        triBtn.setSelected(true);
                        break;

                    case KeyEvent.VK_LEFT:
                        /*
                         * ← 방향키: 중력 값을 50만큼 감소(최소 0)
                         * - 슬라이더/레이블 동기화
                         * - 중력이 작아지면 도형이 느리게 떨어짐(=g값 감소)
                         * 참고: g는 중력 가속도(Gravity), SI 단위 m/s^2
                         */
                        UI.this.world.setGravity(Math.max(0, UI.this.world.getGravity() - 50));
                        gravitySlider.setValue((int) UI.this.world.getGravity());
                        gravityLabel.setText("중력: " + (int) UI.this.world.getGravity() + "  (←/→)");
                        break;

                    case KeyEvent.VK_RIGHT:
                        /*
                         * → 방향키: 중력 값을 50만큼 증가(최대 2000)
                         * - 슬라이더/레이블 동기화
                         * - 중력이 커지면 도형이 빨리 떨어짐
                         */
                        UI.this.world.setGravity(Math.min(2000, UI.this.world.getGravity() + 50));
                        gravitySlider.setValue((int) UI.this.world.getGravity());
                        gravityLabel.setText("중력: " + (int) UI.this.world.getGravity() + "  (←/→)");
                        break;

                    case KeyEvent.VK_UP:
                        /*
                         * ↑ 방향키: 반발계수(탄성) 0.05 증가(최대 1.0)
                         * - 반발계수가 클수록 충돌시 더 많이 튕김(1=완전탄성, 0=비탄성)
                         * 참고: 반발계수 공식 e = 속도비/에너지비 (https://en.wikipedia.org/wiki/Coefficient_of_restitution)
                         */
                        UI.this.world.setRestitution(Math.min(1.0, UI.this.world.getRestitution() + 0.05));
                        restitutionSlider.setValue((int) (UI.this.world.getRestitution() * 100));
                        restitutionLabel.setText("반발계수: " + String.format("%.2f", UI.this.world.getRestitution()) + "  (↑/↓)");
                        break;

                    case KeyEvent.VK_DOWN:
                        /*
                         * ↓ 방향키: 반발계수(탄성) 0.05 감소(최소 0)
                         * - 낮아질수록 충돌 후 잘 안튕김
                         */
                        UI.this.world.setRestitution(Math.max(0.0, UI.this.world.getRestitution() - 0.05));
                        restitutionSlider.setValue((int) (UI.this.world.getRestitution() * 100));
                        restitutionLabel.setText("반발계수: " + String.format("%.2f", UI.this.world.getRestitution()) + "  (↑/↓)");
                        break;

                    case KeyEvent.VK_PAGE_UP:
                        /*
                         * PgUp: 마찰계수 0.05 증가(최대 1.0)
                         * - 마찰계수가 높을수록 잘 멈춤(바닥에 닿을 때 덜 미끄러짐)
                         * 참고: 마찰계수(μ, mu)는 표면 특성에 따라 달라짐
                         */
                        UI.this.world.setFriction(Math.min(1.0, UI.this.world.getFriction() + 0.05));
                        frictionSlider.setValue((int) (UI.this.world.getFriction() * 100));
                        frictionLabel.setText("마찰계수: " + String.format("%.2f", UI.this.world.getFriction()) + "  (PgUp/PgDn)");
                        break;

                    case KeyEvent.VK_PAGE_DOWN:
                        /*
                         * PgDn: 마찰계수 0.05 감소(최소 0)
                         * - 낮아질수록 미끄러워짐
                         */
                        UI.this.world.setFriction(Math.max(0.0, UI.this.world.getFriction() - 0.05));
                        frictionSlider.setValue((int) (UI.this.world.getFriction() * 100));
                        frictionLabel.setText("마찰계수: " + String.format("%.2f", UI.this.world.getFriction()) + "  (PgUp/PgDn)");
                        break;

                    default:
                        // 기타 키 입력은 무시
                        break;
                }
            }
        };

        this.frame.addKeyListener(keyAdapter);
        this.simulationPanel.addKeyListener(keyAdapter);
        this.simulationPanel.setFocusable(true);
        this.simulationPanel.requestFocusInWindow();

        /**
         * 타이머: 16ms(약 60FPS)마다 시뮬레이션과 UI를 자동 갱신
         */
        this.timer = new Timer(16, e -> {
            if (!UI.this.isPaused[0]) {
                updateWalls.run();
                this.world.update(0.016, this.draggedBody[0]);
            }

            updateInfoPanel.run();
            this.simulationPanel.repaint();
        });
        this.timer.start();

        this.frame.setVisible(true);

        SwingUtilities.invokeLater(() -> {
            /*
             * (1) SwingUtilities.invokeLater는 "이 코드 블록을 UI 스레드(Event Dispatch Thread)에서 실행"하도록 예약합니다.
             * (2) updateWalls.run(): 창이 처음 생성된 뒤, 실제 패널 크기에 맞게 벽(경계) 재생성
             * (3) updateInfoPanel.run(): infoPanel의 정보도 즉시 갱신
             *
             * 참고: Swing에서는 UI 관련 작업은 반드시 EDT에서 실행해야 오류/깜빡임 없이 안전하게 동작합니다.
             * 공식: https://docs.oracle.com/javase/tutorial/uiswing/concurrency/initial.html
             */
            updateWalls.run();
            updateInfoPanel.run();
        });
    }
}
