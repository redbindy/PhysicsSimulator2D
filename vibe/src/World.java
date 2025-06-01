import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * World 클래스는 2D 물리 시뮬레이션의 '전체 환경'을 관리합니다.
 * - 도형(RigidBody) 목록과 벽(경계), 중력, 반발계수(탄성), 마찰계수 등 전체 물리 파라미터 포함
 * - 실시간 물리 연산(update), 도형 추가/삭제 등 기능 제공
 *
 * 참고:
 * - 물리 엔진 구조 개념: https://en.wikipedia.org/wiki/Physics_engine
 * - 반발계수: https://en.wikipedia.org/wiki/Coefficient_of_restitution
 * - 마찰계수: https://en.wikipedia.org/wiki/Friction
 */
public final class World {
    // 내부적으로 사용할 리스트의 초기 용량
    private static final int DEFAULT_CAPACITY = 64;

    private final List<RigidBody> bodies; // 동적으로 움직이는 도형 목록
    private final List<RigidBody> walls;  // 벽(경계) 도형 목록
    private double gravity;      // 중력 가속도(SI 단위 m/s^2에 해당, 시뮬레이션 단위)
    private double restitution;  // 반발계수(탄성 계수, 0~1)
    private double friction;     // 마찰계수(0~1)

    /**
     * 기본 생성자: 초기값 설정
     * - 중력 800.0, 반발계수 0.95, 마찰 0.5로 시작
     */
    public World() {
        this.bodies = new ArrayList<RigidBody>(DEFAULT_CAPACITY);
        this.walls = new ArrayList<RigidBody>(DEFAULT_CAPACITY);
        this.gravity = 800.0;
        this.restitution = 0.95;
        this.friction = 0.5;
    }

    // (getter) 동적 도형 목록 반환
    public List<RigidBody> getBodies() {
        return this.bodies;
    }

    // (getter) 벽(경계) 도형 목록 반환
    public List<RigidBody> getWalls() {
        return this.walls;
    }

    // (getter/setter) 중력 가속도
    public double getGravity() {
        return this.gravity;
    }

    public void setGravity(final double gravity) {
        this.gravity = gravity;
    }

    // (getter/setter) 반발계수(탄성)
    public double getRestitution() {
        return this.restitution;
    }

    public void setRestitution(final double restitution) {
        this.restitution = restitution;
    }

    // (getter/setter) 마찰계수
    public double getFriction() {
        return this.friction;
    }

    public void setFriction(final double friction) {
        this.friction = friction;
    }

    // 동적 도형 추가/삭제/전체삭제
    public void addBody(final RigidBody body) {
        this.bodies.add(body);
    }

    public void removeBody(final RigidBody body) {
        this.bodies.remove(body);
    }

    public void clearBodies() {
        this.bodies.clear();
    }

    // 벽(경계) 추가/전체삭제
    public void addWall(final RigidBody wall) {
        this.walls.add(wall);
    }

    public void clearWalls() {
        this.walls.clear();
    }

    /**
     * 물리 시뮬레이션의 메인 루프(프레임마다 호출)
     * @param dt         시간 간격(초 단위, 예: 0.016은 약 60FPS)
     * @param draggedBody 마우스로 현재 드래그 중인 도형(없으면 null)
     */
    public void update(final double dt, final RigidBody draggedBody) {
        final int sweepSteps = 6;         // 시간 분할 단계(충돌 처리의 안정성을 높임, "서브스테핑" 개념)
        final double sweepDt = dt / sweepSteps;

        for (final RigidBody b : this.bodies) {
            b.setOldPosition(b.getPosition());
        }

        // sweepSteps만큼 반복(시간을 쪼개서 여러 번 계산, 더 안정적)
        for (int step = 0; step < sweepSteps; step++) {
            // --- (1) 모든 동적 도형의 속도/위치/회전 갱신(중력 적용) ---
            for (final RigidBody b : this.bodies) {
                if (b.isStatic() || b == draggedBody) {
                    // 정적인 도형(벽)이나 드래그 중인 도형은 이동/회전 계산 생략
                    continue;
                }

                // 중력은 y방향 속도에만 누적
                b.setVelocity(new Vector2D(
                        b.getVelocity().getX(),
                        b.getVelocity().getY() + this.gravity * sweepDt
                ));
                // 위치 이동(= 현재 속도 × 시간)
                b.setPosition(b.getPosition().add(b.getVelocity().multiply(sweepDt)));
                // 회전각 갱신(= 현재 각속도 × 시간)
                b.setAngle(b.getAngle() + b.getAngularVelocity() * sweepDt);
            }

            // --- (2) 모든 도형끼리, 그리고 벽과의 충돌 감지(Manifold) ---
            final List<Manifold> contacts = new ArrayList<>();
            final int N = this.bodies.size();

            // (2-A) 도형-도형 충돌 판정
            for (int i = 0; i < N; i++) {
                for (int j = i + 1; j < N; j++) {
                    final RigidBody bodyA = this.bodies.get(i);
                    final RigidBody bodyB = this.bodies.get(j);

                    if (bodyA == draggedBody && bodyB == draggedBody) {
                        continue; // 둘 다 드래그 중이면 건너뜀
                    }

                    final Manifold m = ManifoldGenerator.collide(bodyA, bodyB);

                    if (m != null && !m.getContacts().isEmpty()) {
                        contacts.add(m);
                    }
                }
            }

            // (2-B) 도형-벽 충돌 판정
            for (final RigidBody box : this.bodies) {
                for (final RigidBody wall : this.walls) {
                    final Manifold m = ManifoldGenerator.collide(box, wall);

                    if (m != null && !m.getContacts().isEmpty()) {
                        contacts.add(m);
                    }
                }
            }

            // --- (3) 충돌 응답 처리(반발, 마찰, 위치보정) ---
            for (final Manifold m : contacts) {
                for (final Vector2D contact : m.getContacts()) {
                    final RigidBody A = m.getA();
                    final RigidBody B = m.getB();

                    final boolean AisDragged = (A == draggedBody);
                    final boolean BisDragged = (B == draggedBody);

                    // 접점의 위치에서 각각의 상대 위치(벡터)
                    final Vector2D ra = contact.subtract(A.getPosition());
                    final Vector2D rb = contact.subtract(B.getPosition());

                    // 각 도형의 접점 속도 = 선속도 + 각속도에 의한 영향
                    final Vector2D va = A.getVelocity().add(ra.perpendicular().multiply(A.getAngularVelocity()));
                    final Vector2D vb = B.getVelocity().add(rb.perpendicular().multiply(B.getAngularVelocity()));
                    final Vector2D rv = vb.subtract(va);

                    // 충돌 방향(법선)에 대한 상대속도
                    final double velAlongNormal = rv.dot(m.getNormal());

                    if (velAlongNormal > 0) {
                        // 이미 서로 멀어지는 중이면 충돌 처리하지 않음
                        continue;
                    }

                    // 매우 느린 충돌은(거의 붙은 상태) 반발계수 0(즉, 튕기지 않음)
                    final double thisRestitution;
                    if (Math.abs(velAlongNormal) < 0.01) {
                        thisRestitution = 0.0;
                    } else {
                        thisRestitution = this.restitution;
                    }

                    // 회전 관성까지 반영한 질량 역수 계산(병진+회전)
                    final double raCrossN = ra.cross(m.getNormal());
                    final double rbCrossN = rb.cross(m.getNormal());

                    final double invMassA = AisDragged ? 0 : A.getInvMass();
                    final double invMassB = BisDragged ? 0 : B.getInvMass();

                    final double invMassSum = invMassA + invMassB
                            + (raCrossN * raCrossN) * (AisDragged ? 0 : A.getInvInertia())
                            + (rbCrossN * rbCrossN) * (BisDragged ? 0 : B.getInvInertia());

                    if (invMassSum == 0) {
                        continue; // 둘 다 정적이면 무시
                    }

                    // (3-A) 반발(충돌) 임펄스(충돌에 의해 전달되는 순간 힘)
                    // 공식: j = -(1+e)·vN / (역질량합)   (e:반발계수, vN:법선방향 속도)
                    double j = -(1 + thisRestitution) * velAlongNormal / invMassSum;
                    j /= m.getContacts().size(); // 여러 접점일 경우 나눔

                    final Vector2D impulse = m.getNormal().multiply(j);

                    // 충돌 임펄스(힘)만큼 각각 속도/각속도 변화
                    if (!AisDragged && invMassA > 0) {
                        A.setVelocity(A.getVelocity().subtract(impulse.multiply(invMassA)));
                        A.setAngularVelocity(A.getAngularVelocity() - ra.cross(impulse) * A.getInvInertia());
                    }

                    if (!BisDragged && invMassB > 0) {
                        B.setVelocity(B.getVelocity().add(impulse.multiply(invMassB)));
                        B.setAngularVelocity(B.getAngularVelocity() + rb.cross(impulse) * B.getInvInertia());
                    }

                    // (3-B) 마찰력 적용(접선방향 임펄스)
                    final Vector2D tangent = new Vector2D(-m.getNormal().getY(), m.getNormal().getX());

                    final Vector2D va2 = A.getVelocity().add(ra.perpendicular().multiply(A.getAngularVelocity()));
                    final Vector2D vb2 = B.getVelocity().add(rb.perpendicular().multiply(B.getAngularVelocity()));
                    final Vector2D rv2 = vb2.subtract(va2);

                    final double vt = rv2.dot(tangent);
                    final double mu = this.friction;

                    double jt = -vt / invMassSum;
                    jt /= m.getContacts().size();

                    final double maxFriction = mu * Math.abs(j);
                    final double frictionImpulseMag = Math.max(-maxFriction, Math.min(jt, maxFriction));

                    final Vector2D frictionImpulse = tangent.multiply(frictionImpulseMag);
                    if (!AisDragged && invMassA > 0) {
                        A.setVelocity(A.getVelocity().subtract(frictionImpulse.multiply(invMassA)));
                        A.setAngularVelocity(A.getAngularVelocity() - ra.cross(frictionImpulse) * A.getInvInertia());
                    }

                    if (!BisDragged && invMassB > 0) {
                        B.setVelocity(B.getVelocity().add(frictionImpulse.multiply(invMassB)));
                        B.setAngularVelocity(B.getAngularVelocity() + rb.cross(frictionImpulse) * B.getInvInertia());
                    }

                    // (3-C) 위치 관통(겹침) 보정(정확한 물리 시뮬레이션 위해 미세 이동)
                    // 참고: Baumgarte stabilization, position correction
                    final double percent = 0.13;         // 보정 계수(실험적으로 조절)
                    final double slop = 0.5;             // 관통 허용 오차
                    final double maxCorrection = 2.5;    // 최대 보정량
                    final double invMassSum2 = invMassA + invMassB;
                    final double corr = Math.min(Math.max(m.getPenetration() - slop, 0) / (invMassSum2 > 0 ? invMassSum2 : 1.0) * percent, maxCorrection);

                    if (invMassSum2 > 0) {
                        if (!AisDragged && invMassA > 0) {
                            A.setPosition(A.getPosition().subtract(m.getNormal().multiply(corr * invMassA / invMassSum2)));
                        }

                        if (!BisDragged && invMassB > 0) {
                            B.setPosition(B.getPosition().add(m.getNormal().multiply(corr * invMassB / invMassSum2)));
                        }
                    }
                }
            }
        }

        // --- (4) 아주 작은 속도/각속도는 0으로 보정(시뮬레이션 정지)
        for (final RigidBody b : this.bodies) {
            if (b.isStatic() || b == draggedBody) {
                continue;
            }

            if (Math.abs(b.getVelocity().getX()) < 1.0) {
                b.setVelocity(new Vector2D(0, b.getVelocity().getY()));
            }

            if (Math.abs(b.getVelocity().getY()) < 1.0) {
                b.setVelocity(new Vector2D(b.getVelocity().getX(), 0));
            }

            if (Math.abs(b.getAngularVelocity()) < 0.02) {
                b.setAngularVelocity(0);
            }
        }
    }
}
