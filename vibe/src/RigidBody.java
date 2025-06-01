import java.awt.*;

/**
 * RigidBody(강체)는 2D 물리 시뮬레이션에서
 * 모든 도형(사각형, 삼각형 등)의 '물리적 성질'을 공통적으로 다루는 추상 클래스입니다.
 *
 * 주요 역할:
 * - 도형의 위치, 각도, 속도, 질량, 관성, 색상, 정적 여부 등 모든 공통 속성 보유
 * - 구체적인 꼭짓점 계산, 내부 포함, 그리기 등은 하위 클래스에서 구현
 *
 * 참고:
 * - Rigid Body Dynamics (Wikipedia): https://en.wikipedia.org/wiki/Rigid_body_dynamics
 * - 강체란? (쉽게 설명): https://physics.info/rigid/
 */
public abstract class RigidBody {
    /**
     * 도형의 현재 중심 위치 벡터
     */
    private Vector2D position;

    /**
     * 도형의 현재 회전 각도(라디안)
     */
    private double angle;

    /**
     * 도형의 이동 속도 벡터(방향/크기)
     */
    private Vector2D velocity;

    /**
     * 도형의 각속도(초당 회전 각, 라디안/초)
     */
    private double angularVelocity;

    /**
     * 질량(무게)
     * 정적 객체는 의미 없음, 동적 객체는 물리적 반응(가속 등)에 사용
     * 참고: F=ma, 뉴턴 운동법칙 (https://en.wikipedia.org/wiki/Newton%27s_laws_of_motion)
     */
    private final double mass;

    /**
     * 역질량(1/mass).
     * 질량이 0(고정체)이면 0, 아니면 1/mass.
     * 곱셈으로 계산 효율성을 높이기 위해 사용.
     */
    private final double invMass;

    /**
     * 관성 모멘트(회전에 대한 저항값).
     * 각 도형별로 수식이 다름.
     * 공식: https://en.wikipedia.org/wiki/List_of_moments_of_inertia
     */
    private final double inertia;

    /**
     * 역관성 모멘트(1/inertia).
     * 관성 모멘트가 0(고정체)이면 0, 아니면 1/inertia.
     */
    private final double invInertia;

    /**
     * 도형의 색상 (시각적 표시)
     */
    private final Color color;

    /**
     * 정적인지(움직이지 않는지) 여부.
     * 정적이면 모든 힘/가속도/회전 무시
     */
    private final boolean isStatic;

    /**
     * 이전 위치(충돌 후 보정, 이동 거리 계산 등에 활용)
     */
    private Vector2D oldPosition;

    /**
     * 꼭짓점 개수 (예: 사각형=4, 삼각형=3)
     */
    protected final int VERTEX_COUNT;

    /**
     * RigidBody 생성자
     *
     * @param position     중심 좌표
     * @param angle        초기 회전 각도(라디안)
     * @param mass         질량(0이면 고정체)
     * @param inertia      관성 모멘트(0이면 고정체)
     * @param color        도형 색상
     * @param isStatic     정적(고정) 객체 여부
     * @param vertexCount  꼭짓점 개수
     */
    public RigidBody(
            final Vector2D position,
            final double angle,
            final double mass,
            final double inertia,
            final Color color,
            final boolean isStatic,
            final int vertexCount
    ) {
        this.position = position;
        this.angle = angle;
        this.velocity = new Vector2D(0, 0);
        this.angularVelocity = 0;
        this.mass = mass;
        this.inertia = inertia;
        this.invMass = isStatic ? 0 : 1.0 / mass;
        this.invInertia = isStatic ? 0 : 1.0 / inertia;
        this.color = color;
        this.isStatic = isStatic;
        this.oldPosition = position;
        this.VERTEX_COUNT = vertexCount;
    }

    /** 현재 중심 위치 반환 */
    public Vector2D getPosition() {
        return this.position;
    }

    /** 중심 위치 설정 */
    public void setPosition(final Vector2D pos) {
        this.position = pos;
    }

    /** 현재 회전 각도 반환 */
    public double getAngle() {
        return this.angle;
    }

    /** 회전 각도 설정 */
    public void setAngle(final double angle) {
        this.angle = angle;
    }

    /** 현재 속도 벡터 반환 */
    public Vector2D getVelocity() {
        return this.velocity;
    }

    /** 속도 벡터 설정 */
    public void setVelocity(final Vector2D v) {
        this.velocity = v;
    }

    /** 각속도 반환 */
    public double getAngularVelocity() {
        return this.angularVelocity;
    }

    /** 각속도 설정 */
    public void setAngularVelocity(final double w) {
        this.angularVelocity = w;
    }

    /** 질량 반환 */
    public double getMass() {
        return this.mass;
    }

    /** 역질량 반환 (곱셈 연산용) */
    public double getInvMass() {
        return this.invMass;
    }

    /** 관성 모멘트 반환 */
    public double getInertia() {
        return this.inertia;
    }

    /** 역관성 모멘트 반환 */
    public double getInvInertia() {
        return this.invInertia;
    }

    /** 도형 색상 반환 */
    public Color getColor() {
        return this.color;
    }

    /** 정적 객체 여부 반환 */
    public boolean isStatic() {
        return this.isStatic;
    }

    /** 이전 위치 반환 */
    public Vector2D getOldPosition() {
        return this.oldPosition;
    }

    /** 이전 위치 설정 */
    public void setOldPosition(final Vector2D p) {
        this.oldPosition = p;
    }

    /**
     * 꼭짓점 좌표 배열 반환 (각 도형마다 구현)
     */
    public abstract Vector2D[] getVertices();

    /**
     * 점이 도형 내부에 포함되는지 판정 (각 도형마다 구현)
     */
    public abstract boolean contains(final Vector2D p);

    /**
     * 도형을 화면에 그림 (각 도형마다 구현)
     */
    public abstract void draw(final Graphics2D g2d, final boolean highlight);
}
