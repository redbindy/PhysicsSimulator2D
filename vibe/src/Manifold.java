import java.util.List;

/**
 * Manifold(매니폴드)는 두 물체가 충돌했을 때,
 * 충돌에 대한 정보를 저장하는 자료구조입니다.
 *
 * 이 구조는 충돌에 관련된 다양한 정보를 캡슐화해서,
 * 이후 충돌 해결(반발, 위치 보정 등)에 사용됩니다.
 *
 * 참고:
 * - 충돌 판정 및 매니폴드 개념: https://learnopengl.com/In-Practice/2D-Game/Collisions/Collision-resolution
 * - Physics Manifold (Wikipedia): https://en.wikipedia.org/wiki/Manifold_(mathematics)
 */
public final class Manifold {
    /**
     * 충돌한 첫 번째 물체(RigidBody 객체)
     */
    private final RigidBody a;

    /**
     * 충돌한 두 번째 물체(RigidBody 객체)
     */
    private final RigidBody b;

    /**
     * 충돌 표면의 '법선 벡터'입니다.
     * 두 물체가 겹친 부분에서, A에서 B로 뻗어나가는 방향을 나타냅니다.
     * (법선 벡터란, 표면에 수직인 벡터를 의미합니다.)
     * 참고: https://en.wikipedia.org/wiki/Normal_(geometry)
     */
    private final Vector2D normal;

    /**
     * 두 물체가 얼마나 겹쳐 있는지를 나타내는 값입니다.
     * '관통 깊이(penetration)'라고 하며,
     * 두 물체가 충돌했을 때의 겹친 거리(단위: 보통 픽셀 혹은 좌표 단위)입니다.
     * 참고: https://www.gamedev.net/tutorials/programming/math-and-physics/collision-detection-r2237/
     */
    private final double penetration;

    /**
     * 두 물체가 실제로 맞닿은(접촉한) 지점들의 좌표 목록입니다.
     * 여러 점일 수 있으며, 각 점은 2D 벡터(Vector2D)로 표현됩니다.
     * 예: 박스-박스 충돌, 다각형-다각형 충돌 등
     * 참고: https://learnopengl.com/In-Practice/2D-Game/Collisions/Collision-resolution
     */
    private final List<Vector2D> contacts;

    /**
     * Manifold 생성자입니다.
     * 충돌한 두 물체, 충돌 법선, 관통 깊이, 접촉점을 받아서 초기화합니다.
     *
     * @param a           충돌한 첫 번째 물체
     * @param b           충돌한 두 번째 물체
     * @param normal      충돌 표면의 법선 벡터
     * @param penetration 관통 깊이(겹침 정도)
     * @param contacts    접촉점 리스트
     */
    public Manifold(
            final RigidBody a,
            final RigidBody b,
            final Vector2D normal,
            final double penetration,
            final List<Vector2D> contacts
    ) {
        this.a = a;
        this.b = b;
        this.normal = normal;
        this.penetration = penetration;
        this.contacts = contacts;
    }

    /**
     * 첫 번째 물체를 반환합니다.
     */
    public RigidBody getA() {
        return this.a;
    }

    /**
     * 두 번째 물체를 반환합니다.
     */
    public RigidBody getB() {
        return this.b;
    }

    /**
     * 충돌 법선 벡터를 반환합니다.
     */
    public Vector2D getNormal() {
        return this.normal;
    }

    /**
     * 관통 깊이(겹침 정도)를 반환합니다.
     */
    public double getPenetration() {
        return this.penetration;
    }

    /**
     * 접촉점(충돌 위치) 목록을 반환합니다.
     */
    public List<Vector2D> getContacts() {
        return this.contacts;
    }
}
