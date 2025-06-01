import java.awt.*;

/**
 * Triangle 클래스는 2D 물리 엔진에서 '정삼각형' 도형을 나타내는 RigidBody(강체) 구현체입니다.
 *
 * 주요 기능:
 * - 정삼각형의 위치, 회전, 크기, 질량, 관성 모멘트, 색상, 정적 여부 등을 보유
 * - 꼭짓점 좌표 계산, 내부 포함 검사, 화면 그리기 등 수행
 *
 * 참고:
 * - 정삼각형의 면적: (√3 / 4) * 한 변^2
 * - 정삼각형의 질량 중심, 관성 모멘트:
 *   https://en.wikipedia.org/wiki/List_of_moments_of_inertia
 * - Rigid Body 개념: https://en.wikipedia.org/wiki/Rigid_body_dynamics
 */
public class Triangle extends RigidBody {
    /**
     * 정삼각형 한 변의 길이(픽셀 단위)
     */
    private final int size;

    /**
     * Triangle(정삼각형) 생성자
     * @param position  중심 좌표 (Vector2D)
     * @param angle     회전 각도(라디안)
     * @param size      한 변의 길이(픽셀)
     * @param density   밀도(단위 면적당 질량)
     * @param color     색상
     * @param isStatic  정적(고정) 여부
     */
    public Triangle(
            final Vector2D position,
            final double angle,
            final int size,
            final double density,
            final Color color,
            final boolean isStatic
    ) {
        // 정삼각형 면적: (√3/4) * size^2
        // 관성 모멘트(중심 기준): (면적 * size^2) / 36
        // 공식 출처: https://en.wikipedia.org/wiki/List_of_moments_of_inertia
        super(
                position,
                angle,
                (Math.sqrt(3) / 4) * size * size * density,
                ((Math.sqrt(3) / 4) * size * size * density * size * size) / 36,
                color,
                isStatic,
                3 // 꼭짓점 3개
        );

        this.size = size;
    }

    /** 정삼각형 한 변의 길이 반환 */
    public int getSize() {
        return this.size;
    }

    /**
     * 정삼각형의 3개 꼭짓점 좌표를 배열로 반환합니다.
     *
     * - 중심을 기준으로, 수직 위쪽(-h/3), 왼쪽/오른쪽(h*2/3/2)에서 꼭짓점 좌표 생성
     * - 모든 꼭짓점에 현재 회전각만큼 회전 후, 실제 위치로 이동
     *
     * 참고:
     * - 정삼각형 높이: h = size * sqrt(3) / 2
     * - 질량 중심, 회전 공식: https://en.wikipedia.org/wiki/Centroid#Centroid_of_a_triangle
     */
    @Override
    public Vector2D[] getVertices() {
        final double h = this.size * Math.sqrt(3) / 2;

        // 중심 기준 꼭짓점(로컬 좌표)
        final Vector2D[] local = {
                new Vector2D(0, -h / 3),
                new Vector2D(-this.size / 2.0, h * 2 / 3 / 2),
                new Vector2D(this.size / 2.0, h * 2 / 3 / 2)
        };

        // 실제 꼭짓점 좌표(회전 + 이동)
        final Vector2D[] verts = new Vector2D[VERTEX_COUNT];
        for (int i = 0; i < VERTEX_COUNT; ++i) {
            verts[i] = this.getPosition().add(local[i].rotate(this.getAngle()));
        }

        return verts;
    }

    /**
     * 정삼각형이 주어진 점(point)을 포함하는지 검사합니다.
     * 내부적으로 PhysicsUtil.pointInPolygon 사용 (Ray casting)
     */
    @Override
    public boolean contains(final Vector2D point) {
        return PhysicsUtil.pointInPolygon(point, this.getVertices());
    }

    /**
     * 화면에 정삼각형을 그립니다.
     *
     * - 꼭짓점 좌표를 int 배열로 변환해서 Polygon을 그림
     * - highlight가 true면 테두리를 노란색 두껍게 강조
     * - 아니면 검은 테두리
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
