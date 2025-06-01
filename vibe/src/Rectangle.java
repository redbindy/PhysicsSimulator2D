import java.awt.*;

/**
 * Rectangle 클래스는 2D 물리 엔진에서 사용하는 '직사각형' 도형의 특화된 RigidBody(강체) 구현체입니다.
 *
 * 주요 기능:
 * - 위치, 회전, 색상, 고정 여부, 밀도 등 물리 속성을 가짐
 * - 사각형의 4개 꼭짓점 좌표 반환, 내부 포함 여부 검사, 화면 그리기
 *
 * 물리 엔진에서 직사각형(박스)의 질량, 관성 모멘트(회전 저항), 꼭짓점 회전 등은
 * 모두 실제 물리공식에 기반함
 *
 * 참고:
 * - Rectangle 질량/관성 모멘트 공식: https://en.wikipedia.org/wiki/List_of_moments_of_inertia
 * - Rigid Body 개념: https://en.wikipedia.org/wiki/Rigid_body_dynamics
 */
public class Rectangle extends RigidBody {
    /**
     * 사각형의 너비(픽셀 단위)
     */
    private final int width;

    /**
     * 사각형의 높이(픽셀 단위)
     */
    private final int height;

    /**
     * Rectangle(직사각형) 생성자입니다.
     * @param position  중심 좌표 (Vector2D)
     * @param angle     회전 각도(라디안)
     * @param width     너비(픽셀)
     * @param height    높이(픽셀)
     * @param density   밀도(단위 면적당 질량)
     * @param color     색상
     * @param isStatic  정적(고정) 여부
     */
    public Rectangle(
            final Vector2D position,
            final double angle,
            final int width,
            final int height,
            final double density,
            final Color color,
            final boolean isStatic
    ) {
        // 질량 계산: mass = 면적 * 밀도
        // 관성 모멘트(회전 저항): (1/12) * 질량 * (width^2 + height^2)
        // 공식 출처: https://en.wikipedia.org/wiki/List_of_moments_of_inertia
        super(
                position,
                angle,
                width * height * density,
                (1.0 / 12.0) * width * height * density * (width * width + height * height),
                color,
                isStatic,
                4 // 사각형은 꼭짓점 4개
        );

        this.width = width;
        this.height = height;
    }

    /**
     * 사각형의 너비 반환
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * 사각형의 높이 반환
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * 사각형의 4개 꼭짓점 좌표를 배열로 반환합니다.
     *
     * 1. 사각형의 중심을 기준으로 -hw, -hh ~ +hw, +hh까지 꼭짓점 로컬 좌표 생성
     * 2. 회전각만큼 회전시킨 뒤, 실제 위치로 평행이동
     *
     * 참고: 2D 회전 공식
     * https://en.wikipedia.org/wiki/Rotation_matrix#In_two_dimensions
     */
    @Override
    public Vector2D[] getVertices() {
        final double hw = this.width / 2.0;
        final double hh = this.height / 2.0;

        // 중심 기준 꼭짓점(로컬)
        final Vector2D[] local = {
                new Vector2D(-hw, -hh),
                new Vector2D(hw, -hh),
                new Vector2D(hw, hh),
                new Vector2D(-hw, hh)
        };

        // 실제 꼭짓점 좌표(회전 + 평행이동)
        final Vector2D[] verts = new Vector2D[VERTEX_COUNT];
        for (int i = 0; i < VERTEX_COUNT; ++i) {
            verts[i] = this.getPosition().add(local[i].rotate(this.getAngle()));
        }

        return verts;
    }

    /**
     * 사각형이 주어진 점(point)을 포함하는지 검사합니다.
     * 내부 구현은 PhysicsUtil.pointInPolygon 사용 (Ray casting)
     */
    @Override
    public boolean contains(final Vector2D point) {
        return PhysicsUtil.pointInPolygon(point, this.getVertices());
    }

    /**
     * 화면에 사각형을 그립니다.
     *
     * - 꼭짓점 좌표를 int 배열로 변환해서 Polygon을 그림
     * - highlight가 true면 테두리를 노란색 두껍게 강조함
     * - 아니면 검은 테두리로 그림
     */
    @Override
    public void draw(final Graphics2D g2d, final boolean highlight) {
        final Vector2D[] v = this.getVertices();

        final int[] xs = new int[VERTEX_COUNT];
        final int[] ys = new int[VERTEX_COUNT];

        for (int i = 0; i < VERTEX_COUNT; i++) {
            xs[i] = (int) v[i].getX();
            ys[i] = (int) v[i].getY();
        }

        g2d.setColor(this.getColor());
        g2d.fillPolygon(xs, ys, VERTEX_COUNT);

        if (highlight) {
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawPolygon(xs, ys, VERTEX_COUNT);
            g2d.setStroke(new BasicStroke(1));
        } else {
            g2d.setColor(Color.BLACK);
            g2d.drawPolygon(xs, ys, VERTEX_COUNT);
        }
    }
}
